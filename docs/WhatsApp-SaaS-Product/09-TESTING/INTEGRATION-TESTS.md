# Integration Tests

Where most of the value is. Real PostgreSQL 17 via Testcontainers.

## Base setup

```java
@SpringBootTest
@Testcontainers
abstract class IntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> db = new PostgreSQLContainer<>("postgres:17")
        .withInitScript("test-init.sql")   // creates the NON-SUPERUSER app role
        .withReuse(true);
}
```

`test-init.sql` mirrors production: creates `wasaas_app` as `NOSUPERUSER`, grants the same
privileges. Spring's datasource then connects **as that role**, not as the container default.

**This is the single most important line in your test setup.** See the superuser trap in
[TESTING-STRATEGY.md](TESTING-STRATEGY.md).

One shared container, truncate between tests:

```java
@AfterEach
void cleanup() {
    jdbc.execute("TRUNCATE tenants, users, contacts, conversations, message_ledger, "
               + "jobs, webhook_events, automation_rules, faqs CASCADE");
}
```

Truncating beats recreating the schema — Flyway runs once per suite instead of per test.

## Job queue tests (F07) — all five required

**1. Concurrency.** Real threads, real database. Not mocks.

```java
@Test
void twoWorkersNeverClaimTheSameJob() throws Exception {
    IntStream.range(0, 100).forEach(i -> jobService.enqueue("NOOP", "{}", null));

    var executor = Executors.newFixedThreadPool(2);
    var processed = Collections.synchronizedList(new ArrayList<UUID>());
    var latch = new CountDownLatch(2);
    for (int i = 0; i < 2; i++) {
        executor.submit(() -> { try { processed.addAll(worker.pollAndProcessAll()); }
                                finally { latch.countDown(); } });
    }
    latch.await(30, SECONDS);

    assertThat(processed).hasSize(100);
    assertThat(new HashSet<>(processed)).hasSize(100);  // no duplicates
}
```

This test is the reason you use `SKIP LOCKED` rather than a naive `SELECT ... UPDATE`. Without
`SKIP LOCKED` it either deadlocks or double-processes.

**2. Crash recovery.** Set `status=RUNNING` with a stale `locked_at`, then confirm the worker
reclaims it. This proves a mid-job crash loses nothing.

**3. Retry and backoff.** A handler that always throws → attempts increment, `run_after`
advances, eventually `DEAD`, never infinite.

**4. Permanent failure.** `PermanentJobException` → `DEAD` immediately, `attempts` not exhausted.

**5. Idempotency.** Enqueue the same key twice → exactly one row.

## Webhook tests (F10)

- Valid HMAC over the raw body → 200 and a persisted event
- **Tampered body** → 403, not processed. Compute the signature over the original, send modified
  bytes. This is the test that proves you verify over raw bytes and not over a re-serialised
  object.
- Missing signature header → 403
- Duplicate `event_id` → exactly one logical effect
- ACK latency: assert the controller path performs no outbound HTTP and returns within budget

## Ledger tests (F08)

- Ledger row exists **before** the send is attempted (assert ordering, e.g. by having the mock
  client verify the row exists when called)
- Status events append; the parent row is never updated
- Monthly per-category counts match a hand-written aggregate
- Same idempotency key → one ledger row
- Full phone number appears nowhere in the ledger

## Full-text / FAQ tests (F14)

Only meaningful against real Postgres — `ts_rank` and `pg_trgm` similarity scores are the thing
under test.

- Exact question → high confidence
- Typo'd question ("wat r ur timings") → still matches via trigram
- Unrelated question → below threshold → escalates
- Threshold is configurable and respected
- Search is tenant-scoped: tenant A's FAQ never matches tenant B's query

## Transaction and isolation gotchas

`@Transactional` on a test rolls back — which means **RLS and constraint behaviour you're
testing may not behave as it does in production**, and background threads (your worker) can't
see uncommitted data.

For queue and worker tests, do **not** use `@Transactional`. Commit for real and clean up in
`@AfterEach`.

## What to assert

Assert **behaviour and outcomes**, not implementation. "A ledger row exists with status SENT and
a wamid" is durable. "`ledgerRepository.save()` was called twice" breaks on every refactor and
proves nothing.
