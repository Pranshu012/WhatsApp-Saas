# Meta Business Setup — Step by Step

**Classification: BUILD NOW (Phase 0). This is the longest lead-time task in the project.
Start it on day one.**

Allow **1–3 weeks** including App Review. Do Phase A development while waiting.

---

## What you're creating

```text
YOUR Meta Business Portfolio        ← your company's identity on Meta
        │
        ├── Business Verification   ← proves you're a real business
        │
        └── YOUR Meta App           ← your software's identity
                │
                ├── WhatsApp product added
                ├── Tech Provider onboarding
                └── App Review → Advanced Access   ← the critical unlock
```

You are **not** creating a WhatsApp Business Account here. Your customers create their own,
later, through Embedded Signup.

---

## Before you start — have these ready

- [ ] A **registered business entity** (see D-05). Sole proprietorship is usually enough to start.
- [ ] **Business registration document** — GST certificate, Certificate of Incorporation, or
      Shop & Establishment licence
- [ ] **PAN** (business or proprietor)
- [ ] **A business address** matching your documents
- [ ] **A business phone number and email** — email on your own domain, not Gmail. Meta treats
      free-email domains with more suspicion, and B2B buyers do too.
- [ ] **Your domain, live, with valid HTTPS.** A parked domain or a `*.pages.dev` URL will
      undermine App Review. Put up a real one-page site describing the product.
- [ ] A personal Facebook account (used to administer the Business Portfolio — Meta requires this)

**The single most common cause of verification rejection is a mismatch** between the business
name, address, or phone number on your documents and what you type into Meta. Copy them exactly,
character for character, including punctuation and abbreviations.

---

## Step 1 — Create a Meta Business Portfolio

1. Go to `business.facebook.com`
2. Create a business portfolio
3. Enter: legal business name (exactly as on your documents), your name, business email
4. Confirm the email

**Name it after your business, not the product**, if they differ — verification checks the legal
entity.

## Step 2 — Business Verification

1. In Business Settings → **Security Centre** → Business Verification → Start verification
2. Enter business details: legal name, address, phone, website
3. Upload your registration document
4. Verify the phone number or email

**Timeline:** typically 2–10 business days. Sometimes minutes; sometimes it stalls.

**If rejected:** read the stated reason. Usually it's a document/detail mismatch, an
unreachable phone number, or a website that doesn't clearly describe a real business. Fix and
resubmit — you get multiple attempts.

## Step 3 — Create your Meta App

1. Go to `developers.facebook.com` → My Apps → Create App
2. Use case: **Other** → App type: **Business**
3. Link it to the Business Portfolio from Step 1
4. Note your **App ID** and **App Secret**

**The App Secret is a credential.** It goes into your `.env`, never into Git, never into a chat
window, never into a screenshot.

## Step 4 — Add the WhatsApp product

1. In the App Dashboard → Add Product → **WhatsApp** → Set up
2. You'll see a test number and a test WABA. **Useful for early experiments, never for a paying
   customer.**

## Step 5 — Tech Provider onboarding

1. App Dashboard → Use cases → Customise (pencil) on the WhatsApp use case
2. Select **Tech Provider onboarding** in the left menu
3. Business Verification (Step 2) must show complete
4. Choose **"Onboard without a partner"** — we go direct to Cloud API, no BSP (ADR-003)
5. Begin App Review

Why "without a partner": a BSP adds a 10–30% per-message markup plus a monthly platform fee, for
infrastructure we're building ourselves.

## Step 6 — App Review for Advanced Access  ⚠️ THE CRITICAL STEP

Request **Advanced Access** for:

- `whatsapp_business_management`
- `whatsapp_business_messaging`

**Without Advanced Access, API calls against WABAs you don't own return error code 200, and the
entire Tech Provider model does not function.** Standard access only works on your own WABA,
which is useless for a SaaS.

You'll need to provide:
- A clear description of what your app does
- A **screencast** showing the Embedded Signup flow and a message being sent
- A privacy policy URL and terms of service URL on your domain

**Chicken-and-egg problem:** the screencast needs a working Embedded Signup flow, which needs
Advanced Access. The way through: build the flow against your **test WABA** (which works with
standard access), record that, and submit. Reviewers understand this.

**Timeline:** a few days to two weeks. Rejections usually mean an unclear screencast or a
privacy policy that doesn't mention WhatsApp data handling.

## Step 7 — Configure webhooks

1. App Dashboard → WhatsApp → Configuration → Webhooks → Edit
2. **Callback URL:** `https://yourdomain.com/api/webhooks/whatsapp`
3. **Verify token:** a random string you generate; put the same value in
   `META_WEBHOOK_VERIFY_TOKEN`
4. Meta immediately calls `GET` your URL with a challenge — **your app must already be deployed
   and responding**, or this fails
5. Subscribe to fields: `messages`, `message_template_status_update`

**This only configures webhooks for your App.** You must *also* subscribe your app to each
customer's WABA via an API call during onboarding (increment F06). Forget that and nothing
arrives — it is the most common integration bug in this model.

For local development, use a tunnel (ngrok, Cloudflare Tunnel) to expose `localhost:8080`.

## Step 8 — Onboarding limits

| State | New business customers per rolling 7 days |
|---|---|
| Default | **10** |
| After Business Verification + App Review + Access Verification | **200** |
| Beyond that | Apply to become a Meta Business Partner |

10/week is exactly right for validating with 10–20 customers. Don't rush to raise it.

---

## Phase 0 checklist

- [ ] Business entity registered, documents in hand
- [ ] Domain live with HTTPS and a real one-page site
- [ ] Privacy policy and terms published (must mention WhatsApp data handling)
- [ ] Business email on your own domain
- [ ] Meta Business Portfolio created
- [ ] **Business Verification: Verified**
- [ ] Meta App created; App ID and Secret in `.env` (never Git)
- [ ] WhatsApp product added
- [ ] Tech Provider onboarding started, "without a partner"
- [ ] **App Review submitted for both permissions with Advanced Access**
- [ ] Webhook URL configured and the handshake succeeded
- [ ] Test WABA works for development

## Things that will waste your time

| Mistake | Cost |
|---|---|
| Document details don't match exactly what you typed | Days, per resubmission |
| Free-email domain for the business email | Higher rejection risk |
| No real website | App Review rejection |
| Privacy policy silent on WhatsApp data | App Review rejection |
| Requesting Standard instead of Advanced Access | You discover it only when error 200 appears in production |
| Configuring the webhook before deploying | Handshake fails, confusing errors |
| Using the test number for a real customer | Message limits, no real WABA, wasted onboarding |
| Committing the App Secret | Rotate immediately; assume it's compromised |
