# Billing Ledger — Implementation

**Classification: BUILD NOW (F08).** Design and DDL in `03-ARCHITECTURE/MESSAGE-LEDGER.md`.

## `LedgerService` — the whole API

```java
public interface LedgerService {

    /** Called BEFORE the Meta API call. Returns the ledger row id. */
    UUID recordOutboundIntent(SendCommand cmd, WhatsAppAccount account,
                              BillingCategory category, ConversationWindow window);

    /** After a successful send. */
    void attachWamid(UUID ledgerId, String wamid);

    /** After a failed send. */
    void recordFailure(UUID ledgerId, String errorCode, String errorMessage);

    /** Inbound message — always free. */
    UUID recordInbound(String wamid, WhatsAppAccount account, String fromE164);

    /** Status webhook — APPENDS an event, never mutates history. */
    void recordStatusEvent(String wamid, String status, Instant at, JsonNode raw);

    /** Dashboard: per-category counts for a month. */
    Map<BillingCategory, Long> countByCategoryForMonth(UUID tenantId, YearMonth month);
}
```

## Determining the billing category

This is the part that's easy to get wrong, and getting it wrong misstates your customer's
expected bill.

```java
BillingCategory resolve(SendCommand cmd, Conversation conv, Instant now) {

    // Free entry point window (Click-to-WhatsApp ad) — everything free for 72h.
    // Stays free after 1 Oct 2026.
    if (conv.freeEntryPointExpiresAt() != null && now.isBefore(conv.freeEntryPointExpiresAt())) {
        return BillingCategory.FREE_ENTRY_POINT;
    }

    if (cmd.templateId() != null) {
        // Meta's ASSIGNED category, not what we requested
        return templateService.metaCategory(cmd.templateId());
    }

    // Free-form reply — only legal inside the service window.
    // FREE until 30 Sep 2026, BILLABLE from 1 Oct 2026 at the utility rate.
    return BillingCategory.SERVICE;
}
```

Note what this does **not** do: it does not decide whether the message is free. It records the
*category*. Whether that category is billable on a given date is a **rate-table** question, and
the rate table is dated — which is exactly why the 1 October change requires no code change,
only a new `whatsapp_rates` row.

## The ordering rule, restated

```java
UUID ledgerId = ledger.recordOutboundIntent(...);   // 1
var result   = client.send(...);                    // 2
ledger.attachWamid(ledgerId, result.wamid());       // 3
```

A crash between 2 and 3 leaves a ledger row without a `wamid` — recoverable, and reconciled when
the status webhook arrives. A crash between 2 and a *post-hoc* write would leave a charged
message with no record at all.

Add a reconciliation query to the runbook: ledger rows older than 10 minutes, `OUTBOUND`, with
`wamid IS NULL` and no failure recorded. That's your "we may have sent something we didn't
record" list, and it should normally be empty.

## Monthly counts

```sql
SELECT billing_category, count(*) AS messages
FROM   message_ledger
WHERE  tenant_id = :tenantId
  AND  direction = 'OUTBOUND'
  AND  status <> 'failed'                  -- Meta bills only DELIVERED messages
  AND  created_at >= :monthStart
  AND  created_at <  :monthEnd
GROUP  BY billing_category;
```

Excluding `failed` matters. Counting failed sends overstates the customer's expected bill, and
the moment your numbers don't reconcile with Meta's invoice, the customer stops trusting them —
which defeats the entire purpose of the ledger.

## Cost estimation — optional, and careful

```java
public Money estimateMonth(UUID tenantId, YearMonth month) {
    var counts = countByCategoryForMonth(tenantId, month);
    long paise = counts.entrySet().stream()
        .mapToLong(e -> e.getValue() * rateRepository
            .findEffective("IN", e.getKey(), month.atDay(1)).rateMinor())
        .sum();
    return Money.ofPaise(paise);   // integer arithmetic throughout
}
```

Two non-negotiables:
1. **Rates come from the dated `whatsapp_rates` table.** Never constants. Meta revises quarterly.
2. **Label it an estimate, and say Meta's invoice is authoritative.** Add 18% GST separately, or
   state clearly that GST is excluded. A confidently wrong number is worse than no number.

## Seeding rates

```sql
-- Meta list rates, India, effective 2026-07-01. Verified 2026-08-18.
-- Source: docs/.../08-META-WHATSAPP/MESSAGE-PRICING.md
INSERT INTO whatsapp_rates (country_code, category, rate_minor, effective_from, source_note) VALUES
  ('IN', 'MARKETING',      8631,  '2026-07-01', 'Meta list rate, verified 2026-08-18'),
  ('IN', 'UTILITY',        1150,  '2026-07-01', 'Meta list rate, verified 2026-08-18'),
  ('IN', 'AUTHENTICATION', 1150,  '2026-07-01', 'Meta list rate, verified 2026-08-18'),
  ('IN', 'SERVICE',           0,  '2026-07-01', 'Free until 2026-09-30'),
  ('IN', 'SERVICE',        1150,  '2026-10-01', 'Billable from 2026-10-01 per Meta announcement'),
  ('IN', 'INBOUND_FREE',      0,  '2026-07-01', 'Inbound always free');
```

Rates are in **paise × 100** here (₹0.8631 = 8631 hundredths of a paisa) because Meta's rates have
four decimal places. Pick a scale, document it in the column comment, and be consistent — this is
the kind of thing that silently produces bills off by 100×.

## Test cases

| Test | Expect |
|---|---|
| Intent recorded before send | Ledger row exists with `wamid IS NULL` |
| `wamid` attached after send | Same row updated |
| Status events append | Parent row's history never rewritten |
| Monthly counts | Match a hand-written query |
| Failed messages excluded from counts | — |
| Same idempotency key | One ledger row |
| Full phone number absent | Only hash + last4 stored |
| Rate lookup respects `effective_from` | SERVICE = 0 on 2026-09-30, 1150 on 2026-10-01 |
