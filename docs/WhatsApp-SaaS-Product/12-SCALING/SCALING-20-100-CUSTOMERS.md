# Scaling: 20–100 Customers (Stage 2)

**MRR: ₹40,000 → ₹200,000. Infrastructure budget: ₹1,000–5,000/month.**

You now have revenue. Spend a little of it to remove risk — not to add capability you don't need.

## The load math

```text
100 customers × 100 inbound/day       = 10,000 inbound/day
                                      + 10,000 outbound/day
                                      = 20,000 messages/day
                                      ≈ 14 messages/minute average
Business hours (~10h)                 ≈ 33/minute
Peak minute                           ≈ 100/minute
```

**100 messages/minute is under 2 per second.** Still roughly 1% of what a single instance and
Postgres queue handle comfortably.

Your 2 OCPU / 12 GB box is still not the constraint. **Your time is.**

## What actually changes

| Pressure | Reality |
|---|---|
| Support volume | 100 customers ≈ 15–25 hours/week. **This binds first.** |
| Single point of failure | An outage now costs ₹200k MRR of goodwill, not ₹0 |
| Manual onboarding | 45 min × 10/month = a part-time job |
| Database size | ~5–15 GB. Fine, but back it up properly. |
| Oracle free-tier dependency | Now a business risk, not a hobby risk |

## Spend money here, in this order

### 1. Leave the Oracle free tier — ₹350–800/month

The highest-value spend at this stage. Oracle **halved** the ARM Always Free allocation in June
2026 with no public announcement, and terminated non-compliant instances. That is not a vendor to
build ₹200k MRR on for free.

Move to a paid VPS with the same `provision.sh`:

| Option | Spec | Cost |
|---|---|---|
| Hetzner CX22 | 2 vCPU / 4 GB | ~₹350/mo |
| Hetzner CX32 | 4 vCPU / 8 GB | ~₹700/mo |
| DigitalOcean / Linode | 4 vCPU / 8 GB | ~₹1,700/mo |
| Oracle Pay-As-You-Go | Same box, no reclaim risk | Variable |

Because provisioning is scripted, this is an afternoon's work. That's the payoff for F22.

Note: Hetzner has no Indian region — latency to Indian users rises. Weigh it against an Indian
VPS provider if it matters.

### 2. Managed PostgreSQL — ₹0–1,500/month

Now that paid tiers are cheap relative to MRR, consider offloading database operations. But
self-hosted on a paid VPS is still perfectly defensible — see ADR-006. The trigger is *your
willingness to be on call for the database*, not throughput.

### 3. A second small box for the worker — ₹350/month

Not for capacity. For **blast radius**: a runaway job or an OOM on the worker no longer takes
down webhook ingestion.

⚠️ **This is your first multi-instance moment.** Sessions are already in Postgres (fine), but
your **in-process per-account send throttle** (F09) is no longer correct across two instances.
Read [WHEN-TO-INTRODUCE-REDIS.md](WHEN-TO-INTRODUCE-REDIS.md) before doing this.

### 4. Automate onboarding — your time, not money

At 10 new customers/month, self-serve onboarding pays for itself. Now — not before — you know
exactly which three steps people get stuck on, because you watched 20 people do it.

### 5. Paid monitoring / status page — ₹0–1,000/month

Better Stack's free tier still covers you. A public status page becomes worth having.

## What still doesn't change

| Still not needed | Why |
|---|---|
| Kafka / RabbitMQ | Postgres queue at ~2 msg/sec vs thousands/sec capacity |
| Kubernetes | Two boxes and systemd |
| Microservices | Still one developer, or two |
| Read replicas | Read load remains trivial |
| Elasticsearch | Postgres full-text fine for FAQ volumes |
| Sharding | Database under 20 GB |

## Now worth doing (engineering)

- [ ] **Rehearse the restore quarterly**, not just monthly-in-theory
- [ ] `EXPLAIN ANALYZE` your top queries against **real** data volumes — the first real index
      surprises appear around here
- [ ] Archive `webhook_events` older than 90 days (also a DPDP win)
- [ ] Partition `message_ledger` by month **only if** queries measurably slow — check first
- [ ] Per-tenant rate limiting to stop one customer's burst affecting others
- [ ] A tenant-level kill switch for support ("pause this tenant's automation")
- [ ] Better job observability — a simple admin view of dead jobs beats `psql` at 2am

## Watch for these signals

| Signal | Meaning | Action |
|---|---|---|
| CPU sustained > 60% | Real pressure, finally | Profile first — it's usually one bad query |
| Job queue depth persistently > 500 | Worker can't keep up | Check handler latency before adding workers |
| p99 webhook ACK > 1s | Ingest path doing too much | Something crept into the controller |
| Postgres connections near max | Pool misconfigured | Fix the pool, don't raise `max_connections` |
| Support > 25 hrs/week | **The real limit** | Hire, or fix the top ticket category |

"Profile first" is the important habit. On a small box, almost every apparent capacity problem is
one missing index or one unbounded query.

## Exit criteria for Stage 2

- [ ] 100 paying customers, MRR ~₹200,000
- [ ] Off the Oracle free tier
- [ ] Worker isolated from web
- [ ] Self-serve onboarding working
- [ ] Support under 25 hours/week (or a hire made)
- [ ] Restore rehearsed at least quarterly
- [ ] Real evidence of a technical constraint — measured, not projected

Next: [SCALING-100-1000-CUSTOMERS.md](SCALING-100-1000-CUSTOMERS.md)
