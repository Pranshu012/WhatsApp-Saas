# Phase D Prompts — Frontend (F17–F20)

Read `docs/WhatsApp-SaaS-Product/06-FRONTEND/APPLICATION-SCREENS.md` and `USER-FLOWS.md`
before F17.

Keep it plain. Your customers are Indian SMB owners on mid-range Android phones, often on
patchy connections. Fast and clear beats polished.

---

## F17 — React setup, auth, app shell

```text
Increment F17. Read docs/WhatsApp-SaaS-Product/06-FRONTEND/FRONTEND-SETUP.md.

Goal: a React + Vite SPA with working auth and an app shell. No features yet.

Requirements:
- React 18 + Vite + TypeScript. Router. A small data-fetching layer (TanStack Query is fine).
  Tailwind for styling. Nothing else without asking me.
- API client with: base URL from env, credentials: 'include' for the session cookie,
  CSRF token handling matching F03, and centralised error handling that maps our ApiError
  shape to user-facing messages.
- Screens: Login, Register, Forgot Password, Reset Password
- App shell: sidebar nav, tenant name in the header, logout
- Protected route wrapper redirecting unauthenticated users to login
- A session bootstrap call to /api/auth/me on load, with a proper loading state (not a flash
  of the login screen)
- Mobile-first responsive. Test at 360px width.
- No secrets in the bundle. Config via VITE_ env vars, documented in .env.example.

Do NOT build: dashboard content, WhatsApp screens, any feature screen. Placeholders only.

Plan first, including the folder structure.

Finally: write docs/IMPLEMENTATION/F17-<slug>.md following
docs/WhatsApp-SaaS-Product/14-CLAUDE-CODE/IMPLEMENTATION-DOC-TEMPLATE.md, and add a row to
docs/IMPLEMENTATION/INDEX.md. Document the key decisions, any divergence from the architecture
docs, and the gotchas someone would trip over. This is part of the increment, not optional.
```

**DoD:** login → shell · refresh keeps you logged in · works at 360px · no secrets in bundle

---

## F18 — WhatsApp Connection screen

```text
Increment F18. Read docs/WhatsApp-SaaS-Product/06-FRONTEND/WHATSAPP-ONBOARDING.md and
docs/WhatsApp-SaaS-Product/08-META-WHATSAPP/EMBEDDED-SIGNUP.md.

Goal: a customer connects their own WhatsApp Business Account from our UI.

Requirements:
- Load Meta's JS SDK and launch the Embedded Signup popup with our app id, config id, and
  the Tech Provider flow parameters (values from VITE_ env vars, never hardcoded)
- Handle the callback: capture the exchangeable code and session info (waba id, phone number
  id), POST to /api/whatsapp/connect from F06
- Handle the user abandoning the popup gracefully — this happens often; do not leave the UI
  in a spinner
- Connection status card: connected number, verified name, quality rating, messaging limit
  tier, connected date
- CRITICAL: display a prominent warning when the WABA has no payment method attached on
  Meta's side. Explain plainly that Meta bills them directly for messages and that messages
  will fail without it, with a link to Meta's WhatsApp Manager. This is the single most
  common onboarding failure in the Tech Provider model — do not bury it.
- Explain the two-bill model on this screen in one short paragraph: our subscription is
  separate from Meta's per-message charges. Plain language, no jargon.
- States: not connected, connecting, connected-healthy, connected-no-payment-method,
  connection-error, token-expired

Do NOT build: multi-number support, number migration from the WhatsApp Business app.

Plan first.

Finally: write docs/IMPLEMENTATION/F18-<slug>.md following
docs/WhatsApp-SaaS-Product/14-CLAUDE-CODE/IMPLEMENTATION-DOC-TEMPLATE.md, and add a row to
docs/IMPLEMENTATION/INDEX.md. Document the key decisions, any divergence from the architecture
docs, and the gotchas someone would trip over. This is part of the increment, not optional.
```

