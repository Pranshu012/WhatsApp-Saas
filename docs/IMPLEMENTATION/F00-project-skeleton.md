# F00 — Project Skeleton

**Status:** Partial — implementation complete; Docker-backed verification pending local tool installation
**Completed:** 2026-08-21
**Commit:** Pending local checkpoint
**Spec:** `../WhatsApp-SaaS-Product/14-CLAUDE-CODE/PROMPTS/PHASE-A-FOUNDATION.md#f00`

## What this does

Creates the Java 21 Spring Boot baseline for the modular monolith. The service can migrate a local PostgreSQL 17 database, expose Actuator health, attach a request ID to each request and log line, and return a consistent error shape for domain errors.

## Files

| File | Purpose |
|---|---|
| `pom.xml` | Spring Boot 3.x Maven build with only approved dependencies. |
| `docker-compose.yml` | Local-only PostgreSQL 17. |
| `src/main/resources/db/migration/V1__baseline.sql` | Enables `pgcrypto`; no feature schema exists yet. |
| `common/exception/*` | Domain exceptions and stable API error responses. |
| `common/logging/RequestIdFilter.java` | Generates and returns `X-Request-Id` while populating MDC. |
| `common/config/SecurityConfig.java` | Keeps the featureless F00 health check accessible; F03 replaces it with session security. |

## Database changes

- Flyway V1 enables PostgreSQL `pgcrypto`. No tables or RLS policies exist yet; F01/F02 own those.

## API surface

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/actuator/health` | none | Liveness/health check. |

## Key decisions and why

- The first local connection uses Docker's bootstrap `wasaas` user. F02 will create and switch to the non-superuser `wasaas_app` role once RLS is introduced; pretending it already exists would prevent a clean F00 boot.
- Exception handling is deliberately small: only explicit domain errors are shaped. Validation and feature-specific error mapping belong to their owning increments.

## Divergence from the architecture docs

None. The temporary local database role is documented above and becomes `wasaas_app` in F02.

## Tests

| Test | Proves |
|---|---|
| `GlobalExceptionHandlerTest` | A `NotFoundException` becomes a clean 404 `ApiError`. |

The Postgres startup, Flyway migration, and health endpoint must still be verified after Docker Desktop becomes available.

## Gotchas — read before touching this code

`ddl-auto` is `validate`, not `update`: schema changes must be a new Flyway migration. `TOKEN_ENCRYPTION_KEY` is declared but intentionally not validated until F05 adds token encryption; validating it now would make a featureless skeleton require a production secret.

## Configuration

| Env var / property | Default | Purpose |
|---|---|---|
| `DB_URL` | local Postgres URL | JDBC connection. |
| `DB_USER` | `wasaas` | Temporary local bootstrap user. |
| `DB_PASSWORD` | `localdev` | Local-only database password. |
| `SERVER_PORT` | `8080` | HTTP port. |

## Known limitations / TODOs

- F01 adds tenant/user schema; F02 introduces RLS and `wasaas_app`.
- F05 validates the encryption key before encrypted tokens exist.
