# 🧠 Psyke Platform

Plataforma de gestão para clínicas de psicologia — multi-tenant, com agendamento, prontuário, receitas, faturas, notificações e portal do paciente.

## Stack

| Camada | Tecnologia |
|--------|-----------|
| **Linguagem** | Java 17 |
| **Framework** | Spring Boot 4.0.6 |
| **ORM** | Hibernate ORM 7.2.12.Final |
| **Database** | PostgreSQL 16 |
| **Migrações** | Flyway |
| **Auth** | Spring Security + JWT (jjwt 0.12.6) |
| **Mensageria** | Apache Kafka 4.1 (KRaft mode) |
| **Documentação** | SpringDoc OpenAPI 3.0.2 |
| **Build** | Maven Wrapper |
| **Utils** | Lombok, HikariCP |
| **PDF** | OpenPDF 1.3.43 |
| **SMS** | Twilio SDK 10.6.10 |

## Arquitetura

- **Multi-tenant** via schema-per-tenant (`tenant_<slug>`)
- **Master schema** (`public`): tenants, users, roles, refresh_tokens
- **Tenant schema** (`tenant_*`): appointments, patients, slots, availability, rooms, invoices, prescriptions, consent_terms, prontuarios, questionnaires, documents, waiting_list, cancellation_logs, recurrence_rules
- **TenantAwareDataSource** (`AbstractRoutingDataSource`) roteia por `TenantContext` (ThreadLocal)
- **JWT** carrega `tenant_id`, `clinic_id`, `user_id`, `roles` como custom claims

## Funcionalidades

| Módulo | Endpoints |
|--------|-----------|
| **Auth** | login, refresh, register, me |
| **Tenants** | CRUD |
| **Clínicas** | CRUD |
| **Usuários** | CRUD |
| **Pacientes** | CRUD |
| **Profissionais** | CRUD |
| **Agendamentos** | CRUD, calendar, status machine, sessões |
| **Salas** | CRUD |
| **Disponibilidade** | CRUD por profissional |
| **Recorrência** | criação em lote de agendamentos |
| **Prontuário** | CRUD por paciente |
| **Evolução Clínica** | CRUD automática via Kafka |
| **Receitas** | CRUD |
| **Termos de Consentimento** | CRUD + assinatura |
| **Faturas** | CRUD |
| **Questionários** | CRUD |
| **Documentos** | upload/download |
| **Portal do Paciente** | `/portal/appointments`, `/portal/prescriptions`, `/portal/invoices` |
| **Notificações** | SMS (Twilio) + Email para eventos de agendamento |
| **PDF Export** | `/export/pdf/appointments/{id}`, `/export/pdf/invoices/{id}`, `/export/pdf/prescriptions/{id}`, `/export/pdf/consent-terms/{id}` |
| **Relatórios** | export CSV de agendamentos e faturas |
| **Dashboard** | estatísticas |
| **Waiting List** | fila de espera com oferta automática |
| **Audit Log** | log de ações |
| **AI** | integração OpenAI para análise de prontuário |

## Infraestrutura (Docker)

```yaml
services:
  postgres-db:
    image: postgres:16
    ports: ["5432:5432"]
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: 123456
      POSTGRES_DB: psyke_master

  kafka:
    image: apache/kafka:4.1.0
    ports: ["9092:9092"]
```

**Start:** `docker compose up -d`

## Configuração

| Variável | Default | Descrição |
|----------|---------|-----------|
| `PSYKE_DB_URL` | `jdbc:postgresql://localhost:5432/psyke_master` | URL do PostgreSQL |
| `PSYKE_DB_USER` | `postgres` | Usuário do banco |
| `PSYKE_DB_PASS` | `postgres` | Senha do banco |
| `PSYKE_JWT_SECRET` | `CHANGE_ME_*` | Chave secreta JWT |
| `PSYKE_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka brokers |
| `PSYKE_BASE_URL` | `http://localhost:8080` | URL base da aplicação |
| `PSYKE_MAIL_HOST` | *(vazio)* | SMTP host |
| `TWILIO_ACCOUNT_SID` | *(vazio)* | Twilio SID (desabilita SMS se vazio) |
| `TWILIO_AUTH_TOKEN` | *(vazio)* | Twilio token |
| `TWILIO_FROM_NUMBER` | *(vazio)* | Número de origem Twilio |
| `PSYKE_AI_ENABLED` | `false` | Habilita features de IA |
| `PSYKE_AI_API_KEY` | *(vazio)* | Chave OpenAI |

## Executar

```bash
# Subir dependências
docker compose up -d

# Rodar testes
.\mvnw.cmd test

# Build
.\mvnw.cmd clean package -DskipTests

# Iniciar aplicação
.\mvnw.cmd spring-boot:run
```

## Testes

```
.\mvnw.cmd test                          # Todos os testes
.\mvnw.cmd test -Dtest=UserServiceTest   # Classe específica
```

- **199 testes** no total
- Mockito `@ExtendWith(MockitoExtension.class)` para unit testes
- `@SpringBootTest` com `@DynamicPropertySource` para integração
- PostgreSQL real via Testcontainers na CI

## Swagger

Disponível em: `http://localhost:8080/swagger-ui.html`

## Estado Machine (Agendamentos)

```
SCHEDULED ↔ CONFIRMED ↔ DONE
    ↕          ↕
CANCELLED   NO_SHOW
```

Transições validadas pelo `AppointmentService.TRANSITIONS`.

## Projetos Relacionados

- [Frontend (Angular)](https://github.com/anomalyco/psyke-frontend)
- [Infraestrutura (Terraform)](https://github.com/anomalyco/psyke-infra)
