# ADR-002 — PostgreSQL as the Job Queue

**Status:** Accepted · 18 August 2026

## Context
We need durable async work: outbound WhatsApp sends, inbound webhook processing, scheduled
messages, template sync. Requirements: survive restarts, retry with backoff, never
double-send (a duplicate message spends our customer's real money), and be observable.

## Decision
A `jobs` table in PostgreSQL. Workers claim with
`SELECT ... FOR UPDATE SKIP LOCKED LIMIT n` inside a transaction. Exponential backoff with
jitter, a retry cap, a `DEAD` terminal status, stale-lock recovery via `locked_at` timeout,
and a unique `idempotency_key`.

No Redis. No Kafka. No RabbitMQ. No SQS.

## Why
- `SKIP LOCKED` (Postgres 9.5+) is a correct, well-understood queue primitive.
- The queue shares transactions with business data — enqueue and state change commit
  atomically. With an external broker you get the dual-write problem and need an outbox
  anyway, i.e. you end up with a Postgres table *plus* a broker.
- Zero additional infrastructure, zero additional cost, zero additional failure mode.
- Debugging is `SELECT * FROM jobs WHERE status = 'DEAD'` — not a broker UI.
- Capacity: comfortably thousands of jobs/minute on modest hardware. Our peak projection at
  1,000 customers is ~500/minute. Roughly 4 orders of magnitude of headroom.

## Alternatives considered
| Option | Rejected because |
|---|---|
| Redis + a queue library | New service to run, back up, and monitor; no transactional coupling; persistence semantics are a config trap |
| Kafka | Built for high-throughput event streaming and replay across consumer groups. We have one consumer and 70 msg/min. Operationally enormous. |
| RabbitMQ | Reasonable at scale, but still a second stateful service for a problem Postgres solves |
| Spring `@Async` / in-memory executor | Work is lost on restart. Unacceptable — losing a queued message loses a customer's message. |
| Quartz | Heavier than needed; still needs Postgres; adds abstraction over a 150-line solution |

## Consequences
**Positive:** one datastore, transactional enqueue, trivial inspection, no cost.
**Negative:** polling has latency (poll interval, so ~1s worst case — fine for messaging);
high write volume eventually causes table bloat, needing autovacuum tuning and archival of
completed jobs.

**Mitigation:** archive `SUCCEEDED` jobs older than N days to a `jobs_archive` table; index
carefully on `(status, run_after)`.

## When we would revisit
- Sustained throughput above ~10,000 messages/minute
- Multiple independent consumer groups needing the same event stream
- Fan-out to several downstream systems

See `12-SCALING/WHEN-TO-INTRODUCE-MESSAGE-BROKER.md`.
