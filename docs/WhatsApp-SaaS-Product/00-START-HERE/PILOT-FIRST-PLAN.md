# Pilot-First Plan

**Read this before anything else if you have not yet registered a business.**

This document overrides parts of the rest of the workspace. Where it conflicts with another
document, this one wins **until the pilot is over**.

---

## The change

The original plan assumed: register a business → get verified by Meta → build → sell.

The revised plan is: **build → validate with 5–10 real businesses → then register and scale.**

That is the right order. You should not spend ₹1,000–2,500/month on a CA and weeks on Meta
Business Verification to find out that shop owners don't actually want this.

## The finding that makes this possible

> **Meta docs, Embedded Signup overview:** while your app is in **development mode**, the
> WhatsApp permissions appear in the Embedded Signup authorisation screen to anyone holding an
> **admin, developer, or tester** role on your app. Only when you switch to **live mode** do you
> need App Review–approved Advanced Access.
>
> **Meta docs, App Review:** Business-type apps are **automatically granted Standard Access** to
> all permissions available to that app type, so you can test at that access level.

So for a pilot:

- Keep your Meta app in **Development mode**
- Add each pilot customer's Facebook account as a **Tester** on your app
- They connect their own real WhatsApp Business Account through Embedded Signup, normally
- Standard Access is enough — **no Business Verification, no App Review, no Advanced Access**

The weeks-long approval wait leaves the critical path entirely. You can be live with pilot
customers roughly **4–5 weeks sooner**.

> ⚠️ Verify this yourself before relying on it. Meta changes access rules without much notice,
> and this is the load-bearing assumption of the whole pilot plan. Confirm on
> `developers.facebook.com` and record the date you checked in
> [ASSUMPTIONS-AND-EXPIRY-DATES.md](ASSUMPTIONS-AND-EXPIRY-DATES.md).

## Pilot constraints (real, and fine)

| Constraint | Effect | Is this a problem? |
|---|---|---|
| Each pilot customer must accept a Tester invite on Facebook | One extra step in onboarding, done on your call | No — you're onboarding manually anyway |
| Without Business Verification: ~250 conversations per 24 hours | Caps volume across the app | No — 10 SMBs won't approach this |
| Dev mode cannot scale | You cannot self-serve onboard strangers | **That's the point.** A pilot is 5–10 people you talk to. |
| Pilot customers still pay Meta for their own messages | Their bill: maybe ₹100–500/month | Actually good — skin in the game makes their feedback honest |

**Note:** Meta's docs also warn that WABAs originally created through a developer app can't
always be onboarded through Embedded Signup afterwards. Have pilot customers use a **fresh**
number and a fresh WABA created through your Embedded Signup flow.

## What you defer (and what it saves)

| Deferred | Saves | Do it when |
|---|---|---|
| Company incorporation | ₹8,000–15,000 one-off | Pilot succeeds |
| GST registration + monthly CA | **₹1,000–2,500/month** | Revenue approaches the registration threshold — **ask your CA for the current figure and your specific position** |
| Razorpay account + KYC | Days of waiting | You start charging properly |
| **F21 — Razorpay billing increment** | **1–2 days of build** | Post-pilot |
| Meta Business Verification | **1–3 weeks of waiting** | Before going live/scaling |
| Meta App Review / Advanced Access | **1–4 weeks of waiting** | Before going live/scaling |
| Lawyer review of ToS + privacy | ₹10,000–25,000 | Before charging money at scale |
| F15 interactive messages, F16 scheduling | ~2 days | Post-pilot, if customers ask |
| F19 full config screens | ~3 days | Post-pilot (you configure for them during pilot) |

**Total deferred: roughly 4–5 weeks of waiting and about a week of build.**

## What you must NOT defer

These look deferrable and are not. Each is far more expensive to retrofit than to build.

| Keep | Why |
|---|---|
| **F02 — multi-tenancy + RLS** | Adding `tenant_id` and row-level security to an existing schema with live data is genuinely painful. Build it once, correctly, at the start. Even 5 customers must not see each other's conversations. |
| **F07 — job queue** | Without it, a crash loses messages. Losing a pilot customer's messages loses the customer *and* the reference. |
| **F08 — message ledger** | Your customer pays Meta directly. Without the ledger you cannot explain their bill or prove you didn't double-send. |
| **F10 — webhook signature verification** | Security is not a scale feature. |
| **F23 — backups, tested** | A pilot customer's lost data is a lost reference customer. |
| Token encryption (F05) | You are holding credentials that can send messages as someone else's business. |

The rule: **defer commercial and cosmetic work; never defer correctness work.**

## Pilot scope — what you actually build

