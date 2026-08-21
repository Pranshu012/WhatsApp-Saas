# Current Status

> Update this at the end of **every** working session. It is the first thing you read
> after a break. Keep it honest — a stale status file is worse than none.

**Last updated:** 21 August 2026

---

```text
Current Phase:        PHASE B — WhatsApp Core (F05–F11)

Current Goal:         Implement F08 Message Ledger.

Last Completed:       F07 - Jobs table and worker
                      (Durable job queue via V8__jobs.sql, SKIP LOCKED claim logic,
                       JobWorker poller, and concurrent transactional execution).

Currently Working On: F08 — Message Ledger

Next Task:            Read F08 specification in PHASE-B-WHATSAPP.md and implement billing/append-only ledger.

Blocked By:           Docker Desktop remains required for Testcontainers parity. Java 21 also
                      requires an interactive macOS administrator-password prompt; Java 26 is
                      currently used for local compilation but is not the project target runtime.

Important Decisions:  - Tech Provider model, customer pays Meta directly (ADR-003, ADR-005)
                      - Modular monolith, single VM (ADR-001)
                      - Postgres job queue, no Redis/Kafka (ADR-002)
                      - Row-level multi-tenancy with tenant_id + RLS (ADR-004)
                      - Self-hosted Postgres on app VM initially (ADR-006)
                      - No AI in core automation path (ADR-007)

Open Questions:       See 13-DECISIONS/DECISIONS.md — 9 items currently open,
                      of which D-01 (core feature scope) blocks Phase 7.
```

---

## Session log

Append a line each session. Newest at top.

| Date | Phase | What I did | Next |
|---|---|---|---|
| 2026-08-21 | F07 | Implemented durable job queue with FOR UPDATE SKIP LOCKED, exponential backoff, locking recovery | F08 Message Ledger |
| 2026-08-21 | F06 | Implemented Embedded Signup callback, MetaGraphClient, webhook subscription, Error Code 200 handling, and connect endpoint | F07 Jobs Table & Worker |
| 2026-08-21 | F05 | Implemented WhatsApp Accounts model with AES-256-GCM TokenCipher envelope encryption, fail-fast key check, and RLS | F06 Embedded Signup & Meta Graph Client |
| 2026-08-21 | F04 | Implemented Password Reset with SHA-256 hashed tokens, 30-min expiry, anti-enumeration, global session revocation, and EmailSender | F05 WhatsApp Accounts & TokenCipher |
| 2026-08-21 | F03 | Implemented Spring Session JDBC, JSON login, logout, me, csrf endpoints, rate limiting, and session-principal TenantContext | F04 Password Reset |
| 2026-08-21 | F02 | Implemented TenantContext, TenantFilterAspect, and V3 RLS migration for Postgres | F03 Authentication |
| 2026-08-21 | F01 | Added tenant/user/membership registration with Argon2id; native end-to-end check and build pass | F02 RLS and tenant context |
| 2026-08-21 | F00 | Verified native PostgreSQL 17, Flyway V1, and HTTP 200 health endpoint | Install Docker, then F01 Testcontainers |
| 2026-08-21 | F00 | Retried Temurin 21 installation; macOS requires interactive sudo authentication | Install Java 21 and Docker Desktop, then verify F00 runtime |
| 2026-08-21 | F00 | Added Spring Boot skeleton, local Postgres compose config, Flyway baseline, error/logging contracts; unit build passes on Java 26 | Install Java 21 + Docker, verify full runtime |
| 2026-08-21 | Setup | Created local repository, copied docs, added cross-agent guidance | Install Java 21 and Docker, then F00 |
| 2026-08-18 | — | Created documentation workspace | Start Phase 0.1 (domain) |

---

## Quick health check

Run through this if you've been away more than a week:

- [ ] Is the Oracle instance still running? (Oracle reclaims idle Always Free instances)
- [ ] Did last night's backup complete? Check the B2 bucket.
- [ ] Any Sentry errors accumulating?
- [ ] Any Meta developer-account emails about policy or pricing changes?
- [ ] Has anything in `00-START-HERE/ASSUMPTIONS-AND-EXPIRY-DATES.md` passed its re-verify date?
