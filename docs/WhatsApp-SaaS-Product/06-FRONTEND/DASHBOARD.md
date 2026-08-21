# Dashboard

Increment **F20**. Route `/`.

## Purpose

Answer three questions in five seconds:
1. Is my WhatsApp connection healthy?
2. How many messages have I sent this month, by category?
3. Is anything broken?

That's it. No vanity charts.

## Layout (mobile-first, 360px)

```text
┌─────────────────────────────────────┐
│ ⚠ WARNING BANNERS (only if real)    │
│   • No payment method on Meta       │
│   • Subscription past due           │
│   • WhatsApp quality rating LOW     │
│   • Token expired — reconnect       │
├─────────────────────────────────────┤
│ Connection                          │
│  +91 ●●●●●● 4321 · Verified · GREEN │
│  Messaging limit: 1,000/24h         │
├─────────────────────────────────────┤
│ This month (Aug 2026)               │
│  Marketing        124               │
│  Utility          891               │
│  Authentication     0               │
│  Service          412               │
│  Inbound (free) 1,203               │
│  ─────────────────────              │
│  Meta bills you directly for these. │
│  Meta's invoice is authoritative.   │
├─────────────────────────────────────┤
│ Delivery (last 7 days)              │
│  Sent 1,402 · Delivered 1,371       │
│  Read 902 · Failed 31  [view]       │
├─────────────────────────────────────┤
│ Automation (last 7 days)            │
│  Auto-replied 834                   │
│  Unmatched     97  [review] ←       │
└─────────────────────────────────────┘
```

## Message counts

Source: `message_ledger`, one indexed query on `(tenant_id, billing_category, created_at)`.

```sql
SELECT billing_category, count(*)
FROM message_ledger
WHERE tenant_id = :tenantId
  AND created_at >= :monthStart
  AND created_at <  :monthEnd
  AND direction = 'OUTBOUND'
GROUP BY billing_category;
```

**Why this matters commercially:** from 1 October 2026 service messages and in-window utility
templates become billable in India. Your customers will get a bigger Meta bill and will ask
you why. This screen is your answer. Ship it before then, not after.

## Rupee estimates — be careful

Do **not** show rupee amounts until the dated `whatsapp_rates` table is populated and current.
A wrong cost estimate is worse than no estimate: the customer will trust your number, get
Meta's invoice, and lose trust in everything else you show them.

When you do show it:
- Read rates from `whatsapp_rates` by effective date, never from constants — Meta revises
  quarterly (1 Jan / 1 Apr / 1 Jul / 1 Oct)
- Label it clearly as an estimate, exclusive of 18% GST
- State that Meta's invoice is authoritative

Current India rates for reference are in `../08-META-WHATSAPP/MESSAGE-PRICING.md`.

## Unmatched messages count

Link it prominently. This is the customer's own to-do list — each unmatched question is one
FAQ entry away from being answered automatically. It drives retention better than any chart.

## Not on the dashboard

Time-series graphs · response-time percentiles · contact growth curves · comparisons to "other
businesses" · anything requiring a charting library. Add a chart when a customer asks for that
specific chart.
