# Message Categories

**Classification: BUILD NOW.** The category decides whether you pay and how much — and it's your
customer's money.

## The five categories

| Category | What it is | Template? | India rate (ex-GST) | Volume tiers |
|---|---|---|---|---|
| **Marketing** | Promotions, offers, re-engagement | Yes | ₹0.8631 | **None, ever** |
| **Utility** | Order/appointment/transaction updates | Yes | ₹0.1150 | From 25M/mo |
| **Authentication** | OTPs, login codes | Yes | ₹0.1150 | From 750k/mo |
| **Service** | Free-form reply inside the 24h window | No | ₹0 → **₹0.1150 from 1 Oct 2026** | **None, ever** |
| **Inbound** | Messages customers send you | — | **Free, always** | — |

Rates verified 18 August 2026, effective 1 July 2026. Meta revises quarterly.

## The windows

```text
Customer messages the business
        │
        ▼
┌─────────────────────────────────────────┐
│  24-hour CUSTOMER SERVICE WINDOW        │
│  • free-form replies PERMITTED           │
│  • free until 30 Sep 2026                │
│  • billable from 1 Oct 2026              │
│  • resets with each new customer message  │
└─────────────────────────────────────────┘
        │ window closes
        ▼
Only APPROVED TEMPLATES may be sent


Customer arrives via Click-to-WhatsApp ad / Page CTA,
you reply within 24h
        │
        ▼
┌─────────────────────────────────────────┐
│  72-hour FREE ENTRY POINT WINDOW         │
│  • EVERYTHING free, including templates  │
│  • STAYS FREE after 1 Oct 2026           │
└─────────────────────────────────────────┘
```

The Free Entry Point window is the most under-used cost lever available. If a customer runs
Click-to-WhatsApp ads, three days of completely free messaging follows each conversation.

The service window is **independent** of the Free Entry Point window — they run on separate
clocks.

## How the category is decided

```text
Is it inbound?                          → INBOUND, free
Is a Free Entry Point window open?      → FREE_ENTRY_POINT, free (even templates)
Is it a template?                       → Meta's ASSIGNED category for that template
Otherwise (free-form, window open)      → SERVICE
```

Note that our code records the **category**; whether that category costs anything on a given date
is a question for the dated `whatsapp_rates` table. That separation is why the 1 October change
needs a database row, not a deploy.

## Cost implications to design around

**Marketing is 7.5× utility, with no volume discount at any scale.** Consequences:

1. Template mis-categorisation is expensive. Surface Meta's assigned category prominently.
2. Meta also caps marketing messages a single user can receive across **all** businesses at
   roughly 2 per 24 hours. Error 131049 means you were blocked for this reason, and it cannot be
   worked around with extra numbers or BSPs. Your customer's marketing deliverability partly
   depends on what other brands are sending that person.
3. Marketing is where a customer's bill gets surprising. One blast to 2,000 contacts is ~₹2,037
   with GST — more than our software fee.

**After 1 October, support-heavy customers see a new line item.** No volume tiers means it scales
linearly. See `OCTOBER-2026-BILLING-CHANGE.md`.

## In our schema

```java
public enum BillingCategory {
    MARKETING, UTILITY, AUTHENTICATION, SERVICE, INBOUND_FREE, FREE_ENTRY_POINT
}
```

Stored on every `message_ledger` row. Also capture the `pricing` object from status webhooks —
Meta tells you what it actually billed (`billable`, `category`), which beats our inference and is
the cleanest reconciliation source.

## Practical guidance for customers

| Goal | Use | Why |
|---|---|---|
| Appointment reminder | Utility template | 7.5× cheaper than marketing |
| Order update | Utility template | Same |
| OTP | Authentication template | Cheapest, purpose-built |
| Answer a question | Service reply in the window | Free until Oct, then cheap |
| Promotion | Marketing template | No way around the cost |
| Re-engage a lapsed customer | Marketing template | Expensive — target carefully |
| Anything after a Click-to-WhatsApp ad | Anything, within 72h | **Free** |

Building this guidance into the UI — the right nudge at the moment of choosing a template — is
worth more to your customers than most features you could ship.
