# TravelEasy Bus Microservice — REST API Specification

**Version**: `v1.0`  
**Base URL**: `http://localhost:8081/api/v1`  
**Authentication**: Stateless JWT Bearer Token (`Authorization: Bearer <token>`)  
**Format**: `application/json`

---

## 1. Authentication & Security
- Public Endpoints (`/buses/search`, `/schedules/{id}/details`, `/schedules/{id}/seat-map`, `/integration/booking/**`) do not require authentication tokens.
- Operator Endpoints require `ROLE_OPERATOR` or `ROLE_ADMIN` and validate `operatorId` claims in JWT tokens to guarantee isolation and resource ownership.

---

## 2. API Endpoints Catalog

### A. Bus Search & Discovery (Public)
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/buses/search` | Search available bus schedules by origin, destination, date, bus type, price range |
| `GET` | `/schedules/{scheduleId}/details` | Get full traveller schedule view with live seat map, boarding/dropping points & fares |
| `GET` | `/schedules/{scheduleId}/seat-map` | Get interactive lower & upper deck physical seat layout and live booking status |

---

### B. Seat Concurrency Locking (Traveller / Integration)
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/schedules/{scheduleId}/seats/lock` | Acquire pessimistic write lock (`SELECT ... FOR UPDATE`) on requested seats with TTL |
| `POST` | `/schedules/{scheduleId}/seats/release` | Explicitly release locked seats before expiry |

---

### C. Booking Service Integration Contract (`/integration/booking`)
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/integration/booking/hold` | Hold seat inventory for Central Booking checkout (`HOLD-<UUID>` token, TTL) |
| `POST` | `/integration/booking/confirm` | Confirm held seats with booking reference; emit `SEAT_BOOKED` event to outbox |
| `POST` | `/integration/booking/cancel` | Cancel booking reference; release seats back to `AVAILABLE` & emit `SEAT_RELEASED` |
| `GET` | `/integration/booking/{ref}/status`| Query booking reference status and confirmed seat allocations |

---

### D. Bus Fleet Management (`/buses`, Operator Protected)
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/buses` | Register a new bus in operator's fleet |
| `GET` | `/buses` | List operator's buses (paginated) |
| `GET` | `/buses/{busId}` | Get bus details by ID |
| `PUT` | `/buses/{busId}` | Update bus properties (registration, amenities, type) |
| `DELETE` | `/buses/{busId}` | Remove bus from fleet |
| `POST` | `/buses/{busId}/seats` | Configure physical seats layout (rows, cols, deck, type) |
| `GET` | `/buses/{busId}/seats` | List physical seats on a bus |

---

### E. Routes & Stops Management (`/routes`, Operator Protected)
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/routes` | Create route with origin, destination, distance, estimated duration |
| `GET` | `/routes` | List operator's routes (paginated) |
| `GET` | `/routes/{routeId}` | Get route details with stops and boarding/dropping points |
| `PUT` | `/routes/{routeId}` | Update route details |
| `DELETE` | `/routes/{routeId}` | Delete route |
| `POST` | `/routes/{routeId}/stops` | Add intermediate stop |
| `POST` | `/routes/{routeId}/boarding-points` | Add passenger boarding point |
| `POST` | `/routes/{routeId}/dropping-points` | Add passenger dropping point |

---

### F. Schedules Management (`/schedules`, Operator Protected)
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/schedules` | Create schedule (automatically clones physical seats into `schedule_seat_inventory`) |
| `GET` | `/schedules` | List operator schedules with filter criteria |
| `GET` | `/schedules/{scheduleId}` | Get schedule details |
| `PUT` | `/schedules/{scheduleId}` | Update schedule date and timing |
| `PATCH` | `/schedules/{scheduleId}/cancel` | Cancel schedule and notify travellers |
| `PATCH` | `/schedules/{scheduleId}/status` | Update trip status (`SCHEDULED` &rarr; `BOARDING` &rarr; `DEPARTED` &rarr; `COMPLETED`) |
| `PATCH` | `/schedules/{scheduleId}/seats/{seatNumber}/block` | Operator block/unblock seat reservation |

---

### G. Operator Dashboard & Analytics (`/operator/dashboard`, Operator Protected)
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/operator/dashboard/summary` | Aggregate KPIs: fleet buses, routes, schedules, occupancy %, gross revenue |
| `GET` | `/operator/dashboard/revenue` | Route-by-route revenue and seat sales breakdown |
| `GET` | `/operator/dashboard/occupancy` | Trip occupancy rates and seat fill metrics |
| `GET` | `/operator/dashboard/schedules/{id}/manifest` | Real-time passenger manifest and seat roster |
| `GET` | `/operator/dashboard/feed/live-bookings` | Real-time booking and lock activity feed |
