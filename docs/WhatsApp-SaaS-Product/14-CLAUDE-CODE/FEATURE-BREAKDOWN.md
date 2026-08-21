# Feature Breakdown — 23 Incremental Slices

Each increment is a **vertical slice**: migration → entity → repository → service →
controller → test. Each one ends in something you can run and verify. Each one is one
Claude Code session and one Git commit.

**Do not reorder.** The dependency arrows are real.

## ⚠️ Pilot scope

If you are running a pilot first (see
[../00-START-HERE/PILOT-FIRST-PLAN.md](../00-START-HERE/PILOT-FIRST-PLAN.md)), build **17 of
these 23**:

| Increment | Pilot |
|---|---|
| F00–F11 | **All required** |
| F12 | Required (simplified — pilot mostly uses in-window session messages) |
| F13, F14 | **Required** |
| F15, F16 | **Deferred** |
| F17, F18 | **Required** |
| F19 | **Simplified** — read-only view; you configure rules on the onboarding call |
| F20 | **Required** |
| F21 | **Deferred entirely** — collect ₹500 by UPI manually during the pilot |
| F22, F23 | **Required** |

**Pilot: ~17 increments, 6–7 weeks.** Full: 23 increments, 10–11 weeks.

**Never deferred, even for 5 customers:** F02 (multi-tenancy + RLS), F07 (job queue), F08
(ledger), F10 (webhook signature verification), F23 (tested backups). Defer commercial and
cosmetic work; never defer correctness work — each of these is far more expensive to retrofit
than to build.

Also note: **F06 is NOT blocked on Advanced Access during the pilot.** In development mode,
Standard Access plus a Tester role on your app is sufficient. See
[../08-META-WHATSAPP/PILOT-MODE-SETUP.md](../08-META-WHATSAPP/PILOT-MODE-SETUP.md).

---

## Phase A — Foundation (F00–F04)
*Goal: a running, multi-tenant, authenticated app with zero WhatsApp involvement.*

| # | Increment | Depends on | Verify by | Size |
|---|---|---|---|---|
| **F00** | Project skeleton: Spring Boot, Postgres, Flyway, Actuator, error handling, request-ID logging | — | `GET /actuator/health` = 200; Flyway V1 applied | S |
| **F01** | Tenant + User + TenantUser model; signup creates tenant + owner atomically | F00 | `POST /api/auth/register` creates both rows in one transaction | M |
| **F02** | Tenant context: request-scoped `TenantContext`, filter, repository-level enforcement, **RLS policies** | F01 | Isolation test: Tenant A cannot read Tenant B's rows, and still can't with a raw query | M |
| **F03** | Authentication: Argon2id, Spring Session JDBC, login/logout, role checks | F02 | Login sets session cookie; session survives app restart | M |
| **F04** | Password reset via email (Brevo/Resend), single-use expiring tokens | F03 | Reset email arrives; token works once | S |

**End of Phase A:** a real multi-tenant SaaS shell. No WhatsApp yet. This is correct —
get isolation right before adding anything valuable to leak.

---

## Phase B — WhatsApp core (F05–F11)
*Goal: a customer can connect their own WABA, and messages flow both ways, durably.*

| # | Increment | Depends on | Verify by | Size |
|---|---|---|---|---|
| **F05** | `whatsapp_accounts` table + `TokenCipher` (envelope encryption). **No Meta calls yet.** | F02 | Token unreadable in DB; round-trip encrypt/decrypt test | S |
| **F06** | Embedded Signup callback: code→token exchange, fetch WABA ID + phone number ID, subscribe app to WABA webhooks | F05 (+ Meta App Review **only for live mode** — not for pilot) | Connect your *own* test business end-to-end | L |
| **F07** | `jobs` table + worker with `FOR UPDATE SKIP LOCKED`, exponential backoff, retry cap, dead-letter, idempotency keys. Runs as `profile=worker`. | F00 | Kill app mid-job → job re-claimable; 2 workers never double-process | L |
| **F08** | `message_ledger` (append-only) with billing category enum, `wamid`, status transitions | F02 | Ledger row written before send; status webhooks append, never update | M |
| **F09** | Outbound send: `WhatsAppCloudClient` + `SendMessageHandler` job. Ledger-first, then API call. | F06, F07, F08 | Send a text to your own number via an enqueued job | M |
| **F10** | Webhook receiver: `GET` verify handshake, `POST` receiver, `X-Hub-Signature-256` verification, persist raw event, ACK <2s, enqueue, dedupe | F07 | Bad signature → 403; duplicate event → one effect; p99 <2s | L |
| **F11** | Inbound processing: parse event → contact → conversation → ledger entry; status callbacks update delivery state | F08, F10 | Real inbound message creates contact + conversation + ledger rows | M |

**End of Phase B:** the engine works. You can receive and send WhatsApp messages reliably,
per tenant, with a full audit trail. **This is the hardest phase — go slowly here.**

---

## Phase C — Automation (F12–F16)
*Goal: the product does something useful without you touching it.*

> **⚠️ `[DECISION REQUIRED]`** — the exact automation feature set is **not** specified in
> the source document. Close **D-01** in `docs/.../13-DECISIONS/DECISIONS.md` before
> starting F12. Everything below is the most defensible default, not a given.

