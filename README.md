# TravelEasy Bus Microservice (`bus-service`)

Production-ready, test-driven **Bus Microservice** and **Angular 20 Frontend** for the TravelEasy platform.

---

## 1. Key Architectural Highlights
- **Independent Database**: Autonomous PostgreSQL database (`bus_db`), Flyway schema migrations (`V1__init_schema.sql`, `V2__create_bus_domain_schema.sql`).
- **High Concurrency Seat Locking**: Pessimistic row-level write locks (`SELECT ... FOR UPDATE`), 10-minute configurable TTL hold tokens, and automatic lock cleanup scheduler.
- **Transactional Domain Event Outbox**: Persists events (`SEAT_LOCKED`, `SEAT_BOOKED`, `SEAT_RELEASED`, `SCHEDULE_CREATED`, `SCHEDULE_CANCELLED`) inside the domain transaction, dispatched via background scheduler with exponential retry.
- **Central Booking Service Contract**: Clear integration endpoints (`/api/v1/integration/booking/**`) for hold, confirm, cancel, and status queries.
- **Security & Multi-Tenancy**: Stateless JWT authentication (`sub`, `roles`, `operatorId`), operator ownership isolation on all fleet/route/schedule endpoints.
- **Full-Featured Angular UI**:
  - **Traveller Module**: Search, dynamic filters, interactive dual-deck seat map with real-time hold timers.
  - **Operator Module**: Analytics dashboard, fleet CRUD, route & stop management, schedule creation wizard, live passenger manifest roster, and seat block/unblock controls.

---

## 2. Directory Layout

```text
bus-service/
├── backend/                  # Java 21 / Spring Boot 3.3.4 Microservice
│   ├── src/main/java/com/traveleasy/bus/
│   │   ├── config/           # Security, OpenAPI, Jackson
│   │   ├── controller/       # REST Controllers (/api/v1)
│   │   ├── dto/              # Request, Response & Integration DTOs
│   │   ├── entity/           # JPA Domain Entities
│   │   ├── enums/            # Domain & Lifecycle Enums
│   │   ├── event/            # Domain Events & Outbox Publisher
│   │   ├── exception/        # Global Exception Handler
│   │   ├── integration/      # Central Booking Service Adapter
│   │   ├── repository/       # Spring Data JPA Repositories
│   │   ├── security/         # JWT Provider, Filter & SecurityUtils
│   │   └── service/          # Domain Services & Schedulers
│   ├── src/test/java/        # 114 Unit, Integration & E2E Tests (100% Pass)
│   ├── Dockerfile
│   └── pom.xml
├── frontend/                 # Angular 20 Standalone UI
│   ├── src/app/
│   │   ├── core/             # BusApiService, AuthService, AuthInterceptor
│   │   ├── models/           # TypeScript Domain Interfaces
│   │   ├── shared/           # SeatMapComponent (Dual Deck Interactive)
│   │   ├── traveller/        # BusSearchComponent, ScheduleDetailsComponent
│   │   └── operator/         # OperatorDashboard, BusFleet, Schedules, Manifest
│   ├── Dockerfile
│   ├── nginx.conf
│   └── package.json
├── docker-compose.yml        # Multi-Container Compose (DB + Backend + Frontend)
├── API-DOCUMENTATION.md      # REST API Specification
├── BUS-INTEGRATION.md        # Central Booking & External Service Handbook
├── SRS-TRACEABILITY.md       # SRS Requirements Matrix
└── README.md
```

---

## 3. Quick Start & Execution

### Running Tests (Backend)
```bash
cd backend
mvn clean test
```
*Result: 114 tests passing across unit, HTTP MockMvc integration, and full E2E lifecycle.*

### Running Frontend (Angular)
```bash
cd frontend
npm install
npm run build
npm start
```
*Frontend runs at `http://localhost:4200`.*

### Docker Compose (Full Stack)
```bash
docker-compose up --build -d
```
- **PostgreSQL Database (`bus_db`)**: `localhost:5432`
- **Bus Backend Microservice**: `http://localhost:8081` (Swagger UI: `http://localhost:8081/swagger-ui.html`)
- **Bus Frontend UI**: `http://localhost:4200`
