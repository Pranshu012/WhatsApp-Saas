# Phase A Prompts — Foundation (F00–F04)

Paste each prompt into a **fresh** Claude Code session. Adjust the package name
`com.example.wasaas` to yours once, everywhere.

---

## F00 — Project skeleton

```text
We're starting a new project. Read docs/WhatsApp-SaaS-Product/00-START-HERE/README.md and
docs/WhatsApp-SaaS-Product/05-BACKEND/BACKEND-SETUP.md first, plus CLAUDE.md.

Goal for this increment ONLY: a running Spring Boot skeleton. No features.

Requirements:
- Java 21, Spring Boot 3.x, Maven wrapper
- Dependencies: Web, Validation, Data JPA, PostgreSQL driver, Flyway, Security, Actuator,
  Testcontainers (test scope). Nothing else.
- Package root: com.example.wasaas
- Create empty feature packages: tenant, user, auth, whatsapp, messaging, template,
  automation, job, ledger, inbox, analytics, common
- common/exception: DomainException, NotFoundException, ApiError (record),
  GlobalExceptionHandler mapping to consistent JSON
- common/logging: RequestIdFilter putting a request ID into MDC, included in every log line
- application.yml + application-local.yml. Config via env vars with local defaults.
  spring.jpa.hibernate.ddl-auto MUST be "validate", never "update".
- docker-compose.yml with Postgres 17 for LOCAL DEV ONLY (production is self-hosted on
  the VM, not Docker)
- Flyway migration V1__baseline.sql: enable pgcrypto extension, nothing else yet
- .env.example documenting every env var with placeholder values
- .gitignore for Java/Maven/IDE/.env

Do NOT create: entities, controllers beyond health, any business logic, Dockerfile,
CI config, or any dependency not listed above.

Plan this first and show me the file list. Don't write code yet.

Finally: write docs/IMPLEMENTATION/F00-<slug>.md following
docs/WhatsApp-SaaS-Product/14-CLAUDE-CODE/IMPLEMENTATION-DOC-TEMPLATE.md, and add a row to
docs/IMPLEMENTATION/INDEX.md. Document the key decisions, any divergence from the architecture
docs, and the gotchas someone would trip over. This is part of the increment, not optional.
```

**Definition of Done**
- [ ] `./mvnw clean verify` green
- [ ] `docker compose up -d db` then app starts, connects to Postgres
- [ ] `GET /actuator/health` → 200
- [ ] Flyway V1 in `flyway_schema_history`
- [ ] A thrown `NotFoundException` → clean JSON `ApiError`, correct status
- [ ] Every log line has a request ID
- [ ] No secrets in Git

---

## F01 — Tenant, User, TenantUser

```text
Increment F01. Read docs/WhatsApp-SaaS-Product/04-DATABASE/DATABASE-DESIGN.md and
docs/WhatsApp-SaaS-Product/03-ARCHITECTURE/MULTI-TENANCY.md first.

Goal: the tenant and user data model, plus a registration endpoint. No login yet (F03).

Requirements:
- Flyway V2__tenants_users.sql creating:
  - tenants: id (uuid pk), business_name, slug (unique), status, created_at, updated_at
  - users: id (uuid pk), email (unique, lowercased), password_hash, full_name, status,
    created_at, updated_at
  - tenant_users: tenant_id fk, user_id fk, role, created_at; composite pk (tenant_id, user_id)
- users and tenants are the ONLY tables without tenant_id — everything else needs it.
  Add a comment in the migration explaining why.
- JPA entities with constructor injection style, no Lombok unless you ask me first
- TenantRepository, UserRepository, TenantUserRepository
- TenantService.registerTenant(command) — creates tenant + user + owner membership in ONE
  @Transactional method. Reject duplicate email and duplicate slug with a clear DomainException.
- Password hashing: Argon2id via Spring Security's Argon2PasswordEncoder. Configure the
  encoder bean now even though login comes in F03.
- POST /api/auth/register with a request record + bean validation. Response must NOT include
  the password hash or any internal IDs beyond what the client needs.
- Tests: happy path, duplicate email, duplicate slug, password is hashed not plaintext.
  Use Testcontainers Postgres, not H2.

Do NOT build: login, sessions, tenant context, RLS (that's F02/F03), email sending,
invitations, or role management UI.

Plan first.

Finally: write docs/IMPLEMENTATION/F01-<slug>.md following
docs/WhatsApp-SaaS-Product/14-CLAUDE-CODE/IMPLEMENTATION-DOC-TEMPLATE.md, and add a row to
docs/IMPLEMENTATION/INDEX.md. Document the key decisions, any divergence from the architecture
docs, and the gotchas someone would trip over. This is part of the increment, not optional.
```

