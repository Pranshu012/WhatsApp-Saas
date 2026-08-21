# When to Introduce a Message Broker

**Short answer: probably never for this product. Certainly not below 5,000 messages/second.**

Full rationale in `../13-DECISIONS/ADR-002-POSTGRES-JOB-QUEUE.md`.

## The headroom you actually have

PostgreSQL with `FOR UPDATE SKIP LOCKED` sustains **thousands of job claims per second** on
modest hardware.

Your load:

| Customers | Peak messages/second | % of Postgres capacity |
|---|---|---|
| 20 | 0.3 | ~0.01% |
| 100 | 1.7 | ~0.05% |
| 1,000 | 8 | ~0.3% |
| 10,000 | 80 | ~3% |

**At 10,000 customers you would still be using about 3% of what the Postgres queue can do.** You
would have ₹20,000,000 MRR. The queue is not your problem at any scale you can currently imagine.

## Why Postgres is genuinely better here (not just simpler)

**1. Transactional enqueue.** This is the decisive advantage.

```java
@Transactional
public void handleInbound(InboundMessage msg) {
    var conversation = conversationRepo.save(...);
    ledgerService.recordOutboundIntent(...);
    jobService.enqueue("SEND_WHATSAPP_MESSAGE", payload, "reply:" + msg.wamid());
    // all three commit together, or none do
}
```

With Kafka you get the dual-write problem: the database commits and the broker publish fails, or
vice versa. The standard fix is the **transactional outbox pattern** — which is a job table in
Postgres. You'd be building this exact design as a workaround for having added Kafka.

**2. One system to operate.** Kafka means brokers, a coordinator, partition rebalancing, consumer
group lag monitoring, and retention tuning. That's a part-time job for one person.

**3. Debuggability.** `SELECT * FROM jobs WHERE status='DEAD'` at 2am, in `psql`, from your phone
over SSH. Compare to inspecting a Kafka topic.

**4. Retries and dead-lettering come free.** `attempts`, `run_after`, `last_error` columns. In
Kafka you build retry topics and DLQ topics.

**5. Backups include the queue.** `pg_dump` captures in-flight jobs. A separate broker needs its
own backup and recovery story.

## The genuine triggers

Introduce a broker only when you observe, **in production, with data**:

| Trigger | Threshold |
|---|---|
| Sustained throughput | > 5,000 messages/second (≈ 600,000 customers) |
| Queue table contention | Lock waits on `jobs` despite correct indexing and tuning |
| **Multi-consumer fan-out** | One event genuinely needs 5+ independent consumers with independent retry and independent lag |
| Event sourcing | You've decided to make event streams your source of truth |
| Cross-team boundaries | Separate teams own separate services and need a contract |

**Multi-consumer fan-out is the only one plausibly reachable.** If a single inbound message must
trigger automation, analytics, a CRM sync, a notification, and a webhook to the customer's system
— each with its own retry semantics — a broker starts earning its cost. Even then, evaluate
Postgres `LISTEN/NOTIFY` plus multiple handler rows first.

## Cheaper things to try first

Before reaching for Kafka, in order:

1. **Add an index.** Most "queue is slow" reports are a missing index on `(status, run_after)`.
2. **More workers.** `SKIP LOCKED` scales horizontally with zero code change — that was the point.
3. **Batch the claim query.** Claim 20 jobs per poll instead of 1.
4. **Tune the poll interval.** Shorter for latency, longer for less DB chatter.
5. **Partition the `jobs` table** by status or created date.
6. **Separate hot and cold job types** into different tables if one type dominates.
7. **Vertical scaling.** 8 vCPU Postgres handles a lot more than 2.

Each is hours of work. Kafka is weeks, plus permanent operational cost.

## If you do it anyway

- Managed (Confluent Cloud, AWS MSK, Redpanda Cloud). Never self-host Kafka as a small team.
- Keep the outbox pattern — write to Postgres in the transaction, relay to the broker
  asynchronously. Do not publish directly from business logic.
- Keep `message_ledger` in Postgres regardless. It's billing evidence and must be queryable and
  consistent.
- Write an ADR explaining what you measured, so the next person understands the decision.

## The honest summary

You will not need a message broker for this product at any scale you're likely to reach as a
bootstrapped solo founder. If you find yourself wanting one, the real reason is usually that it
feels more professional. It isn't — the professional choice is the one that matches the load.

`SELECT ... FOR UPDATE SKIP LOCKED` is a boring, correct, well-understood pattern used in
production by companies far larger than you plan to be.
