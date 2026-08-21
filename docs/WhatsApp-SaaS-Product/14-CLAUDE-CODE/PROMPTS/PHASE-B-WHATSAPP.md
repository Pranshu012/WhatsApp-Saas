# Phase B Prompts — WhatsApp Core (F05–F11)

**This is the hardest phase. Go slowly.** Every increment here has a failure mode that
costs your customer real money or leaks data.

---

## F05 — WhatsApp account model and token encryption

```text
Increment F05. Read docs/WhatsApp-SaaS-Product/03-ARCHITECTURE/WHATSAPP-INTEGRATION.md and
docs/WhatsApp-SaaS-Product/11-SECURITY-COMPLIANCE/SECRETS-MANAGEMENT.md.

Goal: storage for customer WhatsApp accounts, with encrypted tokens. NO Meta API calls yet.

Context: we are a Meta Tech Provider. Each customer owns their own WABA and pays Meta
directly. We store a token scoped to their WABA so we can operate it on their behalf.

Requirements:
- Migration V6__whatsapp_accounts.sql:
  - whatsapp_accounts: id, tenant_id NOT NULL, waba_id, phone_number_id,
    display_phone_number, verified_name, quality_rating, messaging_limit_tier,
    access_token_encrypted (bytea), token_encrypted_at, status, connected_at,
    created_at, updated_at
  - unique constraint on (tenant_id, phone_number_id)
  - RLS policy following the pattern documented in V3__rls.sql
- whatsapp/crypto/TokenCipher: AES-256-GCM. Key from env var (base64), never in code or
  Git. Store the IV/nonce alongside the ciphertext. Provide encrypt(String) and
  decrypt(bytes).
- The entity must NOT expose the decrypted token via a getter that Jackson could serialise.
  Decryption goes through an explicit service method only.
- WhatsAppAccountRepository, WhatsAppAccountService with basic CRUD
- Add a check that fails application startup if the encryption key env var is missing or
  not 32 bytes. Fail fast, don't run insecurely.

Tests: encrypt/decrypt round-trip; ciphertext differs across two encryptions of the same
plaintext (nonce is random); the token cannot be read from the DB without the key;
serialising the entity to JSON never contains the token; missing key fails startup.

Do NOT build: Embedded Signup, any Meta HTTP calls, token refresh.

Plan first, and in the plan state exactly where the key comes from in local dev vs production.

Finally: write docs/IMPLEMENTATION/F05-<slug>.md following
docs/WhatsApp-SaaS-Product/14-CLAUDE-CODE/IMPLEMENTATION-DOC-TEMPLATE.md, and add a row to
docs/IMPLEMENTATION/INDEX.md. Document the key decisions, any divergence from the architecture
docs, and the gotchas someone would trip over. This is part of the increment, not optional.
```

**DoD:** round-trip works · token opaque in DB · never in JSON · startup fails without a valid key

---

## F06 — Embedded Signup callback

```text
Increment F06. Read docs/WhatsApp-SaaS-Product/08-META-WHATSAPP/EMBEDDED-SIGNUP.md and
docs/WhatsApp-SaaS-Product/08-META-WHATSAPP/TECH-PROVIDER-SETUP.md carefully. This is the
core of our business model — get it right.

Goal: a customer completes Meta's Embedded Signup in the browser and we end up holding an
encrypted, WABA-scoped token plus their WABA and phone number IDs.

Flow: frontend launches Meta's popup → Meta returns an exchangeable code to the browser →
frontend POSTs the code to us → we exchange it for a business token → we fetch WABA details
→ we subscribe our app to that WABA's webhooks → we persist everything.

Requirements:
- POST /api/whatsapp/connect { code, wabaId, phoneNumberId } (Meta's session info gives the
  IDs; still verify them against the API rather than trusting the client)
- TokenExchangeService: exchange code → business token against Meta's Graph API
- Fetch and store WABA details: verified name, display phone number, quality rating,
  messaging limit tier
- Subscribe our app to the WABA's webhook fields (this is a separate Graph API call — do
  not forget it, nothing arrives without it)
- Persist via F05's WhatsAppAccountService with the token encrypted
- Idempotent: re-running connect for the same (tenant, phone_number_id) updates rather than
  duplicating
- All Graph API calls through a single MetaGraphClient with configurable base URL and
  Graph API version (version pinned in config, not hardcoded in call sites), timeouts,
  and typed error handling that maps Meta error codes to our DomainException
- Meta error code 200 specifically means our app lacks Advanced Access — surface that as a
  clear, actionable message, not a generic 500
- Log the WABA id and phone number id. NEVER log the code or the token.

Tests: successful connect persists an encrypted token and subscribes webhooks (mock the
Graph API with WireMock or MockWebServer); re-connect is idempotent; Meta error 200 produces
a clear message; token never appears in logs or the response.

Do NOT build: the frontend popup (F18), template sync (F12), sending messages (F09).

Plan first. Flag anything in the docs you think is ambiguous rather than guessing.

Finally: write docs/IMPLEMENTATION/F06-<slug>.md following
docs/WhatsApp-SaaS-Product/14-CLAUDE-CODE/IMPLEMENTATION-DOC-TEMPLATE.md, and add a row to
docs/IMPLEMENTATION/INDEX.md. Document the key decisions, any divergence from the architecture
docs, and the gotchas someone would trip over. This is part of the increment, not optional.
```

