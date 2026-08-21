# Customer Data

## Two levels of "customer"

Get this distinction right — it drives every obligation.

| Term | Who | Your role |
|---|---|---|
| **Customer** | The SMB paying you ₹1,999/month | You are their service provider |
| **End customer** | The SMB's own customer, messaging on WhatsApp | You are a **processor** on your customer's behalf |

Under DPDP, your customer is the Data Fiduciary for their end customers' data. You are the Data
Processor. That means: you act on their instructions, you don't repurpose their data, and you
must be able to delete it on request. See `DPDP-CONSIDERATIONS.md`.

## What you store

### About your customer (the SMB)

| Data | Why | Retention |
|---|---|---|
| Business name, slug | Identity | Life of account |
| Email, name, Argon2id hash | Login | Life of account |
| GSTIN, legal name, address | GST invoicing (B2B input credit) | 8 years (Indian tax law) |
| Razorpay subscription id, payment events | Billing | 8 years |
| WABA id, phone number id, encrypted token | Operating their WhatsApp | Life of account |

### About end customers (their customers)

| Data | Why | Sensitivity |
|---|---|---|
| Phone number (E.164) in `contacts` | Required to reply | **High** |
| Display name | Inbox usability | Medium |
| Message content in conversations | Inbox, automation | **High** |
| Media (via R2) | Inbox | **High** |
| Phone **hash + last 4** in `message_ledger` | Billing reconciliation | Low |
| Timestamps, delivery status | Diagnostics | Low |

## Data minimisation — the deliberate choices

These aren't defaults; they're decisions that shrink your breach impact.

**1. The ledger stores hash + last 4, not the full number.** Billing reconciliation never needs
the full number. `message_ledger` is your largest, longest-lived table — keeping it low-sensitivity
means a leak there is far less damaging than a leak of `contacts`.

**2. Card details never touch your systems.** Razorpay-hosted checkout only. You store a
subscription id, nothing more. This removes PCI scope entirely.

**3. Message bodies are stored but never logged.** Logs go to `journalctl` and Sentry — two
places with weaker access control than your database. Log a ledger id and a length instead.

**4. Media stored under a tenant-scoped prefix**: `{tenantId}/{yyyy}/{mm}/{mediaId}`. Per-tenant
deletion becomes a prefix delete rather than a full-bucket scan.

**5. Phone numbers masked to last 4 in the UI by default,** with a reveal action. Screenshots
get shared.

## What you never store

- Card numbers, CVV, UPI PIN
- Your customer's Facebook password
- End-customer data beyond what arrives via WhatsApp
- Anything scraped, purchased, or enriched from a third party

## Access control

- Two layers of tenant isolation (application + PostgreSQL RLS), verified by tests
- Application connects as a **non-superuser** role — superusers bypass RLS
- R2 bucket is **private**; media served via authenticated endpoints or presigned URLs
- No production database access from your laptop except through SSH to the box
- No copying production data to your local machine, ever, including "just to debug"

That last one is a real temptation and a real risk. Reproduce with synthetic data.

## Retention and deletion

| Event | Behaviour |
|---|---|
| Customer cancels | Data retained 90 days (recovery window), then deleted |
| Customer requests deletion | Within 30 days, all end-customer data purged |
| End customer requests deletion | Your customer instructs you; you purge that contact and their messages |
| Financial records | Retained 8 years regardless (Indian tax law overrides deletion requests) |
| `message_ledger` | Append-only; on deletion, purge to hash-only rows rather than deleting — billing evidence must survive |

**Build the deletion path before launch**, not after the first request. It needs to cover:
`contacts`, `conversations`, message bodies, R2 media under the tenant prefix, and
`webhook_events` raw payloads (which contain message content — easy to forget).

`webhook_events` is the one people miss. It stores raw Meta payloads including full message
bodies and phone numbers. Purge it on a rolling window (e.g. 90 days) as a matter of course.

## Data export

Your customer owns their data. Provide export on request (CSV of contacts, conversations,
message history). Don't build a self-serve export in the MVP — do it manually until someone
asks twice.

**Never make cancellation punitive.** In the Tech Provider model the customer owns their WABA and
can walk away regardless; a hostile export policy just costs you the referral.

## Cross-tenant queries

**Never write a customer-facing feature that aggregates across tenants.** No "compare your
response rate to similar businesses". The feature value is low, and cross-tenant queries are
exactly where isolation leaks originate.

Your own operational queries (support, monitoring) may cross tenants — run them as a separate
admin role, log them, and never expose them through the tenant-facing API.

## Sub-processors — disclose these

Your privacy policy must name them:

| Sub-processor | Data | Location |
|---|---|---|
| Oracle Cloud | Everything (hosting) | India (Mumbai/Hyderabad) |
| Meta | Messages (they're the channel) | Global |
| Cloudflare | Traffic, media (R2) | Global |
| Backblaze B2 | Encrypted backups | US |
| Razorpay | Payment data | India |
| Sentry | Error data (scrubbed) | US/EU |
| Brevo/Resend | Customer emails only | EU/US |
| Better Stack | Uptime data (no customer data) | EU |

Backblaze and Sentry being outside India matters for DPDP transfer disclosure. Backups are
encrypted before upload, and Sentry data is scrubbed — say so in the policy.
