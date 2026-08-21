# Monitoring

Increment **F23**. Free tiers throughout.

## Stack

| Concern | Tool | Free tier |
|---|---|---|
| Errors and exceptions | Sentry | 5,000 errors/month |
| Uptime and heartbeats | **Better Stack** | 10 monitors, 3-min checks |
| Metrics (optional) | Grafana Cloud | 10k series, 50 GB logs |
| Logs | `journalctl` on the box | Free |

## ⚠️ Not UptimeRobot

UptimeRobot's free plan has been **personal/non-commercial only since 1 December 2024**. Using
it to monitor a revenue-generating SaaS violates their terms. Better Stack's free tier permits
commercial use and offers 3-minute checks plus a status page.

## Health endpoints — make them mean something

A health check that only proves the process is alive is nearly useless: the JVM stays up
happily while Postgres is unreachable.

```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true
      show-details: never        # never leak internals publicly
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

- `/actuator/health/liveness` — process alive. Used by systemd.
- `/actuator/health/readiness` — **includes a real DB check**. Used by Caddy and Better Stack.

Add custom indicators:
- **Job queue depth** — degraded if pending jobs exceed a threshold or the oldest pending job
  is older than N minutes. This is your early warning that the worker died.
- **Dead jobs** — degraded if any job hit `DEAD` in the last hour.
- **WhatsApp token health** — degraded if any tenant's token is expired.

The last one matters: an expired token means that customer's automation is silently dead. They
will notice before you do unless you monitor it.

## Better Stack monitors

| Monitor | Target | Interval |
|---|---|---|
| API readiness | `https://api.yourdomain.com/actuator/health/readiness` | 3 min |
| Frontend | `https://app.yourdomain.com` | 5 min |
| Webhook endpoint reachable | `GET /api/webhooks/whatsapp` (Meta's verify handshake path) | 5 min |
| Backup heartbeat | Heartbeat URL, 26h grace | — |
| Worker heartbeat | Heartbeat pinged by a scheduled job in the worker profile | 15 min grace |

The **worker heartbeat** is the one people forget. The web app can be perfectly healthy while
the worker is dead and no message has sent for six hours. Have the worker itself ping a
heartbeat URL on each successful poll cycle.

## Sentry

```yaml
sentry:
  dsn: ${SENTRY_DSN}
  environment: production
  release: ${APP_VERSION}
  traces-sample-rate: 0.1
  send-default-pii: false        # ← mandatory
```

**Scrubbing is not optional.** Configure a `BeforeSendCallback` that strips:
- WhatsApp access tokens (any field matching `token`, `secret`, `authorization`)
- Passwords and hashes
- Session IDs
- Full phone numbers → mask to last 4
- Message bodies → replace with a length and a ledger id

Then **test it**: trigger a deliberate error carrying a fake token and confirm the Sentry event
doesn't contain it. Untested scrubbing is how a token ends up in a third-party SaaS.

5,000 errors/month is generous unless you have a loop. If you're burning through it, that's a
signal about your code, not your plan.

## What to alert on (and what not to)

**Alert immediately:**
- API readiness failing 2 consecutive checks
- Worker heartbeat missing
- Backup heartbeat missing
- Any job reaching `DEAD`
- Razorpay webhook signature verification failing (possible attack or a rotated secret)
- Meta webhook signature verification failing repeatedly

**Log, review weekly, don't wake up for:**
- Individual message send failures (invalid numbers are normal)
- Template rejections by Meta
- Login rate limits tripping
- Single 500s

**Alert fatigue is a real failure mode.** As a solo founder, an alert you learn to ignore is
worse than no alert — you'll ignore the real one too. Start with the five above and add only
when something bites you.

## Logging discipline

Structured JSON to `journalctl`, with the request ID from `RequestIdFilter` on every line.

**Never log:** access tokens · passwords or hashes · session IDs · OTPs · the encryption key ·
full message bodies · full phone numbers (mask to last 4).

```bash
journalctl -u wasaas-web -f
journalctl -u wasaas-worker --since "1 hour ago" | grep ERROR
```

Grafana Cloud log shipping is nice but optional. `journalctl` plus Sentry covers you at MVP
scale, and one less moving part on a 2 OCPU box is a real benefit.

## Weekly 10-minute review

Put it in your calendar:

1. Sentry — any new error types?
2. Jobs — any `DEAD` rows? Any growing queue depth?
3. `pg_stat_statements` — any query slower than last week?
4. Disk usage (`df -h`) — logs and WAL are the usual culprits
5. Unmatched messages across tenants — is deterministic matching holding up? (ADR-007)
6. Backup restore test — monthly, not weekly, but check it's scheduled

## Definition of Done

- [ ] Readiness endpoint fails when Postgres is down (test it: stop Postgres)
- [ ] Custom health indicators for queue depth, dead jobs, token expiry
- [ ] Better Stack monitors configured including **worker** and **backup** heartbeats
- [ ] Sentry live, release-tagged, scrubbing **verified with a test error**
- [ ] Stopping the app alerts you within 3 minutes — tested for real
- [ ] Log rotation configured; disk not filling
- [ ] Weekly review in your calendar
