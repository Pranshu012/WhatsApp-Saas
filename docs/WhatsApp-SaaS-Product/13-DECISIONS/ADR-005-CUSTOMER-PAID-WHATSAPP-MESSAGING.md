# ADR-005 — Customer Pays Meta Directly for Messaging

**Status:** Accepted · 18 August 2026 · **Defines our unit economics**

## Context
WhatsApp messaging is a real, unavoidable, variable cost. India rates verified 18 Aug 2026
(Meta list, effective 1 July 2026, INR, **excluding 18% GST**):

| Category | Rate | Volume tiers |
|---|---|---|
| Marketing | ₹0.8631 | None, ever |
| Utility | ₹0.1150 | From 25M/month |
| Authentication | ₹0.1150 | From 750k/month |
| Service (free-form reply in 24h window) | ₹0 → **₹0.1150 from 1 Oct 2026** | None, ever |

Meta revises rates quarterly (1 Jan / 1 Apr / 1 Jul / 1 Oct). India's marketing rate rose
~10% on 1 Jan 2026.

## Decision
We charge a **flat software subscription** (₹1,999/month recommended). The customer's own
WhatsApp Business Account carries their own payment method and **Meta bills them directly**.
We never resell, mark up, front, or invoice messaging.

## Why
- Our margin becomes independent of message volume. A customer who sends 100× more messages
  costs us nothing extra.
- We are immune to quarterly Meta rate changes — including the 1 Oct 2026 change that makes
  service messages billable.
- No float, no credit risk, no bad debt, no reconciliation against Meta's invoice.
- Most WhatsApp SaaS founders fail precisely here: they blend software revenue with messaging
  cost, then discover a customer's volume has eaten their margin. This structure makes that
  arithmetically impossible.

## Alternatives considered
| Option | Rejected because |
|---|---|
| Wallet / prepaid credits at cost + 15–25% | Needs a Meta credit line or a BSP; float risk; rate-change risk on us; requires provably-correct metering first. **Revisit at Stage 3.** |
| Bundled: flat fee includes N messages, overage billed | Only safe with accurate metering, and creates an obligation that maps to a variable cost. Dangerous during validation. |
| "Unlimited messages" | Financial suicide. Never offer this. |

## Consequences
**Positive:** ~95%+ software gross margin, predictable, defensible.
**Negative:**
- Two bills for the customer (see D-02) — a genuine sales objection to handle head-on.
- The customer must successfully attach a payment method to Meta. Onboarding must verify this
  explicitly, not assume it (increment F18).
- After 1 Oct 2026, customers' Meta bills rise for reply-heavy use cases. **We must warn them
  proactively** — being the vendor who warned them earns more goodwill than any feature.

## Non-negotiable implications for the code
1. Never promise or imply unlimited messaging anywhere in product, pricing page, or contract.
2. Maintain the append-only `message_ledger` with per-category counts (F08) — it is the only
   way to answer "why is my Meta bill ₹4,000?"
3. Consolidate multi-part replies into one message (F15). Each message is billed separately.
4. Meta rates live in a **config table**, never as constants.
5. Per-contact reply rate limits, so no loop can ever run up a customer's bill.

## When we would revisit
Stage 3 (₹5,00,000+/month revenue) and only after the ledger has been correct in production
for months. A wallet model is a second revenue line, but it is also float risk plus
reconciliation plus rate-change exposure. Earn the right to it.
