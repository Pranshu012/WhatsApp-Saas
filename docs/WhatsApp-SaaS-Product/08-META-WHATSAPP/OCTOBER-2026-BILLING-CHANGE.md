# The 1 October 2026 Billing Change

**⚠️ TIME-CRITICAL. Verified 18 August 2026 — roughly six weeks away.**

## What changes

From **1 October 2026**, Meta begins charging for two things that are free today:

| | Until 30 Sep 2026 | From 1 Oct 2026 |
|---|---|---|
| Service messages (free-form replies inside the 24h customer service window) | **Free** | **₹0.1150** (India, ex-GST) |
| Utility templates sent inside an open service window | **Free** | **Billable** |

Additional detail:
- **No volume tiers for service messages** — the rate stays flat however many you send. Support
  costs scale in a straight line.
- The 24-hour window still exists as a **permission** rule (you may send free-form replies), it
  just no longer means those replies are free.
- **Inbound messages remain free.**
- The **72-hour Free Entry Point window** (Click-to-WhatsApp ads, Page CTA) **remains free.**
- Separately, from 1 August 2026, Meta's own AI replies ("Meta Business Agent") are billed per
  token at about $2/1M tokens. Not our cost — we run our own automation.

## Why this matters to us specifically

Our product is a WhatsApp automation SaaS. If the core value proposition is "automatically reply
to your customers", then **our customers' Meta bills go from ₹0 to something real on 1 October**.

We don't pay it (ADR-005) — but we will absolutely be blamed for it, and "why is my Meta bill
₹4,000?" becomes the most common support ticket. How we handle it determines whether it causes
churn or builds trust.

## What we must do — before 1 October

### 1. Per-category message metering, visible to the customer  🔴 BUILD NOW
Increments **F08** (ledger) and **F20** (dashboard). Without a ledger we cannot answer the
question at all. This is the reason F08 sits early in the build order rather than late.

### 2. Audit all pricing language  🔴 BUILD NOW
- No "unlimited replies" anywhere — pricing page, contract, sales deck, WhatsApp pitch
- No implied obligation that maps to a per-unit Meta cost
- Golden Rule 6

### 3. Reply consolidation  🟡 F15
Each message is billed separately. Three short replies cost 3× one consolidated reply. Where a
flow currently sends multiple messages, merge them. This is a genuine feature after October, not
an optimisation.

### 4. Per-contact reply rate limits  🔴 F13
A loop or a misconfigured rule that replies repeatedly now **spends your customer's money**.
Cap auto-replies per contact per hour. Non-negotiable.

### 5. Rate config, not constants  🔴 F08
```sql
INSERT INTO whatsapp_rates (country_code, category, rate_minor, effective_from, source_note)
VALUES ('IN', 'SERVICE', 0,    '2026-07-01', 'Free until 2026-09-30'),
       ('IN', 'SERVICE', 1150, '2026-10-01', 'Billable from 2026-10-01 per Meta announcement');
```
With dated rates, the change is a database row. With constants, it's a deploy under time pressure.

### 6. Proactive customer communication  🔴 BUSINESS ACTION
**Write and send an explanation to every customer before 1 October.** Not after. Not when they
ask.

Being the vendor who warned them earns more goodwill than any feature you could ship in the same
six weeks. Being the vendor who didn't — while their bill changes — is how you lose a customer
you spent weeks acquiring.

### 7. Exploit the Free Entry Point window  🟡 LATER
For customers running Click-to-WhatsApp ads, the 72-hour window stays completely free. Worth
surfacing as guidance in the product.

## Draft customer note

> **Subject: A WhatsApp pricing change from Meta on 1 October**
>
> Hi [name],
>
> Meta is changing how it charges for WhatsApp messages from 1 October 2026. We want you to hear
> it from us first.
>
> **What changes:** today, when a customer messages you and you reply within 24 hours, that reply
> is free. From 1 October, Meta charges about ₹0.12 per reply (plus GST).
>
> **What it means for you:** based on your usage last month ([N] replies), this would have been
> about ₹[X]. You can see your message counts anytime on your dashboard.
>
> **What we're doing:** we've made your replies more efficient so that where we used to send
> three messages we now send one, which reduces what Meta charges you. Your dashboard shows a
> breakdown by message type so there are no surprises.
>
> **What you should know:** messages your customers send you are still free, and if people reach
> you through a Click-to-WhatsApp ad, everything is free for 72 hours.
>
> Happy to walk through your numbers on a call.

## Timeline

| By | Action |
|---|---|
| Now | F08 message ledger with category tagging |
| Now | Audit all pricing and contract language |
| Before F20 ships | Per-tenant, per-category counts visible in the dashboard |
| Before F13 ships | Per-contact reply rate limits |
| Mid-September 2026 | Send the customer note |
| 1 October 2026 | Add the new rate row; verify counts against a customer's actual Meta bill |
| Early October | Check in with every customer about their bill |

## What we deliberately do NOT do

- **Don't absorb the cost.** That breaks ADR-005 and destroys the margin structure.
- **Don't switch to a wallet model in a panic.** It needs a Meta credit line and provably correct
  metering. Stage 3, not a six-week reaction.
- **Don't hide the change.** They'll find out from Meta, and then trust is gone.