| # | Increment | Depends on | Verify by | Size |
|---|---|---|---|---|
| **F12** | Templates: sync approved templates from Meta, store category, create/submit template | F06 | Template list reflects Meta; category stored correctly | M |
| **F13** | Keyword automation rules: pattern → action, per tenant, priority ordering | F11 | Configured keyword produces the right reply | M |
| **F14** | FAQ matching: Postgres full-text + `pg_trgm`, confidence threshold, **escalate below threshold** | F13 | Good match replies; poor match escalates instead of guessing | M |
| **F15** | Interactive replies: button and list messages; reply consolidation (cost control) | F09 | One billable message instead of three | S |
| **F16** | Scheduled messages: `scheduled_messages` table, IST-aware scheduling, enqueue via jobs | F07, F12 | A message scheduled for +5 min actually sends | M |

**End of Phase C:** the product has a reason to exist. Log every unmatched inbound message
from F14 onward — that dataset is the only honest answer to "do we ever need AI?" (ADR-007).

---

## Phase D — Frontend (F17–F20)
*Goal: a customer can self-serve without you on a call.*

| # | Increment | Depends on | Verify by | Size |
|---|---|---|---|---|
| **F17** | React + Vite setup, auth screens, app shell, API client, session handling | F03 | Login → dashboard, protected routes work | M |
| **F18** | WhatsApp Connection screen: launch Embedded Signup popup, show connection status, **payment-method-attached warning** | F06, F17 | Connect flow works from UI; unattached payment method is flagged | M |
| **F19** | Config screens: Automation rules, FAQ, Templates | F13, F14, F12 | A non-technical user can add a rule unaided | L |
| **F20** | Inbox + Dashboard: conversation list, message thread, **per-tenant per-category message counts** | F11, F08 | Counts match the ledger; needed before 1 Oct 2026 | L |

**End of Phase D:** demoable to a stranger. Empty/loading/error/success states on every
screen — not optional; SMB users hit empty states constantly.

---

## Phase E — Production (F21–F23)
*Goal: it runs on real infrastructure, and you can recover from disaster.*

| # | Increment | Depends on | Verify by | Size |
|---|---|---|---|---|
| **F21** | Billing: Razorpay subscription, UPI-first checkout at ₹1,999, subscription state machine, dunning | F03 | A real ₹1 test payment activates a tenant | M |
| **F22** | Infra as scripts: provisioning script, Caddy config, systemd units for web+worker, GitHub Actions deploy | F00 | Script rebuilds the whole box from scratch in <1 hour | L |
| **F23** | Backups + monitoring: nightly `pg_dump` + WAL to Backblaze B2, Sentry, Better Stack, **tested restore** | F22 | You have restored a backup into a scratch DB successfully | M |

**End of Phase E:** you can onboard customer #1. Go to
`docs/.../10-OPERATIONS/CUSTOMER-ONBOARDING.md`.

---

## Deliberately NOT in the MVP

Say no to these when Claude Code (or you) reaches for them:

| Not building | Why | Revisit |
|---|---|---|
| Redis | Nothing is cross-instance yet | `12-SCALING/WHEN-TO-INTRODUCE-REDIS.md` |
| Kafka / RabbitMQ | Postgres queue has ~4 orders of magnitude headroom | `WHEN-TO-INTRODUCE-MESSAGE-BROKER.md` |
| Microservices / K8s | One process, one box, one developer | `WHEN-TO-INTRODUCE-MICROSERVICES.md` |
| AI in automation path | Deterministic is debuggable; free tiers train on your data | ADR-007 |
| Team/multi-user roles beyond owner+member | No customer has asked | After 20 customers |
| Analytics beyond message counts | No customer has asked | After 20 customers |
| Mobile app | Web works on phones | Much later |
| Self-serve onboarding automation | Do it manually 20 times first | After F20 + 20 customers |
| Multi-language UI | Validate English first | `[DECISION REQUIRED]` D-06 |
| Wallet/prepaid message billing | Requires Meta credit line; huge scope | Stage 3, ADR-005 |

---

## Rough sequencing

Sizes are S ≈ half day, M ≈ 1–2 days, L ≈ 3–4 days, assuming you're reviewing properly
rather than accepting diffs blind.

**Pilot track:**
```text
Phase A   F00–F04        ~1.5 weeks
Phase B   F05–F11        ~3 weeks     ← hardest
Phase C   F12–F14        ~1.5 weeks
Phase D   F17,F18,F20    ~2 weeks     (F19 simplified)
Phase E   F22–F23        ~1 week      (F21 deferred)
                         ─────────
                         ~6–7 weeks to first pilot customer
```

**Full track (after the pilot succeeds):**

```text
Phase A   F00–F04    ~1.5 weeks
Phase B   F05–F11    ~3 weeks     ← hardest; don't rush
Phase C   F12–F16    ~2 weeks
Phase D   F17–F20    ~2.5 weeks
Phase E   F21–F23    ~1.5 weeks
                     ─────────
                     ~10–11 weeks to first customer
```

Meta App Review runs in parallel during Phase A. If it's not approved by F06, build F07/F08
(which don't need Meta) and come back.

Update `docs/.../00-START-HERE/CURRENT-STATUS.md` after every increment.