| Phase | Increments | Pilot status |
|---|---|---|
| A — Foundation | F00–F04 | **All required** |
| B — WhatsApp core | F05–F11 | **All required** — this is the product |
| C — Automation | F12–F14 | Required (F12 simplified) |
| | F15, F16 | **Deferred** |
| D — Frontend | F17, F18 | Required |
| | F19 | **Simplified** — you configure rules for them |
| | F20 | Required (inbox + counts) |
| E — Production | F21 billing | **Deferred entirely** |
| | F22, F23 | **Required** |

**Pilot build: ~17 increments, roughly 6–7 weeks** instead of 23 increments and 10–11 weeks.

Detail per increment in [../14-CLAUDE-CODE/FEATURE-BREAKDOWN.md](../14-CLAUDE-CODE/FEATURE-BREAKDOWN.md).

## Charging during the pilot

You have three options. They are not equally good.

| Option | Signal you get | Verdict |
|---|---|---|
| **Completely free** | Weak. People accept free things they don't value. | Avoid |
| **Free now, ₹1,999 later, stated upfront** | Moderate — you learn who flinches | Acceptable |
| **₹500/month, collected manually by UPI** | **Strong. Someone who pays ₹500 will consider ₹1,999.** | **Best** |

₹500 by UPI to your own account needs no Razorpay, no KYC, no code. Send a WhatsApp message
with a UPI ID on the 1st. That's the whole billing system for the pilot.

What matters is not the ₹5,000/month. It's that **paying customers give honest feedback and free
users are polite**. A free pilot user who stops using it will still tell you it's great.

Say this plainly at signup:

> This is an early pilot. ₹500/month for now, and I'll tell you a month before it goes to
> ₹1,999. WhatsApp charges you separately for messages — probably ₹100–500/month at your volume.
> If it isn't useful, tell me and stop; no notice needed.

## Exit criteria — when to register the business

Do not incorporate on optimism. Register when **all** of these are true:

- [ ] 5–10 businesses have used it for **at least 4 continuous weeks**
- [ ] At least **5** have actually paid you something, twice
- [ ] At least **3** say unprompted that they'd pay ₹1,999
- [ ] You know why the ones who churned churned
- [ ] Activation friction is understood — you can name the exact step people get stuck on
- [ ] You know the top 3 feature gaps, from their words not your guesses
- [ ] Fewer than 5 support hours/week per 10 customers
- [ ] No cross-tenant incident, no data loss, no duplicate-send incident

If you cannot tick these after 8 weeks of pilot, the honest read is that this product needs to
change — not that it needs a company registration.

## The transition, once criteria are met

In this order, because each depends on the last:

1. **Register the business** — sole proprietorship is the lightest path in India. Udyam (MSME)
   registration is free and online. Ask your CA what suits your situation.
2. **Engage the CA properly** — GST registration if applicable, monthly filing
3. **Meta Business Verification** — needs your registration documents (1–3 weeks)
4. **Meta App Review** for Advanced Access on `whatsapp_business_management` and
   `whatsapp_business_messaging` (1–4 weeks). You now have a working product to record the demo
   video with — which is exactly what App Review asks for, and much easier than mocking it up.
5. **Razorpay** — account, KYC, then build **F21**
6. **Switch the Meta app to Live mode**
7. **Migrate pilot customers to ₹1,999** — with a month's notice, as promised
8. **Then build** F15, F16, full F19, and whatever the pilot told you to build
9. Follow [../02-ACTIONS/FIRST-10-CUSTOMERS.md](../02-ACTIONS/FIRST-10-CUSTOMERS.md) for scaling

Note step 4 gets easier because of the pilot: App Review requires a screen recording of your app
actually using each permission. You'll have a real product and real usage to record.

## What does not change

- The architecture. All of it. You are not building a throwaway prototype — you're building the
  real thing and validating it with fewer users.
- Multi-tenancy, the ledger, the job queue, webhook security, backups.
- The Tech Provider model — customers own their WABA and pay Meta directly.
- ₹1,999 as the eventual price, and the reason for it (0% UPI MDR under ₹2,000).

## Read next

- [../02-ACTIONS/PILOT-PLAYBOOK.md](../02-ACTIONS/PILOT-PLAYBOOK.md) — how to find and run the pilot
- [../08-META-WHATSAPP/PILOT-MODE-SETUP.md](../08-META-WHATSAPP/PILOT-MODE-SETUP.md) — the dev-mode Meta setup
- [../13-DECISIONS/ADR-008-PILOT-BEFORE-BUSINESS-REGISTRATION.md](../13-DECISIONS/ADR-008-PILOT-BEFORE-BUSINESS-REGISTRATION.md)
- [../START-HERE-MANUAL.txt](../START-HERE-MANUAL.txt) — the step-by-step manual, now pilot-first
