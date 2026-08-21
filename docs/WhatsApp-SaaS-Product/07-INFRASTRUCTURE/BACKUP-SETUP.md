# Backup Setup

Increment **F23**. Read `../10-OPERATIONS/BACKUP-RESTORE-PROCEDURE.md` for the restore drill.

## The rule

**A backup you haven't restored is not a backup.** It's a file you hope is a backup. This
document isn't done until you've restored one and written it in the restore-test log.

## Targets

| Metric | Target | Means |
|---|---|---|
| RPO (data you can lose) | ≤ 5 minutes | WAL archiving |
| RTO (time to restore) | ≤ 2 hours | Documented, rehearsed procedure |
| Retention | 7 daily, 4 weekly, 3 monthly | Automatic pruning |

## ⚠️ Never back up Oracle to Oracle

Same-vendor backup means a single account suspension, billing dispute, or terms change loses
both your production data and your recovery path. Oracle halved the ARM free tier in June 2026
with no announcement — that is a vendor you should not trust with both copies.

Backups go to **Backblaze B2** (10 GB free). Different company, different login, different
payment relationship.

## Layer 1 — Nightly logical dump

`/opt/wasaas/bin/backup.sh`, run at 02:30 IST via cron:

```bash
#!/usr/bin/env bash
set -euo pipefail

STAMP=$(date -u +%Y%m%dT%H%M%SZ)
DUMP="/var/backups/wasaas/wasaas-${STAMP}.dump"

pg_dump --format=custom --compress=9 --no-owner --no-privileges \
        --dbname="$DATABASE_URL" --file="$DUMP"

# Encrypt BEFORE upload — this file contains customer conversation data
age -r "$BACKUP_AGE_PUBLIC_KEY" -o "${DUMP}.age" "$DUMP"
shred -u "$DUMP"

b2 file upload "$B2_BUCKET" "${DUMP}.age" "daily/$(basename "${DUMP}.age")"
rm -f "${DUMP}.age"

# Heartbeat only on success — a missing heartbeat alerts (see MONITORING.md)
curl -fsS "$BETTERSTACK_BACKUP_HEARTBEAT_URL" > /dev/null
```

Custom format (`-Fc`) because it supports selective restore and parallel restore. Plain SQL
doesn't.

## Layer 2 — WAL archiving (point-in-time recovery)

The nightly dump gives you a 24-hour worst-case RPO. WAL archiving cuts that to minutes.

`postgresql.conf`:
```conf
wal_level = replica
archive_mode = on
archive_command = '/opt/wasaas/bin/archive-wal.sh %p %f'
archive_timeout = 300      # force a segment every 5 min → RPO ≈ 5 min
```

`archive-wal.sh` encrypts the segment and uploads it to `wal/` in B2. It **must exit non-zero
on failure** — Postgres retries, and silently succeeding while dropping segments destroys your
recovery chain.

## Encryption and the key

Backups contain end-customer phone numbers and message content. Under DPDP that's personal
data you're responsible for.

Use `age` (simple, modern, no GPG keyring pain). Encrypt with a public key; the private key
decrypts.

**The private key must not live only on the box being backed up.** If the VM dies you need the
key. Store it in: your password manager (primary), and a printed copy in a physical safe place
(secondary). Losing it means your backups are cryptographically useless.

Document in `../11-SECURITY-COMPLIANCE/SECRETS-MANAGEMENT.md` exactly where it lives.

## Retention and pruning

```text
daily/     keep 7
weekly/    keep 4     (Sunday's dump promoted)
monthly/   keep 3     (1st of month promoted)
wal/       keep 8 days (must cover the oldest full dump you'd restore to)
```

WAL retention must exceed your oldest useful base backup, or PITR to that point is impossible.

## Backup monitoring — mandatory

**A silent backup failure is identical to having no backup.** Success emits a heartbeat to
Better Stack; a missing heartbeat within the expected window alerts you. Configure the monitor
with a 26-hour grace for a nightly job.

Test the alert deliberately: break the script once, confirm you get notified, fix it. An
untested alert is an assumption.

## Also back up (not just the database)

| Item | Where | Why |
|---|---|---|
| `/etc/caddy/Caddyfile` | Git (`infra/`) | Config |
| systemd units | Git (`infra/`) | Config |
| `provision.sh`, `deploy.sh` | Git (`infra/`) | The box itself is rebuildable |
| Env file (**secret values redacted**) | Structure in Git, values in password manager | Never commit real secrets |
| R2 media | R2 versioning + a monthly sync to B2 | Media isn't in `pg_dump` |

The point of scripted provisioning: you don't back up the server, you rebuild it.

## Definition of Done

- [ ] Nightly dump runs, encrypted, lands in B2
- [ ] WAL archiving active; segments arriving; `archive_command` fails loudly
- [ ] Retention pruning works (verify after a week, or fake the dates)
- [ ] Backup encryption private key stored **off** the VM, in two places
- [ ] Heartbeat monitor configured with a 26-hour grace period
- [ ] Alert tested by deliberately breaking a run
- [ ] **A restore has been performed and logged** in `../10-OPERATIONS/BACKUP-RESTORE-PROCEDURE.md`
