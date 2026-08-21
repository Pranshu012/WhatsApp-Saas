# Tech Provider Setup

**Classification: BUILD NOW (Phase 0).** Decision rationale in `13-DECISIONS/ADR-003`.

## Tech Provider vs Solution Partner — the whole difference

Verified 18 August 2026:

| | Solution Partner | **Tech Provider (us)** |
|---|---|---|
| Credit line from Meta | Yes | No |
| Who pays Meta for messages | Partner, then invoices the client | **The client, directly** |
| Partner bills for | Software + messages | **Software only** |
| Entry barrier | Meta Business Partner application, lengthy | Business Verification + App Review |
| Capability difference | **None** | **None** |

Meta's own documentation frames the difference as **billing, not capability**. Neither tier
grants a feature the other lacks. So we take the one with no credit line, no float risk, and no
messaging cost on our P&L.

## What this means commercially

```text
Customer pays US:     ₹1,999/month software subscription  → ~95%+ gross margin
Customer pays META:   per-message charges, directly       → never touches our books
```

Our margin is structurally immune to message volume and to Meta's quarterly rate changes —
including the 1 October 2026 change that makes service messages billable.

## What this means technically

1. **Embedded Signup is mandatory.** It's how a customer creates a WABA they own while granting
   our app access.
2. **We hold a scoped access token per customer** — encrypted at rest, never logged.
3. **We must subscribe our app to each customer's WABA** during onboarding. A separate API call.
4. **The customer must attach their own payment method** on Meta. We can't do it for them, and
   messages fail without it.
5. **Advanced Access is required** for `whatsapp_business_management` and
   `whatsapp_business_messaging`, or calls against customer WABAs return error 200.

## Setup sequence

Steps 1–6 are in `META-BUSINESS-SETUP.md`. Then:

### Configure Embedded Signup

1. App Dashboard → **Facebook Login for Business** → Settings
2. Add your redirect/OAuth domain
3. Create a **configuration** for the WhatsApp Embedded Signup flow
4. Note the **config ID** — the frontend needs it (`VITE_META_CONFIG_ID`)
5. Your Embedded Signup page must be served over **valid HTTPS**

### Permissions your config requests

| Permission | Why |
|---|---|
| `whatsapp_business_management` | Read/write WABA settings and templates |
| `whatsapp_business_messaging` | Send and receive messages, manage phone numbers |

Both need **Advanced Access** to work on WABAs you don't own.

### App mode

- **Development mode:** only app admins/developers can complete Embedded Signup. Fine for testing.
- **Live mode:** required for real customers. Switch when App Review is approved.

## Onboarding limits

| State | New customers / rolling 7 days |
|---|---|
| Default | 10 |
| After Business Verification + App Review + Access Verification | 200 |
| More | Apply for Meta Business Partner |

**10/week fits the validation plan exactly.** No need to raise this before you have 20 customers.

## What the customer experiences

```text
1. In our app, clicks "Connect WhatsApp"
2. Meta-hosted popup opens (our branding appears alongside Meta's)
3. Logs into Facebook / creates a Business Portfolio if needed
4. Creates or selects a WhatsApp Business Account
5. Adds and verifies a phone number (SMS or voice OTP)
6. Grants our app the requested permissions
7. Popup closes; our app receives a code + WABA/phone IDs
8. → THEY MUST THEN ADD A PAYMENT METHOD in WhatsApp Manager
```

**Step 8 is not part of the popup.** It's the step everyone forgets, and messages silently fail
without it. For your first 20 customers, do this on a call with them and watch it happen.

Note: customers onboarded this way **own all their WhatsApp assets** and retain full access to
WhatsApp Manager. You cannot restrict that access, and shouldn't want to — it's a genuine trust
argument in sales.

## Testing before Advanced Access

The test WABA that comes with the WhatsApp product works with standard access. Use it to:
- Build and test the send path
- Build and test the webhook receiver
- Record the App Review screencast

You **cannot** test onboarding a third-party WABA without Advanced Access — that's the whole
point of the permission.

## Verification checklist

- [ ] Business Verification: Verified
- [ ] Tech Provider onboarding completed ("without a partner")
- [ ] App Review approved for **both** permissions, **Advanced Access**
- [ ] Embedded Signup config created; config ID recorded
- [ ] OAuth/redirect domain configured
- [ ] App switched to **Live** mode before the first real customer
- [ ] Webhook configured and handshake successful
- [ ] Test WABA working for development
- [ ] You can articulate the two-bill model in one sentence to a customer

## Common failures

| Symptom | Cause |
|---|---|
| Error code 200 on any customer WABA call | No Advanced Access. Not a code bug. |
| Embedded Signup won't open | HTTPS invalid, wrong config ID, or domain not whitelisted |
| Only you can complete signup | App still in Development mode |
| Signup completes but no webhooks | App not subscribed to that WABA (the F06 API call) |
| Sends fail after successful connect | **Customer has no payment method on Meta** |
| Cannot onboard an existing WABA | WABAs created via a developer app can't be selected in Embedded Signup |