**Definition of Done**
- [ ] `POST /api/auth/register` creates all three rows in one transaction
- [ ] Rollback verified: a failure mid-way leaves no orphan tenant
- [ ] Password stored as Argon2id hash; plaintext appears nowhere
- [ ] Duplicate email/slug → 409 with a clear message
- [ ] Tests use Testcontainers

---

## F02 — Tenant context and Row-Level Security

```text
Increment F02. This is the most important increment in the project. Read
docs/WhatsApp-SaaS-Product/03-ARCHITECTURE/MULTI-TENANCY.md and
docs/WhatsApp-SaaS-Product/04-DATABASE/MULTI-TENANT-DATABASE-RULES.md fully before planning.

Goal: two defence layers so one tenant can never read another's data.

Requirements:
- tenant/context/TenantContext: request-scoped holder of the current tenant id.
  Must fail loudly (throw) if read when unset — never silently return null.
- tenant/context/TenantContextFilter: populates TenantContext from the authenticated
  principal. For now, since auth lands in F03, populate from a request header
  X-Tenant-Id and add a clear TODO(F03) to switch to the session principal.
- Layer 1 (application): a base repository pattern or JPA @Filter/Specification approach so
  tenant-scoped queries automatically include tenant_id. Recommend the simplest approach
  that can't be forgotten — explain your choice in the plan.
- Layer 2 (database): Row-Level Security. Migration V3__rls.sql that:
  - creates a non-superuser application role
  - enables RLS and FORCE ROW LEVEL SECURITY on tenant-scoped tables
  - a policy using current_setting('app.tenant_id')
  - the connection sets app.tenant_id per transaction from TenantContext
- Document in the migration how a developer adds RLS for a NEW table, since every future
  migration must do this.

Tests (all mandatory):
1. Tenant A's repository query cannot return Tenant B's rows
2. A deliberately-written raw query WITHOUT tenant_id still cannot cross tenants (proves RLS)
3. Reading TenantContext when unset throws rather than defaulting
4. Disabling RLS makes test 2 fail — proving the test actually exercises RLS and isn't
   passing for the wrong reason

Test 4 matters: an isolation test that passes when protection is off is worthless.

Do NOT build: authentication, any new feature tables.

Plan first, and in the plan explain how a future developer could accidentally bypass this
and what stops them.

Finally: write docs/IMPLEMENTATION/F02-<slug>.md following
docs/WhatsApp-SaaS-Product/14-CLAUDE-CODE/IMPLEMENTATION-DOC-TEMPLATE.md, and add a row to
docs/IMPLEMENTATION/INDEX.md. Document the key decisions, any divergence from the architecture
docs, and the gotchas someone would trip over. This is part of the increment, not optional.
```

**Definition of Done**
- [ ] All four tests pass
- [ ] Test 2 fails when RLS is disabled (verify by hand once)
- [ ] App connects as a non-superuser role (superusers bypass RLS)
- [ ] `app.tenant_id` set per transaction, not per connection
- [ ] Migration documents the pattern for future tables

---

## F03 — Authentication and sessions

