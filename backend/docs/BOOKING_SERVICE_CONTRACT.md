# TravelEasy: Bus Service to Booking Service Integration Contract

**Document Version**: 1.0.0  
**Target Module**: `booking-management-service`  
**Provider Service**: `bus-service` (Port `8082` default, context path `/api/v1/integration/booking`)  
**Architecture Style**: RESTful JSON over HTTP + Transactional DB Concurrency Guards  

---

## 1. Overview & Responsibilities

The **Bus Microservice** owns the physical bus topology, routes, scheduled trips, and real-time seat inventory. The **Booking Management Service** orchestrates traveller bookings, passenger manifests, payment callbacks, and itinerary ticketing.

This document serves as the immutable interface agreement between the two services.

```
+--------------------------+                 +--------------------------+
| Booking Management Svc   |                 | Bus Microservice         |
|                          |                 |                          |
|  1. Hold Request         | --------------> |  Pessimistic DB Lock     |
|     (Hold for 10 min)    | <-------------- |  Lock Token + Pricing    |
|                          |                 |                          |
|  2. Payment Confirmed    | --------------> |  Transition to BOOKED    |
|     (Confirm Request)    | <-------------- |  Booking Ref Linked      |
|                          |                 |                          |
|  3. Cancellation/Refund  | --------------> |  Restore to AVAILABLE    |
|     (Cancel Request)     | <-------------- |  Inventory Released      |
+--------------------------+                 +--------------------------+
```

---

## 2. API Endpoints Reference

Base Path: `/api/v1/integration/booking`

### 2.1 Hold Seats (Pre-Payment Reservation)
- **Method**: `POST`
- **Path**: `/api/v1/integration/booking/hold`
- **Description**: Atomically acquires a DB row-level lock (`SELECT ... FOR UPDATE`) on requested seat numbers for a schedule. Locks remain valid for 10 minutes (TTL).
- **Request Body**:
```json
{
  "scheduleId": "c2b64d04-1234-4b55-8901-abcdef123456",
  "seatNumbers": ["1A", "1B"],
  "userId": "9a123456-7890-4123-8abc-1234567890ab",
  "holdDurationMinutes": 10
}
```
- **Response Body (`200 OK`)**:
```json
{
  "success": true,
  "message": "Seats placed on hold successfully",
  "data": {
    "lockToken": "HOLD-3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "scheduleId": "c2b64d04-1234-4b55-8901-abcdef123456",
    "lockedSeats": ["1A", "1B"],
    "lockExpiryTime": "2026-10-15T08:10:00",
    "seatPrices": [
      {
        "seatNumber": "1A",
        "seatType": "SEATER",
        "baseAmount": 500.00,
        "taxAmount": 25.00,
        "totalAmount": 525.00
      },
      {
        "seatNumber": "1B",
        "seatType": "SEATER",
        "baseAmount": 500.00,
        "taxAmount": 25.00,
        "totalAmount": 525.00
      }
    ],
    "totalHoldAmount": 1050.00,
    "currency": "INR"
  },
  "timestamp": "2026-10-15T08:00:00Z"
}
```
- **Error Codes**:
  - `404 NOT_FOUND` (`BUS_NOT_FOUND`): Schedule or seat not found.
  - `409 CONFLICT` (`BUS_SEAT_ALREADY_BOOKED` / `BUS_SEAT_ALREADY_LOCKED`): One or more seats are already taken by another traveller.

---

### 2.2 Confirm Booking (Post-Payment Transition)
- **Method**: `POST`
- **Path**: `/api/v1/integration/booking/confirm`
- **Description**: Transitions seats from `LOCKED` to `BOOKED` and permanently attaches the `bookingReference`. This operation is **idempotent** (re-invocations with the same bookingReference succeed safely).
- **Request Body**:
```json
{
  "scheduleId": "c2b64d04-1234-4b55-8901-abcdef123456",
  "bookingReference": "BKG-2026-987654",
  "lockToken": "HOLD-3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "seatNumbers": ["1A", "1B"],
  "passengers": [
    {
      "seatNumber": "1A",
      "passengerName": "John Doe",
      "passengerAge": 32,
      "passengerGender": "MALE"
    },
    {
      "seatNumber": "1B",
      "passengerName": "Jane Doe",
      "passengerAge": 29,
      "passengerGender": "FEMALE"
    }
  ]
}
```
- **Response Body (`200 OK`)**:
```json
{
  "success": true,
  "message": "Seats confirmed and booked successfully",
  "data": {
    "confirmed": true,
    "bookingReference": "BKG-2026-987654",
    "scheduleId": "c2b64d04-1234-4b55-8901-abcdef123456",
    "confirmedSeats": ["1A", "1B"],
    "totalAmount": 1050.00,
    "currency": "INR",
    "departureTime": "2026-10-15T08:00:00",
    "busRegistrationNumber": "DL-01-AB-1234",
    "origin": "Delhi",
    "destination": "Jaipur",
    "confirmedAt": "2026-10-15T08:02:15"
  },
  "timestamp": "2026-10-15T08:02:15Z"
}
```

---

### 2.3 Cancel Booking / Release Seats
- **Method**: `POST`
- **Path**: `/api/v1/integration/booking/cancel`
- **Description**: Releases previously booked seats back to `AVAILABLE` status for other passengers to book.
- **Request Body**:
```json
{
  "scheduleId": "c2b64d04-1234-4b55-8901-abcdef123456",
  "bookingReference": "BKG-2026-987654",
  "seatNumbers": ["1A"],
  "cancellationReason": "Passenger requested cancellation"
}
```
- **Response Body (`200 OK`)**:
```json
{
  "success": true,
  "message": "Booking cancelled and seats released",
  "data": {
    "cancelled": true,
    "bookingReference": "BKG-2026-987654",
    "scheduleId": "c2b64d04-1234-4b55-8901-abcdef123456",
    "releasedSeats": ["1A"],
    "refundEligible": true,
    "message": "Booking cancelled successfully and seats released",
    "cancelledAt": "2026-10-15T12:00:00"
  },
  "timestamp": "2026-10-15T12:00:00Z"
}
```

---

### 2.4 Query Booking Allocation Status
- **Method**: `GET`
- **Path**: `/api/v1/integration/booking/status/{bookingReference}`
- **Description**: Returns live allocation status of all seats linked to a specific booking reference.

---

## 3. Concurrency & Reliability Invariants

1. **Lock Expiration (TTL)**:
   - Holds are strictly valid for `10 minutes`.
   - A scheduled background job sweeps expired locks every minute.
2. **Pessimistic Row-Level Lock**:
   - `SELECT ... FOR UPDATE` ensures two concurrent requests attempting to hold the same seat will execute serially; the second request will immediately receive `409 Conflict`.
3. **Zero DB Sharing**:
   - The Booking Service must NEVER directly query or write to the Bus Database (`bus_db`). All interactions must go via these published HTTP integration endpoints.
