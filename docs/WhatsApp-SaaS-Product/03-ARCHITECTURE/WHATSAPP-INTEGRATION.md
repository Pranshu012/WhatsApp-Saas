# WhatsApp Integration — From Zero

**Classification: BUILD NOW (increments F05–F11).** Setup steps in `08-META-WHATSAPP/`.

If you have never worked with the WhatsApp Business Platform, read this before anything else.

---

## The hierarchy

```text
Meta Business Portfolio  (formerly "Business Manager")
        │  a container owned by a legal business entity
        ▼
WhatsApp Business Account (WABA)
        │  the WhatsApp-specific account: templates, quality rating,
        │  messaging limits, billing
        ▼
Business Phone Number  (phone_number_id)
        │  the actual number that sends and receives.
        │  One WABA can hold several numbers.
        ▼
WhatsApp Cloud API      ← Meta-hosted. This is what our code calls.
```

**Two portfolios matter and confusing them wastes days:**

| Portfolio | Owns | Purpose |
|---|---|---|
| **Ours** | Our Meta App, our Tech Provider status | Our software's identity to Meta |
| **The customer's** | Their WABA, their phone number, their payment method | Their WhatsApp presence |

We never own the customer's WABA. That's the whole point of ADR-003.

---

## Key concepts

**Tech Provider** — Meta's designation for software vendors who onboard other businesses.
No credit line; the customer pays Meta directly. See ADR-003.

**Embedded Signup** — a Meta-hosted popup, launched from our frontend, in which the customer
creates or selects their WABA, verifies a phone number, and grants our app access. It returns
an exchangeable code plus the WABA and phone number IDs. Assets created this way are **owned by
the customer**, and they retain full access to WhatsApp Manager — we cannot restrict that.

**WABA ID** — identifies the customer's WhatsApp Business Account. Used for templates and
webhook subscription.

**Phone Number ID** — identifies the sending number. Used for every send. **Not** the phone
number itself; it's an opaque Meta ID. Store both.

**Access token** — after the code exchange we hold a token scoped to that customer's WABA.
This is the most sensitive thing in our database: it can message that business's entire
customer list in their name. Encrypted at rest, never logged (see `SECURITY.md`).

**App Review + Advanced Access** — our Meta App must be approved for
`whatsapp_business_management` and `whatsapp_business_messaging` with **Advanced Access** to
operate WABAs we don't own. Without it, calls return **error code 200** and the business model
does not work. This is the long-lead-time item in Phase 0.

**Webhooks** — Meta POSTs inbound messages and delivery statuses to our HTTPS endpoint. Two
separate things are required: configuring the webhook URL on our App, **and** subscribing our
App to each customer's WABA (an API call during onboarding). Miss the second and nothing arrives.

**Message templates** — pre-approved message formats, required to initiate a conversation
outside the 24-hour customer service window. Meta assigns the billing category, and its
assignment overrides what you requested.

**Customer service window** — a customer messaging your business opens a 24-hour window during
which free-form (non-template) replies are permitted. **Until 30 Sept 2026 those replies are
free; from 1 Oct 2026 they are billable** at the utility/authentication rate. Track
`service_window_expires_at` accurately.

**Free Entry Point window** — if the customer arrives via a Click-to-WhatsApp ad or a Facebook
Page CTA and you reply within 24 hours, a **72-hour** window opens in which everything,
including templates, is free. This stays free after October — worth exploiting.

---

## Message categories and what they cost

India, Meta list rates effective 1 July 2026, INR, **excluding 18% GST**:

| Category | Rate | When |
|---|---|---|
| Marketing | ₹0.8631 | Promotions, offers, re-engagement. No volume discount, ever. |
| Utility | ₹0.1150 | Order updates, reminders, receipts — tied to a customer action |
| Authentication | ₹0.1150 | OTPs |
| Service | ₹0 → **₹0.1150 from 1 Oct 2026** | Free-form replies inside the 24h window |
| Inbound | Free, always | Messages customers send you |

Marketing costs **7.5×** utility. Template mis-categorisation is therefore an expensive
mistake — and it's your customer's money. Surface Meta's assigned category prominently.

---

## What our application stores

| Data | Where | Notes |
|---|---|---|
| `waba_id` | `whatsapp_accounts` | Templates, webhook subscription |
| `phone_number_id` | `whatsapp_accounts` | Every send |
| `display_phone_number` | `whatsapp_accounts` | Display only |
| `verified_name` | `whatsapp_accounts` | Display only |
| `access_token` | `whatsapp_accounts`, **AES-256-GCM** | Never logged, never in a response |
| `quality_rating`, `messaging_limit_tier` | `whatsapp_accounts` | Surface in UI — customers care |
| Templates + Meta-assigned category | `templates` | Meta is authoritative |
| `wamid` per message | `message_ledger` | Meta's message id; dedupe + status matching |
| End-customer phone | `contacts` (full) / `message_ledger` (hash + last4) | Deliberate asymmetry |
| `service_window_expires_at` | `conversations` | Commercially significant after Oct 2026 |

**We do not store:** the customer's Meta password, their payment details, or their Meta
invoices. We never see any of those.

---

## What can go wrong, and what it means

| Symptom | Cause | Fix |
|---|---|---|
| Error code **200** | App lacks Advanced Access | Complete App Review. Not a code bug. |
| Webhooks never arrive | App not subscribed to that WABA | The subscribe API call during onboarding |
| Sends fail after a successful connect | **No payment method on the customer's WABA** | Customer adds it in WhatsApp Manager. Most common real failure. |
| Template rejected | Wording looks promotional, or category mismatch | Rewrite; check Meta's categorisation guidance |
| Messages send but never deliver | Recipient not on WhatsApp, or blocked you | Surface the error plainly |
| Quality rating drops to red | Users marking messages as spam | Reduce marketing volume, improve targeting. Meta will cap sending. |
| Signature verification always fails | HMAC computed over reserialised JSON | Use the **raw** request bytes |
| Messaging limit hit | New WABAs start with low limits | Limits rise with good-quality usage over time |

## DO NOT BUILD YET

WhatsApp Flows · product catalogs / commerce · payments in WhatsApp · Click-to-WhatsApp ad
management · multiple numbers per tenant · number migration from the WhatsApp Business app
(unless D-07 says most customers need it) · Coexistence mode.
