# F01 — Tenant and User Model

**Status:** Complete — native PostgreSQL verification; Testcontainers parity pending
**Completed:** 2026-08-21
**Commit:** Pending
**Spec:** `../WhatsApp-SaaS-Product/14-CLAUDE-CODE/PROMPTS/PHASE-A-FOUNDATION.md#f01`

## What this does

Businesses can register an owner account through `POST /api/auth/register`. Registration creates a tenant, user, and OWNER membership in a single transaction; email and slug collisions get clear conflict responses, and passwords are stored only as Argon2id hashes.

## Files

| File | Purpose |
|---|---|
| `V2__tenants_users.sql` | Creates tenant, user, and membership schema with lower-case email and enum constraints. |
| `tenant/TenantService.java` | Owns the atomic registration transaction. |
| `tenant/TenantUser.java` | Represents an OWNER/MEMBER relationship using a composite key. |
| `auth/RegistrationController.java` | Validated registration API boundary. |
| `common/config/PasswordConfig.java` | Provides Spring Security's Argon2id encoder. |

## Database changes

- V2 creates `tenants`, `users`, and `tenant_users`.
- `tenants` and `users` are documented exceptions to `tenant_id`; `tenant_users` is the tenant membership join table.
- Uniqueness of `users.email` and `tenants.slug` is enforced in the database; enum check constraints prevent invalid statuses and roles.
- RLS is deliberately not present yet: F02 owns it for every relevant table.

## API surface

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/auth/register` | none | Create tenant, owner, and membership. |

## Key decisions and why

- **Membership uses `(tenant_id, user_id)` as its primary key.** A user can later join more than one tenant without duplicating their global identity.
- **The response intentionally excludes IDs and password material.** Neither is needed by the current client, and excluding them narrows accidental exposure.
- **Bouncy Castle is an explicit runtime dependency.** Spring Security supplies the Argon2 encoder API but requires Bouncy Castle's implementation at runtime.

## Divergence from the architecture docs

The docs require Testcontainers PostgreSQL integration tests. Docker is not available on this machine, so unit tests and a native PostgreSQL 17 end-to-end registration check are recorded instead. This must be rerun with Testcontainers before production readiness.

## Tests

| Test | Proves |
|---|---|
| `TenantServiceTest.registersOwnerWithHashedPasswordAndNormalizedIdentifiers` | Email normalization and Argon2id hashing. |
| `TenantServiceTest.rejectsDuplicateEmail` | Email conflict is rejected. |
| `TenantServiceTest.rejectsDuplicateSlug` | Slug conflict is rejected. |
| Native registration check | One registration writes exactly one row to each of the three tables. |

Transactional rollback on an injected mid-transaction failure and Testcontainers verification remain to be added when Docker is available.

## Gotchas — read before touching this code

`users.email` must remain lower-case; the service normalizes it and the database constraint rejects otherwise. Do not add tenant context or RLS logic here: F02 establishes both layers together.

## Configuration

| Env var / property | Default | Purpose |
|---|---|---|
| `DB_URL` | local PostgreSQL | Runs Flyway V2 and JPA validation. |

## Known limitations / TODOs

- F03 adds login and sessions; registration does not authenticate the user.
- F02 adds application tenant context and PostgreSQL RLS.
- Docker/Testcontainers tests remain required before production readiness.