**DoD:** connect flow completes from the UI · abandonment handled · payment-method warning prominent · two-bill model explained

---

## F19 — Configuration screens

> **PILOT TRACK: build a simplified version.** A read-only list of rules and FAQs plus the FAQ
> tester is enough — during the pilot you configure rules yourself on the onboarding call. Ten
> customers do not need self-serve editing. Build the full prompt below after the pilot.

```text
Increment F19. Read docs/WhatsApp-SaaS-Product/06-FRONTEND/AUTOMATION-CONFIGURATION.md.

Goal: a non-technical SMB owner can configure automation without calling you.

Requirements:
- Automation Rules: list, create, edit, enable/disable, reorder by priority (drag or simple
  up/down — up/down is fine and less fragile). Plain-language match type labels ("message
  contains", not "CONTAINS"). Live preview of what a test message would match.
- Hide REGEX behind an "advanced" toggle. Most users should never see it.
- FAQ: list, add, edit, delete question/answer pairs. A "test a question" box that calls the
  matcher and shows the match plus confidence — this is the feature that teaches users how
  to write good FAQs.
- Templates: list with Meta status and category badges. Submit new template. Show rejection
  reasons clearly. Warn visibly when a template is MARKETING category, with the cost
  implication in rupees.
- Show the cost warning from F15 when a configured action produces multiple messages.
- Every screen needs: empty state with a concrete first action, loading skeleton, error state
  with a retry, success confirmation. SMB users hit empty states constantly — an empty state
  that just says "No data" is a support ticket.

Do NOT build: a visual flow-chart builder, A/B testing, template version history.

Plan first.

Finally: write docs/IMPLEMENTATION/F19-<slug>.md following
docs/WhatsApp-SaaS-Product/14-CLAUDE-CODE/IMPLEMENTATION-DOC-TEMPLATE.md, and add a row to
docs/IMPLEMENTATION/INDEX.md. Document the key decisions, any divergence from the architecture
docs, and the gotchas someone would trip over. This is part of the increment, not optional.
```

**DoD:** a non-technical person can add a rule and an FAQ unaided · FAQ tester works · cost warnings visible · all four states on every screen

---

## F20 — Inbox and dashboard

```text
Increment F20.

Goal: see conversations, and see message counts. The counts are commercially urgent.

Requirements:
- Inbox: conversation list (contact name/number, last message preview, last activity,
  service-window countdown), message thread view, manual free-text reply while the service
  window is open
- Show the 24-hour service window status clearly per conversation, and disable free-text
  reply when it has expired with an explanation that a template is required. Getting this
  wrong is how users hit confusing Meta errors.
- Dashboard with per-tenant, per-category message counts for the current month, straight from
  F08's ledger: marketing / utility / authentication / service / inbound. Plus delivery
  outcome breakdown (sent, delivered, read, failed).
- Add a clear note that these are OUR counts for THEIR reference, and Meta's invoice is
  authoritative. Do not display rupee estimates yet unless the rate config from F08 is
  complete and dated — a wrong cost estimate is worse than none.
- Polling for new messages (simple interval) is fine. Do NOT add WebSockets or SSE yet.
- Mobile-first: the inbox must be usable one-handed on a 360px screen.

Tests/checks: counts reconcile exactly against a direct ledger query; service window state
correct at the boundary; failed messages show the Meta error in plain language.

Do NOT build: real-time push, agent assignment, canned responses, tags, CSV export,
charts beyond simple counts.

Plan first.

Finally: write docs/IMPLEMENTATION/F20-<slug>.md following
docs/WhatsApp-SaaS-Product/14-CLAUDE-CODE/IMPLEMENTATION-DOC-TEMPLATE.md, and add a row to
docs/IMPLEMENTATION/INDEX.md. Document the key decisions, any divergence from the architecture
docs, and the gotchas someone would trip over. This is part of the increment, not optional.
```

**DoD:** counts match the ledger exactly · service window state correct · usable at 360px · no WebSockets