**DoD:** your own test business connects end to end · webhooks subscribed · token encrypted · idempotent · error 200 handled clearly

**Blocked on:** Meta App Review approval for Advanced Access — **only if your app is in live
mode**. On the pilot track your app stays in development mode, where Standard Access plus a
Tester role is sufficient, so this is **not blocked**. See
`../../08-META-WHATSAPP/PILOT-MODE-SETUP.md`.

---

## F07 — Jobs table and worker

```text
Increment F07. Read docs/WhatsApp-SaaS-Product/03-ARCHITECTURE/BACKGROUND-JOBS.md and
docs/WhatsApp-SaaS-Product/05-BACKEND/JOB-PROCESSING.md fully.

Goal: a durable job queue using ONLY Postgres. No Redis, no broker (see ADR-002).

Requirements:
- Migration V7__jobs.sql:
  - jobs: id, tenant_id (nullable for system jobs — document why), job_type,
    payload (jsonb), status (PENDING/RUNNING/SUCCEEDED/FAILED/DEAD),
    idempotency_key (unique, nullable), attempts, max_attempts,
    run_after (timestamptz), locked_at, locked_by, last_error, created_at, updated_at
  - index on (status, run_after) for the claim query
  - unique index on idempotency_key where not null
- Claim query MUST use SELECT ... FOR UPDATE SKIP LOCKED with a LIMIT, in a transaction.
  Show me the exact SQL in the plan.
- JobHandler interface: String jobType(); void handle(Job job);
  Handlers registered via Spring so adding a job type needs no changes to the worker.
- JobWorker: @Scheduled poller, active only under the "worker" Spring profile. Configurable
  poll interval, batch size, and lock timeout.
- Retry: exponential backoff with jitter (e.g. 2^attempts seconds, capped). On exceeding
  max_attempts → status DEAD, never infinite retry.
- Stale lock recovery: a job RUNNING with locked_at older than the lock timeout becomes
  claimable again. This is what makes a crash mid-job safe.
- Distinguish retryable from permanent failures: a handler throwing PermanentJobException
  goes straight to DEAD without retries.
- JobService.enqueue(...) with optional idempotency key — enqueueing the same key twice
  must not create two jobs.
- Application must run correctly with --spring.profiles.active=worker (worker only) and
  with "web" (no polling).

Tests (all mandatory):
1. Two concurrent workers never claim the same job (drive this with real concurrent threads
   against Testcontainers Postgres, not mocks)
2. Killing a worker mid-job leaves the job re-claimable after the lock timeout
3. Retries back off and eventually reach DEAD
4. PermanentJobException → DEAD immediately, attempts not exhausted
5. Duplicate idempotency key → one job

Do NOT build: specific job handlers (F09/F11), a jobs admin UI, priorities, cron scheduling.

Plan first, and include the exact claim SQL.

Finally: write docs/IMPLEMENTATION/F07-<slug>.md following
docs/WhatsApp-SaaS-Product/14-CLAUDE-CODE/IMPLEMENTATION-DOC-TEMPLATE.md, and add a row to
docs/IMPLEMENTATION/INDEX.md. Document the key decisions, any divergence from the architecture
docs, and the gotchas someone would trip over. This is part of the increment, not optional.
```

**DoD:** all five tests pass · web and worker profiles both run correctly · no new dependency added

---

## F08 — Message ledger

