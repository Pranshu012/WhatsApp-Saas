# Error Handling and Retry

**Classification: BUILD NOW.**

## Exception hierarchy

```java
DomainException (code, httpStatus)
├── NotFoundException            404
├── ConflictException            409
├── ValidationException          400
├── UnprocessableException       422
├── ForbiddenException           403
└── RateLimitedException         429

PermanentJobException            // job → DEAD, no retry
MetaTransientException           // job → retry
MetaPermanentException           // job → DEAD
```

## Global handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ApiError> domain(DomainException e) {
        // 4xx: log at WARN, no stack trace — these are expected
        log.warn("Domain error code={} msg={}", e.code(), e.getMessage());
        return ResponseEntity.status(e.status()).body(toApiError(e));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException e) { ... }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception e) {
        // 5xx: log at ERROR with stack trace, report to Sentry
        log.error("Unexpected error", e);
        return ResponseEntity.status(500).body(new ApiError(
            "INTERNAL_ERROR",
            "Something went wrong. Please try again.",   // never leak internals
            MDC.get("requestId"), Instant.now(), Map.of()));
    }
}
```

The distinction matters operationally: 4xx at WARN without stack traces keeps your logs readable
and your Sentry quota (5,000 errors/month free) spent on real bugs rather than validation
failures.

## Customer-readable messages

| Internal | What the customer sees |
|---|---|
| Meta 131026 | "This number isn't on WhatsApp." |
| Meta 131047 | "The 24-hour reply window has closed. Send an approved template instead." |
| Meta 190 | "Your WhatsApp connection needs to be renewed. Please reconnect." |
| Meta 200 | "There's a configuration issue on our side. We've been notified." |
| Template not approved | "This template is still awaiting Meta's approval." |
| Subscription `PAST_DUE` | "Your subscription payment failed. Sending is paused — your data is safe." |

Meta error 200 is **our** problem, not the customer's, so the message says so and it should
alert you, not them.

## Retry decision table

| Failure | Retry? | Notes |
|---|---|---|
| HTTP 429 from Meta | ✅ Backoff | Respect `Retry-After` if present |
| HTTP 5xx from Meta | ✅ Backoff | |
| Timeout / connection reset | ✅ Backoff | |
| Meta 130429, 131048 | ✅ Backoff hard | 131048 also means check quality rating |
| Meta 1, 2 (internal) | ✅ | |
| DB deadlock / serialisation failure | ✅ Immediate, then backoff | |
| Meta 131026 (not on WhatsApp) | ❌ | Retrying won't put them on WhatsApp |
| Meta 190 (token revoked) | ❌ | Needs human action |
| Meta 200 (no Advanced Access) | ❌ | Deployment problem — alert us |
| Meta 132000/132001 (template) | ❌ | Fix the config |
| Validation failure | ❌ | |
| Unknown `phone_number_id` | ❌ | |

Getting the ❌ column right is what stops five pointless retries per bad number and surfaces the
dead-letter signal you actually need.

## Backoff

```java
Duration backoff(int attempts) {
    long secs   = Math.min((long) Math.pow(2, attempts), 900);
    long jitter = ThreadLocalRandom.current().nextLong(secs / 4 + 1);
    return Duration.ofSeconds(secs + jitter);
}
// 2s, 4s, 8s, 16s, 32s ... capped at 15 min, ± jitter
```

Jitter is not decoration. During a Meta outage, every failed job retries at the same moment
without it — a self-inflicted thundering herd exactly when the upstream is already struggling.

## Dead letter

`DEAD` is terminal but not deleted.

```sql
-- daily alert query
SELECT job_type, count(*) FROM jobs
WHERE status = 'DEAD' AND updated_at > now() - INTERVAL '1 day'
GROUP BY job_type;
```

Runbook: investigate the cause, fix it, then requeue **manually** (`status='PENDING'`,
`attempts=0`). Never auto-requeue — a job that dies repeatedly is telling you something, and
looping it hides the message.

## Circuit breaker — BUILD LATER

Resilience4j around the Meta client, so a sustained Meta outage fails fast instead of consuming
every worker thread on timeouts.

**Not in the MVP.** With one worker and backoff already in place, the queue simply drains slowly,
which is acceptable. Add it when you run multiple workers and a stuck upstream could starve them.

## Logging rules

```java
log.warn("Send failed tenantId={} ledgerId={} metaCode={} attempt={}", ...);   // GOOD
log.error("Failed: {}", exception.getMessage());                                // BAD — may contain a token
```

Exception messages from HTTP clients sometimes include request headers. Never log a raw exception
message from the Meta client — log the mapped error code. And configure Sentry's `beforeSend` to
scrub token-shaped strings, then **test that scrubbing** with a deliberate fake token.

## Test cases

| Test | Expect |
|---|---|
| Each Meta code → correct class | Parameterised test over the table above |
| Transient → retries with growing delay | — |
| Permanent → DEAD, `attempts` not exhausted | — |
| Backoff caps at 15 min | — |
| Jitter present | Two computations differ |
| 4xx logged at WARN without stack trace | — |
| 500 response leaks nothing internal | Assert the body |
| Token never in a log line | Capture appender output |
