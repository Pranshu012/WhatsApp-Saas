# Pricing and Monetization

## The price

**₹1,999/month per business.** One plan. No tiers.

### Why ₹1,999 specifically

Not a round-number instinct — **UPI carries 0% MDR on transactions below ₹2,000** under the NPCI
waiver (verified 18 Aug 2026). At ₹1,999 with UPI, our payment processing cost is effectively
zero. At ₹2,500 on a card it would be 2.36%.

| Price | Method | Processing cost | Net |
|---|---|---|---|
| ₹1,999 | UPI | **₹0** | ₹1,999 |
| ₹1,999 | Card | 2.36% = ₹47 | ₹1,952 |
| ₹2,500 | Card | 2.36% = ₹59 | ₹2,441 |

So: **make UPI the default, prominent payment path.** Cards as fallback. Recurring via UPI
AutoPay (~0.5% + GST) or Razorpay Subscriptions (adds ~0.99%).

### Why one plan

Tiers require deciding what to withhold, which you cannot do sensibly before you know what
customers value. One plan also removes a decision from the sales conversation. Add tiers when a
customer asks for something you'd genuinely charge more for.

**`[DECISION REQUIRED]` D-03:** trial length, annual discount, per-additional-number pricing.

## The two-bill model — say it plainly

```text
We charge:    ₹1,999/month for the software
Meta charges: per message, directly to the customer's card
```

Script for sales calls:

> "You'll pay us ₹1,999 a month for the software. Meta charges you separately for the messages —
> for your volume that's roughly ₹[X] a month, on your own card. We don't mark that up; you pay
> Meta what Meta charges. You'll see both, and your dashboard shows your message counts so
> nothing is a surprise."

Say this **in the first conversation**, not at onboarding. If it's a dealbreaker, you want to know
on call one. That's D-02.

## Why we don't resell messages

| | Reselling | Ours |
|---|---|---|
| Gross margin | Volume-dependent | **Flat ~95%+** |
| Meta quarterly rate changes | Our problem | Not our problem |
| 1 Oct 2026 billing change | Our problem | Not our problem |
| Float / credit risk | Real | None |
| Needs Meta credit line | Yes | No |
| Metering must be perfect | Yes, or you lose money | Nice to have |

See ADR-005. A wallet model is a legitimate Stage-3 second revenue line — but only after the
ledger has been provably correct in production for months.

## Estimating a customer's Meta cost

Do this on the sales call. India rates, ex-GST, verified 18 Aug 2026:

| Use case | Volume/month | Category | Meta cost |
|---|---|---|---|
| Appointment reminders | 500 | Utility ₹0.1150 | ₹57.50 + GST ≈ **₹68** |
| Order updates | 1,000 | Utility | ₹115 + GST ≈ **₹136** |
| Support replies (after Oct 2026) | 1,000 | Service ₹0.1150 | ₹115 + GST ≈ **₹136** |
| One marketing blast | 2,000 | Marketing ₹0.8631 | ₹1,726 + GST ≈ **₹2,037** |

**The marketing line is the one to flag.** A single blast costs more than our software fee.
Customers should know before they run it, not after.

For most utility-focused customers, Meta costs ₹70–200/month against our ₹1,999 — an easy
conversation.

## Unit economics

| | Per customer/month |
|---|---|
| Revenue | ₹1,999 |
| Payment processing (UPI) | ₹0 |
| Marginal infrastructure | ~₹0 |
| Marginal messaging | ₹0 (customer pays Meta) |
| **Gross margin** | **~100%** |

Fixed costs at 20 customers: ~₹2,700/month (infra ₹100, GST/CA ₹1,000–2,500, domain ₹80).
Against ₹39,980 MRR.

**Your real costs are time and acquisition.** Optimising infrastructure spend is optimising the
smallest line on the page.

## Billing implementation (F21)

- Razorpay Subscriptions, UPI AutoPay preferred
- State machine: `TRIALING → ACTIVE → (PAST_DUE) → CANCELLED/EXPIRED`
- Driven **only** by verified webhooks, never a client callback
- `PAST_DUE` → **block outbound sends, never block login, data access, or export.** Locking
  someone out of their own customer conversations over a failed card earns a chargeback and a bad
  review.
- Dunning: retry schedule, email notification, 7-day grace period
- **GST invoice fields required** — GSTIN, legal name, address. B2B customers need them for input
  credit and will ask every month otherwise.

## Never do this

| Don't | Why |
|---|---|
| Promise "unlimited messages" | Golden Rule 6. It maps to a real per-unit Meta cost. |
| Mark up Meta's rates silently | Destroys the transparency that differentiates us |
| Absorb messaging cost to close a deal | Breaks the entire margin structure |
| Discount below ₹1,999 to win an early customer | You'll anchor your whole price list to it |
| Offer a free tier | Free users generate support load and teach you nothing about willingness to pay |
| Long free pilots | A customer who won't pay ₹1,999 isn't validating anything |

## When to raise the price

Signals it's too low:
- Nobody objects to the price at all
- Customers say "that's it?"
- You're spending hours of support on a ₹1,999 account
- Churn is near zero and referrals are strong

Raise for **new** customers first; grandfather the early ones. They took the risk with you, and
the goodwill is worth more than the delta.
