# ADR-003 — Meta Tech Provider (not Solution Partner)

**Status:** Accepted · 18 August 2026 · **Business-critical**

## Context
Meta offers two partner tiers for businesses that onboard *other* businesses onto the
WhatsApp Business Platform. Verified 18 August 2026:

| | Solution Partner | Tech Provider |
|---|---|---|
| Credit line from Meta | Yes | No |
| Who pays Meta for messages | The partner, who then invoices the client | **The client, directly** |
| What the partner bills for | Software + messages | **Software only** |
| Barrier to entry | Meta Business Partner application, lengthy | Business Verification + App Review |
| Capability difference | **None** | **None** |

Onboarding limits: 10 new business customers per rolling 7-day window by default; 200/week
after completing Business Verification, App Review, and Access Verification.

## Decision
Register as a **Tech Provider**. Onboard customers via **Embedded Signup**. Each customer
owns their own WABA and attaches their own payment method to Meta.

## Why
- Message cost never enters our P&L. Our gross margin is structurally immune to message
  volume and to Meta's quarterly rate changes.
- No float risk, no credit exposure, no reconciliation burden, no bad debt if a customer
  disappears mid-month.
- No Meta credit line required — realistic for a bootstrapped solo founder.
- 10 customers/week default limit exactly fits the 10–20 customer validation target.
- The customer owns their WhatsApp assets, which is also a genuine trust argument in sales:
  they are not locked to us.

## Alternatives considered
| Option | Rejected because |
|---|---|
| Solution Partner | Requires a Meta credit line and Business Partner status; puts message cost, float risk, and rate-change risk on us during validation |
| Reselling through a BSP (Twilio, 360dialog, Gupshup, Wati) | 10–30% per-message markup *plus* a monthly platform fee, for infrastructure we are building ourselves |
| Customer brings their own BSP | Fragmented integrations, unpredictable webhook shapes, support nightmare |

## Consequences
**Positive:** ~95%+ software gross margin; immune to Meta rate changes; no credit risk.
**Negative:**
- The customer sees **two bills** (ours + Meta's). This is a real sales objection — see D-02.
- The customer must attach a payment method on Meta. If they don't, messages fail. **This is
  the single most common onboarding failure in this model** — hence the prominent warning in
  increment F18.
- We must complete App Review for `whatsapp_business_management` and
  `whatsapp_business_messaging` with **Advanced Access**. Without it, API calls against
  customer-owned WABAs return error code 200 and the whole model fails.

## When we would revisit
Consider a wallet/prepaid model (Solution Partner or BSP-backed) at Stage 3+, once the
message ledger has been provably correct in production for several months. Never during
validation. See ADR-005.
