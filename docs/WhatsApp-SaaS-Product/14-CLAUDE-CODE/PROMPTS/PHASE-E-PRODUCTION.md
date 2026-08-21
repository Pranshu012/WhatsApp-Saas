# Phase E Prompts — Production (F21–F23)

---

## F21 — Razorpay subscription billing

> **PILOT TRACK: skip this increment entirely.** During the pilot you collect ₹500/month by UPI
> manually — a WhatsApp message with your UPI ID on the 1st. No Razorpay, no KYC, no code.
> Build this in the post-pilot transition (see `../../00-START-HERE/PILOT-FIRST-PLAN.md`).

```text
Increment F21. Read docs/WhatsApp-SaaS-Product/01-BUSINESS/PRICING-AND-MONETIZATION.md.

Goal: collect Rs 1,999/month from customers. Our software fee only — Meta bills them
separately for messages.

Commercial context (verified 18 Aug 2026, re-verify before launch): Razorpay is 2% + 18% GST
= 2.36% effective on domestic cards and netbanking, but UPI is 0% under Rs 2,000 due to the
NPCI MDR waiver. That is exactly why the price is Rs 1,999. Make UPI the default and most
prominent payment path.

Requirements:
- Migration V15__subscriptions.sql: id, tenant_id NOT NULL, plan_code, status
  (TRIALING/ACTIVE/PAST_DUE/CANCELLED/EXPIRED), razorpay_subscription_id,
  current_period_start, current_period_end, trial_ends_at, cancelled_at, created_at,
  updated_at; RLS per V3
- Migration: payment_events table storing raw Razorpay webhook payloads, append-only
- Razorpay Subscriptions integration with UPI AutoPay preferred, cards as fallback
- Razorpay webhook endpoint with signature verification (same discipline as F10: verify
  before parsing, constant-time compare, persist raw, enqueue, ACK fast)
- Subscription state machine driven ONLY by verified webhooks, never by client callbacks.
  A client saying "payment succeeded" is not evidence.
- Feature gating: PAST_DUE and beyond → block outbound sends but never delete data and never
  block login or data export. Locking someone out of their own customer conversations over a
  failed card is how you earn a chargeback and a bad review.
- Dunning: retry schedule, email notifications on failure, grace period (configurable,
  suggest 7 days)
- GST invoice fields on the tenant: GSTIN, legal name, address. B2B customers need these for
  input credit — without it they will ask, every month.

Tests: subscription activates on verified webhook; unverified webhook rejected; PAST_DUE
blocks sends but not login or export; state changes only via webhook; duplicate webhook
idempotent.

Do NOT build: multiple plans, annual billing, coupons, proration, a billing portal beyond
current status and invoices.

Plan first. Flag anything about Indian GST invoicing you are unsure about rather than
guessing — I will confirm with my CA.

Finally: write docs/IMPLEMENTATION/F21-<slug>.md following
docs/WhatsApp-SaaS-Product/14-CLAUDE-CODE/IMPLEMENTATION-DOC-TEMPLATE.md, and add a row to
docs/IMPLEMENTATION/INDEX.md. Document the key decisions, any divergence from the architecture
docs, and the gotchas someone would trip over. This is part of the increment, not optional.
```

**DoD:** a real Rs 1 test payment activates a tenant · state driven only by verified webhooks · PAST_DUE degrades gracefully · GST fields captured

---

## F22 — Infrastructure as scripts

