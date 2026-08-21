# Production Deployment — Full Runbook

Increment **F22**. This is the end-to-end order for taking a bare VM to a live product.
Follow it top to bottom; each step assumes the previous one worked.

## Phase 1 — Accounts and DNS (do first; some steps have waiting periods)

| # | Action | Doc | Wait time |
|---|---|---|---|
| 1 | Register domain | [CLOUDFLARE-SETUP.md](CLOUDFLARE-SETUP.md) | up to 24h for DNS |
| 2 | Add domain to Cloudflare, change nameservers | ↑ | up to 24h |
| 3 | Create Oracle Cloud account (region: Mumbai/Hyderabad — **irreversible**) | [ORACLE-CLOUD-SETUP.md](ORACLE-CLOUD-SETUP.md) | minutes |
| 4 | Start Meta Business Verification | `../08-META-WHATSAPP/META-BUSINESS-SETUP.md` | **days to weeks** |
| 5 | Create Backblaze B2 bucket | [BACKUP-SETUP.md](BACKUP-SETUP.md) | minutes |
| 6 | Create Sentry + Better Stack accounts | [MONITORING.md](MONITORING.md) | minutes |
| 7 | Create Razorpay account, start KYC | `../01-BUSINESS/PRICING-AND-MONETIZATION.md` | **days** |

Steps 4 and 7 are the long poles. Start them on day one, in parallel with development.

## Phase 2 — The VM

| # | Action | Doc |
|---|---|---|
| 8 | Create ARM instance, 2 OCPU / 12 GB, Ubuntu 24.04 | [ORACLE-CLOUD-SETUP.md](ORACLE-CLOUD-SETUP.md) |
| 9 | Reserve a static public IP and attach it | ↑ |
| 10 | Security List: 443, 80 open; 22 to your IP only; 5432 closed | ↑ |
| 11 | Confirm iptables isn't silently dropping traffic | ↑ (the Ubuntu trap) |
| 12 | Point `api.yourdomain.com` A record at the IP, **DNS-only (grey)** | [CLOUDFLARE-SETUP.md](CLOUDFLARE-SETUP.md) |

## Phase 3 — Provision (`infra/provision.sh`)

Run the script. It should be idempotent — safe to re-run. In order, it does:

| # | Action | Doc |
|---|---|---|
| 13 | System update, `unattended-upgrades`, UFW (22/80/443), fail2ban | — |
| 14 | Create the `deploy` user; disable password SSH and root login | `../11-SECURITY-COMPLIANCE/SECURITY-REQUIREMENTS.md` |
| 15 | Install Temurin JDK 21 for **aarch64** | — |
| 16 | Install PostgreSQL 17, apply tuning, create DB and **non-superuser** app role | [POSTGRES-SETUP.md](POSTGRES-SETUP.md) |
| 17 | Enable `pgcrypto`, `pg_trgm`, `pg_stat_statements` | ↑ |
| 18 | Configure WAL archiving | [BACKUP-SETUP.md](BACKUP-SETUP.md) |
| 19 | Install Caddy, write the Caddyfile | [CADDY-SETUP.md](CADDY-SETUP.md) |
| 20 | Create `/opt/wasaas/{bin,releases,logs}`, and the root-owned `0600` env file | — |
| 21 | Install systemd units for `wasaas-web` and `wasaas-worker`, enable both | [INFRASTRUCTURE-OVERVIEW.md](INFRASTRUCTURE-OVERVIEW.md) |
| 22 | Create a swap file | — |
| 23 | Install `backup.sh`, `archive-wal.sh`, `restore.sh` + cron entries | [BACKUP-SETUP.md](BACKUP-SETUP.md) |
| 24 | Install the idle-prevention cron heartbeat | [ORACLE-CLOUD-SETUP.md](ORACLE-CLOUD-SETUP.md) |

**Target: under one hour, on a fresh VM, unattended.** If it isn't scripted, it isn't done —
when Oracle changes terms again you'll be glad.

## Phase 4 — Secrets

Populate `/opt/wasaas/env` (root-owned, `0600`). Never in Git, never in CI.