```text
Increment F03. Read docs/WhatsApp-SaaS-Product/03-ARCHITECTURE/AUTHENTICATION-AND-AUTHORIZATION.md
and docs/WhatsApp-SaaS-Product/03-ARCHITECTURE/SECURITY.md.

Goal: real login with server-side sessions in Postgres. Keep the app stateless.

Requirements:
- Spring Session JDBC. Migration V4__spring_session.sql with the official Spring Session
  JDBC schema for Postgres.
- SecurityConfig: form-less JSON login, session cookie HttpOnly + Secure + SameSite=Lax,
  CSRF enabled for cookie-based auth with a token endpoint the SPA can read.
- POST /api/auth/login, POST /api/auth/logout, GET /api/auth/me
- On successful login, resolve the user's tenant membership and REPLACE the F02 header-based
  TenantContextFilter with population from the authenticated principal. Remove the
  X-Tenant-Id header path entirely — it must not be usable in production.
- Role model within a tenant: OWNER, MEMBER. Method-level authorization for owner-only actions.
- Login failures must be generic ("invalid credentials") — never reveal whether the email exists.
- Rate-limit login attempts per email+IP. Simple in-Postgres counter is fine. No Redis.
- Tests: successful login, wrong password, unknown email (identical response shape),
  session survives restart, X-Tenant-Id header no longer grants access, logout invalidates,
  rate limit trips.

Do NOT build: JWT, OAuth, SSO, refresh tokens, remember-me, 2FA, password reset (that's F04).

Plan first.

Finally: write docs/IMPLEMENTATION/F03-<slug>.md following
docs/WhatsApp-SaaS-Product/14-CLAUDE-CODE/IMPLEMENTATION-DOC-TEMPLATE.md, and add a row to
docs/IMPLEMENTATION/INDEX.md. Document the key decisions, any divergence from the architecture
docs, and the gotchas someone would trip over. This is part of the increment, not optional.
```

**Definition of Done**
- [ ] Login → session cookie; `/api/auth/me` returns the user + tenant
- [ ] Session survives an application restart (it's in Postgres)
- [ ] `X-Tenant-Id` header is gone and grants nothing
- [ ] Unknown email and wrong password are indistinguishable
- [ ] Rate limiting works
- [ ] No token, password, or session ID in any log

---

## F04 — Password reset

```text
Increment F04.

Goal: password reset by email.

Requirements:
- Migration V5__password_reset_tokens.sql: id, user_id, token_hash (store the HASH, never
  the raw token), expires_at, used_at, created_at
- POST /api/auth/forgot-password — always returns 200 regardless of whether the email exists
  (no account enumeration)
- POST /api/auth/reset-password — validates token, single use, expiry (30 min), sets new
  password, invalidates all existing sessions for that user
- Email via Brevo or Resend over SMTP/API, behind a small EmailSender interface with a
  no-op implementation for local dev and tests. Do not hardcode any provider specifics into
  the service.
- Tests: valid reset works; token cannot be reused; expired token rejected; unknown email
  returns 200 with no email sent; existing sessions invalidated after reset.

Do NOT build: email templates beyond plain text, an email queue, notification preferences.

Plan first.

Finally: write docs/IMPLEMENTATION/F04-<slug>.md following
docs/WhatsApp-SaaS-Product/14-CLAUDE-CODE/IMPLEMENTATION-DOC-TEMPLATE.md, and add a row to
docs/IMPLEMENTATION/INDEX.md. Document the key decisions, any divergence from the architecture
docs, and the gotchas someone would trip over. This is part of the increment, not optional.
```

**Definition of Done**
- [ ] Reset flow works end to end locally with the no-op sender logging the link
- [ ] Token stored hashed; raw token only in the email
- [ ] Single-use and expiry enforced
- [ ] Sessions invalidated after a reset
- [ ] No enumeration via response differences or timing

---

## Phase A complete

You now have a multi-tenant, authenticated SaaS shell with proven isolation.

Before Phase B, confirm:
- [ ] Meta App Review status — F06 is blocked on Advanced Access approval
- [ ] `CURRENT-STATUS.md` updated
- [ ] All Phase A tests green in one `./mvnw clean verify`
- [ ] You can explain the RLS mechanism to someone else without looking it up
