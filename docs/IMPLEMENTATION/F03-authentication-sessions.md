# F03 — Authentication and Server-Side Sessions

## Status
Complete — verified with Spring Session JDBC, rate limiting, and automated security test suite.

## Summary
Implemented stateless server-side authentication using Spring Session JDBC backed by PostgreSQL:
- Formless JSON login, logout, and `/me` endpoints.
- Replaced temporary `X-Tenant-Id` header with session-principal `TenantPrincipal` context binding.
- Timing attack mitigation on unknown email via pre-computed Argon2 dummy password verification.
- In-Postgres login rate limiting (5 attempts in 15 mins per email+IP) using `REQUIRES_NEW` transaction propagation.
- Cookie-based session handling (`HttpOnly`, `SameSite=Lax`) and CSRF token endpoint for SPA.
- 7 comprehensive automated tests verifying all security paths.

## Key Files
- `SecurityConfig.java`: Spring Security filter chain with JSON error responses and CSRF token repository.
- `AuthService.java`: Authentication service handling credentials check, timing defense, and session creation.
- `AuthController.java`: REST controller exposing `/api/auth/login`, `/api/auth/logout`, `/api/auth/me`, and `/api/auth/csrf`.
- `TenantPrincipal.java`: UserDetails implementation storing user, tenant ID, and role.
- `LoginAttemptService.java`: Rate limiting counter in PostgreSQL.
- `V4__spring_session.sql`: Official Spring Session JDBC PostgreSQL schema.
- `V5__login_attempts.sql`: Rate limiting attempts table and indexes.
- `AuthenticationTest.java`: 7 integration tests asserting security and isolation behavior.

## Configuration hardening (2026-08-22)

- Production configuration no longer supplies a fallback database or Flyway password. Deployments
  must inject their credentials through environment variables.
- Production CORS has no allowed origins by default. The local profile explicitly permits the
  Vite development origins; production must set `APP_CORS_ALLOWED_ORIGINS` to its real frontend URL.
