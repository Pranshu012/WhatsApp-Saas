# Business Plan

Written so you could explain this to another person in ten minutes.

---

## The problem

Indian SMBs — clinics, salons, coaching centres, boutiques, local retailers — run their customer
communication on WhatsApp because that's where their customers already are. But they run it
manually, on a phone, one thread at a time.

Consequences:
- The owner answers the same questions dozens of times a day
- Enquiries arriving after hours go cold
- Appointment reminders and order updates get forgotten
- There is no record beyond one person's phone
- Growth is capped by how fast one person can type

The official WhatsApp Business API solves this, but it is developer infrastructure, not a
product. An SMB owner cannot use it directly.

## What we sell

A WhatsApp automation SaaS. The customer connects their **own** WhatsApp Business number through
our app, configures automatic replies, an FAQ, and scheduled reminders, and sees every
conversation in one shared inbox.

**⚠️ The precise feature set is `[DECISION REQUIRED]` D-01.** The five candidate positionings are
in `13-DECISIONS/DECISIONS.md`. Do not decide this at your desk — decide it after ten customer
conversations. See `CUSTOMER-VALIDATION.md`.

## Who we sell to

Indian SMBs with:
- 20+ customer WhatsApp conversations per day
- Repetitive, answerable enquiries
- One or two people handling messages
- ₹2,000/month of discretionary software budget
- Already on WhatsApp Business (many are — see D-07)

See `TARGET-CUSTOMER.md`.

## The business model — this is the important part

We are a Meta **Tech Provider**, not a Solution Partner (ADR-003).

```text
Customer pays US:    ₹1,999/month flat software subscription
Customer pays META:  per-message charges, DIRECTLY, on their own card
```

The customer owns their WhatsApp Business Account. Meta bills them. **We never touch messaging
money.**

Why this matters more than any technical decision in the project:

| | Reselling messages | Our model |
|---|---|---|
| Gross margin | Squeezed by volume | **~95%+, flat** |
| Meta raises rates (quarterly) | Our problem | Not our problem |
| 1 Oct 2026 billing change | Our problem | Not our problem |
| Customer sends 100× more | Costs us more | Costs us nothing |
| Float / credit risk | Real | None |
| Bad debt if they vanish | Real | None |

Most WhatsApp SaaS founders fail exactly here: they blend software revenue with messaging cost,
then discover a heavy customer has eaten their margin. Our structure makes that arithmetically
impossible.

**The trade-off:** the customer sees two bills, and must attach a card to Meta themselves. That's
a real sales objection (D-02) and the most common onboarding failure. We handle it head-on rather
than pretending otherwise.

## Pricing

₹1,999/month. Deliberately under ₹2,000 because **UPI carries 0% MDR below ₹2,000** under the
NPCI waiver — so our payment processing cost is effectively zero. Cards would cost 2.36%.

See `PRICING-AND-MONETIZATION.md`.

## Unit economics

| Line | Amount |
|---|---|
| Revenue per customer | ₹1,999/month |
| Payment processing (UPI) | ~₹0 |
| Marginal infrastructure per customer | ~₹0 |
| Marginal messaging cost | **₹0 — customer pays Meta** |
| **Gross margin** | **~100%** |

Fixed costs at 20 customers: infrastructure ~₹100/month, GST/CA ₹1,000–2,500/month, domain ~₹80.
So roughly **₹2,700/month of fixed cost against ₹39,980 of revenue.**

The real costs are your time and customer acquisition — not servers.

## First 10 customers

Sell before you finish building. Target: paying customers, not sign-ups.

1. Talk to 10 SMBs about their WhatsApp workflow. Don't pitch. Close D-01 with what you learn.
2. Pick one vertical. One. A dental clinic and a boutique need different things, and you cannot
   serve both well while learning.
3. Onboard each customer **manually, on a video call.** This is your highest-value learning
   channel, and it catches the payment-method failure before it becomes a support ticket.
4. Get money from all 10. A free pilot teaches you nothing about willingness to pay.

Detail: `02-ACTIONS/FIRST-10-CUSTOMERS.md`.

## First 20 customers

- Referrals from the first 10, within the same vertical
- Fix what the first 10 complained about — don't add what nobody asked for
- Only now consider self-serve onboarding
- ₹39,980 MRR at 20 customers

