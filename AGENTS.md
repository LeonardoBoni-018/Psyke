# Psyke Platform — Project Context

## Stack
- Java 17 · Spring Boot 4.0.6 · Hibernate ORM 7.2.12.Final · Tomcat 11 · PostgreSQL 16
- Apache Kafka 4.1 (KRaft mode) · Flyway · Spring Security JWT · SpringDoc OpenAPI
- Lombok · HikariCP · Maven Wrapper

## Architecture
- **Multi-tenant** via schema-per-tenant: `tenant_<slug>` schemas
- **Master schema** (`public`): tenants, users, roles, refresh_tokens
- **Tenant schema** (`tenant_*`): appointments, slots, availability, waiting_list, cancellation_logs, recurrence_rules, rooms
- **TenantAwareDataSource** (AbstractRoutingDataSource) routes by `TenantContext` (ThreadLocal)
- **JWT** carries tenant_id, clinic_id, user_id, roles as custom claims

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

## Package Structure
```
com.br.psyke.psyke
  ├── ai/          — AI features (OpenAI integration)
  ├── config/      — Security, Flyway, Kafka, OpenAPI configs
  ├── controller/  — REST controllers
  ├── dto/         — Request/response records
  ├── event/       — Kafka event records + producers
  ├── exception/   — Custom exceptions + global handler
  ├── model/       — JPA entities
  ├── repository/  — Spring Data JPA repositories
  ├── security/    — JWT, TenantContext, TenantAwareDataSource
  └── service/     — Business logic
```

## State Machine (Appointment)
```
SCHEDULED ↔ CONFIRMED ↔ DONE
    ↕          ↕
CANCELLED   NO_SHOW
```
Transitions enforced by `AppointmentService.TRANSITIONS` map.

## Infrastructure (Docker)
- PostgreSQL on `localhost:5432`, DB `psyke_master`, user `postgres`, password `123456`
- Kafka on `localhost:9092`, topics: appointment.scheduled, .confirmed, .cancelled, .session.created, waiting-list.offer
- Start: `docker compose up -d`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Common Commands
```
.\mvnw.cmd test                        # Run all tests
.\mvnw.cmd clean package -DskipTests   # Build JAR
.\mvnw.cmd spring-boot:run             # Start app
.\mvnw.cmd test -Dtest=UserServiceTest  # Single test class
```

## Test Credentials
After creating a tenant and logging in:
- JWT token goes in `Authorization: Bearer <token>` header
- Public endpoints: `/auth/login`, `/auth/refresh`, `POST /tenants`, `/confirm/**`, `/cancel/**`
