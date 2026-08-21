# Master Action Plan

**This is your main execution document.** Phases 0–14, chronological. Tick the boxes.

Maps to increments F00–F23 in `14-CLAUDE-CODE/FEATURE-BREAKDOWN.md`, where the paste-ready
Claude Code prompts live.

---

## PHASE 0 — Business + Account Setup

**Objective:** Every external dependency in motion before you write code.
**Why:** Meta verification and App Review take 1–3 weeks. It's the only task you cannot compress
by working harder, so it starts on day one.
**Prerequisites:** None. Start today.

### Tasks
- [ ] Decide company name and domain (**D-04**)
- [ ] Decide business entity type; talk to a CA (**D-05**)
- [ ] Register the business entity; collect documents (GST cert / CoI / PAN)
- [ ] Register the domain
- [ ] Put up a real one-page site describing the product
- [ ] Publish a privacy policy and terms — **must mention WhatsApp data handling**
- [ ] Business email on your own domain (not Gmail)
- [ ] Create Meta Business Portfolio
- [ ] **Submit Meta Business Verification**
- [ ] Create Meta App (Business type), record App ID + Secret into `.env`
- [ ] Add the WhatsApp product; note the test number
- [ ] Start **Tech Provider onboarding**, choose "without a partner"
- [ ] **Submit App Review: `whatsapp_business_management` + `whatsapp_business_messaging`, Advanced Access**
- [ ] Create Oracle Cloud account, **home region Mumbai or Hyderabad** (irreversible)
- [ ] Provision a 2 OCPU / 12 GB ARM instance and confirm it actually starts
- [ ] Create accounts: Cloudflare, GitHub, Backblaze B2, Sentry, Better Stack, Brevo/Resend
- [ ] Open a Razorpay account (needs PAN, bank account, GST cert)
- [ ] **Run 10 customer validation conversations** (`01-BUSINESS/CUSTOMER-VALIDATION.md`)
- [ ] Close **D-01, D-02, D-03, D-07** in `13-DECISIONS/DECISIONS.md`

### Expected output
Verified Meta business, App Review submitted, a working VM, and a decided product scope.

### Definition of Done
- [ ] Business Verification shows **Verified**
- [ ] App Review submitted (approval may still be pending — continue to Phase 1)
- [ ] Oracle ARM instance running in an Indian region
- [ ] Domain live with valid HTTPS
- [ ] D-01 answered in writing, with a date

### Common mistakes
Document details not matching exactly what you typed (days lost per resubmission) · requesting
Standard instead of Advanced Access · deciding D-01 at your desk instead of from conversations ·
picking the wrong Oracle home region (cannot be changed).

### What NOT to build yet
Anything. No code in Phase 0.

---

## PHASE 1 — Project Foundation  → increment F00

**Objective:** A Spring Boot skeleton that runs.
**Why:** A clean foundation prevents the two classic early messes: secrets in Git and schema drift.
**Prerequisites:** Phase 0 started (not necessarily finished).

### Tasks
- [ ] Create private Git repo; `git init` **before** the first Claude Code session
- [ ] Copy this docs workspace into `docs/` and commit
- [ ] Set up Claude Code (`14-CLAUDE-CODE/CLAUDE-CODE-SETUP.md`)
- [ ] Copy `CLAUDE.md` from the template into the repo root
- [ ] Create `.claude/settings.json` with allow/deny lists
- [ ] Generate the Spring Boot project (Java 21, the exact dependency list in `05-BACKEND/BACKEND-SETUP.md`)
- [ ] Create feature packages
- [ ] `GlobalExceptionHandler`, `ApiError`, `DomainException`
- [ ] `RequestIdFilter` (MDC)
- [ ] `application.yml` + `application-local.yml`, env-var driven
- [ ] `docker-compose.yml` for local Postgres
- [ ] Flyway `V1__baseline.sql` (extensions only)
- [ ] `.env.example`, `.gitignore`
- [ ] Fail-fast startup check for `TOKEN_ENCRYPTION_KEY`

### Definition of Done
- [ ] `./mvnw clean verify` green
- [ ] App starts and connects to Postgres; Flyway V1 applied
- [ ] `GET /actuator/health` → 200
- [ ] A thrown `NotFoundException` → clean JSON `ApiError`
- [ ] Every log line carries a request ID
- [ ] No secrets in Git

### Files that should exist
`pom.xml` · `Application.java` · `application.yml` · `common/exception/*` · `common/logging/*` ·
`db/migration/V1__baseline.sql` · `docker-compose.yml` · `.env.example` · `CLAUDE.md`

### What NOT to build yet
Entities, controllers beyond health, Dockerfile for production, CI config.

---

## PHASE 2 — Database  → F01, F02

