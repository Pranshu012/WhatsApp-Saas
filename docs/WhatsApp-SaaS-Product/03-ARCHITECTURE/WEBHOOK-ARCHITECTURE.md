# Webhook Architecture

**Classification: BUILD NOW (increment F10).** Implementation detail in `05-BACKEND/WEBHOOK-IMPLEMENTATION.md`.

## The constraint that shapes everything

Meta retries webhooks that fail or respond slowly, and **repeatedly failing endpoints get
disabled**. A disabled webhook means the product silently stops working for every customer.

So the receiver does exactly three things and nothing else:

```text
Meta
 │
 ▼
Verify X-Hub-Signature-256   ← before parsing anything
 │
 ▼
Persist raw payload → webhook_events
 │
 ▼
Enqueue job → jobs
 │
 ▼
Return 200                    ← target p99 under 2 seconds
 ...
 (later, asynchronously)
 │
 ▼
Worker → business logic
```

**No outbound HTTP. No automation. No template lookups. No Meta API calls.** If it can be slow
or throw, it belongs in the worker.

---

## Verification endpoint (`GET`)

Meta calls this once when you configure the webhook:

```text
GET /api/webhooks/whatsapp?hub.mode=subscribe
                          &hub.verify_token=<your token>
                          &hub.challenge=<random string>
```

Return the challenge as plain text if `hub.verify_token` matches yours. Compare in **constant
time** — a timing-attack on a shared secret is cheap to prevent and free to get right.

---

## Signature verification (`POST`)

Meta signs the request body: `X-Hub-Signature-256: sha256=<hmac>`, HMAC-SHA256 over the **raw
request body** using your app secret.

Two things go wrong here constantly:

1. **Using the parsed-and-reserialised body.** Jackson may reorder keys or change whitespace,
   and the HMAC no longer matches. You need the **exact raw bytes**. Use
   `ContentCachingRequestWrapper`, a servlet filter that caches the body, or accept
   `@RequestBody byte[]`.
2. **Verifying after parsing.** If the payload is malformed or hostile, you've already parsed
   attacker-controlled input. Verify first.

Use `MessageDigest.isEqual` (constant time), not `String.equals`.

Invalid signature → **403**, log a warning with the source IP, do not process, do not persist
as valid.

---

## Raw event persistence

```sql
CREATE TABLE webhook_events (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id         TEXT NULL,           -- Meta's id where present
    waba_id          TEXT NULL,
    phone_number_id  TEXT NULL,
    raw_payload      JSONB NOT NULL,
    signature_valid  BOOLEAN NOT NULL,
    status           TEXT NOT NULL DEFAULT 'RECEIVED',
                     -- RECEIVED | PROCESSED | IGNORED | FAILED
    ignore_reason    TEXT NULL,
    received_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at     TIMESTAMPTZ NULL
);

CREATE UNIQUE INDEX idx_webhook_event_id ON webhook_events (event_id)
    WHERE event_id IS NOT NULL;
```

**Why store the raw payload:** when something goes wrong in three weeks, the raw JSON is the
only thing that tells you what Meta actually sent versus what you thought it sent. It is also
how you replay events after fixing a parsing bug. Cheap insurance.

Note this table has no `tenant_id` — at ingest time we haven't resolved the tenant yet (we have
a `phone_number_id`). The worker resolves tenant from `phone_number_id` → `whatsapp_accounts`.
Document this exception in the migration, and make sure nothing exposes this table through a
tenant-facing API.

---

## Deduplication

Meta may deliver the same event more than once — that's expected behaviour, not a bug.

**Two places you can dedupe:**

| Where | How | Trade-off |
|---|---|---|
| At ingest | Unique index on `event_id`; on conflict, ACK 200 without enqueuing | Simple, but not every event type carries a usable id |
| In the handler | Idempotency key on the job + idempotent business logic | Always works; slightly more code |

**Recommendation: both.** Ingest-level dedupe handles the common case cheaply; handler-level
idempotency is the guarantee. For inbound messages, `wamid` is a reliable dedupe key even when
`event_id` is absent.

Whichever you choose, decide explicitly and write it down — silent assumptions here produce
duplicate replies, and duplicate replies spend your customer's money.

---

## What the worker does with the event

```text
PROCESS_WEBHOOK_EVENT job
 │
 ├── resolve tenant from phone_number_id → whatsapp_accounts
 │
 ├── inbound message?
 │     ├── upsert contact
 │     ├── upsert conversation, set service_window_expires_at = msg_ts + 24h
 │     ├── insert message_ledger (INBOUND_FREE)
 │     └── publish InboundMessageReceived  → automation engine
 │
 ├── status callback?
 │     └── append message_ledger_status_events by wamid
 │
 └── unknown/unsupported type?
       └── mark webhook_events IGNORED with a reason — never throw, never silently drop
```

Marking unknown types `IGNORED` with a reason matters: Meta adds event types over time, and you
want a queryable record of "we received something we don't handle" rather than a stack trace
or silence.

---

## Failure behaviour

| Failure | Result |
|---|---|
| Signature invalid | 403, not processed. Meta will not retry a 403 — correct, it was not us. |
| DB write fails | 500 → Meta retries → dedupe handles the eventual duplicate |
| Worker handler throws | Job retries with backoff; the raw event is safe in `webhook_events` |
| Handler permanently fails | Job → `DEAD`; event stays `RECEIVED`; alert on `DEAD` count |
| Meta sends a shape we don't know | `IGNORED` with reason; no error, no data loss |

## DO NOT BUILD YET

A webhook replay UI · event sourcing off `webhook_events` · fan-out to multiple consumers ·
a separate webhook ingestion service · WebSocket push to the frontend.
