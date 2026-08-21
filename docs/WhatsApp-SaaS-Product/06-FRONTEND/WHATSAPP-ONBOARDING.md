# WhatsApp Onboarding Screen

Increment **F18**. Route `/whatsapp`. Read `../08-META-WHATSAPP/EMBEDDED-SIGNUP.md` alongside.

This is the highest-risk screen in the product. Most churn-before-activation happens here.

## Before the popup — set expectations

Show this *before* the Connect button, not after they're stuck:

> **What you'll need (about 10 minutes)**
> - A Facebook account you can log into
> - A phone number **not currently active on WhatsApp** (personal or Business app)
> - Access to that phone to receive an OTP
> - Your business name, address, and website
>
> WhatsApp charges you directly for messages. Our ₹1,999/month covers the software only.
> You'll add a payment method in Meta's WhatsApp Manager after connecting.

Every one of those bullets is a real support ticket you're pre-empting. The phone number one
especially: customers routinely try to use a number already on WhatsApp Business and hit a
confusing Meta error.

## The popup

```ts
FB.login(callback, {
  config_id: import.meta.env.VITE_META_CONFIG_ID,
  response_type: 'code',
  override_default_response_type: true,
  extras: { setup: {}, featureType: '', sessionInfoVersion: '3' }
});
```

Meta also posts session info to `window` via a message listener — that's where the WABA ID and
phone number ID arrive. Capture both the `code` and the session info, then POST to
`/api/whatsapp/connect`.

Verify the IDs server-side against the Graph API. Never trust IDs from the browser.

## States — all six must be handled

| State | UI |
|---|---|
| **Not connected** | Expectations block + Connect button |
| **Connecting** | Spinner with "Waiting for Meta…" and a Cancel option |
| **Abandoned** | Popup closed without completing → back to Not connected, plain message, no error styling. This is common and not a failure. |
| **Connected, healthy** | Number, verified name, quality rating, messaging tier, connected date |
| **Connected, no payment method** | ⚠️ **Prominent banner** (see below) |
| **Error / token expired** | Plain explanation + Reconnect |

Do not leave the UI spinning when the popup is dismissed. Listen for the window close.

## The payment-method warning

The single most common Tech Provider onboarding failure: the customer connects successfully,
sends nothing, and concludes the product is broken — because they never added a payment method
in Meta's WhatsApp Manager.

```text
⚠️  Your WhatsApp account has no payment method

Meta bills you directly for messages you send. Without a payment
method, your messages will fail.

This is separate from your ₹1,999/month subscription with us.

[ Add payment method in WhatsApp Manager → ]
```

Red banner, top of the screen, not dismissible until resolved. Repeat it on the dashboard.
Being annoying here is correct.

## The two-bill explanation

Put this on the screen permanently, in one short paragraph:

> **Two separate bills.** We charge ₹1,999/month for the software. Meta charges you directly
> for messages — currently about ₹0.115 per utility or service message and ₹0.86 per marketing
> message in India, plus GST. You control that spend and see it in WhatsApp Manager.

Customers who understand this from day one don't get angry in month two. Customers who don't
understand it churn and tell people you have hidden charges.

## Connection health

Poll or refresh on load:
- **Quality rating** GREEN / YELLOW / RED — explain plainly what RED means (Meta may restrict
  sending) and that it's driven by their customers' block/report rate
- **Messaging limit tier** — how many unique customers they can message in 24h
- **Token status** — if expired, prompt reconnect. Do not fail silently.

## Not in the MVP

Multiple phone numbers per tenant · migrating a number off the WhatsApp Business app ·
changing display name · business profile editing (send them to WhatsApp Manager).
