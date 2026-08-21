# DPDP Considerations

India's **Digital Personal Data Protection Act, 2023**. Practical guidance, not legal advice —
have a lawyer review your policy and terms once before your first B2B customer.

> ⚠️ **Verify current status.** DPDP rules and enforcement timelines have been rolling out in
> stages. This document reflects understanding as of **August 2026**. Re-check before launch and
> quarterly thereafter — see `../00-START-HERE/ASSUMPTIONS-AND-EXPIRY-DATES.md`.

## Your role in the Act's language

| DPDP term | Who |
|---|---|
| **Data Principal** | The end customer whose data is processed |
| **Data Fiduciary** | Your SMB customer — they decide why and how |
| **Data Processor** | **You** — you process on the Fiduciary's behalf |

For your *own* customer records (the SMB's email, GSTIN, billing) you are the Fiduciary.

This split matters: as a Processor you don't obtain consent from end customers, but you do owe
your customer security, deletion on instruction, and breach notification.

## Obligations you must actually implement

### 1. Purpose limitation

Process end-customer data only to deliver the service. No repurposing — no aggregating
conversations for market insight, no training anything on it, no selling it.

Reinforced architecturally by ADR-007: no LLM in the automation path, so conversations never
leave your infrastructure for a third party that might train on them. **Free LLM tiers commonly
permit training on submitted prompts** — Gemini's free tier does. That would be a genuine DPDP
problem, not a theoretical one.

### 2. Data minimisation

Already designed in:
- `message_ledger` stores phone **hash + last 4**, not the full number
- No card data (Razorpay-hosted checkout)
- No enrichment, no third-party data purchase
- `webhook_events` raw payloads purged on a rolling window

### 3. Security safeguards

DPDP requires "reasonable security safeguards". Yours:

- Two-layer tenant isolation (application + PostgreSQL RLS), test-verified
- Non-superuser database role (superusers bypass RLS)
- AES-256-GCM encryption of WhatsApp tokens
- TLS everywhere; Postgres bound to localhost
- Backups encrypted before leaving India
- Sentry scrubbing verified with a test error
- Documented access control and logging

Document these. "Reasonable safeguards" is easier to argue when you can point at a written
control set and passing tests.

### 4. Breach notification

You must notify affected parties and the Data Protection Board. As a Processor, notify **your
customer** promptly — they carry the Fiduciary obligation.

Have this ready before you need it:

```text
1. Contain (see ../10-OPERATIONS/INCIDENT-RESPONSE.md → SEV1)
2. Assess: which tenants, which data, how many records, what window
3. Notify affected customers within 24 hours — plain language, no minimising
4. Notify the Data Protection Board per current rules
5. Written post-mortem with the concrete fix
6. Keep records — you may need to demonstrate the response
```

**Do not quietly patch a real leak.** That converts a technical incident into a legal one.

### 5. Deletion on instruction

Your customer's end customer exercises a right → your customer instructs you → you delete.
Within 30 days, covering:

- [ ] `contacts` row
- [ ] `conversations` and message bodies
- [ ] R2 media under `{tenantId}/...` for that contact
- [ ] **`webhook_events` raw payloads** — contains message bodies and full phone numbers
- [ ] `message_ledger` → reduce to hash-only; **do not delete** (billing evidence, and it's
      append-only by design)

The `webhook_events` one is the easy miss. Raw Meta payloads contain everything.

**Build and test this path before launch.** A deletion request you can't fulfil is a compliance
failure and an embarrassing customer conversation.

### 6. Cross-border transfer

DPDP permits transfer except to government-restricted countries. Yours:

| Destination | Data | Mitigation |
|---|---|---|
| Backblaze B2 (US) | Backups | **Encrypted before upload** — plaintext never leaves India |
| Sentry (US/EU) | Error data | Scrubbed of PII, tokens, message bodies |
| Meta (global) | Messages | Inherent to WhatsApp; disclose it |
| Cloudflare (global) | Traffic, media | Bucket private |

Disclose all of these in the privacy policy. The encryption-before-upload and scrubbing are what
make the US destinations defensible — so verify they actually work.

Primary data stays in India (Oracle Mumbai/Hyderabad). Good posture, and better latency.

## Your contractual position

**Terms of service must state:**
- You are a Processor acting on the customer's instructions
- The customer warrants they have valid opt-in for every contact they message
- The customer is responsible for the lawfulness of message content
- You charge for software; **Meta bills the customer directly** for messages
- Retention periods, including the 8-year financial-records exception
- Breach notification commitment
- Sub-processors named

The opt-in warranty is not boilerplate — it's what separates your position from your customer's
if they message purchased numbers.

**Do not build bulk contact import or campaign features in the MVP.** Beyond scope discipline: a
CSV of purchased numbers destroys their WABA quality rating, generates complaints, and creates a
consent problem your terms alone won't fully insulate you from.

## What you don't need at your scale

- A Data Protection Officer (Significant Data Fiduciary obligations don't apply at 20 customers)
- A Consent Manager registration
- A DPIA (worth doing informally; not required)
- ISO 27001 or SOC 2

Revisit at ~1,000 customers or your first enterprise customer, whichever comes first.

## Pre-launch checklist

- [ ] Privacy policy live, naming all sub-processors and cross-border transfers
- [ ] Terms of service live, with the Processor role and opt-in warranty
- [ ] Lawyer has reviewed both (once)
- [ ] Deletion path **built and tested**, including `webhook_events`
- [ ] Backup encryption verified working before upload
- [ ] Sentry scrubbing verified with a test error containing fake PII
- [ ] Breach response steps written and readable under stress
- [ ] `opt_in_status` tracked on contacts and visible in the UI
- [ ] Retention windows implemented (90-day cancellation, rolling `webhook_events` purge)
- [ ] Re-verified DPDP rule status this quarter
