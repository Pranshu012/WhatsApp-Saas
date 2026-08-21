# ADR-008 — Pilot Before Business Registration

**Status:** Accepted
**Date:** 19 August 2026
**Supersedes:** the sequencing (not the content) of Phase 0 in `../02-ACTIONS/MASTER-ACTION-PLAN.md`

## Context

The original plan sequenced: register a business → Meta Business Verification → Meta App Review →
build → sell. That front-loads roughly ₹10,000–25,000 of one-off cost, ₹1,000–2,500/month of
recurring CA cost, and **4–5 weeks of approval waiting** — all before a single customer has
confirmed the product is worth having.

For a solo bootstrapped founder, that ordering optimises for looking legitimate rather than for
learning whether the product works.

## Decision

**Validate with 5–10 pilot customers before registering a business or seeking Meta verification.**

The pilot runs on a Meta app in **development mode**, with each pilot customer added in a
**Tester** role.

## What makes it feasible

Meta's own documentation:

1. **Embedded Signup overview** — while an app is in development mode, the WhatsApp permissions
   appear in the authorisation screen for anyone with an admin, developer, or tester role on the
   app. The Advanced Access restriction applies only in live mode.
2. **App Review** — Business-type apps are automatically granted Standard Access to all
   permissions available to that app type, explicitly so they can be tested at that level.

Therefore a real Tech Provider flow — customer connects their own WABA, we hold a scoped token,
messages flow both ways — works for a small known group without Business Verification or App
Review.

## Consequences

**Positive**

- 4–5 weeks removed from the critical path
- ~₹2,000/month and ~₹15,000 of one-off cost deferred until validated
- F21 (Razorpay billing) deferred entirely — 1–2 days of build, plus KYC waiting
- Pilot scope is ~17 increments (~6–7 weeks) rather than 23 (~10–11 weeks)
- App Review becomes *easier* later: it requires a screen recording of the app using each
  permission, and after a pilot you have a real product to record instead of a mock-up
- Failing costs weeks instead of months and rupees instead of tens of thousands

**Negative**

- Each pilot customer must accept a Facebook Tester invitation — an extra onboarding step, and a
  trust conversation you must handle well
- ~250 conversations per 24 hours across the app without Business Verification
- Cannot self-serve onboard strangers; every customer is a manual conversation
- Pilot customers are not on a real subscription, so churn signal is softer than with full billing
- If Meta changes its development-mode access rules, the pilot path closes. **This is the
  single-point assumption of the plan.**

**Neutral**

- No architectural change. The pilot is built on the real architecture with real multi-tenancy,
  a real ledger, and real backups. It is not a throwaway prototype.

## Explicitly not deferred

Deferring correctness would be a false economy — each of these is far more expensive to retrofit
than to build:

- **F02 multi-tenancy and Row-Level Security.** Adding `tenant_id` and RLS to a live schema is
  painful and risky. Even 5 customers must not see each other's data.
- **F07 job queue.** A crash that loses a pilot customer's messages loses the customer and the
  reference.
- **F08 message ledger.** Customers pay Meta directly; without the ledger you cannot explain
  their bill or prove you didn't double-send.
- **F10 webhook signature verification.** Security is not a scale feature.
- **F23 backups, with a tested restore.**

The rule adopted: **defer commercial and cosmetic work; never defer correctness work.**

## Pricing during the pilot

**₹500/month, collected manually by UPI.** No Razorpay, no KYC, no code.

A free pilot produces polite feedback. Someone paying ₹500 gives honest feedback and has
demonstrated they'll consider ₹1,999. The revenue is irrelevant; the signal is not.

Customers are told upfront that the price moves to ₹1,999 with a month's notice, and that Meta
bills them separately for messages.

## Exit criteria

Register the business when: 5–10 businesses have used it 4+ continuous weeks; at least 5 have
paid twice; at least 3 say unprompted they'd pay ₹1,999; churn reasons are known; activation
friction is understood; support is under 5 hours/week per 10 customers; and there has been no
cross-tenant, data-loss, or duplicate-send incident.

If these aren't met after 8 weeks, the product needs to change — not the company structure.

## Alternatives considered

**Register first, as originally planned.** Rejected: spends money and 4–5 weeks of waiting before
any validation.

**Pilot on a single shared WABA (yours), simulating multi-tenancy.** Rejected: doesn't validate
onboarding — which is the riskiest, highest-churn part of the product — and doesn't test the real
token and webhook paths.

**Skip the pilot; launch to strangers immediately.** Rejected: requires the full verification
path anyway, and gives worse feedback because you can't sit on a call with each user.

**Free pilot.** Rejected: free users are polite. See pricing above.

## Review

Revisit when the exit criteria are met, or after 8 weeks of pilot, whichever is first.

Re-verify the Meta development-mode assumption quarterly — it underpins everything here.
