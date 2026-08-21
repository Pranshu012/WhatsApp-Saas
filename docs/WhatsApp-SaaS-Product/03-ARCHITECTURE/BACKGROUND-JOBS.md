# Background Jobs — PostgreSQL as the Queue

**Classification: BUILD NOW (increment F07).** See `ADR-002` for why not Redis/Kafka.

---

## Why we need a queue at all

Three hard requirements:

1. **Webhooks must ACK in under 2 seconds.** Meta retries on slow or failed responses and
   eventually disables a persistently failing endpoint. So webhook handling must be:
   persist → enqueue → return 200.
2. **Work must survive restarts.** A queued message that vanishes on deploy is a customer's
   message that never arrived.
3. **Sends must retry, but never duplicate.** A duplicate WhatsApp message spends our
   customer's real money.

An in-memory executor fails (2). An external broker fails (transactional coupling) and adds a
service to run. Postgres satisfies all three.

---

## The `jobs` table

```sql
CREATE TABLE jobs (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID NULL,              -- NULL for system jobs (e.g. backup checks)
    job_type         TEXT NOT NULL,
    payload          JSONB NOT NULL,
    status           TEXT NOT NULL DEFAULT 'PENDING',
                     -- PENDING | RUNNING | SUCCEEDED | FAILED | DEAD
    idempotency_key  TEXT NULL,
    attempts         INT  NOT NULL DEFAULT 0,
    max_attempts     INT  NOT NULL DEFAULT 5,
    run_after        TIMESTAMPTZ NOT NULL DEFAULT now(),
    locked_at        TIMESTAMPTZ NULL,
    locked_by        TEXT NULL,
    last_error       TEXT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- the claim query's index
CREATE INDEX idx_jobs_claimable ON jobs (status, run_after)
    WHERE status IN ('PENDING', 'RUNNING');

-- idempotency
CREATE UNIQUE INDEX idx_jobs_idempotency ON jobs (idempotency_key)
    WHERE idempotency_key IS NOT NULL;
```

`tenant_id` is nullable here because some jobs are system-level. This is the one documented
exception to the "every table has NOT NULL tenant_id" rule — note it in the migration.

---

## The claim query — the heart of it

```sql
UPDATE jobs
SET    status    = 'RUNNING',
       locked_at = now(),
       locked_by = :workerId,
       attempts  = attempts + 1,
       updated_at = now()
WHERE  id IN (
    SELECT id FROM jobs
    WHERE  (status = 'PENDING' AND run_after <= now())
       OR  (status = 'RUNNING' AND locked_at < now() - INTERVAL '5 minutes')
    ORDER BY run_after
    FOR UPDATE SKIP LOCKED
    LIMIT  :batchSize
)
RETURNING *;
```

**Why each piece matters:**

- `FOR UPDATE` locks the selected rows for this transaction.
- `SKIP LOCKED` means a second worker skips locked rows instead of blocking. This is what
  makes concurrent workers safe and non-blocking. Without it, workers serialise.
- The `RUNNING AND locked_at < now() - 5 min` clause is **stale-lock recovery**: a worker that
  crashed mid-job left the row `RUNNING` forever. After the timeout it becomes claimable again.
  Without this clause, every crash permanently strands a job.
- `attempts + 1` on claim, not on failure — so a worker that dies without reporting still
  consumes an attempt and cannot loop forever.
- `ORDER BY run_after` gives rough FIFO.

---

## Retry and backoff

```java
Duration backoff(int attempts) {
    long base = (long) Math.pow(2, attempts);          // 2, 4, 8, 16, 32 s
    long capped = Math.min(base, 900);                 // cap at 15 min
    long jitter = ThreadLocalRandom.current().nextLong(capped / 4 + 1);
    return Duration.ofSeconds(capped + jitter);
}
```

Jitter matters: without it, a Meta outage causes every failed job to retry at the same instant,
which is a self-inflicted thundering herd.

On failure:
```text
attempts < max_attempts  → status = PENDING, run_after = now() + backoff(attempts)
attempts >= max_attempts → status = DEAD
PermanentJobException    → status = DEAD immediately, regardless of attempts
```

**Retryable vs permanent — get this right:**

| Retryable | Permanent (`PermanentJobException`) |
|---|---|
| HTTP 429 rate limit | Invalid template name |
| HTTP 5xx | Recipient not on WhatsApp |
| Connection timeout | Permission error (Meta code 200) |
| Meta transient errors | Malformed payload |
| DB deadlock | Token revoked (needs human action) |

Retrying a permanent failure wastes attempts and delays the dead-letter signal you actually
need to see.

---

## Dead letter handling

`DEAD` is a terminal status, not a deletion. Then:

- A daily job counts `DEAD` jobs per type and alerts if above a threshold
- The support runbook (`10-OPERATIONS/PRODUCTION-RUNBOOK.md`) covers investigation
- Requeue manually after fixing the cause: reset to `PENDING`, `attempts = 0`
- Never auto-requeue `DEAD` jobs. They are dead for a reason and looping them hides the bug.

---

## Idempotency

```java
jobService.enqueue(
    "SEND_WHATSAPP_MESSAGE",
    payload,
    "reply:" + inboundWamid + ":" + ruleId   // deterministic
);
```

The unique index means a second enqueue with the same key is a no-op (catch the constraint
violation and return the existing job). Keys must be **deterministic** — derived from stable
identifiers, never from `UUID.randomUUID()` or a timestamp.

Suggested key patterns:

| Job | Key |
|---|---|
| Automation reply | `reply:{inbound_wamid}:{rule_id}` |
| Scheduled message | `sched:{scheduled_message_id}` |
| Template sync | `tplsync:{waba_id}:{yyyy-MM-dd}` |
| Webhook processing | `wh:{webhook_event_id}` |

---

## The worker

```java
@Component
@Profile("worker")          // ← only polls under the worker profile
public class JobWorker {

    @Scheduled(fixedDelayString = "${jobs.poll-interval-ms:1000}")
    public void poll() {
        List<Job> claimed = jobRepository.claimBatch(workerId, batchSize);
        for (Job job : claimed) {
            JobHandler handler = handlers.get(job.jobType());
            // each job in its own transaction — one failure must not roll back the batch
        }
    }
}
```

Handlers register themselves via Spring, so adding a job type needs no change to the worker:

```java
public interface JobHandler {
    String jobType();
    void handle(Job job);       // throw PermanentJobException for no-retry failures
}
```

**`@Profile("worker")` is why the same JAR does both roles.** The `web` profile runs no
poller; the `worker` profile serves no HTTP. Later, scaling workers is `systemctl start
wasaas-worker@2` — not a rewrite.

---

## Capacity

Postgres `SKIP LOCKED` handles thousands of jobs per minute on modest hardware. Our peak
projection at 1,000 customers is ~500/minute. **Roughly four orders of magnitude of headroom.**

Housekeeping that will matter eventually: archive `SUCCEEDED` jobs older than 7 days to
`jobs_archive` to prevent table bloat, and keep autovacuum aggressive on this table.

## DO NOT BUILD YET

Job priorities · cron expression scheduling · a jobs admin UI · workflow/DAG orchestration ·
distributed locks · Kafka/RabbitMQ. Triggers in `12-SCALING/WHEN-TO-INTRODUCE-MESSAGE-BROKER.md`.
