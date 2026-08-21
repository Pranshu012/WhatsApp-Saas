# Incident Response

You are on call, alone. The goal of this document is to remove decision-making from the moment
you're stressed.

## Severity

| Level | Definition | Response |
|---|---|---|
| **SEV1** | Platform down, data loss, or a cross-tenant leak | Drop everything |
| **SEV2** | Messages not flowing for multiple tenants; billing broken | Within 1 hour |
| **SEV3** | One tenant affected; degraded feature | Same day |
| **SEV4** | Cosmetic, single-user | Next release |

**A suspected cross-tenant data leak is always SEV1**, even if only one row. It's the one
incident class that ends the business.

## The first five minutes

Do these in order. Do not skip to debugging.

1. **Confirm it's real.** Better Stack, then `curl https://api.yourdomain.com/actuator/health`.
2. **Write down the time and what you observed.** A note now saves the post-mortem later.
3. **Stop the bleeding before finding the cause.** Restarting a stuck worker beats a root-cause
   analysis while messages queue.
4. **Assess blast radius.** One tenant or all? Data at risk or just availability?
5. **Tell customers if it's SEV1/SEV2.** A WhatsApp message before they notice buys enormous
   goodwill. Silence costs you more than the outage.

## Playbooks

### API down / health check failing

```bash
systemctl status wasaas-web
journalctl -u wasaas-web -n 200 --no-pager
df -h                    # full disk is a common cause
free -h
systemctl status postgresql
```

| Cause | Fix |
|---|---|
| Disk full | Clear old logs/WAL; check `/var/log/caddy` and archive dir |
| OOM kill | Check `dmesg`; reduce `work_mem` or JVM heap; restart |
| Postgres down | `systemctl start postgresql`; check its logs |
| Failed deploy | Roll back: repoint the release symlink, restart |
| Bad migration | See below — the hard case |

### Messages not sending

Most likely the worker, not the web app. They fail independently and the web health check won't
tell you.

```sql
SELECT status, count(*) FROM jobs GROUP BY status;
SELECT * FROM jobs WHERE status='DEAD' ORDER BY updated_at DESC LIMIT 20;
SELECT min(created_at) FROM jobs WHERE status='PENDING';   -- how far behind?
```

```bash
systemctl status wasaas-worker
journalctl -u wasaas-worker -n 200 --no-pager
```

| Cause | Fix |
|---|---|
| Worker dead | `systemctl restart wasaas-worker` — then find out why |
| All jobs DEAD with Meta error 190 | Tenant tokens expired → notify affected tenants to reconnect |
| All jobs DEAD with error 200 | Your app lost Advanced Access → Meta App Review |
| One tenant's jobs DEAD | Their payment method or quality rating — customer-side |
| Queue growing, worker alive | Handler is slow or stuck; check for a hung HTTP call without a timeout |

### Webhooks not arriving

```sql
SELECT max(received_at) FROM webhook_events;
```

If the newest is hours old: check Caddy is up and the certificate is valid, check Meta's webhook
subscription still exists (it can be dropped), check Meta's status page. Meta retries for a
while — a short outage loses nothing.

### ⚠️ Suspected cross-tenant data leak — SEV1

1. **Take the affected endpoint offline** rather than investigating live
2. Capture evidence before changing anything: the request, the response, the tenant ids
3. Verify the two layers:
   ```sql
   SELECT rolsuper FROM pg_roles WHERE rolname = 'wasaas_app';   -- MUST be false
   SELECT tablename, rowsecurity FROM pg_tables WHERE schemaname='public';
   ```
4. Find the query path. Almost always: a raw query without `tenant_id`, or a superuser
   connection, or `app.tenant_id` set per connection instead of per transaction
5. Fix, add the failing test first, redeploy
6. **Notify affected customers.** Under DPDP you have breach obligations — see
   `../11-SECURITY-COMPLIANCE/DPDP-CONSIDERATIONS.md`. Do not quietly patch a real leak.

### Data loss / bad migration

Worst case. Slow down.

1. **Stop the application.** Prevent further writes.
2. Assess: what was lost, over what window?
3. If a migration caused it, is a forward fix possible? Prefer forward fixes to restores.
4. If restoring, follow `BACKUP-RESTORE-PROCEDURE.md` exactly — into a **scratch** database
   first, verify, then promote.
5. `message_ledger` is append-only, which means it is your reconstruction source for what was
   actually sent. Use it.

### Razorpay webhook signature failures

Either the secret rotated (check the dashboard) or someone is probing. Never disable
verification to "unblock" payments — an unverified webhook can activate subscriptions for free.

## Communicating

**SEV1/SEV2 template:**

> We're aware of an issue affecting [what]. Your data is safe. I'm working on it now and will
> update you within 30 minutes.

Then actually update in 30 minutes, even with "still working on it."

**After resolution:**

> Fixed as of [time]. Cause: [one plain sentence]. [What was affected]. To prevent recurrence:
> [one concrete change].

Be specific about the cause. Vagueness reads as either incompetence or dishonesty.

## Post-incident (same day, 20 minutes)

Write in `PRODUCTION-RUNBOOK.md`:
- Timeline
- Root cause (the actual cause, not "the worker died")
- What made detection slow
- **The test or alert that now exists so this can't recur silently**

If you can't name a test or an alert, you haven't finished the incident.

## Don't

- Debug in production without a copy of the evidence
- `DELETE`/`UPDATE` production data without a backup taken *first*
- Disable a security control to restore service
- Blame Meta publicly without checking your own logs
- Skip the write-up because you're tired — that's precisely when the lesson is lost
