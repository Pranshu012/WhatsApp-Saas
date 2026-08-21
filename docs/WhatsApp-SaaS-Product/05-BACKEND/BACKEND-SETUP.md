# Backend Setup

**Classification: BUILD NOW (F00).**

## Prerequisites

| Tool | Version | Check |
|---|---|---|
| JDK | 21 (Temurin) | `java -version` |
| Maven | wrapper (committed) | `./mvnw -v` |
| Docker | any recent | `docker --version` — **local dev only**, production is systemd |
| Git | any | `git --version` |
| psql | 17 client | `psql --version` |

## Project generation

Use [start.spring.io](https://start.spring.io) or your IDE. Exactly these dependencies:

| Dependency | Why |
|---|---|
| Spring Web | REST API |
| Spring Data JPA | Persistence |
| PostgreSQL Driver | — |
| Flyway Migration | Schema as code |
| Spring Security | Auth |
| Validation | Request validation |
| Spring Boot Actuator | Health checks |
| Spring Session JDBC | Sessions in Postgres |
| Testcontainers | Integration tests |

**Nothing else.** Not Lombok (adds a build-time dependency and obscures constructors — decide
deliberately if you want it), not MapStruct, not Redis, not Kafka, not Actuator Prometheus yet.

Java 21, Maven, JAR packaging.

## Local Postgres

`docker-compose.yml` — **local development only**:

```yaml
services:
  db:
    image: postgres:17
    environment:
      POSTGRES_DB: wasaas
      POSTGRES_USER: wasaas
      POSTGRES_PASSWORD: localdev
    ports: ["5432:5432"]
    volumes: ["pgdata:/var/lib/postgresql/data"]
volumes:
  pgdata:
```

```bash
docker compose up -d db
```

Production runs Postgres natively on the VM via `infra/provision.sh` (F22), not Docker. Fewer
moving parts on a 12 GB box, and simpler backups.

**Note for F02 onward:** create the non-superuser `wasaas_app` role locally too, and have the app
connect as it. If local uses a superuser, RLS is inert locally and your isolation tests pass for
the wrong reason.

## Configuration

`application.yml`:

```yaml
spring:
  application:
    name: wasaas
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/wasaas}
    username: ${DB_USER:wasaas_app}
    password: ${DB_PASSWORD:localdev}
    hikari:
      maximum-pool-size: ${DB_POOL_MAX:10}
  jpa:
    hibernate:
      ddl-auto: validate          # NEVER update or create
    open-in-view: false           # avoid lazy loading during serialisation
    properties:
      hibernate.jdbc.time_zone: UTC
  flyway:
    enabled: true
    validate-on-migrate: true
  session:
    store-type: jdbc
    jdbc.initialize-schema: never  # Flyway owns it

server:
  port: ${SERVER_PORT:8080}
  forward-headers-strategy: framework   # behind Caddy

management:
  endpoints.web.exposure.include: health,info
  endpoint.health.show-details: never   # never leak internals publicly

logging:
  pattern.console: "%d{ISO8601} %-5level [%X{requestId}] %logger{36} - %msg%n"

app:
  meta:
    app-id: ${META_APP_ID:}
    app-secret: ${META_APP_SECRET:}
    graph-version: ${META_GRAPH_VERSION:v21.0}   # pinned in config, not in call sites
    webhook-verify-token: ${META_WEBHOOK_VERIFY_TOKEN:}
  crypto:
    token-key: ${TOKEN_ENCRYPTION_KEY:}          # base64, 32 bytes
  jobs:
    poll-interval-ms: 1000
    batch-size: 10
    lock-timeout-secs: 300
```

`open-in-view: false` matters: the default `true` keeps a Hibernate session open through view
rendering, which hides N+1 queries and causes lazy-loading surprises during JSON serialisation.

## `.env.example` — commit this, never the real `.env`

```bash
DB_URL=jdbc:postgresql://localhost:5432/wasaas
DB_USER=wasaas_app
DB_PASSWORD=changeme
TOKEN_ENCRYPTION_KEY=base64:REPLACE_WITH_32_RANDOM_BYTES
META_APP_ID=
META_APP_SECRET=
META_GRAPH_VERSION=v21.0
META_WEBHOOK_VERIFY_TOKEN=
RAZORPAY_KEY_ID=
RAZORPAY_KEY_SECRET=
RAZORPAY_WEBHOOK_SECRET=
SENTRY_DSN=
```

Generate the encryption key: `openssl rand -base64 32`

## `.gitignore`

```text
target/
*.class
.env
.env.local
*.pem
*.p12
.idea/  .vscode/  *.iml
.claude/settings.local.json
```

## Fail-fast startup checks

Add a `@PostConstruct` validation that refuses to start when a required secret is missing or
malformed:

```java
if (tokenKey == null || Base64.getDecoder().decode(tokenKey).length != 32) {
    throw new IllegalStateException("TOKEN_ENCRYPTION_KEY must be 32 base64-encoded bytes");
}
```

Running with a broken encryption key is worse than not running — you'd write tokens you can
never decrypt.

## Commands

```bash
./mvnw clean verify              # build + all tests. The gate before "done".
./mvnw spring-boot:run           # run locally
./mvnw test -Dtest=ClassName     # one test
./mvnw spring-boot:run -Dspring-boot.run.profiles=worker
```

## Definition of Done (F00)

- [ ] `./mvnw clean verify` green
- [ ] App starts, connects to Postgres, Flyway V1 applied
- [ ] `GET /actuator/health` → 200
- [ ] `NotFoundException` → clean JSON `ApiError` with the right status
- [ ] Every log line carries a request ID
- [ ] Startup fails with a clear message when `TOKEN_ENCRYPTION_KEY` is missing
- [ ] No secrets in Git (`git log -p | grep -i secret` finds nothing)
