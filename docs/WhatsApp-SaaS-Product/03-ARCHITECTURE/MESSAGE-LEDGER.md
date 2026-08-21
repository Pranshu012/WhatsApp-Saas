# Message Ledger

**Classification: BUILD NOW (increment F08).** This is your billing evidence.

## Why this exists

Our customer pays Meta directly (ADR-005). So when their Meta invoice says ₹4,000, they will
ask **us** why. We do not see their invoice. Without an independent, immutable record of every
message we sent on their behalf — tagged by billing category — we cannot answer.

From **1 October 2026** this becomes urgent: service messages (free-form replies inside the
24-hour window) and in-window utility templates become billable at ~₹0.115 each in India.
Reply-heavy customers will see their bill go from ₹0 to something real, and "why?" becomes the
most common support ticket.

Three jobs:
1. **Billing reconciliation** — explain a customer's Meta bill
2. **Delivery audit** — "did my message send?" answered with evidence
3. **Product analytics** — which categories, which templates, which volumes

---

## Design: append-only, always

```sql
CREATE TABLE message_ledger (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id              UUID NOT NULL REFERENCES tenants(id),
    whatsapp_account_id    UUID NOT NULL REFERENCES whatsapp_accounts(id),
    direction              TEXT NOT NULL,          -- INBOUND | OUTBOUND
    wamid                  TEXT NULL,              -- Meta's message id
    recipient_phone_hash   TEXT NOT NULL,          -- SHA-256, for grouping
    recipient_phone_last4  TEXT NOT NULL,          -- for human support
    billing_category       TEXT NOT NULL,
        -- MARKETING | UTILITY | AUTHENTICATION | SERVICE | INBOUND_FREE
    conversation_window    TEXT NULL,
        -- IN_WINDOW | OUT_OF_WINDOW | FREE_ENTRY_POINT
    template_name          TEXT NULL,
    status                 TEXT NOT NULL,          -- latest status, denormalised for queries
    status_at              TIMESTAMPTZ NULL,
    idempotency_key        TEXT NULL,
    job_id                 UUID NULL,
    error_code             TEXT NULL,
    error_message          TEXT NULL,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_ledger_wamid ON message_ledger (tenant_id, wamid)
    WHERE wamid IS NOT NULL;
CREATE INDEX idx_ledger_monthly ON message_ledger (tenant_id, billing_category, created_at);
CREATE INDEX idx_ledger_recipient ON message_ledger (tenant_id, recipient_phone_hash, created_at);

CREATE TABLE message_ledger_status_events (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ledger_id    UUID NOT NULL REFERENCES message_ledger(id),
    status       TEXT NOT NULL,      -- sent | delivered | read | failed
    occurred_at  TIMESTAMPTZ NOT NULL,
    raw_payload  JSONB NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

Both tables get RLS policies per the standard pattern.

### Append-only means append-only

Status transitions are **new rows** in `message_ledger_status_events`. The `status` column on
`message_ledger` is a denormalised convenience for queries, updated only by the status-event
handler — never by business logic.

Consider a trigger or revoked `UPDATE` grant to enforce immutability at the database level.
The tradeoff: it complicates the denormalised `status` column. A reasonable middle ground is a
trigger that permits updating only `status`, `status_at`, `wamid`, `error_code`, and
`error_message`, and rejects changes to `tenant_id`, `billing_category`, `created_at`, or
`recipient_phone_hash`.

---

## Ledger-first ordering — non-negotiable

```java
// CORRECT
UUID ledgerId = ledgerService.recordOutboundIntent(...);   // 1. record intent
SendResult result = whatsAppClient.send(...);              // 2. call Meta
ledgerService.attachWamid(ledgerId, result.wamid());       // 3. record outcome
```

```java
// WRONG — a crash between send and record loses all evidence of a charged message
SendResult result = whatsAppClient.send(...);
ledgerService.record(result);
```

If we crash after step 2 but before step 3, we have a ledger row with no `wamid` — recoverable,
and reconciled when the status webhook arrives. If we crashed with the wrong ordering, our
customer was charged for a message we have no record of. That is the one situation we cannot
explain.

---

## Phone number handling — deliberate asymmetry

| Table | Stores | Why |
|---|---|---|
| `contacts` | Full E.164 number | We need it to actually send messages |
| `message_ledger` | SHA-256 hash + last 4 digits | We don't need the full number here, and storing it multiplies our DPDP exposure across millions of rows |

The hash lets you group by recipient. Last 4 digits let a support conversation work
("the message to the number ending 4821"). Neither reconstructs the number.

---

## The queries that matter

**Monthly count per category** — this is what the dashboard shows and what the customer
reconciles against Meta:

```sql
SELECT billing_category, count(*) AS messages
FROM   message_ledger
WHERE  tenant_id = :tenantId
  AND  direction = 'OUTBOUND'
  AND  status <> 'failed'
  AND  created_at >= date_trunc('month', now())
GROUP  BY billing_category;
```

Note `status <> 'failed'`: **Meta bills only delivered messages.** Counting failed sends
would overstate the customer's expected bill and destroy trust in your numbers.

**Delivery outcome breakdown:**
```sql
SELECT status, count(*) FROM message_ledger
WHERE tenant_id = :tenantId AND created_at >= date_trunc('month', now())
GROUP BY status;
```

---

## Cost estimation — handle with care

You *can* multiply counts by the rate config to show an estimated rupee figure. Two rules:

1. **Rates come from a config table, dated** — never constants. Meta revises quarterly.
2. **Label it an estimate and say Meta's invoice is authoritative.** A wrong cost estimate is
   worse than no estimate, because the customer will trust it and then be surprised.

```sql
CREATE TABLE whatsapp_rates (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_code   TEXT NOT NULL,          -- 'IN'
    category       TEXT NOT NULL,
    rate_minor     BIGINT NOT NULL,        -- paise, integer — never floating point for money
    currency       TEXT NOT NULL DEFAULT 'INR',
    effective_from DATE NOT NULL,
    effective_to   DATE NULL,
    source_note    TEXT                    -- 'Meta list rate, verified 2026-08-18'
);
```

Seed with the rates from `08-META-WHATSAPP/MESSAGE-PRICING.md`. Add a re-verification reminder
each quarter.

---

## DO NOT BUILD YET

Invoicing · payment collection for messages (we never touch that money — ADR-005) ·
cost forecasting · per-template ROI analytics · CSV export.

The MVP need is: accurate counts, per category, per month, per tenant, visible in the UI
before 1 October 2026.
