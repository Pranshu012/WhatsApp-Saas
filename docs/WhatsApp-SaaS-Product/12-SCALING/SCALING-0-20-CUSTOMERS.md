# Scaling: 0–20 Customers (Stage 1)

**MRR: ₹0 → ₹39,980. Infrastructure: ~₹100/month.**

## The load math

This is the number that justifies the entire architecture. Run it yourself; don't take it on
faith.

Assume a busy SMB: **100 inbound messages/day**, each producing ~1 automated reply.

```text
20 customers × 100 inbound/day        = 2,000 inbound/day
                                     + 2,000 outbound/day
                                     = 4,000 messages/day
4,000 ÷ 24 ÷ 60                       ≈ 2.8 messages/minute
Business hours concentration (~10h)   ≈ 6.7 messages/minute
Peak minute (3× average)              ≈ 20 messages/minute
```

**20 messages/minute is one message every three seconds.**

A single Spring Boot instance on 2 OCPU handles hundreds of requests per second. PostgreSQL with
`FOR UPDATE SKIP LOCKED` handles thousands of job claims per second. You are running at roughly
**0.1% of the platform's capacity.**

## What this means practically

You will hit **business** limits long before technical ones:

| Limit | Value | Binding at |
|---|---|---|
| **Meta onboarding rate** | 10 new business customers per rolling 7 days | ~40/month max |
| Your support capacity | ~10 hours/week solo | ~50 customers |
| Your sales capacity | Manual onboarding, 45 min each | ~10–15/month realistically |
| Postgres job throughput | Thousands/second | ~500,000 customers |
| Single JVM request capacity | Hundreds/second | Not in this decade |

The Meta onboarding limit rises to 200/week only after Business Verification + App Review +
Access Verification. Even then, your own time is the constraint.

## Infrastructure at this stage

Exactly what's in `../07-INFRASTRUCTURE/INFRASTRUCTURE-OVERVIEW.md`. Nothing more.

| Component | Spec | Utilisation at 20 customers |
|---|---|---|
| Oracle ARM VM | 2 OCPU / 12 GB | ~10–15% CPU, ~5 GB RAM |
| PostgreSQL 17 | Same box, `shared_buffers` 2 GB | Database under 1 GB |
| Job queue | Postgres `SKIP LOCKED` | Queue empty most of the time |
| Cloudflare Pages | Free | Trivial |

Storage growth: roughly 500 KB–2 MB per customer per month for messages and ledger rows. At 20
customers that's under 500 MB/year. Your 50 GB boot volume is fine for years.

## What to monitor (and the numbers that would surprise you)

| Metric | Expected | Investigate above |
|---|---|---|
| CPU | 10–15% | 40% sustained |
| RAM | ~5 GB | 9 GB |
| Job queue depth | 0–5 | 100, or oldest pending > 10 min |
| p99 webhook ACK | < 200 ms | 1 second |
| Database size | < 1 GB | 10 GB |
| Disk | < 30% | 70% |

If any of these is unexpectedly high at this stage, it is a **bug**, not a scaling problem.
Adding capacity would hide it. Find it.

## Do NOT add anything

This is the discipline that determines whether you reach 20 customers at all.

| Temptation | Reality at this stage |
|---|---|
| Redis | Nothing is cross-instance. Nothing needs a cache. |
| Kafka / RabbitMQ | Postgres queue runs at 0.1% capacity |
| Kubernetes | One process on one box |
| Microservices | You are one developer |
| Read replicas | Read load is trivial |
| Connection pooler (PgBouncer) | HikariCP with 30 total connections is fine |
| CDN for API | Cloudflare already fronts it |
| Elasticsearch | Postgres full-text handles your FAQ volume |
| Autoscaling | Nothing to scale |
| Multiple instances | You'd need Redis for sessions and rate limits — a whole new class of bug |

Every item above costs setup time, adds a failure mode, and consumes context you need for
product. See the individual `WHEN-TO-INTRODUCE-*.md` documents for the actual trigger conditions.

## Where your time should go instead

At this stage, engineering is **not** your bottleneck. In rough priority order:

1. **Getting customers.** 20 paying customers is the whole objective.
2. **Manual onboarding calls.** Each one tells you what to build next.
3. **Reviewing unmatched messages** across tenants — the honest input to "is deterministic
   matching enough?" (ADR-007)
4. **Closing open decisions** in `../13-DECISIONS/DECISIONS.md`
5. **The safety net**: a *tested* restore, working alerts, a rollback you've rehearsed
6. Only then: features

## Exit criteria for Stage 1

Move to Stage 2 thinking when:

- [ ] 20 paying customers, retained past month 2
- [ ] MRR ≈ ₹40,000
- [ ] Churn under 10%/month
- [ ] You know why customers stay (asked, not assumed)
- [ ] Support under 10 hours/week
- [ ] Restore tested; alerts proven
- [ ] Something is *actually* straining — not projected to strain

That last one matters. Migrate on evidence, never on anticipation.

Next: [SCALING-20-100-CUSTOMERS.md](SCALING-20-100-CUSTOMERS.md)
