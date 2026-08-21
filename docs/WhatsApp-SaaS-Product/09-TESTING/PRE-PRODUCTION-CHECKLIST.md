# Pre-Production Checklist

Run before **first launch**, and the abbreviated version before every subsequent release.

## Code and tests

- [ ] `./mvnw clean verify` green on a clean clone
- [ ] All four multi-tenancy tests pass (F02), including the meta-test where disabling RLS
      makes the isolation test fail
- [ ] All five job-queue tests pass (F07)
- [ ] `everyTableHasTenantIdExceptDocumentedExceptions` passes
- [ ] `applicationRoleIsNotSuperuser` passes
- [ ] No `TODO` or `FIXME` in a security- or money-related path
- [ ] `ddl-auto: validate` in every profile; never `update`
- [ ] No `System.out.println` in production code paths
- [ ] Manual WhatsApp checklist from `WHATSAPP-TESTING.md` completed against real Meta

## Data and database

- [ ] Every table has `tenant_id` except the three documented exceptions
- [ ] RLS `ENABLE` **and** `FORCE` on every tenant-scoped table
- [ ] Every table indexed leading with `tenant_id`
- [ ] `message_ledger` append-only (verified — try an UPDATE and confirm it fails or is caught)
- [ ] Flyway migrations apply cleanly on an empty database
- [ ] `EXPLAIN ANALYZE` on the job claim query, the ledger monthly count, and the FAQ search —
      all using indexes
- [ ] `whatsapp_rates` populated with current dated India rates

## Security

- [ ] App connects as a non-superuser role
- [ ] Token encryption key present, 32 bytes, and startup fails without it
- [ ] No secret in Git or Git history
- [ ] `/actuator/env`, `/actuator/heapdump`, `/actuator/threaddump` unreachable publicly
- [ ] `/actuator/health` shows no details
- [ ] Security headers present (HSTS, nosniff, frame-deny)
- [ ] TLS valid; HTTP redirects; TLS 1.2 minimum
- [ ] Meta webhook signature verification tested with a tampered body
- [ ] Razorpay webhook signature verification tested
- [ ] Sentry scrubbing verified with a test error containing a fake token
- [ ] Login rate limiting active
- [ ] SSH: key-only, root login disabled, port 22 restricted to your IP
- [ ] UFW active; only 22/80/443; 5432 closed at both firewalls

## Infrastructure

- [ ] Oracle instance within 2 OCPU / 12 GB (post-June-2026 limits)
- [ ] Static reserved IP attached
- [ ] Both systemd services enabled and surviving a reboot (test it)
- [ ] Swap configured
- [ ] `provision.sh` rebuilds a fresh VM unattended in under an hour
- [ ] `deploy.sh` is idempotent and rolls back on a failed health check — **rollback tested**
- [ ] Log rotation configured; `df -h` healthy
- [ ] Idle-prevention heartbeat cron installed

## Backups — do not launch without these

- [ ] Nightly encrypted dump lands in Backblaze B2 (**not** Oracle)
- [ ] WAL archiving active; segments arriving; `archive_command` fails loudly on error
- [ ] **A restore has been performed into a scratch DB and verified**, and logged in
      `../10-OPERATIONS/BACKUP-RESTORE-PROCEDURE.md`
- [ ] Backup encryption **private key stored off the VM**, in two places
- [ ] Backup heartbeat monitor with a 26-hour grace, and the alert tested by breaking a run
- [ ] Retention pruning verified

## Monitoring

- [ ] Readiness endpoint fails when Postgres is stopped (test it)
- [ ] Custom health indicators: queue depth, dead jobs, expired tokens
- [ ] Better Stack monitors live, **including the worker heartbeat**
- [ ] Stopping the app alerts you within 3 minutes — tested for real
- [ ] Sentry receiving events, release-tagged

## Meta / WhatsApp

- [ ] Business Verification complete
- [ ] App Review approved for `whatsapp_business_management` + `whatsapp_business_messaging`
      (**Advanced Access**, not Standard — otherwise every customer call fails with error 200)
- [ ] Webhook callback URL configured and verified in production
- [ ] Webhook fields subscribed
- [ ] Embedded Signup configuration id set in the frontend env
- [ ] Your own test business connected end to end in **production**
- [ ] Onboarding limit understood: 10 new business customers per rolling 7 days by default,
      200/week after verification + App Review + Access Verification
- [ ] Customers understand they pay Meta directly (copy present on the connection screen)

## Billing

- [ ] Razorpay KYC complete, live keys in place
- [ ] A real ₹1 test payment activated a tenant
- [ ] Webhook signature verification enforced; state changes only via webhook
- [ ] Price is ₹1,999 — under ₹2,000 for the 0% UPI MDR band
- [ ] UPI is the default, most prominent payment path
- [ ] GST fields (GSTIN, legal name, address) captured on the tenant
- [ ] PAST_DUE blocks sending but **not** login or data export
- [ ] Your CA engaged for monthly GST filing (₹1,000–2,500/month)

## Legal and compliance

- [ ] Privacy policy live, covering DPDP obligations
- [ ] Terms of service live, stating clearly that Meta bills the customer directly
- [ ] Data deletion process documented and tested (`../11-SECURITY-COMPLIANCE/DPDP-CONSIDERATIONS.md`)
- [ ] Cancellation and refund policy published

## Documentation

- [ ] `../00-START-HERE/CURRENT-STATUS.md` current
- [ ] `../13-DECISIONS/DECISIONS.md` — no blocking open decisions
- [ ] `../00-START-HERE/ASSUMPTIONS-AND-EXPIRY-DATES.md` re-verified this quarter
- [ ] `docs/IMPLEMENTATION/` up to date with every shipped increment
- [ ] `../10-OPERATIONS/PRODUCTION-RUNBOOK.md` complete and readable at 2am

## Per-release short version

- [ ] `./mvnw clean verify` green
- [ ] Migrations backward compatible with running code
- [ ] Manual WhatsApp checklist run
- [ ] Deployed via the approval-gated pipeline
- [ ] Health check green post-deploy; rollback path confirmed available
- [ ] `CURRENT-STATUS.md` and `docs/IMPLEMENTATION/` updated
