# Implementation Index

| # | Feature | Status | Date | Doc |
|---|---|---|---|---|
| F00 | Project skeleton | Complete — verified with native PostgreSQL; Docker/Java 21 parity pending | 2026-08-21 | [F00](F00-project-skeleton.md) |
| F01 | Tenant and user model | Complete — native verification; Testcontainers parity pending | 2026-08-21 | [F01](F01-tenant-user-model.md) |
| F02 | Tenant context and Row-Level Security | Complete — verified with native PostgreSQL RLS and isolation tests | 2026-08-21 | [F02](F02-tenant-isolation-rls.md) |
| F03 | Authentication and server-side sessions | Complete — verified with Spring Session JDBC, rate limiting, and test suite | 2026-08-21 | [F03](F03-authentication-sessions.md) |
| F04 | Password reset | Complete — verified with SHA-256 hashed tokens, 30-min expiry, and test suite | 2026-08-21 | [F04](F04-password-reset.md) |
| F05 | WhatsApp account model and token encryption | Complete — verified with AES-256-GCM envelope encryption, fail-fast key check, and RLS | 2026-08-21 | [F05](F05-whatsapp-account-model-token-encryption.md) |
| F06 | Embedded Signup callback and Meta Graph Client | Complete — verified with code exchange, webhook subscription, error 200 handling, and test suite | 2026-08-21 | [F06](F06-embedded-signup-callback.md) |
| F07 | Jobs table and worker | Complete — verified with FOR UPDATE SKIP LOCKED, exponential backoff, and concurrency tests | 2026-08-21 | [F07](F07-jobs-table-worker.md) |
| F08 | Message ledger | Complete — verified with phone hashing, append-only events, monthly aggregation, and DB trigger guard | 2026-08-21 | [F08](F08-message-ledger.md) |
