# First 20 Customers

**Objective: 20 paying, retained customers — and a decision about what comes next.**

## What changes from the first 10

| | First 10 | Next 10 |
|---|---|---|
| Acquisition | Cold outreach, walk-ins | **Referrals from the first 10** |
| Onboarding | Fully manual, 90 min | Manual, but 45 min with a better script |
| Features | Build nothing new | Fix the top 3 complaints |
| Infra | ₹500/month | Still under ₹2,000 |
| Your time | Mostly onboarding | Split between selling and fixing |

## Referrals are the engine now

Your first 10 are in one vertical. Clinic owners know clinic owners. Ask directly:

> "This is working well for you. Do you know two other [clinic owners] who deal with the same
> WhatsApp mess? I'd rather grow by referral than cold-calling."

Ask **after** they've seen value — around day 30, not day 2. A referral from a genuinely happy
customer converts at a rate no cold outreach matches.

## What to build now — and only now

Look at the list you wrote during the first 10 onboarding calls. Build only what meets the bar:

**The bar: three or more unprompted requests from paying customers.**

Likely candidates given the shape of this product:
- A starter library of pre-written templates for your vertical (fastest path to first send)
- Better FAQ authoring, driven by the `unmatched_messages` data
- Whatever booking or billing system they all mention
- Multi-user access, if tenants now have real teams (D-08)

**Still not:** bulk campaigns · AI · a flow builder · multi-channel · a mobile app.

## Now consider self-serve onboarding

You've done it manually 20 times. You know exactly where people get stuck. **Now** automating it
is informed rather than speculative.

What to automate first, in order:
1. The Meta payment-method warning and check — the most common failure
2. Guided template creation with vertical-specific starting points
3. Suggested automation rules based on their vertical
4. A first-message test built into onboarding

Keep the human call as an **option**, not a removal. Some customers will always want it, and for
₹1,999/month it's affordable.

## Metrics that matter now

| Metric | Target | Meaning |
|---|---|---|
| **Day-90 retention** | >80% | The real validation. Month one is novelty. |
| Referral rate | >30% of new customers | Product-market fit signal |
| Support messages per customer per week | <2 | Should fall as you fix things |
| Onboarding time | <45 min | Should fall as the script improves |
| Churn reason | Written down every time | The most valuable data you have |

**Write down why every churned customer left, in their words.** Three customers leaving for the
same reason is the clearest instruction you will ever get.

## Revenue at 20 customers

```text
20 × ₹1,999            = ₹39,980 MRR
Infrastructure         ≈ ₹100
GST / CA               ≈ ₹2,000
Domain                 ≈ ₹80
                       ─────────
Net                    ≈ ₹37,800/month
```

**Reinvestment at this level:** the highest-return spend is **buying back your own time** — a few
hours a week of support help beats any infrastructure upgrade. See
`12-SCALING/REVENUE-FUNDED-INFRASTRUCTURE.md`.

Infrastructure budget: still under ₹2,000/month. You do not need managed Postgres yet. The
trigger for that is "I cannot afford to lose 24 hours of data," roughly ₹50,000 MRR.

## The decision at 20

You now have real data. Choose deliberately:

**Option A — Go deeper in this vertical.** Become the default WhatsApp layer for this trade. Build
the integrations and workflows they specifically need. This is the strategy in `PRODUCT-VISION.md`,
and it's the defensible one.

**Option B — Add a second vertical.** Only if the first is genuinely saturated locally, or the
product needs almost no change to serve the second.

**Option C — Stop and reassess.** If retention is below 60% at day 90, more customers won't fix
it. Something about the product or the market is wrong. Better to face that at 20 customers than
at 200.

## What to be honest with yourself about

- Is anyone **actually** using it, or just paying and forgetting? Check the message counts.
- Would they be upset if it disappeared tomorrow?
- Are you selling, or hiding in the codebase?
- Is the two-bill model costing you deals? (revisit D-02 with real data)
- Did the October 2026 change cause churn? What did you learn?

## Definition of Done

- [ ] 20 businesses paying
- [ ] Day-90 retention above 80%
- [ ] 30%+ of new customers came from referrals
- [ ] Top 3 complaints from the first 10 are fixed
- [ ] Support load under 2 messages per customer per week
- [ ] Every churn reason written down
- [ ] A deliberate, written decision on A, B, or C
- [ ] Infrastructure still under ₹2,000/month

Then re-read `12-SCALING/SCALING-20-100-CUSTOMERS.md` — **before** spending anything.
