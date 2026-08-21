# Message Templates

**Classification: BUILD NOW (F12).** Implementation in `05-BACKEND/TEMPLATE-SERVICE.md`.

## Why they exist

Outside the 24-hour customer service window you **cannot** send free-form text. You must use a
template Meta has pre-approved. Templates also carry the **billing category**, which drives cost
by a factor of 7.5 in India.

## Categories — Meta decides, not you

| Category | Content | India rate |
|---|---|---|
| **Utility** | Order updates, appointment reminders, receipts — tied to a transaction the customer already agreed to | ₹0.1150 |
| **Authentication** | OTPs, login codes | ₹0.1150 |
| **Marketing** | Promotions, offers, re-engagement, anything that sells | **₹0.8631** |

You request a category on submission. **Meta assigns the final one, and Meta's assignment wins.**
A template you submitted as Utility may come back as Marketing — at 7.5× the cost, charged to
your customer.

So: store both the requested and assigned category, and **warn visibly when they differ**.

## What gets a Utility template rejected or reclassified

| Problem | Example | Fix |
|---|---|---|
| Promotional language | "Special offer just for you!" | Remove; describe the transaction only |
| No transactional trigger | "Hope you're well, visit us soon" | Tie it to something the customer did |
| Vague content | "Update regarding your request" | Be specific |
| Placeholder at the very start or end | `{{1}}, your order shipped` | Add surrounding text |
| Too many consecutive placeholders | `{{1}} {{2}} {{3}}` | Add connecting words |

**The reliable test:** does this message reference something the customer actively did? If not,
it's marketing, whatever you call it.

## Structure

```text
Header      (optional)  text | image | document | video
Body        (required)  up to 1024 chars, supports {{1}}, {{2}}, ...
Footer      (optional)  up to 60 chars, no variables
Buttons     (optional)  quick reply, or call-to-action (URL / phone)
```

Placeholders are **positional**. Meta doesn't support named variables, so don't build an
abstraction that pretends otherwise — you'd have to unwind it.

## Good and bad examples

**Good utility template:**
```
Header: Appointment confirmed
Body:   Hi {{1}}, your appointment at {{2}} is confirmed for {{3}}.
        Reply CANCEL to cancel.
Footer: Sunrise Dental
```
Specific, transactional, placeholders surrounded by text.

**Will be reclassified as marketing:**
```
Body: Hi {{1}}! We miss you. Book now and get 20% off!
```

## Lifecycle

```text
Draft (in our UI)
   → submit to Meta
   → PENDING          (minutes to hours — show this honestly)
   → APPROVED | REJECTED (with reason)
        │
   APPROVED → usable
        │
   may later become PAUSED (poor quality) or DISABLED (repeated violations)
```

A template can be **paused** by Meta if recipients repeatedly report it. Handle `PAUSED` — it
means sends will fail, and the customer needs to know why.

## Our rules

1. **Meta is the source of truth** for status, category, and rejection reason. A local edit never
   overwrites them on sync.
2. **Block sends on non-APPROVED templates before the API call** — clearer error, no wasted call,
   no quality-metric impact.
3. **Validate variable count locally.** A 3-placeholder template sent with 2 values fails with
   error 132000; catching it locally is a much better experience.
4. **Show the rejection reason to the customer.** They wrote the content; they need to fix it.
5. **Warn on MARKETING templates** in the UI, with the rupee implication. It's their money.
6. **Sync daily** plus on connect, plus on `message_template_status_update` webhooks.

## Language

Templates are per-language (`en`, `hi`, `en_US`). Same name + different language = different
template. Our unique constraint is `(tenant_id, name, language)`.

MVP is English only (D-06). Storage supports more; the UI doesn't need to yet.

## DO NOT BUILD YET

A visual template designer · version history · A/B testing · automatic re-categorisation appeals ·
bulk template import · a template marketplace or library.

**A starter library of 5–10 pre-written, Meta-friendly utility templates per vertical would be
genuinely valuable** — it's the fastest path to a customer's first successful send. But write them
as documentation for the onboarding call first, and only build a UI for it once you've seen which
ones customers actually pick.