```text
Increment F22. Read docs/WhatsApp-SaaS-Product/07-INFRASTRUCTURE/PRODUCTION-DEPLOYMENT.md,
ORACLE-CLOUD-SETUP.md, CADDY-SETUP.md and POSTGRES-SETUP.md.

Goal: rebuild the entire production box from scratch, from a script, in under an hour.

Context: single Oracle Cloud Always Free ARM VM (2 OCPU / 12 GB, aarch64, Ubuntu 24.04) in
Mumbai or Hyderabad. Oracle halved this free tier in June 2026 with no announcement, so
portability is a hard requirement, not a nice-to-have. The script must work on any Ubuntu
VPS, not just Oracle.

Requirements:
- infra/provision.sh — idempotent, re-runnable, non-interactive:
  - system packages, unattended-upgrades, UFW (allow 22/80/443 only), fail2ban
  - a non-root deploy user
  - Temurin JDK 21 for aarch64
  - PostgreSQL 17: install, tune modestly for 12 GB RAM (document each setting's reasoning),
    create the app database and a NON-SUPERUSER app role (superusers bypass RLS — this is
    critical), local-only listen address
  - Caddy with automatic TLS for our domain, reverse proxy to 127.0.0.1:8080, security
    headers, request body size limit
  - systemd units: wasaas-web.service and wasaas-worker.service, both with the same JAR and
    different SPRING_PROFILES_ACTIVE, Restart=always, EnvironmentFile from a root-owned
    0600 env file
  - swap file (12 GB RAM is fine but the JVM plus Postgres on one box benefits from headroom)
  - a cron'd lightweight health task, since Oracle reclaims instances it judges idle
- infra/deploy.sh — build JAR, upload, restart services, health check, rollback on failure
- .github/workflows/deploy.yml — build, test, and deploy on push to main. Secrets from GitHub
  Secrets. Manual approval gate before production.
- infra/README.md — the exact runbook order for a bare VM

Everything configurable via variables at the top of the script. No hardcoded hostnames,
paths, or domains.

Do NOT build: Docker in production, Kubernetes, Terraform, Ansible, blue-green deploys,
multi-instance. One box, systemd, shell scripts.

Plan first.

Finally: write docs/IMPLEMENTATION/F22-<slug>.md following
docs/WhatsApp-SaaS-Product/14-CLAUDE-CODE/IMPLEMENTATION-DOC-TEMPLATE.md, and add a row to
docs/IMPLEMENTATION/INDEX.md. Document the key decisions, any divergence from the architecture
docs, and the gotchas someone would trip over. This is part of the increment, not optional.
```

**DoD:** script provisions a fresh VM end to end · app runs as non-superuser Postgres role · HTTPS valid on real domain · both services restart after reboot · deploy rolls back on failed health check

---

## F23 — Backups, monitoring, and a tested restore

```text
Increment F23. Read docs/WhatsApp-SaaS-Product/07-INFRASTRUCTURE/BACKUP-SETUP.md and
docs/WhatsApp-SaaS-Product/10-OPERATIONS/BACKUP-RESTORE-PROCEDURE.md.

Goal: survive losing the entire VM. This increment is not done until you have RESTORED a
backup, not merely taken one.

Requirements:
- infra/backup.sh: nightly pg_dump (custom format, compressed), plus WAL archiving for
  point-in-time recovery. Upload to Backblaze B2 — a DIFFERENT vendor from Oracle. Never
  back up Oracle to Oracle.
- Retention: 7 daily, 4 weekly, 3 monthly. Prune automatically.
- Encrypt backups at rest before upload (they contain customer conversation data — DPDP
  exposure). Document where the backup encryption key lives; it must NOT live only on the
  box being backed up.
- infra/restore.sh: restore a named backup into a scratch database and run verification
  queries (row counts per table, latest ledger timestamp, RLS policies present).
- Backup monitoring: a failed or missing backup must ALERT. A silent backup failure is the
  same as no backup. Emit a heartbeat to Better Stack on success; alert on missing heartbeat.
- Sentry: Spring Boot integration, environment tagging, release version, and a scrubbing
  config that strips tokens, passwords, phone numbers, and message bodies before send.
- Better Stack: uptime monitor on a real health endpoint (one that checks DB connectivity,
  not just that the process is alive), 3-minute interval, status page.
- Optional: Grafana Cloud agent for JVM and Postgres metrics. Skip if it slows you down.
- docs: append a RESTORE TEST LOG table to BACKUP-RESTORE-PROCEDURE.md with columns date,
  backup used, restore duration, verification result, issues found.

Do NOT build: cross-region replication, streaming replicas, a second VM, automated failover.

Plan first.

Finally: write docs/IMPLEMENTATION/F23-<slug>.md following
docs/WhatsApp-SaaS-Product/14-CLAUDE-CODE/IMPLEMENTATION-DOC-TEMPLATE.md, and add a row to
docs/IMPLEMENTATION/INDEX.md. Document the key decisions, any divergence from the architecture
docs, and the gotchas someone would trip over. This is part of the increment, not optional.
```

**DoD**
- [ ] Nightly backup lands in B2, encrypted
- [ ] **You have restored it into a scratch DB and verified row counts** — logged in the doc
- [ ] A deliberately broken backup run triggers an alert
- [ ] Sentry scrubbing verified: trigger a test error containing a fake token and confirm it's stripped
- [ ] Stopping the app alerts you within 3 minutes (test it)
- [ ] Health endpoint fails when Postgres is down

---

## After F23

You are ready for customer #1. Go to
`docs/WhatsApp-SaaS-Product/10-OPERATIONS/CUSTOMER-ONBOARDING.md`.

**Do not start building new features.** Re-read Golden Rules 1 and 2. The next 20 customers
tell you what to build; guessing does not.
