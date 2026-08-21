# When to Introduce Redis

**Short answer: when you run more than one application instance. Not before.**

## Why not now

Redis is the most commonly added unnecessary dependency in small SaaS products. Reasons given are
usually "caching" and "sessions". Neither applies to you yet:

| Claimed need | Reality at MVP |
|---|---|
| Session storage | Spring Session JDBC in Postgres. Sessions survive restarts. Works with one instance *or* many. |
| Caching | Your queries are indexed and sub-millisecond. There is nothing slow to cache. |
| Rate limiting | Single instance → an in-process counter or a Postgres counter is correct |
| Job queue | Postgres `FOR UPDATE SKIP LOCKED` — see ADR-002 |
| Pub/sub | Spring's in-process `ApplicationEventPublisher` |
| Distributed locks | Nothing is distributed yet |

## The cost of adding it early

- Another process to install, configure, monitor, secure, and back up
- Another failure mode: Redis down → sessions gone → every user logged out
- Another 200–500 MB of RAM on a 12 GB box shared with Postgres and two JVMs
- Persistence configuration decisions (RDB vs AOF) you'd rather not make yet
- A cache-invalidation class of bug that doesn't currently exist in your product

**Caching adds a correctness risk.** Right now, if the database says a rule is disabled, the rule
is disabled. With a cache, that's true only eventually.

## The actual trigger

**You are about to run a second instance of the application.**

That happens at Stage 2 (splitting the worker onto its own box) or Stage 3 (multiple app
instances behind a load balancer).

At that moment, specific things break:

### 1. Per-account send throttle (F09) — the real one

F09 implements an in-process throttle to respect Meta's per-number rate limits. With two worker
instances, each has its own counter, so you can send at **2× the intended rate** and trip Meta's
limits — degrading your customer's quality rating and burning their money.

This is the concrete reason you'd add Redis. It's also why F09's prompt says to leave a TODO
pointing here.

### 2. Login rate limiting

Per-instance counters mean N instances allow N× the attempts. Postgres counters still work; Redis
is faster but not required.

### 3. Scheduled-job sweep locking

Two workers running `ENQUEUE_DUE_SCHEDULED_MESSAGES` simultaneously. Note: if your idempotency
keys are correct (`sched:{id}`), duplicate enqueues collapse to one job anyway — the deterministic
key is your real protection. A lock is a nicety, not the safety mechanism.

### 4. Sessions — actually still fine

Spring Session JDBC works across instances. You may move sessions to Redis for latency; you don't
have to.

## What NOT to use Redis for, even then

- **The job queue.** Postgres gives you transactional enqueue with your business writes. Moving
  the queue to Redis loses that and buys throughput you don't need. See ADR-002.
- **Primary data.** Ever.
- **Caching, until you've measured a slow query and failed to fix it with an index.**

## Migration when the time comes

Small, if the code was written correctly in the first place:

1. Managed Redis (~₹800–2,000/month). Do not self-host on the app box.
2. Replace the in-process throttle with a Redis-backed sliding window
3. Optionally move rate limiting and sessions
4. Add a health indicator for Redis
5. **Decide the failure mode explicitly**: if Redis is down, does the throttle fail open (risk
   over-sending) or closed (stop sending)? For a throttle protecting your customer's money and
   quality rating, **fail closed**.

That last decision is the one to think about carefully. Most teams default to fail-open and
discover the consequence during an incident.

## Checklist before adding Redis

- [ ] I am definitely running more than one instance
- [ ] I have named the specific shared state that breaks (not "it'll be faster")
- [ ] It isn't already solved by a deterministic idempotency key
- [ ] Managed, not self-hosted on the app box
- [ ] Health check and alert configured
- [ ] Failure mode decided and documented
- [ ] Recorded as an ADR in `../13-DECISIONS/`

If you can't complete the second box, you don't need Redis yet.