**Objective:** Tenant model with **provable** isolation.
**Why:** The most expensive thing to retrofit and the most damaging to get wrong.

### Tasks
- [ ] `V2__tenants_users.sql` — tenants, users, tenant_users
- [ ] JPA entities and repositories
- [ ] `TenantService.registerTenant()` — atomic tenant + owner creation
- [ ] `POST /api/auth/register`
- [ ] `TenantContext` that **throws** when unset
- [ ] `TenantContextFilter` with `clear()` in a `finally`
- [ ] Repository-level tenant enforcement
- [ ] `V3__rls.sql` — non-superuser app role, RLS policies with `USING` **and** `WITH CHECK`
- [ ] Transaction-local `app.tenant_id` via `set_config(..., true)`
- [ ] Document the pattern for future tables inside the migration

### Definition of Done
- [ ] Registration creates all three rows in one transaction; rollback verified
- [ ] Passwords stored as Argon2id
- [ ] **Test 1:** repository query can't cross tenants
- [ ] **Test 2:** raw query omitting `tenant_id` still can't cross tenants
- [ ] **Test 3:** `TenantContext.require()` throws when unset
- [ ] **Test 4:** disabling RLS makes Test 2 **fail**
- [ ] Tests connect as the **non-superuser** role
- [ ] Testcontainers, not H2

### Common mistakes
Missing `WITH CHECK` (cross-tenant writes) · `set_config(..., false)` (leaks across pooled
connections) · superuser in tests (RLS inert, tests meaningless) · no `finally { clear(); }`.

---

## PHASE 3 — Authentication  → F03, F04

**Objective:** Real login with server-side sessions.
**Prerequisites:** Phase 2 complete.

### Tasks
- [ ] `V4__spring_session.sql` (official Spring Session JDBC schema)
- [ ] `SecurityConfig`: JSON login, HttpOnly/Secure/SameSite cookie, CSRF + token endpoint
- [ ] `POST /api/auth/login`, `/logout`, `GET /api/auth/me`
- [ ] **Remove the F02 `X-Tenant-Id` header path entirely**; populate from the session principal
- [ ] OWNER/MEMBER role checks
- [ ] Generic login failures; dummy hash on unknown email (timing)
- [ ] Login rate limiting per (email, IP) — Postgres counter, no Redis
- [ ] `V5__password_reset.sql`; forgot/reset endpoints; store token **hashed**
- [ ] `EmailSender` interface + no-op local implementation

### Definition of Done
- [ ] Login → session cookie; `/me` returns user + tenant + role
- [ ] Session survives an app restart
- [ ] **`X-Tenant-Id` header grants nothing** (asserted by test)
- [ ] Unknown email and wrong password indistinguishable
- [ ] Reset token single-use, expires, invalidates all sessions
- [ ] Rate limit trips
- [ ] No password, token, or session ID in any log

### What NOT to build yet
JWT · OAuth · SSO · 2FA · remember-me · invitations.

---

## PHASE 4 — WhatsApp Integration  → F05, F06

**Objective:** A customer can connect their own WABA.
**Prerequisites:** Phase 3 complete **and App Review approved.** If pending, do Phase 6 first.

### Tasks
- [ ] `V6__whatsapp_accounts.sql` with RLS
- [ ] `TokenCipher` — AES-256-GCM, key from env, nonce stored with ciphertext
- [ ] Startup fails if the key is missing or not 32 bytes
- [ ] Entity exposes no Jackson-serialisable token getter
- [ ] `MetaGraphClient` — pinned Graph version from config, timeouts, typed errors
- [ ] `POST /api/whatsapp/connect`: exchange code → **verify IDs against Meta** → subscribe app to WABA → persist encrypted
- [ ] Idempotent upsert on `(tenant_id, phone_number_id)`
- [ ] Map Meta error 200 to a clear, actionable message

### Definition of Done
- [ ] Your own test business connects end to end
- [ ] Webhooks subscribed for that WABA
- [ ] Token opaque in the DB, absent from logs and responses
- [ ] Reconnect updates rather than duplicating
- [ ] Error 200 produces a clear message, not a 500

### Common mistakes
**Forgetting the subscribe-app-to-WABA call** (nothing ever arrives) · trusting client-supplied
WABA IDs · logging the code or token · hardcoding the Graph version at call sites.

---

## PHASE 5 — Webhooks  → F10

**Objective:** Receive Meta events safely and fast.
**Prerequisites:** Phase 6 (jobs) — build F07 first.

### Tasks
- [ ] `GET` verification handshake, constant-time token compare
- [ ] `POST` receiver: HMAC over **raw bytes**, verified **before** parsing
- [ ] `V9__webhook_events.sql` with a unique partial index on `event_id`
- [ ] Persist raw payload; enqueue; **ACK 200 in under 2s**
- [ ] Deduplicate; decide and document ingest-level vs handler-level
- [ ] Zero outbound calls and zero business logic in the controller
- [ ] Structured logging that never includes the payload

