# Backup and Recovery

**Classification: BUILD NOW (increment F23). Mandatory.**

## Why this is not optional

We self-host PostgreSQL on a single free-tier VM (ADR-006). There is no managed safety net.
Losing the VM without off-box backups is total data loss and the end of the business.

And this is a live risk, not theoretical: **Oracle halved its Always Free ARM allocation in
June 2026 with no public announcement**, and emailed users that non-compliant instances would
be terminated. Free tiers are marketing budgets, and marketing budgets get cut.

**The rule: an untested backup is not a backup.**

---

## What we back up

| Data | Method | Frequency | Retention |
|---|---|---|---|
| PostgreSQL | `pg_dump` custom format, compressed | Nightly | 7 daily, 4 weekly, 3 monthly |
| PostgreSQL WAL | Continuous archiving | Continuous | 7 days (enables PITR) |
| Uploaded media | Already in Cloudflare R2 | — | R2 is the primary store |
| Application config | In Git | — | — |
| Secrets | **Manual, offline** — see below | On change | — |

### The secrets problem

Backups are encrypted. If the encryption key lives only on the VM being backed up, losing the
VM loses the key, and your backups are decorative.

**Store, outside the VM:** the backup encryption key, the WhatsApp token encryption key, the
Meta app secret, the DB password, the B2 credentials, and the Razorpay keys. A password
manager plus one printed copy in a physical safe place. Verify quarterly that you can actually
retrieve them.

---

## Destination: Backblaze B2

**Never back up Oracle to Oracle.** A provider-level account issue — suspension, a policy
change, a billing dispute — takes the primary and the backup together. B2 gives 10 GB free with
1 GB/day free egress, which is ample.

Cloudflare R2 is an acceptable alternative, but if you already use R2 for media, B2 keeps the
failure domains separate.

---

## The scripts

`infra/backup.sh` — nightly, via cron:

```text
1. pg_dump -Fc  (custom format: parallel restore, selective restore)
2. gzip
3. Encrypt (age or gpg) with the backup key
4. Upload to B2 with a dated key: wasaas/db/2026-08-18T02-00Z.dump.gz.age
5. Prune per retention policy
6. Emit a success heartbeat to Better Stack
7. On ANY failure: exit non-zero, no heartbeat → alert fires
```

Step 6 is the one people skip. **A silent backup failure is identical to having no backup.**
Better Stack's heartbeat monitor alerts when the expected ping doesn't arrive.

`infra/restore.sh` — restores a named backup into a **scratch** database and verifies:

```text
1. Download from B2
2. Decrypt, decompress
3. createdb wasaas_restore_test
4. pg_restore
5. Verification queries:
   - row counts per table (compare against expected ranges)
   - latest message_ledger created_at (how stale is this backup?)
   - RLS policies present on all tenant tables
   - flyway_schema_history version matches the deployed app
6. Report, then drop the scratch DB
```

---

## Restore test procedure — do this, don't just read it

**Schedule: before the first customer, then monthly.**

1. Run `infra/restore.sh` against last night's backup
2. Record the **wall-clock duration** — this is your real RTO, and it is always longer than you guess
3. Confirm all verification queries pass
4. Log the result in the table below
5. If anything failed, fix it **now**. A backup you cannot restore is a spreadsheet of hope.

### Restore test log

| Date | Backup used | Duration | Verification | Issues found |
|---|---|---|---|---|
| _(fill this in — an empty table means you have never tested a restore)_ | | | | |

---

## Disaster recovery: total VM loss

Target: **under 1 hour**.

```text
1. Provision a fresh Ubuntu VM (Oracle, or Hetzner CX22 ≈ ₹380/mo fallback)   ~10 min
2. Run infra/provision.sh                                                      ~15 min
3. Run infra/restore.sh against the latest backup, into the real DB            ~10 min
4. Deploy the JAR (infra/deploy.sh)                                            ~5 min
5. Point DNS at the new IP (Cloudflare, low TTL)                               ~5 min
6. Verify: health endpoint, a test webhook, one test send                      ~10 min
```

**Data loss window:** up to 24 hours with nightly dumps alone; minutes with WAL archiving.
This is exactly why WAL archiving is in scope and not deferred.

**Prerequisites that make this possible — all in increment F22/F23:**
- `provision.sh` is idempotent and works on **any** Ubuntu VPS, not just Oracle
- DNS TTL is low (300s) so failover is fast
- Secrets are retrievable from outside the dead VM
- You have done the restore before, under no pressure

---

## Oracle-specific risks

| Risk | Mitigation |
|---|---|
| Idle-instance reclamation | Cron'd lightweight CPU task; real traffic usually suffices |
| Quiet free-tier reduction (happened June 2026) | Quarterly re-verification (`ASSUMPTIONS-AND-EXPIRY-DATES.md`); priced fallback ready |
| Signup rejection / account suspension | Backups at a different vendor; portable provisioning script |
| Regional ARM capacity exhaustion | Mumbai and Hyderabad both viable; script works anywhere |

## When to move to managed Postgres

The trigger is a sentence, not a metric: **"I cannot afford to lose 24 hours of data."**
Roughly ₹50,000/month revenue. Managed Postgres with point-in-time recovery is the **first**
genuinely worth-paying-for upgrade — because data loss ends the business while downtime only
annoys people. See `12-SCALING/REVENUE-FUNDED-INFRASTRUCTURE.md`.

## DO NOT BUILD YET

Streaming replicas · automated failover · cross-region replication · a second VM ·
hot standby · continuous backup verification automation.
