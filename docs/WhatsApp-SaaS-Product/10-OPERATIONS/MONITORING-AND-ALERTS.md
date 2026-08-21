# Monitoring and Alerts — Operations View

Setup is in `../07-INFRASTRUCTURE/MONITORING.md`. This is what you *do* with it day to day.

## The five alerts that wake you up

Only these. Everything else is a log entry you review weekly.

| Alert | Source | Means | First action |
|---|---|---|---|
| API readiness failing 2× | Better Stack | Platform down or DB unreachable | `INCIDENT-RESPONSE.md` → API down |
| **Worker heartbeat missing** | Better Stack | No message has sent since it stopped | `systemctl restart wasaas-worker` |
| **Backup heartbeat missing** | Better Stack | You have no recovery path tonight | `BACKUP-RESTORE-PROCEDURE.md` |
| Job reached DEAD | App → Sentry | A message permanently failed | Check the error code |
| Webhook signature failures repeating | App → Sentry | Rotated secret, or an attack | Verify the app secret |

**The worker heartbeat is the one people omit.** The web app can be perfectly healthy while the
worker has been dead for six hours and not one message has been delivered. Your customers will
find out before you do without it.

## Alert fatigue is a real failure mode

As a solo founder, an alert you learn to ignore is worse than no alert — you'll ignore the real
one in the same breath. Start with those five. Add one only after an incident proves you needed
it. Remove any alert that fires more than twice without action.

## Daily — 2 minutes, with coffee

```sql
SELECT status, count(*) FROM jobs GROUP BY status;
```

- Any `DEAD`? Investigate.
- `PENDING` growing over yesterday? The worker is falling behind.
- Sentry: any new error *type*? (Volume of a known error matters less.)

## Weekly — 10 minutes, calendared

1. `df -h` — logs and WAL fill disks; this is the most common slow-motion outage
2. `pg_stat_statements` top 10 by total time — anything new or worse than last week?
3. Delivery failure rate per tenant — a spike is usually their contact list quality
4. Quality ratings across tenants — any YELLOW or RED? Contact that customer proactively.
5. Expired or expiring tokens — a customer whose token expired has silently dead automation
6. Unmatched-message rate across tenants — is deterministic matching holding? (ADR-007)
7. Sentry error budget — burning through 5,000/month means a loop, not a bigger plan

## Monthly — 30 minutes

- [ ] **Restore a backup into a scratch DB and verify it.** Log it. Non-negotiable.
- [ ] `./mvnw org.owasp:dependency-check-maven:check`
- [ ] Review `../00-START-HERE/ASSUMPTIONS-AND-EXPIRY-DATES.md` — Meta revises WhatsApp rates
      quarterly (1 Jan / 1 Apr / 1 Jul / 1 Oct); free tiers change without notice
- [ ] Check `whatsapp_rates` matches Meta's current published India rates
- [ ] Oracle instance still within Always Free limits
- [ ] Review support-ticket categories — which one is a product fix?

## Health indicators worth having

Beyond "is the process alive":

| Indicator | Degraded when | Why |
|---|---|---|
| DB connectivity | Query fails | The JVM stays up happily while Postgres is gone |
| Job queue depth | Oldest PENDING > 10 min | Early warning the worker is stuck |
| Dead jobs | Any in the last hour | Something permanently failed |
| Token health | Any tenant token expired | That customer's automation is silently dead |
| Webhook freshness | No event in 2 hours during business hours | Subscription may have been dropped |

The token and webhook-freshness ones catch **silent** failures — the dangerous kind, where
nothing errors and nothing works.

## What you deliberately don't monitor

APM traces · per-endpoint latency percentiles · JVM GC dashboards · custom Grafana boards ·
synthetic user-journey monitoring · log aggregation beyond `journalctl` + Sentry.

Every one of these is useful at scale and a distraction at 20 customers on a 2 OCPU box. Add
when a real incident proves you needed it, not in anticipation.
