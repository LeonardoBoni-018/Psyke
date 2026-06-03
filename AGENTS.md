# Psyke Platform — Project Context

## Stack
- Java 17 · Spring Boot 4.0.6 · Hibernate ORM 7.2.12.Final · Tomcat 11 · PostgreSQL 16
- Apache Kafka 4.1 (KRaft mode) · Flyway · Spring Security JWT · SpringDoc OpenAPI
- Lombok · HikariCP · Maven Wrapper · OpenPDF 1.3.43 · Twilio SDK 10.6.10

## Architecture
- **Multi-tenant** via schema-per-tenant: `tenant_<slug>` schemas
- **Master schema** (`public`): tenants, users, roles, refresh_tokens
- **Tenant schema** (`tenant_*`): appointments, patients, slots, availability, rooms, waiting_list, cancellation_logs, recurrence_rules, invoices, prescriptions, consent_terms, prontuarios, questionnaires, documents, treatment_plans, notification_configs, audit_logs
- **TenantAwareDataSource** (AbstractRoutingDataSource) routes by `TenantContext` (ThreadLocal)
- **JWT** carries tenant_id, clinic_id, user_id, roles as custom claims
- **Patient Portal** reads user UUID from `Authentication.getPrincipal()` and resolves patient via email match
- **Notifications** via Kafka consumers: `AppointmentNotifier` (SMS + Email), `AppointmentCancelledConsumer` (waiting list), `SessionEventConsumer` (auto-evolution)
- **PDF Export** uses OpenPDF (MIT license, no commercial restrictions)

## Key Conventions
- All tables/columns in **English**
- All DTOs in `/dto/` as Java records
- All models in `/model/` with Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`
- All services annotated `@Service @RequiredArgsConstructor`
- All controllers annotated `@RestController @RequestMapping @RequiredArgsConstructor`
- Exceptions in `/exception/` extend `RuntimeException`, handled in `GlobalExceptionHandler`
- Kafka events in `/event/` as Java records, producers in same package
- OpenAPI `@Schema` on all DTO request/response records
- Tests: Mockito `@ExtendWith(MockitoExtension.class)` for unit; `@SpringBootTest` for integration
- `@DynamicPropertySource` overrides `spring.datasource.password` to `123456` in tests
- Flyway migrations in `src/main/resources/db/migration/tenant/` for tenant schemas

## Package Structure
```
com.br.psyke.psyke
  ├── ai/             — AI features (OpenAI integration)
  ├── config/         — Security, Flyway, Kafka, OpenAPI configs
  ├── controller/     — REST controllers (26 controllers)
  │   └── prontuario/ — Prontuario sub-controller
  ├── dto/            — Request/response records
  ├── event/          — Kafka event records, producers, consumers
  ├── exception/      — Custom exceptions + global handler
  ├── model/          — JPA entities
  ├── repository/     — Spring Data JPA repositories
  ├── security/       — JWT, TenantContext, TenantAwareDataSource
  └── service/        — Business logic (26 services)
      └── prontuario/ — ProntuarioService
```

## Modules / Endpoints by Path

| Path | Module |
|------|--------|
| `/auth` | Authentication (login, refresh, register, me) |
| `/tenants` | Tenant CRUD |
| `/clinics` | Clinic CRUD |
| `/users` | User CRUD |
| `/patients` | Patient CRUD |
| `/professionals` | Professional CRUD |
| `/appointments` | Appointment CRUD + status machine |
| `/appointments/recurring` | Recurrence batch creation |
| `/rooms` | Room CRUD |
| `/professionals/{id}/availability` | Availability CRUD |
| `/invoices` | Invoice CRUD |
| `/prescriptions` | Prescription CRUD |
| `/consent-terms` | Consent Term CRUD + signature |
| `/prontuarios` | Prontuario (medical records) |
| `/questionnaires` | Questionnaire CRUD |
| `/questionnaire-assignments` | Assignment CRUD |
| `/documents` | Document upload/download |
| `/treatment-plans` | Treatment Plan CRUD |
| `/notification-configs` | Per-user notification preferences |
| `/dashboard` | Dashboard statistics |
| `/reports` | CSV export (appointments, invoices) |
| `/waiting-list` | Waiting list management |
| `/audit-logs` | Audit log query |
| `/portal` | Patient Portal (appointments, prescriptions, invoices) |
| `/export/pdf` | PDF Export (appointments, invoices, prescriptions, consent terms) |
| `/confirm/**`, `/cancel/**` | Public appointment confirmation/cancellation |

## State Machine (Appointment)
```
SCHEDULED ↔ CONFIRMED ↔ DONE
    ↕          ↕
CANCELLED   NO_SHOW
```
Transitions enforced by `AppointmentService.TRANSITIONS` map.

## Kafka Topics
- `appointment.scheduled` → `AppointmentNotifier` (SMS + Email to patient)
- `appointment.confirmed` → `AppointmentNotifier`
- `appointment.cancelled` → `AppointmentNotifier` + `AppointmentCancelledConsumer` (waiting list offer)
- `appointment.session.created` → `SessionEventConsumer` (auto-evolution)

## Infrastructure (Docker)
- PostgreSQL on `localhost:5432`, DB `psyke_master`, user `postgres`, password `123456`
- Kafka on `localhost:9092`, KRaft mode (no Zookeeper)
- Start: `docker compose up -d`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Common Commands
```
.\mvnw.cmd test                          # Run all tests (199 tests)
.\mvnw.cmd clean package -DskipTests     # Build JAR
.\mvnw.cmd spring-boot:run               # Start app (local, without Docker)
.\mvnw.cmd test -Dtest=UserServiceTest   # Single test class

# Docker Dev
docker compose up -d                     # Start full stack (DB + Kafka + API)
docker compose up -d psyke-api           # Start only API (keep DB + Kafka running)
docker compose logs -f psyke-api         # Follow API logs
docker compose down                      # Stop everything
```

## Docker Dev Workflow
1. First build: `docker compose build psyke-api` (~5 min to download deps)
2. Start: `docker compose up -d`
3. API at `http://localhost:8080` — hot reload via DevTools (saved files recompile → restart)
4. `docker compose down` to stop everything
- DevTools watches `/app/src` (mounted from `./src`). Changes to any `.java` file trigger automatic restart when the IDE recompiles.
- VS Code + Java Extension Pack compiles on save automatically.

## Test Credentials
After creating a tenant and logging in:
- JWT token goes in `Authorization: Bearer <token>` header
- Public endpoints (no auth): `/auth/login`, `/auth/refresh`, `POST /tenants`, `/confirm/**`, `/cancel/**`

## Env Variables for New Features
| Variable | Default | Description |
|----------|---------|-------------|
| `TWILIO_ACCOUNT_SID` | *(empty)* | Twilio SID (empty = SMS fallback to logging) |
| `TWILIO_AUTH_TOKEN` | *(empty)* | Twilio auth token |
| `TWILIO_FROM_NUMBER` | *(empty)* | Twilio sender number |
