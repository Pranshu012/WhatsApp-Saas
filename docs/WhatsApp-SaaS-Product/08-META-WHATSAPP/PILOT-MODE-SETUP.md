# Meta Setup for the Pilot (Development Mode)

The lightweight Meta setup for 5–10 pilot customers. **No Business Verification. No App Review.
No Advanced Access.**

Read [../00-START-HERE/PILOT-FIRST-PLAN.md](../00-START-HERE/PILOT-FIRST-PLAN.md) first.
Full production setup is in [META-BUSINESS-SETUP.md](META-BUSINESS-SETUP.md) and
[TECH-PROVIDER-SETUP.md](TECH-PROVIDER-SETUP.md) — you'll need those *after* the pilot.

---

## Why this works

Meta's Embedded Signup documentation states that while your app is in **development mode**, the
WhatsApp permissions appear in the authorisation screen to anyone with an **admin, developer, or
tester** role on your app. Only in **live mode** are you restricted to App Review–approved
Advanced Access permissions.

Separately, Meta's App Review documentation states that Business-type apps are **automatically
granted Standard Access** to all permissions available to that app type, specifically so you can
test at that level.

Put together: **development mode + Tester role = a working Tech Provider flow for a small,
known group of businesses.**

> ⚠️ Confirm this on Meta's developer docs before you build on it, and note the date in
> `../00-START-HERE/ASSUMPTIONS-AND-EXPIRY-DATES.md`. This is the assumption the entire pilot
> timeline rests on.

---

## Step 1 — Personal Facebook account

You need one. Real name. If you don't have one, create it now — Meta sometimes flags brand-new
accounts, so earlier is better.

## Step 2 — Meta Business Portfolio

https://business.facebook.com → create a business portfolio.

You can use your own name as the business name for now. **Do not start Business Verification.**
That's the step requiring registration documents, and it's exactly what you're deferring.

## Step 3 — Create the app

https://developers.facebook.com → My Apps → Create App → type **Business**.

Add the **WhatsApp** product to it.

This automatically gives you:
- A **test WhatsApp Business Account**
- A **test business phone number** (free, can message up to 5 verified recipients)
- A pre-approved `hello_world` template

The test number is what you develop against for F05–F16. It costs nothing and needs no payment
method.

Store in your password manager:

| Value | Sensitivity |
|---|---|
| App ID | Public — safe in the frontend |
| **App Secret** | **Secret — server only, never in the SPA bundle** |
| Webhook verify token (you choose it) | Secret |

## Step 4 — Keep the app in Development mode

There is a Live/Development toggle at the top of the App Dashboard.

**Leave it on Development.** Switching to Live is what triggers the Advanced Access requirement.
You'll switch after the pilot, once App Review is approved.

## Step 5 — Configure Embedded Signup

Follow [EMBEDDED-SIGNUP.md](EMBEDDED-SIGNUP.md) for the configuration itself — the setup is
identical to production. You need:

- Facebook Login for Business → a configuration with **WhatsApp Embedded Signup**
- Products: WhatsApp Cloud API
- Access token expiration: **Never**
- Assets: WhatsApp accounts, with `whatsapp_business_management` and
  `whatsapp_business_messaging`
- Save the **config_id** — the frontend needs it (`VITE_META_CONFIG_ID`, public by design)

## Step 6 — Webhooks

Callback URL: `https://api.yourdomain.com/api/webhooks/whatsapp`
(during local development, an ngrok URL — see [WEBHOOKS.md](WEBHOOKS.md))

Subscribe to the message and status fields. Remember: subscribing your **app** to webhooks is
separate from subscribing to each **customer's WABA** — the latter is an API call your F06 code
makes at connect time, and nothing arrives without it.

---

## Step 7 — Adding a pilot customer (repeat per customer)

This is the one step that differs from production. Do it on your onboarding call, screen-shared.

**Before the call, from your side:**

1. App Dashboard → **App Roles** → **Roles** → Add People
2. Add them as **Tester** (not Developer — Tester is the minimum needed and the least
   privileged)
3. You'll need their Facebook account name or ID. Ask for it beforehand.
4. Meta sends them an invitation

**On the call, from their side:**

5. They accept the invitation at https://developers.facebook.com/settings/developer/requests/
   — this URL is worth sending them directly, it's hard to find otherwise
6. They then click **Connect WhatsApp** in your app and go through Embedded Signup normally
7. They select or create their WABA and phone number
8. Your F06 code exchanges the code, stores the encrypted token, subscribes webhooks

**Tell them plainly what the Tester role means:**

> This adds you as a tester on our app while we're in pilot. It only lets our software connect to
> the WhatsApp account you choose. You can remove it any time from your Facebook developer
> settings, and it gives us no access to your personal Facebook account.

Being upfront here prevents the "why do you need developer access to my Facebook?" conversation
turning into a lost customer.

---

## Pilot limits — know these before you promise anything

| Limit | Value | Impact on a 10-customer pilot |
|---|---|---|
| Conversations without Business Verification | ~250 per 24 hours | Not binding at pilot volume, but **watch it** |
| Test number recipients | 5 verified numbers | Development only — pilot customers use their own real numbers |
| App roles | Practically limited | Fine for 10; a reason you can't grow this way |
| Messaging limit tier (new WABA) | Typically 250 unique customers/24h initially | Their limit, rises with quality and verification |

**The 250/24h conversation cap is the one to monitor.** If one pilot customer has a busy day, it
is shared across your app. Add a health check on it and tell customers if you're approaching it —
better than silent failures.

---

## Costs during the pilot

| Item | Cost |
|---|---|
| Meta app, test WABA, test number | ₹0 |
| Your development messages (test number) | ₹0 |
| Pilot customers' real messages | **They pay Meta directly** — roughly ₹100–500/month each at pilot volume |

You pay Meta nothing. That does not change between pilot and production — it's the Tech Provider
model (ADR-003, ADR-005).

**Each pilot customer still needs a payment method attached in WhatsApp Manager.** This is the
single most common onboarding failure. Verify it on the call, with them, before you hang up.

---

## What changes after the pilot

| Now | After |
|---|---|
| Development mode | Live mode |
| Standard Access (automatic) | Advanced Access (App Review) |
| Tester role per customer | Anyone can sign up |
| ~250 conversations/24h | Normal limits |
| No Business Verification | Verified |
| ~10 customers max, manually added | 10 new/rolling 7 days, then 200/week after full verification |

The transition sequence is in
[../00-START-HERE/PILOT-FIRST-PLAN.md](../00-START-HERE/PILOT-FIRST-PLAN.md).

One genuine benefit of doing the pilot first: **App Review requires a screen recording of your
app actually using each requested permission.** After a pilot you have a real product and real
usage to record, instead of building a mock-up specifically for the submission.

---

## Definition of Done

- [ ] Personal Facebook account exists
- [ ] Business portfolio created (**Business Verification NOT started**)
- [ ] Business-type app created with the WhatsApp product
- [ ] App is in **Development** mode and stays there
- [ ] App ID, App Secret, verify token stored in the password manager
- [ ] Test WABA and test number working — you sent yourself a `hello_world`
- [ ] Embedded Signup configuration created; `config_id` saved
- [ ] Webhook callback URL configured and verified
- [ ] You have confirmed the dev-mode/Tester behaviour on Meta's current docs, with the date
      recorded in `../00-START-HERE/ASSUMPTIONS-AND-EXPIRY-DATES.md`
