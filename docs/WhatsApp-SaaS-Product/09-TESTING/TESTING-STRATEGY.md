# Testing Strategy

## Why this matters more than usual here

Two failure modes in this product are business-ending rather than annoying:

1. **A cross-tenant data leak.** One customer sees another's conversations. In a B2B SaaS with
   20 customers who all know each other, that's the end.
2. **A duplicate or runaway send.** Your customer pays Meta directly. A retry bug spends *their*
   money — which is worse than spending yours, not better.

Everything else is a bug. These two are existential. The test suite is weighted accordingly.

## Shape of the suite

```text
        ┌─────────────────┐
        │  Manual E2E     │  ~10 flows, run before each release
        │  (real Meta)    │  Not automated — Meta sandbox is limited
        ├─────────────────┤
        │  Integration    │  ← the bulk of the value
        │  Testcontainers │  RLS, SKIP LOCKED, webhooks, jobs, full-text
        ├─────────────────┤
        │  Unit           │  Pure logic: matching, categorisation, backoff, crypto
        └─────────────────┘
```

**Deliberately integration-heavy.** The risky behaviour in this system lives in Postgres
(RLS, `SKIP LOCKED`, `tsvector` ranking) and in HTTP boundaries (HMAC over raw bytes). Unit
tests with mocks cannot see any of it — a mocked repository will happily "prove" isolation
that doesn't exist.

## ⚠️ Testcontainers, never H2

H2 does not implement: Row-Level Security, `FOR UPDATE SKIP LOCKED`, `tsvector`/`ts_rank`,
`pg_trgm`, `jsonb` operators, `timestamptz` semantics.

An H2 test suite for this application passes green while production is broken. Use
Testcontainers PostgreSQL 17 everywhere.

## ⚠️ The Testcontainers superuser trap

**Testcontainers' default user is a superuser. Superusers bypass RLS.**

If you write RLS tests against the default container user, they pass — and they'd pass with
RLS deleted entirely. You will believe you have two layers of defence and have one.

Every integration test that touches tenant data must connect as a **non-superuser** role created
in the container init script, mirroring production.

The proof is a test that *fails* when RLS is off. See
[MULTI-TENANCY-TESTING.md](MULTI-TENANCY-TESTING.md).

## Speed

One shared container per test run via a singleton pattern (or `@ServiceConnection` with a static
container), truncating tables between tests rather than recreating the schema. Full suite target:
**under 3 minutes.** A slow suite is a suite you stop running.

## What must have a test (non-negotiable)

| Area | Requirement |
|---|---|
| Every new table | Tenant isolation test + RLS-enforcement test |
| Job queue | Concurrency, crash recovery, retry, dead-letter, idempotency |
| Webhooks | Signature valid/invalid/missing, dedupe, ACK latency |
| Outbound send | Ledger-before-send ordering, idempotency, error classification |
| Token crypto | Round-trip, never serialised, startup fails without a key |
| FAQ matching | Match, typo tolerance, below-threshold escalation |
| Automation | Priority (one rule fires), per-contact rate limit, regex safety |
| Billing | State changes only via verified webhook, PAST_DUE degradation |

## What deliberately has no test

Getters and setters · Spring configuration classes · DTO mapping without logic · third-party
library behaviour · the UI beyond a smoke test.

Coverage percentage is not a goal. **The eight areas above are the goal.** 60% coverage that
includes all of them beats 90% that misses one.

## Frontend testing

Minimal in the MVP: a smoke test that the app renders and login works. Manual testing at 360px
width for every screen.

Playwright E2E is worth adding after 20 customers, not before. You'd spend a week on it and
find bugs you'd have found by using the product.

## Manual pre-release checklist

Automated tests can't reach Meta's real API. Before each release, by hand:

1. Register a new account
2. Connect a WhatsApp account (Embedded Signup, real Meta)
3. Send a message from a personal phone to the business number
4. Confirm auto-reply arrives
5. Confirm delivery status updates in the inbox
6. Send a manual reply from the inbox
7. Confirm window-closed state disables free text
8. Schedule a message for +5 min; confirm it sends once
9. Trigger a failed send (invalid number); confirm plain-language error
10. Check the dashboard counts against a direct ledger query

Keep this in `PRE-PRODUCTION-CHECKLIST.md` and actually run it.

## Read next

[UNIT-TESTS.md](UNIT-TESTS.md) · [INTEGRATION-TESTS.md](INTEGRATION-TESTS.md) ·
[MULTI-TENANCY-TESTING.md](MULTI-TENANCY-TESTING.md) ·
[WHATSAPP-TESTING.md](WHATSAPP-TESTING.md) · [SECURITY-TESTING.md](SECURITY-TESTING.md) ·
[PRE-PRODUCTION-CHECKLIST.md](PRE-PRODUCTION-CHECKLIST.md)