```bash
SPRING_PROFILES_ACTIVE=prod,web        # worker unit overrides this
DATABASE_URL=jdbc:postgresql://localhost:5432/wasaas
DATABASE_USER=wasaas_app
DATABASE_PASSWORD=
TOKEN_ENCRYPTION_KEY=                  # base64, exactly 32 bytes
META_APP_ID=
META_APP_SECRET=                       # server only, never in the SPA
META_WEBHOOK_VERIFY_TOKEN=
META_GRAPH_VERSION=v21.0
RAZORPAY_KEY_ID=
RAZORPAY_KEY_SECRET=
RAZORPAY_WEBHOOK_SECRET=
R2_ACCESS_KEY_ID=
R2_SECRET_ACCESS_KEY=
R2_BUCKET=
R2_ENDPOINT=
B2_APPLICATION_KEY_ID=
B2_APPLICATION_KEY=
B2_BUCKET=
BACKUP_AGE_PUBLIC_KEY=
BREVO_API_KEY=
SENTRY_DSN=
BETTERSTACK_BACKUP_HEARTBEAT_URL=
BETTERSTACK_WORKER_HEARTBEAT_URL=
APP_VERSION=
```

Every value also goes in your password manager. The `age` **private** key goes there too —
and it must not live on this VM. See `../11-SECURITY-COMPLIANCE/SECRETS-MANAGEMENT.md`.

## Phase 5 — First deploy

| # | Action |
|---|---|
| 25 | Build the JAR locally: `./mvnw clean package` |
| 26 | `scp` it to `/opt/wasaas/releases/` |
| 27 | Run migrations (as the migrator role, not the app role) |
| 28 | `systemctl start wasaas-worker` → check `journalctl -u wasaas-worker` |
| 29 | `systemctl start wasaas-web` → check readiness |
| 30 | Start Caddy; watch for certificate issuance in its logs |
| 31 | `curl https://api.yourdomain.com/actuator/health` → 200, valid cert |

## Phase 6 — Frontend

| # | Action |
|---|---|
| 32 | Connect the repo to Cloudflare Pages; set `VITE_` vars |
| 33 | Deploy; confirm `app.yourdomain.com` loads |
| 34 | Confirm `_redirects` works — refresh on a deep route |
| 35 | Register a test account end to end |

## Phase 7 — Meta wiring

| # | Action | Doc |
|---|---|---|
| 36 | Set the webhook callback URL to `https://api.yourdomain.com/api/webhooks/whatsapp` | `../08-META-WHATSAPP/WEBHOOKS.md` |
| 37 | Complete Meta's verification handshake | ↑ |
| 38 | Subscribe webhook fields | ↑ |
| 39 | Connect **your own** test business via Embedded Signup | `../08-META-WHATSAPP/EMBEDDED-SIGNUP.md` |
| 40 | Send and receive a real message | — |

## Phase 8 — Safety net (do NOT skip; do NOT launch without it)

| # | Action | Doc |
|---|---|---|
| 41 | Confirm the nightly backup runs and lands in B2 | [BACKUP-SETUP.md](BACKUP-SETUP.md) |
| 42 | **Restore a backup into a scratch DB and verify it** | `../10-OPERATIONS/BACKUP-RESTORE-PROCEDURE.md` |
| 43 | Confirm WAL segments are arriving | [BACKUP-SETUP.md](BACKUP-SETUP.md) |
| 44 | Better Stack monitors live, including worker + backup heartbeats | [MONITORING.md](MONITORING.md) |
| 45 | Sentry live; scrubbing verified with a test error carrying a fake token | ↑ |
| 46 | Stop the app; confirm you're alerted within 3 minutes | ↑ |
| 47 | Reboot the VM; confirm both services come back automatically | — |
| 48 | Test a rollback deploy | [CI-CD.md](CI-CD.md) |

## Go-live checklist

- [ ] `https://api.yourdomain.com` — valid TLS, health 200
- [ ] `https://app.yourdomain.com` — loads, deep-route refresh works
- [ ] Register → login → connect WhatsApp → send → receive, all working
- [ ] App connects to Postgres as a **non-superuser** (`SELECT rolsuper` → false)
- [ ] RLS enabled on every tenant-scoped table
- [ ] Both systemd services survive a reboot
- [ ] Backup taken **and restored**, logged
- [ ] Monitoring alerts tested for real
- [ ] No secret in Git (`git log -p | grep -i` for your key names)
- [ ] Razorpay KYC complete; a ₹1 test payment succeeded
- [ ] Meta Business Verification complete; Advanced Access approved
- [ ] `../00-START-HERE/CURRENT-STATUS.md` updated

## If something breaks

Go to `../10-OPERATIONS/PRODUCTION-RUNBOOK.md`. Don't debug production from memory at 2am —
that's what the runbook is for.