```text
Increment F08. Read docs/WhatsApp-SaaS-Product/03-ARCHITECTURE/MESSAGE-LEDGER.md,
docs/WhatsApp-SaaS-Product/05-BACKEND/BILLING-LEDGER.md and
docs/WhatsApp-SaaS-Product/08-META-WHATSAPP/OCTOBER-2026-BILLING-CHANGE.md.

Goal: an append-only record of every message, tagged by billing category. This is our
billing evidence and, after 1 Oct 2026, the only way to explain a customer's Meta invoice.

Requirements:
- Migration V8__message_ledger.sql:
  - message_ledger: id, tenant_id NOT NULL, whatsapp_account_id, direction (INBOUND/OUTBOUND),
    wamid (Meta's message id, nullable until the API responds), recipient_phone_hash,
    recipient_phone_last4, billing_category (MARKETING/UTILITY/AUTHENTICATION/SERVICE/
    INBOUND_FREE), template_name, conversation_window (IN_WINDOW/OUT_OF_WINDOW/FREE_ENTRY_POINT),
    status, status_at, idempotency_key, job_id, error_code, error_message, created_at
  - message_ledger_status_events: id, ledger_id, status, occurred_at, raw_payload jsonb
    — status transitions are NEW ROWS here, never updates to message_ledger
  - unique index on (tenant_id, wamid) where wamid is not null
  - index on (tenant_id, billing_category, created_at) for monthly counts
  - RLS policies per the V3 pattern
- Store the recipient phone number HASHED plus last 4 digits only. We do not need the full
  number in the ledger and storing it increases our DPDP exposure.
- LedgerService:
  - recordOutboundIntent(...) → called BEFORE the Meta API call, returns the ledger id
  - attachWamid(ledgerId, wamid) → after a successful send
  - recordFailure(ledgerId, errorCode, message)
  - recordStatusEvent(wamid, status, payload) → appends to status_events
  - countByCategoryForMonth(tenantId, yearMonth) → single indexed query
- Add a DB-level guard against mutation of message_ledger rows after creation where
  practical (trigger or revoked UPDATE), and explain the tradeoff in the plan.

Tests: outbound intent recorded before send; wamid attached after; status events append and
never mutate the parent; monthly per-category counts correct; two sends with the same
idempotency key produce one ledger row; full phone number is not stored anywhere.

Do NOT build: cost calculation in rupees (rates live in config, added in a later increment),
invoicing, a UI.

Plan first.

Finally: write docs/IMPLEMENTATION/F08-<slug>.md following
docs/WhatsApp-SaaS-Product/14-CLAUDE-CODE/IMPLEMENTATION-DOC-TEMPLATE.md, and add a row to
docs/IMPLEMENTATION/INDEX.md. Document the key decisions, any divergence from the architecture
docs, and the gotchas someone would trip over. This is part of the increment, not optional.
```

**DoD:** ledger-first ordering enforced · append-only proven by test · monthly counts one query · no full phone numbers

---

## F09 — Outbound send

```text
Increment F09. Read docs/WhatsApp-SaaS-Product/05-BACKEND/WHATSAPP-SERVICE.md.

Goal: send a WhatsApp message, asynchronously, idempotently, with a ledger trail.

Requirements:
- WhatsAppCloudClient: send text, send template, send interactive (buttons/list) against
  the Cloud API using the tenant's decrypted token. Timeouts, and typed mapping of Meta
  error codes.
- SendMessageJobHandler (job_type = SEND_WHATSAPP_MESSAGE):
  1. load tenant's whatsapp account
  2. recordOutboundIntent on the ledger (BEFORE the API call)
  3. call the API
  4. attachWamid on success / recordFailure on error
- MessagingService.send(...) enqueues a job with an idempotency key derived from a caller
  supplied key. It must NOT call the API directly. Nothing in the codebase may call
  WhatsAppCloudClient outside the job handler — call this out in the plan.
- Classify Meta errors: rate limits and 5xx are retryable; invalid template, invalid number,
  and permission errors are PermanentJobException.
- Respect Meta's per-number rate limits with a simple per-account throttle. In-process is
  fine for now — we are single-instance. Add a TODO noting this needs Redis when we go
  multi-instance (see 12-SCALING/WHEN-TO-INTRODUCE-REDIS.md). Do NOT add Redis now.

Tests: enqueue → worker sends → ledger has wamid (mock Meta); retryable error retries;
permanent error goes DEAD with no retry; same idempotency key sends once even if enqueued
twice; nothing calls the client synchronously from a controller.

Do NOT build: templates management (F12), scheduling (F16), bulk campaigns.

Plan first.

Finally: write docs/IMPLEMENTATION/F09-<slug>.md following
docs/WhatsApp-SaaS-Product/14-CLAUDE-CODE/IMPLEMENTATION-DOC-TEMPLATE.md, and add a row to
docs/IMPLEMENTATION/INDEX.md. Document the key decisions, any divergence from the architecture
docs, and the gotchas someone would trip over. This is part of the increment, not optional.
```

**DoD:** real message sent to your own number via the queue · ledger complete · error classes correct · one send per idempotency key

---

## F10 — Webhook receiver

