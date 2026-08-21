# Scaling: 100–1,000 Customers (Stage 3)

**MRR: ₹200,000 → ₹2,000,000. Infrastructure budget: ₹15,000–40,000/month.**

> **Note on the budget.** An earlier estimate put this stage at ₹5,000–15,000/month. That is too
> low once you include a managed database, redundancy, a staging environment, paid monitoring, and
> a real backup strategy. Budget **₹15,000–40,000** — still only 1–2% of MRR.

## The load math

```text
1,000 customers × 100 inbound/day     = 100,000 inbound/day
                                      + 100,000 outbound/day
                                      = 200,000 messages/day
                                      ≈ 140 messages/minute average
Business hours (~10h)                 ≈ 330/minute
Peak minute                           ≈ 500/minute  ≈ 8/second
```

**8 messages/second at peak.** A tuned Postgres job queue handles this without noticing. A single
well-sized instance handles the HTTP load.

The honest conclusion: **even at 1,000 customers, the original architecture holds.** What changes
is your tolerance for downtime, not your throughput ceiling.

## What actually forces change

| Driver | Why it forces change |
|---|---|
| **Availability expectations** | ₹2M MRR means an outage is a serious event. Single box is no longer acceptable. |
| **Team size** | 3–8 people. Coordination, not throughput, is the constraint. |
| **Database operations** | Someone must own backups, upgrades, tuning, PITR. That someone should be a vendor. |
| **Multi-instance requirements** | Once you run 2+ app instances, shared state becomes real |
| **Compliance** | Enterprise customers ask for SOC 2, pen-test reports, uptime SLAs |

## Target architecture

```text
                Cloudflare
                    │
            ┌───────┴───────┐
            ▼               ▼
      app-instance-1  app-instance-2      (web profile, behind a load balancer)
            │               │
            └───────┬───────┘
                    ▼
              Redis (sessions, rate limits, distributed locks)
                    │
            ┌───────┴───────┐
            ▼               ▼
      worker-1        worker-2            (worker profile)
            └───────┬───────┘
                    ▼
        Managed PostgreSQL (primary + read replica, PITR, automated backups)
```

Still a **modular monolith**, deployed multiple times. Not microservices. See
[WHEN-TO-INTRODUCE-MICROSERVICES.md](WHEN-TO-INTRODUCE-MICROSERVICES.md).

## Budget breakdown

| Item | Monthly |
|---|---|
| 2 app instances (4 vCPU / 8 GB each) | ₹3,000 |
| 2 worker instances (2 vCPU / 4 GB each) | ₹1,400 |
| Managed PostgreSQL + read replica | ₹8,000–20,000 |
| Managed Redis (small) | ₹800–2,000 |
| Load balancer | ₹1,000 |
| Staging environment | ₹2,000 |
| Monitoring (paid tiers) | ₹2,000–4,000 |
| Object storage + egress | ₹500–1,500 |
| **Total** | **₹19,000–35,000** |

Roughly **1–2% of MRR.** The ratio you've maintained since ₹100/month.

## Order of introduction

Do these in order. Each has a prerequisite.

**1. Managed PostgreSQL.** Highest value. Removes the operational burden you'd otherwise carry
personally, and gives you PITR and replicas without work. Migrate with `pg_dump`/restore during a
maintenance window.

**2. Redis — only now.** Required *because* you're going multi-instance:
- Sessions (or keep them in Postgres — that still works)
- **Per-account send throttles** (the F09 in-process throttle is now wrong)
- Distributed locks for scheduled-job sweeps
Read [WHEN-TO-INTRODUCE-REDIS.md](WHEN-TO-INTRODUCE-REDIS.md) first.

**3. Multiple app instances + load balancer.** Now safe, because shared state has a home.

**4. Multiple workers.** `SKIP LOCKED` already makes this safe — that was the point of ADR-002.
No code change needed.

**5. Read replica.** Point dashboard and analytics queries at it. Never point writes or
ledger-before-send reads at a replica — replication lag would break idempotency guarantees.

**6. Staging environment.** With a team, you can no longer test in production.

## What still isn't needed

| Still no | Why |
|---|---|
| **Kafka** | 8 msg/sec peak. Postgres queue handles orders of magnitude more. See [WHEN-TO-INTRODUCE-MESSAGE-BROKER.md](WHEN-TO-INTRODUCE-MESSAGE-BROKER.md) |
| **Microservices** | A modular monolith deployed N times gives you scaling without distributed-systems tax |
| **Kubernetes** | 4–6 VMs with systemd and a deploy script. K8s when the team asks for it and can operate it. |
| **Sharding** | Database maybe 100–200 GB. Vertical scaling and partitioning cover it. |
| **Elasticsearch** | Postgres full-text still fine unless you build real search as a feature |

## Database work that becomes real here

- **Partition `message_ledger` by month.** It's your largest table. Partitioning makes retention
  a `DROP PARTITION` instead of a long `DELETE`.
- Archive `webhook_events` aggressively (90 days).
- Review indexes against real query patterns — `pg_stat_statements` and `pg_stat_user_indexes`
  will show unused indexes costing write throughput.
- Connection pooler (PgBouncer) once instance count × pool size approaches `max_connections`.

## Business changes at this stage

- **Meta onboarding**: 200/week ceiling now matters. Ensure Business Verification, App Review, and
  Access Verification are all complete.
- **Consider the credit-line model.** At this volume, becoming a Solution Partner with a Meta
  credit line lets you resell messaging with margin. Large scope — see ADR-005 — and it puts
  message cost on your P&L. A deliberate business decision, not an upgrade.
- **Pricing tiers** become sensible. ₹1,999 flat leaves money on the table at the top end.
  Note the ₹2,000 UPI MDR boundary — above it you pay 2.36% on those payments.
- **Hire support first, then engineering.** Support volume scales linearly with customers;
  engineering doesn't.

## Signals that you're wrong about not needing something

Be honest with yourself. Introduce the thing when you observe, with data:

| Introduce | When you observe |
|---|---|
| Kafka | Sustained > 5,000 msg/sec, or genuine multi-consumer event fan-out |
| Microservices | A specific module needs independent scaling **and** an independent team owns it |
| Kubernetes | > 20 instances, or the team already runs K8s well |
| Sharding | Single-instance vertical scaling exhausted (> 500 GB, > 64 vCPU) |
| Elasticsearch | Full-text search is a headline feature, not a fallback matcher |

Measured, in production, on real data. Not projected on a whiteboard.

Next: [REVENUE-FUNDED-INFRASTRUCTURE.md](REVENUE-FUNDED-INFRASTRUCTURE.md)