Detail: `02-ACTIONS/FIRST-20-CUSTOMERS.md`.

## Revenue milestones

| MRR | Customers | What it funds | Infra budget |
|---|---|---|---|
| ₹10,000 | ~5 | Nothing. Verify your restore works. Bank it. | ₹500 |
| ₹25,000 | ~13 | **Buy back your time** — support help beats any infra upgrade | ₹1,500 |
| ₹50,000 | ~25 | Managed Postgres with PITR — the first upgrade genuinely worth paying for | ₹3,000–5,000 |
| ₹1,00,000 | ~50 | Second instance, load balancer, Redis for cross-instance limits | ₹8,000–15,000 |
| ₹5,00,000 | ~250 | Multi-AZ DB, read replica, on-call, a second engineer | ₹25,000–50,000 |

**Rule: infrastructure stays at 3–8% of MRR.** Full plan in
`12-SCALING/REVENUE-FUNDED-INFRASTRUCTURE.md`.

## Risks, honestly

| Risk | Severity | Mitigation |
|---|---|---|
| **Meta bans or restricts our app** | Existential | Strict policy compliance; never help customers spam; monitor quality ratings |
| **SMBs won't accept two bills** (D-02) | High | Test in validation calls before building. If it fails, the model changes. |
| **1 Oct 2026 billing change causes churn** | Medium-high | Proactive communication + visible metering. See `08-META-WHATSAPP/OCTOBER-2026-BILLING-CHANGE.md`. |
| **Wrong feature set** (D-01) | High | Ten conversations before Phase C |
| **Customers can't attach a payment method** | Medium-high | Manual onboarding calls; prominent warning; watch customer #1 do it |
| **Cross-tenant data leak** | Existential | Two-layer isolation (ADR-004) + four mandatory tests |
| **Oracle free tier changes again** | Medium | Already happened June 2026. Tested restore + portable provisioning + ₹380 fallback. |
| **Established competitors** (Wati, AiSensy, Interakt) | Medium | They resell messages at a markup and serve everyone. We're cheaper, transparent, and vertical-focused. |
| **Support load exceeds one person** | Medium | This is the real scaling constraint, not CPU. Budget for help at ₹25,000 MRR. |
| **Solo founder burnout** | High | Ship less. The Golden Rules exist for this. |

## Competitive position

Wati, AiSensy, Interakt, Gupshup, DoubleTick and others already serve this market. Most are BSPs
or resell through one, which means a 10–30% per-message markup plus a platform fee.

Our differentiation:
1. **Transparent pricing** — flat software fee, Meta at cost, no markup
2. **Customer owns their WABA** — no lock-in, which is a genuine trust argument
3. **One vertical, done properly** — not a generic inbox for everyone
4. **Founder-level support** during validation

What we do **not** claim: better features, more integrations, or a nicer UI. We won't win on those
against a funded team.

## Assumptions that could be wrong

Listed in `00-START-HERE/ASSUMPTIONS-AND-EXPIRY-DATES.md`. The five that matter most:

1. SMBs will pay ₹1,999/month for this
2. They'll accept two separate bills
3. They can attach a payment method to Meta unaided
4. Deterministic rules cover enough cases without AI (ADR-007)
5. The October change won't cause churn

Every one is testable with conversations, not code.

## What we deliberately do NOT build initially

Bulk marketing campaigns · a visual flow builder · AI chatbot · multi-channel (Instagram, SMS) ·
CRM features · team/agent assignment · a mobile app · multi-language UI · analytics beyond
message counts · self-serve onboarding · wallet-based message billing.

Full list with revisit triggers: `14-CLAUDE-CODE/FEATURE-BREAKDOWN.md`.

## Long-term vision

If validation works, the path is: **own one vertical completely.** Become the default WhatsApp
layer for, say, Indian dental clinics — with the templates, workflows, and integrations that
vertical specifically needs. Vertical depth is defensible against horizontal competitors; a
generic inbox is not.

Only after that: a second vertical, or a wallet model as a second revenue line (ADR-005, Stage 3).

**But none of that matters until 20 businesses are paying and still using it after three months.**
