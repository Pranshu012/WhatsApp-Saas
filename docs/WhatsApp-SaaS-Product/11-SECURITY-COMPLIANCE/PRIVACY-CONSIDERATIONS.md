# Privacy Considerations

Practical privacy posture. Legal specifics in `DPDP-CONSIDERATIONS.md`.

## The uncomfortable truth to be honest about

You are handling conversations between Indian SMBs and their customers. Those conversations
contain order details, addresses, complaints, health queries, and sometimes payment discussion.
You did not collect this data and you have no relationship with the people in it.

That argues for a conservative posture: store the minimum, keep it in India where you can, delete
it on schedule, and never repurpose it.

## Privacy policy — what it must actually say

Not boilerplate. These specific points:

1. **You are a processor, not the fiduciary.** Your customer (the SMB) is responsible for their
   end customers' data; you act on their instructions.
2. **What you store** — the tables in `CUSTOMER-DATA.md`, in plain language.
3. **Sub-processors, named**, including that Backblaze (backups, encrypted) and Sentry (errors,
   scrubbed) are outside India.
4. **Meta's role.** Messages pass through WhatsApp; Meta's own terms and privacy policy apply to
   that leg. You cannot and do not control it.
5. **Retention periods**, including the 8-year financial-records exception.
6. **How to request deletion or export**, with a real email address that a human reads.
7. **Two separate billing relationships** — you charge for software, Meta charges the customer
   directly for messages. This belongs in the terms too.
8. **Breach notification** commitment.

Get a lawyer to review it once. ₹10,000–25,000 for an Indian SaaS privacy policy and terms review
is money well spent before your first B2B customer asks.

## Consent — whose job is it?

**Your customer's, and you should say so loudly.**

WhatsApp requires businesses to have opt-in before messaging customers. Your customer obtains it;
you provide the tooling. But if your product makes it easy to message people who never opted in,
you share the reputational damage and their quality rating collapses.

Practical steps:

- Terms of service: the customer warrants they have opt-in for every contact they message
- Store `opt_in_status` on `contacts` and surface it in the UI
- Onboarding: explain that marketing to non-opted-in contacts leads to blocks, reports, a RED
  quality rating, and Meta restricting their sending
- **Do not build bulk import or campaign features in the MVP.** Beyond scope discipline, a CSV
  upload of purchased numbers is the fastest route to a destroyed WABA — and it will be your
  product that gets blamed.

That last point is a genuine product-design decision, not just a privacy note.

## Where data lives

| Data | Location | Why it matters |
|---|---|---|
| Database, application | Oracle Cloud Mumbai/Hyderabad | India — good for DPDP and latency |
| Media | Cloudflare R2 | Global edge; bucket private |
| Backups | Backblaze B2 (US) | **Encrypted before upload** — the plaintext never leaves India |
| Errors | Sentry (US/EU) | **Scrubbed** — no PII, no tokens, no message bodies |

Encrypting backups before upload is what makes the US storage defensible. Test that the scrubbing
and encryption actually work — untested controls are assumptions.

## Privacy by design — decisions already made

| Decision | Privacy benefit |
|---|---|
| Ledger stores phone hash + last 4 | The largest, longest-lived table is low-sensitivity |
| No LLM in the automation path (ADR-007) | Customer conversations never leave your infrastructure for a third-party model |
| Razorpay-hosted checkout | Zero card data, zero PCI scope |
| Phone masked to last 4 in the UI | Screenshots leak less |
| Tenant-prefixed media keys | Deletion is a prefix delete |
| No cross-tenant customer-facing analytics | Removes the main leak vector |

**ADR-007 deserves emphasis here.** Free LLM tiers commonly permit training on submitted prompts
— Gemini's free tier does. Sending your customers' customers' conversations to a third-party
model that trains on them would be a serious DPDP problem, and it's a large part of why the
automation path is deterministic.

## Team practices

- Never copy production data to your laptop, including "just to debug this one thing"
- Reproduce bugs with synthetic data
- Never paste real customer data into a Claude Code session, an issue, or a screenshot
- Support access: use the admin role, log it, and don't browse conversations out of curiosity
- Deny-list `.env` and key files in `.claude/settings.json`

## Transparency with your customers

Tell them, in onboarding:

- What you can see (yes, you can technically read their conversations — say so)
- What you do with it (nothing beyond operating the service and supporting them)
- Where it's stored
- How to get it out or delete it

Honesty here is a competitive advantage in the Indian SMB market, where "what happens to my
data" is a real hesitation and most competitors say nothing.

## Not in the MVP

Cookie consent banner (you use one functional session cookie — no tracking cookies, so no banner
needed; keep it that way) · analytics on your own product beyond aggregate counts · a DPO (not
required at your scale) · privacy certifications.

**Don't add Google Analytics or a tracking pixel to the app.** It creates a consent obligation
you currently don't have, for data you won't act on.
