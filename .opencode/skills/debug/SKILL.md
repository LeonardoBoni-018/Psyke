---
name: debug
description: Use when the user reports a runtime error, compilation failure, test failure, or unexpected behavior. Also use for Hibernate, Kafka, or multi-tenant datasource issues.
---

# Debug — Psyke Platform

## Always check first
1. **Docker status**: `docker ps` — PostgreSQL and Kafka must be running
2. **Port conflicts**: `netstat -ano | findstr :8080` and `:5432`
3. **Latest logs**: `cat target/app.log` or `docker logs postgres-db`
4. **Test output**: `.\mvnw.cmd test` — isolate failing test with `-Dtest=ClassName`

## Common issues

### 403 Forbidden on Swagger
- Token missing in `Authorization` header → click **Authorize** in Swagger UI
- Token expired → refresh via `POST /auth/refresh`

### "Cannot find symbol: class LocalDate"
- Missing `import java.time.LocalDate;` in the service class

### Hibernate 7.x parameter type inference
- Native queries with `IS NULL OR col = :param` fail → use native SQL with `CAST(... AS type)`
- See `AppointmentRepository.findAllByFilters()`

### Two KafkaTemplate beans conflict
- Use `@Qualifier("appointmentKafkaTemplate")` and `@Qualifier("sessionKafkaTemplate")`
- In tests: `@MockitoBean(name = "appointmentKafkaTemplate")`

### Port 5432 in use
- Native PostgreSQL service may be running: `net stop PostgreSQL` (Admin)
- Or check: `Get-Service PostgreSQL`

### Tenant datasource not found
- Tenant schema must exist (created by Flyway in `AuthService.createTenant()`)
- Check `tenant_*` schemas in PostgreSQL: `\dn` in psql
