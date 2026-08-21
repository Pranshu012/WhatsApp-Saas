# Open Decisions Log

Anything not settled by `SOURCE-architecture-and-cost-strategy.md` lives here.
**Do not let Claude Code guess these.** Answer them yourself, write the answer here with a
date, then continue.

Status: `OPEN` · `CLOSED` · `DEFERRED`

---

## D-01 — Core automation feature set  🔴 BLOCKS PHASE C (F12–F16)

**Status:** OPEN
**Why it matters:** The source document defines the business model and infrastructure but
never says what the product actually *does*. Building the wrong feature is the most
expensive mistake available to you — far worse than any infrastructure choice.

**The question:** For your first 10 customers, which single job does this product do?

Candidate answers (pick ONE as primary):

| Option | Product is… | Best for | Message categories |
|---|---|---|---|
| A | Auto-reply + FAQ bot | Any SMB with repetitive enquiries | Service, Utility |
| B | Appointment reminders | Clinics, salons, tuition centres | Utility |
| C | Order/delivery updates | D2C, kirana, local retail | Utility |
| D | Lead capture + qualification | Real estate, education, services | Service, Marketing |
| E | Payment/invoice reminders | B2B services, rentals | Utility |

**Recommendation:** Do not answer this from your desk. Talk to 10 SMBs first
(`01-BUSINESS/CUSTOMER-VALIDATION.md`). Pick the option that 7 of 10 say they'd pay for.

**Note on economics:** Options B, C and E are Utility-heavy (₹0.115/msg) — cheap for your
customer, easy to justify. Option D drifts into Marketing (₹0.8631/msg, 7.5× more, no volume
discount). Option A becomes billable from 1 Oct 2026. This changes your pitch, not just your
code.

**Answer:** _______________  **Date:** ________

---

## D-02 — Will customers accept two separate bills?

**Status:** OPEN
**Why it matters:** The entire Tech Provider model (ADR-003, ADR-005) depends on the customer
attaching their own card to Meta. If Indian SMBs refuse, you need a wallet model — which
needs a Meta credit line and is a Stage-3 scope.

**How to test:** In every validation call, say plainly: *"You'll pay us ₹1,999/month for the
software, and Meta will charge your card separately for messages — roughly ₹X for your
volume."* Record the reaction verbatim.

**Answer:** _______________  **Date:** ________

---

## D-03 — Pricing: is ₹1,999/month right?

**Status:** OPEN
**Source position:** ₹1,999 recommended, chosen partly because UPI is 0% MDR under ₹2,000.
**Open:** trial length? annual discount? per-number pricing if a customer has two numbers?

**Answer:** _______________  **Date:** ________

---

## D-04 — Company name, domain, brand

**Status:** OPEN
**Blocks:** Phase 0 entirely — Meta Business Verification needs the legal entity name.
**Answer:** _______________  **Date:** ________

---

## D-05 — Business entity type

**Status:** OPEN
Sole proprietorship vs LLP vs Pvt Ltd. Affects Meta verification documents, Razorpay
onboarding, and GST. Talk to a CA. Cheapest workable path for validation is usually sole
proprietorship + GST registration.

**Answer:** _______________  **Date:** ________

---

## D-06 — UI language

**Status:** DEFERRED (revisit after 20 customers)
English only for MVP. Hindi/regional adds translation and testing surface with no validation
behind it yet.

---

## D-07 — Do we support customers already on the WhatsApp Business app?

**Status:** OPEN
Meta supports onboarding a number already in use with the WhatsApp Business app, but the
Embedded Signup flow must be customised for it, and Coexistence has its own behaviour. Many
Indian SMBs are already on the Business app, so this may be unavoidable.

**Recommendation:** Ask in validation calls. If >half are already on the Business app, this
becomes a Phase B requirement, not a nice-to-have.

**Answer:** _______________  **Date:** ________

---

## D-08 — Multi-user access per tenant

**Status:** DEFERRED
OWNER + MEMBER roles exist in the schema from F01. No invitation flow until a customer asks.

---

## D-09 — Data retention for end-customer conversations

**Status:** OPEN
**Why it matters:** DPDP Act. You are storing conversations between your customer and *their*
customers. You need a stated retention period and a deletion path.
**Recommendation:** 12 months rolling, documented in your terms, deletion job in Phase E.

**Answer:** _______________  **Date:** ________

---

## Closed decisions

Move items here with the date and reasoning. Never delete them — the reasoning is the value.

| ID | Decision | Answer | Date |
|---|---|---|---|
| — | — | — | — |

---

## D-10 — Pilot pricing: how much, and how collected

**Status:** Decided — 19 August 2026
**Decision:** ₹500/month, collected manually by UPI to a personal/proprietor account.
No Razorpay, no KYC, no F21.

**Reasoning:** A free pilot produces polite feedback; paying users produce honest feedback.
Someone who pays ₹500 will consider ₹1,999; someone who won't pay ₹500 definitely won't. The
revenue is irrelevant — the signal is the point. Customers are told upfront that the price moves
to ₹1,999 with a month's notice, and that Meta bills them separately for messages.

See [ADR-008](ADR-008-PILOT-BEFORE-BUSINESS-REGISTRATION.md).

---

## D-11 — When to register the business

**Status:** Decided — 19 August 2026
**Decision:** After the pilot exit criteria are met, not before. Structure to be confirmed with
a CA; sole proprietorship with Udyam registration is the expected lightest path.

**Trigger:** 5–10 businesses using it 4+ continuous weeks · 5+ have paid twice · 3+ say
unprompted they'd pay ₹1,999 · churn reasons known · activation friction understood · support
under 5 hrs/week per 10 customers · zero cross-tenant, data-loss, or duplicate-send incidents.

**If not met after 8 weeks of pilot:** the product needs to change, not the company structure.

See [PILOT-FIRST-PLAN.md](../00-START-HERE/PILOT-FIRST-PLAN.md).
