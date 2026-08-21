# ADR-006 — Self-Hosted PostgreSQL on the Application VM (Initially)

**Status:** Accepted · 18 August 2026

## Context
We need PostgreSQL for 0–20 customers at effectively zero cost. Managed free tiers were
evaluated on 18 August 2026.

**Neon free:** 0.5 GB storage + **100 CU-hours/month per project**, compute scales to zero
after 5 minutes idle. Our application is a continuously-queried webhook receiver, so the
database never idles. At Neon's minimum 0.25 CU, a full month is 730h × 0.25 =
**182 CU-hours** — the free quota is exhausted around day 16, after which compute suspends
until the next billing cycle. **Disqualifying for an always-on product.**

**Supabase free:** 500 MB, 2 projects, projects pause after 7 days inactivity (tightened
Feb 2026). Workable for an active app, but adds network latency, a second vendor, and we'd be
using ~5% of a platform whose value is the bundled auth/storage/realtime we've decided to
self-implement.

## Decision
Run PostgreSQL 17 on the same Oracle Cloud Always Free VM as the application
(2 OCPU / 12 GB ARM). Application connects over localhost as a **non-superuser** role.

## Why
- No quota, no cold starts, no cross-network latency, sub-millisecond queries.
- Full extension access, which we actually depend on: `pg_trgm` (FAQ typo tolerance),
  `pgcrypto`, `pg_stat_statements`, and real `SKIP LOCKED` and RLS semantics.
- 12 GB RAM is generous for both a JVM and Postgres at this scale.
- Zero cost, zero vendor dependency.

## Alternatives considered
| Option | Rejected because |
|---|---|
| Neon free | Quota arithmetic above — suspends mid-month |
| Supabase free | Latency + second vendor for a fraction of its value |
| Managed paid (₹1,500–2,500/mo) | Correct eventually, wrong at ₹0 revenue. See the trigger below. |
| SQLite | No RLS, no `SKIP LOCKED`, no concurrent write story. Wrong tool. |

## Consequences
**Positive:** free, fast, no limits, no vendor risk.
**Negative:** **we own backups, patching, and tuning.** There is no safety net. A VM loss
without off-box backups is total data loss and the end of the business.

**Mandatory mitigations (not optional):**
1. Nightly `pg_dump` + WAL archiving to **Backblaze B2** — a different vendor from Oracle.
   Never back up Oracle to Oracle.
2. Backups encrypted before upload (they contain conversation data — DPDP exposure), with the
   encryption key stored somewhere other than the box being backed up.
3. A single idempotent provisioning script, so rebuilding on any Ubuntu VPS takes under an hour.
4. **A tested restore.** An untested backup is not a backup.
5. Alert on a missing or failed backup. A silent backup failure equals no backup.

Note also: Oracle halved this free tier in June 2026 with no public announcement. Portability
is a hard requirement here, not a nice-to-have. Priced fallback: Hetzner CX22 ≈ ₹380/month.

## When we would revisit — the trigger is explicit
Move to managed PostgreSQL with point-in-time recovery when the sentence
**"I cannot afford to lose 24 hours of data"** becomes true. That is roughly ₹50,000/month
revenue (~25–50 customers). It is the **first** genuinely worth-paying-for upgrade, because
data loss ends the business while downtime only annoys people.

The migration is `pg_dump`/`pg_restore` — about 30 minutes. This decision is cheap to reverse,
which is exactly why it's safe to make.