### Definition of Done
- [ ] Meta handshake succeeds
- [ ] Tampered body → 403, not processed
- [ ] Duplicate event → one logical effect
- [ ] p99 under 2 seconds, measured
- [ ] ArchUnit (or equivalent) asserts the controller doesn't reach handlers

### Common mistakes
HMAC over reserialised JSON · verifying after parsing · `String.equals` on the signature ·
business logic in the controller · 500 on duplicates (Meta retries forever).

---

## PHASE 6 — Message Processing  → F07, F08, F09, F11

**Objective:** Durable queue, billing ledger, and both message directions working.
**Prerequisites:** Phase 2.

### Tasks
- [ ] `V7__jobs.sql`; claim query with `FOR UPDATE SKIP LOCKED`
- [ ] Stale-lock recovery via `locked_at` timeout
- [ ] Exponential backoff **with jitter**; retry cap; `DEAD` status
- [ ] `PermanentJobException` → immediate `DEAD`
- [ ] `JobHandler` interface + Spring-registered registry
- [ ] `JobWorker` under `@Profile("worker")`
- [ ] `V8__message_ledger.sql` + status events + `whatsapp_rates`, seeded with dated rates
- [ ] `LedgerService` — **intent recorded before the API call**
- [ ] `WhatsAppCloudClient` (text, template, interactive)
- [ ] `SendMessageJobHandler` — ledger → send → attach `wamid`
- [ ] `MessagingService.send()` enqueues only
- [ ] Meta error classification table implemented
- [ ] `V10__contacts_conversations.sql`; inbound processor; 24h service window tracking
- [ ] Status webhooks append ledger status events

### Definition of Done
- [ ] Two concurrent workers never double-claim (**real threads**, Testcontainers)
- [ ] Crash mid-job → re-claimable after timeout
- [ ] Duplicate idempotency key → one job, one send
- [ ] `web` profile does not poll
- [ ] Real message sent to your own number via the queue
- [ ] Ledger complete; monthly per-category counts one query
- [ ] Inbound message creates contact + conversation + ledger row
- [ ] Service window computed correctly

### Common mistakes
Sending before writing the ledger · non-deterministic idempotency keys · mocking `SKIP LOCKED`
instead of testing real concurrency · retrying permanent Meta errors · one transaction around a
whole batch.

---

## PHASE 7 — Automation Engine  → F12–F16

**🔴 BLOCKED until D-01 is closed.** Do not start without a written answer.

**Objective:** The product does something useful unattended.

### Tasks
- [ ] `V11__templates.sql`; sync from Meta; Meta's category wins
- [ ] Warn when Meta's assigned category differs from what was requested
- [ ] Block sends on non-APPROVED templates before the API call
- [ ] `V12__automation_rules.sql`; priority ordering; **first match wins**
- [ ] Regex compiled with a timeout; catastrophic patterns rejected at save
- [ ] **Per-contact reply rate limit** (spends customer money otherwise)
- [ ] `V13__faqs.sql`; full-text + `pg_trgm`; configurable confidence threshold
- [ ] Below threshold → **escalate, never guess**
- [ ] Log every unmatched message (`unmatched_messages`) — the ADR-007 dataset
- [ ] Interactive button/list replies; **reply consolidation** for cost
- [ ] `V14__scheduled_messages.sql`; minute scheduler; deterministic idempotency key
- [ ] Reject scheduled free-text at creation time

### Definition of Done
- [ ] One rule fires per message, by priority
- [ ] Typo'd FAQ question still matches (trigram)
- [ ] Low confidence escalates
- [ ] Reply storm impossible
- [ ] Three logical reply parts → one send
- [ ] Scheduler double-run → one send
- [ ] Unmatched messages logged

### What NOT to build
LLM calls (ADR-007) · visual flow builder · multi-step conversation state · recurring schedules ·
bulk campaigns.

---

## PHASE 8 — Frontend  → F17–F20

**Objective:** A customer can self-serve.

### Tasks
- [ ] React + Vite + TS, Tailwind, router, TanStack Query
- [ ] API client: `credentials: 'include'`, CSRF, centralised error mapping
- [ ] Auth screens + app shell + protected routes + session bootstrap
- [ ] WhatsApp Connection screen: Embedded Signup popup, abandonment handling
- [ ] **Prominent payment-method warning** + two-bill explanation
- [ ] Automation rules UI (plain-language labels; regex behind "advanced")
- [ ] FAQ UI with a "test a question" box showing confidence
- [ ] Templates UI with Meta status/category badges and cost warnings
- [ ] Inbox: conversation list, thread, service-window countdown, reply disabled when closed
- [ ] Dashboard: **per-category message counts** (needed before 1 Oct 2026)
- [ ] Every screen: empty / loading / error / success states
- [ ] Mobile-first; test at 360px

