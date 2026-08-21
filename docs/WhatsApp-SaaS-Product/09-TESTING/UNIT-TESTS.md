# Unit Tests

Scope: pure logic with no database and no HTTP. Fast, many, cheap.

## What belongs here

| Component | What to test |
|---|---|
| `TokenCipher` | Round-trip; two encryptions of the same plaintext differ (random nonce); wrong key fails; malformed input fails cleanly |
| Retry backoff calculator | Delay grows exponentially; jitter applied; capped; attempt N+1 > attempt N |
| Error classifier | Meta code → retryable vs permanent, for every code you handle |
| Billing categoriser | Template category + window state → billing category |
| Phone normaliser | Indian numbers → E.164; `+91`, `0`, `91`, 10-digit forms all normalise identically; rejects garbage |
| Phone masker | `+919876543210` → `●●●●●● 3210`; never returns the full number |
| Rule matcher | EXACT / CONTAINS / STARTS_WITH / REGEX, case sensitivity |
| Regex validator | Rejects catastrophic patterns; compile timeout enforced |
| Service-window calculator | Open/closed at boundaries; timezone-correct |
| Idempotency key builder | Deterministic — same inputs always give the same key |

## Patterns

**Table-driven for classifiers.** `@ParameterizedTest` with a `@CsvSource` beats twelve
near-identical methods:

```java
@ParameterizedTest
@CsvSource({
    "131026, PERMANENT",   // recipient not on WhatsApp
    "132000, PERMANENT",   // template param mismatch
    "80007,  RETRYABLE",   // rate limit
    "500,    RETRYABLE",
    "200,    PERMANENT"    // missing Advanced Access — no point retrying
})
void classifiesMetaErrors(int code, Failure expected) { ... }
```

**Test boundaries, not the middle.** For the service window, the interesting cases are
23h59m59s (open), 24h00m00s (closed), and across a date change. Not "12 hours ago".

**Determinism.** Any test involving time takes a `Clock`. Never `Instant.now()` in a testable
path — inject `Clock.fixed(...)`. A test that fails at midnight is worse than no test.

## Idempotency keys — worth spelling out

These keys are what stop duplicate sends, and duplicate sends spend your customer's money.
They must be **deterministic** functions of stable inputs:

```text
reply:{wamid}:{ruleId}          automated reply to a specific inbound message
sched:{scheduledMessageId}      scheduled message
wh:{webhookEventId}             webhook processing
tplsync:{wabaId}:{date}         daily template sync
```

Test: the same inputs produce byte-identical keys across JVM restarts. No timestamps, no
`UUID.randomUUID()`, no hash-order dependence.

## What does not belong here

- Anything asserting tenant isolation → integration test with real RLS
- Anything asserting queue behaviour → integration test with real `SKIP LOCKED`
- Anything mocking a repository to prove a query is correct — that tests the mock

**A mock cannot prove a security property.** If a test's purpose is "tenant A can't see tenant
B", it belongs in [MULTI-TENANCY-TESTING.md](MULTI-TENANCY-TESTING.md) with a real database.

## Mocking

Mockito, sparingly. Mock the outbound HTTP client and the email sender. Don't mock your own
services to test other services — if that feels necessary, the modules are too entangled and
the fix is in the design, not the test.

## Target

Fast (whole unit suite under 20 seconds) and boring. If a unit test needs elaborate setup,
it's probably an integration test wearing a disguise.
