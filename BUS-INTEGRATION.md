# TravelEasy Bus Microservice — Teammate Integration Guide

This guide describes how external microservices (**Booking Service**, **Auth Service**, **Payment Service**, **Notification Service**) integrate with the Bus Microservice.

---

## 1. Architecture & Database Isolation
- The Bus Microservice owns its independent PostgreSQL database (`bus_db`).
- **No direct database sharing is permitted.**
- All communication must occur over REST APIs (`/api/v1`) or asynchronous domain events.

---

## 2. Central Booking Service Integration Flow

```
[Central Booking Service]                       [Bus Microservice]
         |                                              |
         | ---- 1. POST /api/v1/integration/booking/hold ----> (Pessimistic DB lock, 10m TTL)
         | <--- Returns lockToken, seat prices, total ---|
         |                                              |
     [Payment]                                          |
         | ---- 2. POST /api/v1/integration/booking/confirm -> (Status -> BOOKED, Outbox event)
         | <--- Returns confirmation & booking details -|
         |                                              |
  (If cancelled):                                       |
         | ---- 3. POST /api/v1/integration/booking/cancel --> (Status -> AVAILABLE, Outbox event)
         | <--- Returns cancellation confirmation ------|
```

### Request & Response Examples:

#### Step 1: Hold Seats
- `POST /api/v1/integration/booking/hold`
```json
{
  "scheduleId": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
  "seatNumbers": ["1A", "1B"],
  "holdDurationMinutes": 10
}
```
Response:
```json
{
  "success": true,
  "data": {
    "lockToken": "HOLD-7f8e9d0a-1b2c-3d4e-5f6a-7b8c9d0e1f2a",
    "scheduleId": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
    "lockedSeats": ["1A", "1B"],
    "lockExpiryTime": "2026-10-15T14:40:00",
    "totalHoldAmount": 1100.0,
    "currency": "INR"
  }
}
```

#### Step 2: Confirm Booking
- `POST /api/v1/integration/booking/confirm`
```json
{
  "scheduleId": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
  "seatNumbers": ["1A", "1B"],
  "lockToken": "HOLD-7f8e9d0a-1b2c-3d4e-5f6a-7b8c9d0e1f2a",
  "bookingReference": "BKG-2026-10001"
}
```

#### Step 3: Cancel Booking
- `POST /api/v1/integration/booking/cancel`
```json
{
  "scheduleId": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
  "bookingReference": "BKG-2026-10001",
  "cancellationReason": "Customer requested cancellation"
}
```

---

## 3. Asynchronous Outbox Events
When seats or schedules change state, domain events are written transactionally to `bus_events_outbox`:
- `SEAT_LOCKED`: Seat hold acquired
- `SEAT_BOOKED`: Seat reservation confirmed
- `SEAT_RELEASED`: Seat booking cancelled or lock expired
- `SCHEDULE_CREATED`: New schedule published
- `SCHEDULE_CANCELLED`: Trip cancelled by operator

Message Broker Topic: `traveleasy.bus.events`
