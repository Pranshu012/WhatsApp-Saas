# Scaling Strategy — Overview

**Detail per stage in `12-SCALING/`.** This is the summary and the philosophy.

## The load reality

| Customers | Messages/month | Average/min | Peak/min |
|---|---|---|---|
| 20 | 60,000 | ~1.4 | ~50 |
| 100 | 300,000 | ~7 | ~150 |
| 1,000 | 3,000,000 | ~70 | ~500 |

A single Spring Boot instance on 2 ARM cores is **idle** at 500 messages/minute. Postgres
doesn't notice 3M inserts per month.

**You will hit business limits long before technical ones:** support capacity, sales
throughput, Meta quality ratings, onboarding time, churn. Any architecture decision justified
by "but what about scale" is almost certainly wrong for the next three years.

## What changes, and when

| Component | 0–20 | 20–100 | 100–1,000 | Trigger to change |
|---|---|---|---|---|
| App instances | 1 process | 1 web + 1 worker | 2+ web, 2+ worker | **Zero-downtime deploys**, not CPU |
| Codebase | Modular monolith | Same | **Same** | Never |
| Database | Self-hosted on app VM | Self-hosted, tuned | Managed + PITR + replica | "I can't lose 24h of data" |
| Queue | Postgres `SKIP LOCKED` | Same | **Same** | >10k msg/min or multi-consumer fan-out |
| Cache | Caffeine | Caffeine | Redis | Cross-instance rate limiting |
| Sessions | Postgres | Postgres | Postgres or Redis | Optional |
| Monitoring | Sentry + Better Stack | + Grafana free | Grafana paid, per-tenant dashboards | ~100 customers |
| Deploys | scp + restart | Same | Rolling / blue-green | 2+ instances |
| Environments | prod | prod | prod + staging | First customer-visible regression |
| Infra cost | ₹100/mo | ₹2,000–6,000 | ₹15,000–40,000 | Keep at 3–8% of MRR |

## The one design choice that makes this work

**Web/worker split via Spring profile.** The same JAR runs as an API server or a job consumer
depending on `--spring.profiles.active`. Scaling the message pipeline — the only part that will
ever need it — is running more worker processes.

No microservices. No split codebase. No split database. No distributed tracing across network
boundaries. A config change.

## Never needed at 1,000 customers

Kubernetes · microservices · Kafka · service mesh · event sourcing/CQRS · multi-region ·
sharding · GraphQL federation · a data warehouse.

## Spending rule

**Infrastructure should be 3–8% of MRR.** The first rupees above ₹0 go to:
1. **Not losing data** (managed Postgres with PITR)
2. **Knowing when it's broken** (monitoring)

In that order. Everything else waits for a specific triggering incident.

See `12-SCALING/REVENUE-FUNDED-INFRASTRUCTURE.md` for the revenue-indexed plan.