### Definition of Done
- [ ] Login → shell; refresh keeps you logged in
- [ ] Embedded Signup completes from the UI
- [ ] Counts reconcile exactly with a direct ledger query
- [ ] Service-window state correct at the boundary
- [ ] Usable one-handed at 360px
- [ ] No secrets in the bundle
- [ ] No WebSockets

---

## PHASE 9 — Customer Onboarding  → F21

**Objective:** Take money, and have a repeatable onboarding.

### Tasks
- [ ] `V15__subscriptions.sql` + `payment_events`
- [ ] Razorpay Subscriptions, **UPI AutoPay preferred**
- [ ] Webhook with signature verification (same discipline as Meta)
- [ ] State machine driven **only** by verified webhooks
- [ ] `PAST_DUE` blocks sends, **never** login or export
- [ ] Dunning + 7-day grace
- [ ] GST invoice fields on the tenant (GSTIN, legal name, address)
- [ ] Write the onboarding call script (`10-OPERATIONS/CUSTOMER-ONBOARDING.md`)

### Definition of Done
- [ ] A real ₹1 test payment activates a tenant
- [ ] Unverified webhook rejected
- [ ] `PAST_DUE` degrades gracefully
- [ ] GST fields captured

---

## PHASE 10 — Testing  → F13 checkpoint

### Tasks
- [ ] All four multi-tenancy isolation tests
- [ ] Duplicate webhook tests
- [ ] Concurrency test for job claiming (real threads)
- [ ] Retry / permanent-failure tests
- [ ] Token encryption and non-logging tests
- [ ] End-to-end inbound → reply with a mocked Meta
- [ ] ArchUnit: `WhatsAppCloudClient` only called from `job.handler`
- [ ] `09-TESTING/PRE-PRODUCTION-CHECKLIST.md` fully ticked

### Definition of Done
- [ ] `./mvnw clean verify` green
- [ ] RLS-disabled test **fails** (proving it works)
- [ ] No test uses H2 or a superuser connection

---

## PHASE 11 — Production  → F22, F23

### Tasks
- [ ] `infra/provision.sh` — idempotent, works on **any** Ubuntu VPS
- [ ] Postgres tuned for 12 GB; **non-superuser app role**
- [ ] Caddy + auto-TLS + security headers + body limits
- [ ] systemd units: `wasaas-web`, `wasaas-worker` (same JAR, different profile)
- [ ] UFW (22/80/443), fail2ban, unattended-upgrades, swap
- [ ] Cron'd health task (Oracle reclaims idle instances)
- [ ] `infra/deploy.sh` with health check and rollback
- [ ] GitHub Actions deploy with manual approval
- [ ] `infra/backup.sh` — nightly `pg_dump` + WAL → **encrypted** → Backblaze B2
- [ ] Retention 7/4/3; automatic pruning
- [ ] **Backup heartbeat to Better Stack; alert on missing**
- [ ] Sentry with scrubbing; **test the scrubbing**
- [ ] Better Stack uptime monitor on a health endpoint that checks the DB
- [ ] `infra/restore.sh` + **perform a restore test and log it**

### Definition of Done
- [ ] Script rebuilds the box from scratch in under an hour
- [ ] HTTPS valid on the real domain
- [ ] Meta webhooks reach production
- [ ] Both services restart after reboot
- [ ] **You have restored a backup and verified it** — logged in the doc
- [ ] Stopping the app alerts you within 3 minutes (tested)
- [ ] Sentry scrubbing verified with a fake token

---

## PHASE 12 — First Customer

### Tasks
- [ ] Onboard customer #1 **manually, on a video call**
- [ ] Walk them through Embedded Signup
- [ ] **Verify their payment method is attached to Meta** — do not assume
- [ ] Create and submit their first template
- [ ] Configure 3–5 automation rules and FAQs with them
- [ ] Send a real test message
- [ ] Take payment
- [ ] Write down **every** point of confusion

### Definition of Done
- [ ] Their own WABA connected, owned by them
- [ ] Payment method confirmed attached
- [ ] At least one approved template
- [ ] A real message sent to a real customer of theirs
- [ ] They're paying
- [ ] You have a written list of confusions

---

## PHASE 13 — First 10 Customers
See `FIRST-10-CUSTOMERS.md`. **Do not build new infrastructure.**

---

## PHASE 14 — First 20 Customers
See `FIRST-20-CUSTOMERS.md`. Then re-read `12-SCALING/REVENUE-FUNDED-INFRASTRUCTURE.md` before
spending anything.
