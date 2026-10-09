# TravelEasy Bus Microservice — SRS Requirements Traceability Matrix

| Req ID | Requirement Description | Implementation Components | Automated Verification Tests | Status |
|---|---|---|---|---|
| **REQ-BUS-01** | Stateless JWT Authentication & Operator Isolation | `JwtTokenProvider.java`, `SecurityConfig.java`, `SecurityUtils.java` | `JwtTokenProviderTest.java`, `BusControllerIntegrationTest.java` | ✅ VERIFIED |
| **REQ-BUS-02** | Independent Database Schema (13 tables, Flyway) | `V1__init_schema.sql`, `V2__create_bus_domain_schema.sql`, JPA Entities | Flyway integration tests | ✅ VERIFIED |
| **REQ-BUS-03** | Bus Fleet CRUD & Validation | `BusController.java`, `BusServiceImpl.java`, `BusRepository.java` | `BusServiceTest.java`, `BusControllerIntegrationTest.java` | ✅ VERIFIED |
| **REQ-BUS-04** | Route & Boarding/Dropping Point Management | `RouteController.java`, `RouteServiceImpl.java`, `RouteRepository.java` | `RouteServiceTest.java`, `RouteControllerIntegrationTest.java` | ✅ VERIFIED |
| **REQ-BUS-05** | Schedule Management & Automatic Seat Inventory | `ScheduleController.java`, `ScheduleServiceImpl.java`, `ScheduleRepository.java` | `ScheduleServiceTest.java`, `ScheduleControllerIntegrationTest.java` | ✅ VERIFIED |
| **REQ-BUS-06** | Physical Seat Map & Lower/Upper Deck Layout | `SeatController.java`, `SeatServiceImpl.java`, `SeatRepository.java` | `SeatServiceTest.java`, `SeatControllerIntegrationTest.java` | ✅ VERIFIED |
| **REQ-BUS-07** | Concurrency Seat Locking (`SELECT FOR UPDATE`, TTL) | `SeatLockServiceImpl.java`, `ScheduleSeatInventoryRepository.java` | `SeatLockServiceTest.java`, `SeatLockControllerIntegrationTest.java` | ✅ VERIFIED |
| **REQ-BUS-08** | Fare Management & Price Calculation | `FareController.java`, `FareServiceImpl.java`, `FareRepository.java` | `FareServiceTest.java`, `FareControllerIntegrationTest.java` | ✅ VERIFIED |
| **REQ-BUS-09** | Schedule Availability & Lifecycle Management | `ScheduleAvailabilityController.java`, `ScheduleAvailabilityServiceImpl.java` | `ScheduleAvailabilityServiceTest.java`, `ScheduleAvailabilityIntegrationTest.java` | ✅ VERIFIED |
| **REQ-BUS-10** | Multi-Criteria Bus Domain Search Query Engine | `BusSearchController.java`, `BusSearchServiceImpl.java` | `BusSearchServiceTest.java`, `BusSearchControllerIntegrationTest.java` | ✅ VERIFIED |
| **REQ-BUS-11** | Bus Details & Traveller Schedule View | `ScheduleController.java` (`/schedules/{id}/details`), `ScheduleServiceImpl.java` | `ScheduleServiceTest.java`, `ScheduleControllerIntegrationTest.java` | ✅ VERIFIED |
| **REQ-BUS-12** | Operator Dashboard & Fleet KPIs | `OperatorDashboardController.java`, `OperatorDashboardServiceImpl.java` | `OperatorDashboardServiceTest.java`, `OperatorDashboardControllerIntegrationTest.java` | ✅ VERIFIED |
| **REQ-BUS-13** | Central Booking Service Integration Contract | `BusBookingIntegrationController.java`, `BusBookingIntegrationServiceImpl.java` | `BusBookingIntegrationServiceTest.java`, `BusBookingIntegrationControllerIntegrationTest.java` | ✅ VERIFIED |
| **REQ-BUS-14** | Transactional Event Outbox & Domain Event Publisher | `BusEventOutbox.java`, `EventOutboxServiceImpl.java`, `OutboxEventPublisherScheduler.java` | `EventOutboxServiceTest.java`, `OutboxEventPublisherSchedulerTest.java` | ✅ VERIFIED |
| **REQ-BUS-15** | Operator Manifest & Live Booking Feed | `OperatorDashboardController.java` (`/manifest`, `/feed/live-bookings`) | `OperatorDashboardServiceTest.java`, `OperatorDashboardControllerIntegrationTest.java` | ✅ VERIFIED |
| **REQ-BUS-16** | End-to-End Lifecycle Verification | Complete workflow across Bus & Central Booking | `BusMicroserviceE2EIntegrationTest.java` (All 11 steps passing) | ✅ VERIFIED |
| **REQ-BUS-17** | Angular Bus Traveller UI Module | `bus-search.component`, `schedule-details.component`, `seat-map.component` | Angular production build verified | ✅ VERIFIED |
| **REQ-BUS-18** | Angular Bus Operator UI Module | `operator-dashboard.component`, `bus-fleet.component`, `operator-schedules.component`, `passenger-manifest.component` | Angular production build verified | ✅ VERIFIED |
| **REQ-BUS-19** | Dockerization & Multi-Container Compose | `Dockerfile` (backend & frontend), `docker-compose.yml`, `nginx.conf` | Docker compose config verified | ✅ VERIFIED |
