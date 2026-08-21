# Infrastructure Overview

## The whole thing

```text
                    Customers (browsers)
                            │
            ┌───────────────┴───────────────┐
            │                               │
   Cloudflare Pages                 Cloudflare DNS/proxy
   app.yourdomain.com                api.yourdomain.com
   (React SPA, free,                        │
    unlimited bandwidth)                    ▼
                              ┌──────────────────────────────┐
                              │  Oracle Cloud Always Free    │
                              │  ARM Ampere A1               │
                              │  2 OCPU / 12 GB / Ubuntu 24  │
                              │  Mumbai or Hyderabad         │
                              │                              │
                              │  Caddy (auto-TLS) :443       │
                              │       │                      │
                              │       ▼                      │
                              │  wasaas-web    (:8080)       │
                              │  wasaas-worker (same JAR)    │
                              │       │                      │
                              │       ▼                      │
                              │  PostgreSQL 17 (localhost)   │
                              └──────────────────────────────┘
                                   │              │
                    ┌──────────────┘              └──────────┐
                    ▼                                        ▼
          Cloudflare R2                            Backblaze B2
          (media, 10 GB free,                      (encrypted backups,
           zero egress fees)                        10 GB free)

  External: Meta Graph API · Razorpay · Brevo/Resend · Sentry · Better Stack
```

One VM. One process type run twice with different Spring profiles. One database on the same
box. That's the entire production estate.

## Monthly cost at MVP

| Item | Cost |
|---|---|
| Oracle Cloud VM | ₹0 (Always Free) |
| PostgreSQL | ₹0 (self-hosted on the VM) |
| Cloudflare Pages + DNS | ₹0 |
| Cloudflare R2 (10 GB) | ₹0 |
| Backblaze B2 (10 GB) | ₹0 |
| Sentry (5k errors/mo) | ₹0 |
| Better Stack (10 monitors) | ₹0 |
| Brevo (300 emails/day) | ₹0 |
| GitHub Actions (2,000 min/mo) | ₹0 |
| **Domain** | **~₹80–100/mo** (₹1,000/yr) |
| **Total infrastructure** | **~₹100/month** |

Unavoidable non-infrastructure costs: GST compliance via a CA **₹1,000–2,500/month** — larger
than your entire infrastructure bill. Budget for it. Razorpay fees only apply on revenue.

At 20 customers × ₹1,999 = ₹39,980 MRR, infrastructure is ~0.25% of revenue. That's the point
of this architecture.

## ⚠️ Oracle free tier — read this before you build on it

**Effective 15 June 2026, Oracle halved the ARM Always Free allocation** from 4 OCPU / 24 GB
to **2 OCPU / 12 GB**. There was no public announcement. Instances exceeding the new limits
were terminated on or after 18 August 2026.

What follows from that:

1. **Provision within 2 OCPU / 12 GB.** Verified sufficient for this workload — see
   `../12-SCALING/SCALING-0-20-CUSTOMERS.md` for the load math (20 customers ≈ 1.4 msg/min).
2. **Portability is a hard requirement.** `infra/provision.sh` must work on any Ubuntu 24.04
   VPS. If Oracle changes terms again, you move in an afternoon, not a fortnight.
3. **Never back up Oracle to Oracle.** Backups go to Backblaze B2 — a different vendor.
4. **Choose your home region at signup carefully — it cannot be changed.** Mumbai
   (`ap-mumbai-1`) or Hyderabad (`ap-hyderabad-1`) for Indian latency.
5. Oracle may reclaim instances it judges idle. A lightweight cron heartbeat mitigates this.

Untouched by the June 2026 change: 200 GB block storage, 2 AMD micro instances, 10 TB/month
egress.

**Fallback if Oracle becomes unusable:** Hetzner CX22 (~₹350/month) or a ₹500/month Indian
VPS. Your provisioning script means this is a one-hour migration. See
`../12-SCALING/REVENUE-FUNDED-INFRASTRUCTURE.md`.

## Free tiers evaluated and rejected

| Option | Why rejected |
|---|---|
| **Neon free Postgres** | 100 CU-hours/month/project. An always-on webhook receiver at 0.25 CU burns ~182 CU-hours/month — the project suspends around day 16. **Disqualifying for a webhook-driven product.** |
| Supabase free | 500 MB, and projects pause after 7 days of inactivity (tightened Feb 2026). Pausing is fatal for webhooks. |
| Fly.io / Heroku free | No longer exist |
| Render free | Spins down when idle — webhooks lost |
| **UptimeRobot free** | **Personal/non-commercial only since 1 December 2024.** Using it for a business violates their terms. Use Better Stack instead (10 monitors, 3-minute checks, commercial use permitted). |

## Two Spring profiles, one JAR

```text
wasaas-web.service     SPRING_PROFILES_ACTIVE=prod,web
                       Serves HTTP. Does NOT poll the job queue.

wasaas-worker.service  SPRING_PROFILES_ACTIVE=prod,worker
                       Polls the job queue. Serves no traffic.
```

Same artifact, same deploy, separate systemd units. A stuck job never blocks a webhook ACK,
and a traffic spike never starves the worker. You get most of the operational benefit of
separate services with none of the deployment complexity. See `../13-DECISIONS/ADR-001`.

## Read next

| Task | File |
|---|---|
| Create the VM | [ORACLE-CLOUD-SETUP.md](ORACLE-CLOUD-SETUP.md) |
| DNS, SPA hosting, R2 | [CLOUDFLARE-SETUP.md](CLOUDFLARE-SETUP.md) |
| Database install and tuning | [POSTGRES-SETUP.md](POSTGRES-SETUP.md) |
| TLS and reverse proxy | [CADDY-SETUP.md](CADDY-SETUP.md) |
| Backups | [BACKUP-SETUP.md](BACKUP-SETUP.md) |
| Errors, uptime, metrics | [MONITORING.md](MONITORING.md) |
| Automated deploys | [CI-CD.md](CI-CD.md) |
| The full runbook | [PRODUCTION-DEPLOYMENT.md](PRODUCTION-DEPLOYMENT.md) |
