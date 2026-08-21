# Backup and Restore Procedure

Setup: `../07-INFRASTRUCTURE/BACKUP-SETUP.md`. This document is the **drill**.

## ⚠️ The rule

**A backup you have never restored is not a backup.** It is a file you hope is a backup.

This procedure must be executed at least **monthly** and logged at the bottom of this file.
If the log is empty, assume you have no backups.

## Before you touch anything (production incident)

1. **Stop the application.** `systemctl stop wasaas-web wasaas-worker`. Prevent further writes
   on top of a damaged state.
2. **Take a backup of the current broken state first.** You may need it as evidence, or to
   recover rows the restore predates.
3. **Never restore over the live database.** Restore into a scratch DB, verify, then promote.
4. Decide the target point in time. Write it down.

Step 3 is the one people skip under pressure, and it converts a recoverable incident into an
unrecoverable one.

## Restore drill (monthly, non-production)

### 1. Fetch and decrypt

```bash
b2 file download "b2://$B2_BUCKET/daily/wasaas-20260819T021500Z.dump.age" ./restore.dump.age
age -d -i ~/.age/backup-key.txt -o ./restore.dump ./restore.dump.age
```

If the private key isn't to hand, **stop and fix that first** — an unreadable backup is the same
as no backup. The key must live off the VM (password manager + a physical copy).

### 2. Restore into a scratch database

```bash
sudo -u postgres createdb wasaas_restore_test
pg_restore --dbname=wasaas_restore_test --no-owner --no-privileges \
           --jobs=2 ./restore.dump
```

`--jobs=2` matches your 2 OCPU box. Higher just contends.

### 3. Verify — this is the actual test

```sql
\c wasaas_restore_test

-- Row counts per table
SELECT relname, n_live_tup FROM pg_stat_user_tables ORDER BY relname;

-- Data recency: how much did we lose?
SELECT max(created_at) FROM message_ledger;
SELECT max(received_at) FROM webhook_events;
SELECT count(*) FROM tenants;

-- Structural integrity: are the safety controls still there?
SELECT tablename, rowsecurity FROM pg_tables
WHERE schemaname='public' AND rowsecurity = false;
-- ^ every tenant-scoped table must be absent from this result

SELECT count(*) FROM pg_policies WHERE schemaname='public';

-- Schema version
SELECT version, description, success FROM flyway_schema_history
ORDER BY installed_rank DESC LIMIT 5;
```

**Check RLS explicitly.** `pg_restore --no-owner --no-privileges` can drop role-dependent
objects. A restore that loses your RLS policies looks successful and leaves you with one layer
of tenant isolation instead of two.

Also verify the non-superuser app role exists and works against the restored DB.

### 4. Point-in-time recovery (if you need finer than nightly)

Restore the base backup, then replay WAL to a target time:

`recovery.conf` / `postgresql.auto.conf`:
```conf
restore_command = '/opt/wasaas/bin/restore-wal.sh %f %p'
recovery_target_time = '2026-08-19 14:23:00+05:30'
recovery_target_action = 'promote'
```

Then start Postgres and watch the logs until recovery completes. Verify as in step 3.

WAL retention must cover the oldest base backup you'd restore to, or PITR to that point is
impossible.

### 5. Clean up

```bash
sudo -u postgres dropdb wasaas_restore_test
shred -u ./restore.dump ./restore.dump.age
```

Restored data is real customer data. Don't leave it on disk.

### 6. Log it

Fill in the table below. **An unlogged drill didn't happen.**

## Promoting a restore to production (real incident only)

1. Verify the scratch restore fully (step 3)
2. Rename: `wasaas` → `wasaas_broken_<date>`, `wasaas_restore_test` → `wasaas`
3. Confirm privileges: `wasaas_app` exists, is `NOSUPERUSER`, has grants
4. Confirm RLS is `ENABLE` **and** `FORCE` on every tenant-scoped table
5. Start the worker first, then web
6. Health check, then a manual end-to-end message test
7. Keep `wasaas_broken_<date>` for at least 7 days
8. Reconcile: use `message_ledger` to determine what was actually sent during the gap, and
   notify affected customers if messages were lost

## What a restore does NOT recover

| Not in `pg_dump` | Where it lives |
|---|---|
| R2 media files | R2 versioning + monthly B2 sync |
| Secrets / env file | Password manager |
| Server configuration | `infra/` in Git — rebuild with `provision.sh` |
| Meta webhook subscription | Re-subscribe via Graph API after a URL change |
| TLS certificates | Caddy re-issues automatically |

## Restore test log

Fill this in. Every row is evidence you can actually recover.

| Date | Backup used | Restore duration | Rows verified | RLS intact? | Issues found | By |
|---|---|---|---|---|---|---|
| | | | | | | |
| | | | | | | |
| | | | | | | |

**First entry is due before launch** — see `../09-TESTING/PRE-PRODUCTION-CHECKLIST.md`.
