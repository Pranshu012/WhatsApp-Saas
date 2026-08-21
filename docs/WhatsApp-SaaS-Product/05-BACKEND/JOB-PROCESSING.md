# Job Processing — Implementation

**Classification: BUILD NOW (F07).** Design in `03-ARCHITECTURE/BACKGROUND-JOBS.md`.

## Interfaces

```java
public interface JobHandler {
    String jobType();
    void handle(Job job) throws Exception;   // throw PermanentJobException for no-retry
}

public class PermanentJobException extends RuntimeException { ... }
```

Handlers are Spring beans; a registry maps `jobType()` → handler at startup. Adding a job type
requires zero changes to the worker.

```java
@Component
public class JobHandlerRegistry {
    private final Map<String, JobHandler> byType;

    public JobHandlerRegistry(List<JobHandler> handlers) {
        this.byType = handlers.stream()
            .collect(toMap(JobHandler::jobType, identity()));
    }
    public JobHandler get(String type) { ... }
}
```

Fail startup if two handlers claim the same `jobType` — `toMap` throws on duplicate keys, which
is the behaviour you want.

## Claiming

```java
@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    @Modifying
    @Query(value = """
        UPDATE jobs SET status = 'RUNNING', locked_at = now(), locked_by = :workerId,
                        attempts = attempts + 1, updated_at = now()
        WHERE id IN (
            SELECT id FROM jobs
            WHERE (status = 'PENDING' AND run_after <= now())
               OR (status = 'RUNNING' AND locked_at < now() - make_interval(secs => :lockTimeoutSecs))
            ORDER BY run_after
            FOR UPDATE SKIP LOCKED
            LIMIT :batchSize
        )
        RETURNING *
        """, nativeQuery = true)
    List<Job> claimBatch(String workerId, int lockTimeoutSecs, int batchSize);
}
```

## The worker

```java
@Component
@Profile("worker")                       // ← the whole web/worker split hinges on this
public class JobWorker {

    private final String workerId = hostname() + ":" + ProcessHandle.current().pid();

    @Scheduled(fixedDelayString = "${jobs.poll-interval-ms:1000}")
    public void poll() {
        List<Job> claimed = jobService.claimBatch(workerId);
        for (Job job : claimed) {
            runOne(job);                 // each job in its OWN transaction
        }
    }

    private void runOne(Job job) {
        try {
            registry.get(job.jobType()).handle(job);
            jobService.markSucceeded(job.id());
        } catch (PermanentJobException e) {
            jobService.markDead(job.id(), e.getMessage());
        } catch (Exception e) {
            jobService.markFailedForRetry(job.id(), e.getMessage());
        }
    }
}
```

**Each job in its own transaction.** If job 3 of a batch of 10 fails, jobs 1–2 must stay
committed. A single transaction around the batch would roll all of them back — and with
`attempts` already incremented, you'd get confusing partial reprocessing.

## Retry

```java
@Transactional
public void markFailedForRetry(UUID id, String error) {
    Job job = jobRepository.findById(id).orElseThrow();
    if (job.attempts() >= job.maxAttempts()) {
        job.markDead(error);
    } else {
        job.markPending(Instant.now().plus(backoff(job.attempts())), error);
    }
}

private Duration backoff(int attempts) {
    long secs   = Math.min((long) Math.pow(2, attempts), 900);          // cap 15 min
    long jitter = ThreadLocalRandom.current().nextLong(secs / 4 + 1);   // avoid herd
    return Duration.ofSeconds(secs + jitter);
}
```

## Handler example — with correct ordering

```java
@Component
class SendWhatsAppMessageHandler implements JobHandler {
    public String jobType() { return "SEND_WHATSAPP_MESSAGE"; }

    public void handle(Job job) {
        var cmd     = parse(job.payload());
        var account = accountService.requireConnected(cmd.tenantId());

        // 1. LEDGER FIRST — before the API call
        UUID ledgerId = ledgerService.recordOutboundIntent(cmd, account);

        try {
            var result = cloudClient.send(account, cmd);       // 2. call Meta
            ledgerService.attachWamid(ledgerId, result.wamid()); // 3. record outcome
        } catch (MetaPermanentException e) {
            ledgerService.recordFailure(ledgerId, e.code(), e.getMessage());
            throw new PermanentJobException(e.getMessage());     // no retry
        } catch (MetaTransientException e) {
            ledgerService.recordFailure(ledgerId, e.code(), e.getMessage());
            throw e;                                            // retry
        }
    }
}
```

Ledger-first is not stylistic. If we crash between the Meta call and the ledger write with the
opposite ordering, our customer was charged for a message we have no record of — the one thing
we cannot explain to them.

## Error classification

```java
// permanent — do not retry
131026  recipient not on WhatsApp
132000  template param count mismatch
132001  template does not exist
   200  app lacks Advanced Access  (a deployment problem, not a message problem)
   190  token invalid/expired      (needs human action — mark account TOKEN_EXPIRED)

// transient — retry with backoff
   130429  rate limit
   131048  spam rate limit
   1, 2    internal Meta errors
   HTTP 5xx, timeouts, connection resets
```

## Idempotency keys — deterministic only

| Job | Key |
|---|---|
| `SEND_WHATSAPP_MESSAGE` (automation reply) | `reply:{inbound_wamid}:{rule_id}` |
| `SEND_WHATSAPP_MESSAGE` (scheduled) | `sched:{scheduled_message_id}` |
| `PROCESS_WEBHOOK_EVENT` | `wh:{webhook_event_id}` |
| `SYNC_TEMPLATES` | `tplsync:{waba_id}:{yyyy-MM-dd}` |

Never `UUID.randomUUID()`, never a timestamp. The whole point is that a retried enqueue produces
the same key.

## Configuration

```yaml
jobs:
  poll-interval-ms: 1000
  batch-size: 10
  lock-timeout-secs: 300
  default-max-attempts: 5
```

## Test cases (F07)

| Test | Method |
|---|---|
| Two workers never double-claim | Real concurrent threads against Testcontainers Postgres — **not mocks** |
| Crash mid-job → re-claimable | Claim, don't complete, advance the clock past lock timeout |
| Retries back off then DEAD | Handler that always throws |
| `PermanentJobException` → immediate DEAD | Assert `attempts` not exhausted |
| Duplicate idempotency key → one job | Enqueue twice |
| Web profile does not poll | Start with `web`, assert no claims |

Test 1 must use genuine concurrency. Mocking `SKIP LOCKED` tests nothing — the whole point is
real database locking behaviour.
