# Revenue-Funded Infrastructure

The governing rule for every infrastructure spending decision.

## The rule

**Infrastructure cost must stay between 1% and 8% of MRR.**

| Stage | Customers | MRR | Infra budget | Actual target |
|---|---|---|---|---|
| 1 | 0–20 | ₹0–40,000 | ₹0–3,200 | **~₹100** (0.25%) |
| 2 | 20–100 | ₹40,000–200,000 | ₹400–16,000 | **₹1,000–5,000** (1–2.5%) |
| 3 | 100–1,000 | ₹200,000–2,000,000 | ₹2,000–160,000 | **₹15,000–40,000** (1–2%) |

You start at 0.25% and stay near 1–2%. That's a healthy SaaS gross margin and it's achievable
because the architecture was chosen for it.

## The corollary that matters more

**Never spend ahead of revenue.**

Every rupee spent on infrastructure before you have customers is a rupee of runway gone, buying
capacity for load that doesn't exist. Worse, it buys **complexity** — and complexity costs you the
scarce resource, which is your attention.

Concretely: ₹15,000/month of "proper" infrastructure at zero revenue is ₹180,000/year and a
distributed system to debug, in exchange for handling load you could serve from a free VM at 0.1%
utilisation.

## The unavoidable costs (be honest about these)

Infrastructure is nearly free. These are not:

| Cost | Monthly | Notes |
|---|---|---|
| **GST compliance (CA)** | **₹1,000–2,500** | **Larger than your entire infrastructure bill.** Non-negotiable once registered. |
| Domain | ~₹80–100 | Amortised annual |
| Razorpay fees | 0–2.36% of revenue | **UPI is 0% under ₹2,000** — the reason the price is ₹1,999 |
| Company registration (one-off) | ₹8,000–15,000 | If incorporating |
| Legal review (one-off) | ₹10,000–25,000 | Privacy policy + terms, once |

At ₹1,999 × 20 = ₹39,980 MRR: infrastructure ~₹100, CA ~₹2,000, Razorpay ~₹0 (if UPI-dominant).
**Your accountant costs 20× your servers.** Plan the budget accordingly — this surprises people.

## The Razorpay/UPI detail worth protecting

- Domestic cards/netbanking: 2% + 18% GST = **2.36% effective**
- **UPI: 0% MDR on transactions under ₹2,000** (NPCI waiver)
- Razorpay Subscriptions: +0.99%
- UPI AutoPay: ~0.5% + GST

**This is why the price is ₹1,999, not ₹2,000 or ₹2,499.** Crossing ₹2,000 costs you ~2.4% on
every payment. On ₹40,000 MRR that's ₹960/month — ten times your infrastructure bill.

Make UPI the default and most prominent payment method. If you ever raise prices past ₹2,000,
model the MDR cost explicitly rather than assuming it's noise.

## Spending priority when revenue arrives

Not "faster servers". In this order:

**1. Remove single points of failure** (Stage 2, ~₹350–800/month)
Leave the Oracle free tier. Oracle halved the ARM allocation in June 2026 with no announcement.
Building ₹200k MRR on a free tier that changes silently is a business risk, not a saving.

**2. Reduce operational burden** (Stage 2–3, ₹0–8,000/month)
Managed Postgres so you're not personally on call for database recovery at 2am.

**3. Isolate blast radius** (Stage 2, ~₹350/month)
Worker on its own box. A runaway job stops taking down webhook ingestion.

**4. Buy back your time** (any stage)
Support tooling, automated onboarding, a status page. Your time is the scarcest resource and the
one that doesn't scale.

**5. Only then: capacity.**
By the time you need it, you'll have measured it.

## What buys you the most for ₹0

Habits, not hardware:

- **Scripted provisioning.** `provision.sh` means changing vendors is an afternoon. That's what
  makes free tiers safe to use — you're never trapped.
- **Postgres for everything.** Queue, sessions, full-text search, rate limits. Each avoided
  dependency is saved money *and* saved attention.
- **Deterministic idempotency keys.** They prevent duplicate sends, which spend your customer's
  money. Free to implement, expensive to omit.
- **A tested restore.** Free. The difference between an incident and an extinction event.
- **Saying no.** The biggest cost saving in this whole document is not building Redis, Kafka,
  Kubernetes, and microservices you don't need.

## Decision framework

Before any infrastructure spend, answer all five:

1. **What problem am I solving?** Must be observed in production, with data. Not projected.
2. **What's the cheapest fix?** Usually an index, a config change, or one more worker.
3. **What does this cost as a % of MRR?** Above 8% → not yet.
4. **What new failure mode does it add?** Every component can fail. Name how.
5. **Can I revert it in an afternoon?** If not, wait longer.

If you can't answer 1 with a metric, you're not solving a problem — you're shopping.

## Anti-patterns

| Anti-pattern | Why it hurts |
|---|---|
| Pre-scaling "before launch" | Buys capacity for load that doesn't exist |
| Adding Redis "because SaaS needs Redis" | New failure mode, no problem solved |
| Kubernetes for one service | Weeks of setup, permanent operational cost |
| Managed everything at zero revenue | ₹15,000/month before your first customer |
| Multi-region before product-market fit | Solving a problem you don't have |
| Ignoring the CA cost | The largest real fixed cost; surprises people at filing time |
| Pricing above ₹2,000 without modelling MDR | Silently gives away 2.36% of revenue |

## The through-line

The architecture in this workspace was chosen so that **infrastructure cost scales with revenue
rather than ahead of it**. That's why you start at ₹100/month and stay at 1–2% of MRR through
1,000 customers.

Protect that. Every dependency you add before it's needed erodes it — not mainly in rupees, but
in the attention you need for the thing that actually determines whether this works: getting and
keeping customers.