```text
Increment F10. Read docs/WhatsApp-SaaS-Product/05-BACKEND/WEBHOOK-IMPLEMENTATION.md and
docs/WhatsApp-SaaS-Product/03-ARCHITECTURE/WEBHOOK-ARCHITECTURE.md fully.

Goal: receive Meta webhooks safely and fast. ACK under 2 seconds, always.

Requirements:
- GET /api/webhooks/whatsapp — Meta verification handshake (hub.mode, hub.verify_token,
  hub.challenge). Compare verify_token in constant time.
- POST /api/webhooks/whatsapp:
  1. verify X-Hub-Signature-256 HMAC-SHA256 over the RAW request body using the app secret,
     constant-time comparison. This MUST happen before any JSON parsing. You will need the
     raw bytes — use ContentCachingRequestWrapper or an HttpMessageConverter approach and
     explain your choice.
  2. invalid signature → 403, log a warning, do not process
  3. persist the raw payload to webhook_events
  4. enqueue a processing job
  5. return 200
- Migration V9__webhook_events.sql: id, event_id (Meta's id where available), waba_id,
  phone_number_id, raw_payload jsonb, signature_valid, received_at, processed_at, status
  - unique index on event_id where not null, for deduplication
- Deduplication: the same event delivered twice must produce exactly one logical effect.
  Decide whether you dedupe at ingest or in the handler and justify it in the plan.
- ABSOLUTELY NO business logic and NO outbound HTTP in the controller or ingest service.
- Structured logging of receipt (event id, waba id, latency). Never log full message bodies.

Tests: valid signature → 200 and persisted; tampered body → 403 and not persisted as valid;
missing signature header → 403; duplicate event → one effect; a slow downstream does not
slow the ACK (assert the controller does not touch the job handler); p99 under 2s with a
simple benchmark test.

Do NOT build: parsing message content into domain objects (F11).

Plan first, and be explicit about how you get the raw body bytes for HMAC.

Finally: write docs/IMPLEMENTATION/F10-<slug>.md following
docs/WhatsApp-SaaS-Product/14-CLAUDE-CODE/IMPLEMENTATION-DOC-TEMPLATE.md, and add a row to
docs/IMPLEMENTATION/INDEX.md. Document the key decisions, any divergence from the architecture
docs, and the gotchas someone would trip over. This is part of the increment, not optional.
```

**DoD:** Meta handshake passes · bad signature 403 · duplicates safe · p99 <2s measured · zero outbound calls in the controller

---

## F11 — Inbound message processing

```text
Increment F11.

Goal: turn raw webhook events into domain data — contacts, conversations, ledger entries,
delivery status.

Requirements:
- Migration V10__contacts_conversations.sql:
  - contacts: id, tenant_id NOT NULL, phone_e164, phone_hash, display_name, last_seen_at,
    opt_in_status, created_at, updated_at; unique (tenant_id, phone_e164)
  - conversations: id, tenant_id NOT NULL, contact_id, whatsapp_account_id,
    last_inbound_at, last_outbound_at, service_window_expires_at, status, created_at,
    updated_at
  - RLS policies per V3 pattern
- ProcessWebhookEventHandler (job type PROCESS_WEBHOOK_EVENT), handling two event shapes:
  - inbound messages: upsert contact, upsert/refresh conversation, set
    service_window_expires_at = message timestamp + 24h, write an INBOUND_FREE ledger entry,
    publish an InboundMessageReceived Spring event (F13 will consume it)
  - status callbacks: recordStatusEvent on the ledger by wamid
- The 24-hour service window matters commercially: after 1 Oct 2026 replies inside it are
  billable. Track service_window_expires_at accurately and expose it — do not approximate.
- Handle unknown or unsupported event types by marking the webhook_event as IGNORED with a
  reason. Never throw and never silently drop.
- Contacts store the full phone number (we need it to reply); the LEDGER stores only hash +
  last4. Keep that distinction — note it in the plan.

Tests: inbound message creates contact + conversation + ledger; repeat inbound updates
rather than duplicating; service window computed correctly across a timezone boundary;
status callback appends a status event; unknown event type marked IGNORED not failed;
inbound for tenant A never visible to tenant B.

Do NOT build: automation replies (F13), the inbox UI (F20).

Plan first.

Finally: write docs/IMPLEMENTATION/F11-<slug>.md following
docs/WhatsApp-SaaS-Product/14-CLAUDE-CODE/IMPLEMENTATION-DOC-TEMPLATE.md, and add a row to
docs/IMPLEMENTATION/INDEX.md. Document the key decisions, any divergence from the architecture
docs, and the gotchas someone would trip over. This is part of the increment, not optional.
```

**DoD:** real inbound message produces contact + conversation + ledger · service window accurate · status callbacks land · unknown types handled

---

## Phase B complete — checkpoint

Stop and verify properly before Phase C:

- [ ] Full round trip: someone messages your test number → contact and conversation created
      → you enqueue a reply → it sends → status callback recorded
- [ ] `./mvnw clean verify` green
- [ ] Kill the app mid-send; restart; confirm no duplicate message reached the phone
- [ ] Search the codebase for the token: it appears only in `TokenCipher` and the account service
- [ ] `grep -ri "token" logs/` returns nothing sensitive
- [ ] `CURRENT-STATUS.md` updated

If any of these fail, fix it now. Phase C builds directly on all of it.
