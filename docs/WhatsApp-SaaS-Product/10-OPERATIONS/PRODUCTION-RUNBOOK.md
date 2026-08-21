# Production Runbook

Written to be readable at 2am when you're half awake. Commands you can copy without thinking.

## Where things are

| Thing | Location |
|---|---|
| Application | `/opt/wasaas/releases/` (symlink `current`) |
| Env file | `/opt/wasaas/env` (root, `0600`) |
| Scripts | `/opt/wasaas/bin/` |
| Caddy config | `/etc/caddy/Caddyfile` |
| Postgres config | `/etc/postgresql/17/main/` |
| App logs | `journalctl -u wasaas-web` / `-u wasaas-worker` |
| Caddy logs | `/var/log/caddy/api.log` |
| Backups | Backblaze B2, `daily/` `weekly/` `monthly/` `wal/` |

## Everyday commands

```bash
# Status
systemctl status wasaas-web wasaas-worker postgresql caddy

# Logs (follow)
journalctl -u wasaas-web -f
journalctl -u wasaas-worker -f
journalctl -u wasaas-worker --since "1 hour ago" | grep -i error

# Restart (worker first if a migration changed job payloads)
sudo systemctl restart wasaas-worker
sudo systemctl restart wasaas-web

# Health
curl -s https://api.yourdomain.com/actuator/health | jq
curl -s localhost:8080/actuator/health/readiness | jq

# Resources
df -h && free -h && uptime

# Caddy
sudo caddy validate --config /etc/caddy/Caddyfile
sudo systemctl reload caddy

# Database
sudo -u postgres psql wasaas
```

## Diagnostic queries

```sql
-- Queue health — check this first for anything message-related
SELECT status, count(*) FROM jobs GROUP BY status;
SELECT min(created_at) AS oldest_pending FROM jobs WHERE status='PENDING';
SELECT job_type, last_error, count(*) FROM jobs WHERE status='DEAD'
GROUP BY job_type, last_error ORDER BY count(*) DESC;

-- Webhook freshness
SELECT max(received_at) FROM webhook_events;
SELECT count(*) FROM webhook_events WHERE signature_valid = false
  AND received_at > now() - interval '1 hour';

-- Message flow, last hour
SELECT direction, billing_category, status, count(*) FROM message_ledger
WHERE created_at > now() - interval '1 hour'
GROUP BY 1,2,3;

-- Tenant health
SELECT t.business_name, w.display_phone_number, w.quality_rating,
       w.messaging_limit_tier, w.status
FROM whatsapp_accounts w JOIN tenants t ON t.id = w.tenant_id;

-- Safety controls (run after ANY restore or migration)
SELECT rolsuper FROM pg_roles WHERE rolname='wasaas_app';        -- must be false
SELECT tablename FROM pg_tables
WHERE schemaname='public' AND rowsecurity = false;               -- review every row

-- Slow queries
SELECT calls, round(mean_exec_time::numeric,1) AS avg_ms, query
FROM pg_stat_statements ORDER BY total_exec_time DESC LIMIT 10;

-- Connections
SELECT count(*), state FROM pg_stat_activity GROUP BY state;
```

## Deploy and rollback

```bash
# Deploy (normally via the approval-gated GitHub Action)
/opt/wasaas/bin/deploy.sh

# Manual rollback to the previous release
ls -lt /opt/wasaas/releases/
sudo ln -sfn /opt/wasaas/releases/<previous>.jar /opt/wasaas/current.jar
sudo systemctl restart wasaas-worker wasaas-web
curl -s localhost:8080/actuator/health/readiness | jq
```

Rollback does **not** undo a migration. That's why migrations must be backward compatible with
the previous release — see `../07-INFRASTRUCTURE/CI-CD.md`.

## Symptom → action

| Symptom | Go to |
|---|---|
| Health check failing | `INCIDENT-RESPONSE.md` → API down |
| Messages not sending | `INCIDENT-RESPONSE.md` → Messages not sending. Check the **worker** first. |
| No webhooks arriving | `INCIDENT-RESPONSE.md` → Webhooks |
| Suspected data leak | `INCIDENT-RESPONSE.md` → SEV1. Take the endpoint down first. |
| Data loss | `BACKUP-RESTORE-PROCEDURE.md`. Restore to **scratch**, never over live. |
| Disk full | Clear `/var/log/caddy`, old WAL, old releases. Then find what grew. |
| One customer's messages failing | Their payment method / quality rating — `CUSTOMER-SUPPORT.md` |
| All Meta calls fail with error 200 | Your app lost Advanced Access → Meta App Review |
| All Meta calls fail with error 190 | Tokens expired → tenants must reconnect |
| Razorpay signature failures | Secret rotated, or probing. Never disable verification. |

## Emergency contacts and accounts

Keep these in your password manager, not here:

- Oracle Cloud console + which region (irreversible at signup)
- Cloudflare (DNS, Pages, R2)
- Backblaze B2 + **the `age` private key** (must not live on the VM)
- Meta Business Manager / App dashboard
- Razorpay dashboard
- Sentry, Better Stack, Brevo
- Domain registrar
- Your CA (GST filing, ₹1,000–2,500/month)

## Post-incident log

Append every incident. Timeline, root cause, why detection was slow, and — the part that
matters — the test or alert that now exists so it can't recur silently.

| Date | Sev | Summary | Root cause | Detection gap | Test/alert added |
|---|---|---|---|---|---|
| | | | | | |
| | | | | | |

If you can't fill the last column, the incident isn't closed.
