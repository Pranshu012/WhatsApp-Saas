# Week 3

**Theme: auth complete, and the job queue — the engine of the whole system.**

## Monday — F03
- [ ] Run the **F03** prompt: Spring Session JDBC, login, roles
- [ ] **Verify the `X-Tenant-Id` header path from F02 is completely removed**
- [ ] Confirm session survives an app restart
- [ ] Confirm unknown email and wrong password are indistinguishable
- [ ] Commit

## Tuesday — F04
- [ ] Run the **F04** prompt: password reset
- [ ] Confirm the token is stored **hashed**
- [ ] Confirm single-use and expiry
- [ ] Confirm all sessions invalidate after a reset
- [ ] Commit

## Wednesday — F07, the job queue
- [ ] Read `03-ARCHITECTURE/BACKGROUND-JOBS.md` fully first
- [ ] Run the **F07** prompt
- [ ] Review the claim SQL carefully — `SKIP LOCKED`, stale-lock recovery, `attempts + 1` on claim
- [ ] Verify the concurrency test uses **real threads**, not mocks
- [ ] Verify `web` profile does not poll
- [ ] Commit

This is the single most important increment after F02. Take the whole day if needed.

## Thursday — F08, the ledger
- [ ] Read `03-ARCHITECTURE/MESSAGE-LEDGER.md` and `08-META-WHATSAPP/OCTOBER-2026-BILLING-CHANGE.md`
- [ ] Run the **F08** prompt
- [ ] Verify append-only behaviour is enforced
- [ ] Verify the `whatsapp_rates` seed includes both SERVICE rows (0 before Oct, 1150 after)
- [ ] Verify no full phone numbers are stored in the ledger
- [ ] Commit

## Friday — F05 + Meta check
- [ ] Check App Review status
- [ ] Run the **F05** prompt: `whatsapp_accounts` + `TokenCipher`
- [ ] Verify startup fails without a valid encryption key
- [ ] Verify the token never appears in JSON
- [ ] Commit

## If App Review is approved
Bring **F06** (Embedded Signup) forward. It's the highest-risk remaining increment and you want
maximum time on it.

## If still pending
Continue with F09 (outbound send) using the test WABA. It works with standard access.

## End of week 3

- [ ] F00–F05, F07, F08 complete
- [ ] Auth works, sessions persist
- [ ] Job queue survives crashes and never double-claims
- [ ] Ledger records categories with dated rates
- [ ] Tokens encrypted, startup fails without a key

## Watch out for

- **Ledger-first ordering.** If Claude Code writes send-then-record, correct it. This is the one
  that loses billing evidence.
- **Non-deterministic idempotency keys.** `UUID.randomUUID()` defeats the entire mechanism.
- **Retrying permanent Meta errors.** Check the classification table is actually implemented.
- Mocked concurrency tests. `SKIP LOCKED` can only be tested against real Postgres.
