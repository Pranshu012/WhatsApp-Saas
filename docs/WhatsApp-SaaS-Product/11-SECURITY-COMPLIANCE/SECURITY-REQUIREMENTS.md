# Security Requirements

## Threat model — what actually matters here

| Threat | Impact | Priority |
|---|---|---|
| **Cross-tenant data leak** | Business-ending in a 20-customer B2B market where customers know each other | **Critical** |
| **WhatsApp token theft** | Attacker sends messages as your customer, spending their money, destroying their quality rating | **Critical** |
| Webhook forgery | Fake inbound messages trigger automated sends → spends customer money | High |
| Razorpay webhook forgery | Free subscriptions | High |
| Account takeover | Access to conversation history | High |
| DPDP breach obligation | Regulatory + reputational | High |
| DDoS | Availability only | Low (Cloudflare absorbs) |

The top two are the ones to design around. Everything below serves them.

## Non-negotiable controls

### 1. Two layers of tenant isolation

Application-level scoping **and** PostgreSQL Row-Level Security. Layer 2 exists because layer 1
depends on a human remembering a `WHERE` clause at 1am.

**The app must connect as a non-superuser role.** Superusers bypass RLS, which silently reduces
you to one layer. Verify: `SELECT rolsuper FROM pg_roles WHERE rolname='wasaas_app'` → `false`.

Every tenant-scoped table: `ENABLE` **and** `FORCE ROW LEVEL SECURITY`. `ENABLE` alone doesn't
apply policies to the table owner.

Set `app.tenant_id` **per transaction**, never per connection — pooled connections are reused
across tenants.

Tests: `../09-TESTING/MULTI-TENANCY-TESTING.md`.

### 2. Token protection

WhatsApp access tokens are the crown jewels — they let you send messages as the customer.

- AES-256-GCM at rest, key from env var (base64, exactly 32 bytes)
- **Application startup fails if the key is missing or wrong length.** Never run insecurely.
- Random nonce per encryption, stored with the ciphertext
- No getter that Jackson can serialise; decryption only via an explicit service method
- Never in logs, exception messages, API responses, or Sentry events
- Test that a token string appears nowhere in captured log output during a full send flow

### 3. Webhook signature verification

Both Meta and Razorpay.

- HMAC-SHA256 over the **raw request bytes**, before any JSON parsing
- **Constant-time comparison** (`MessageDigest.isEqual`), never `String.equals`
- Invalid → 403, log a warning, do not process
- Never disable verification to "unblock" something

An unverified Razorpay webhook means anyone can activate a subscription for free. An unverified
Meta webhook means anyone can trigger sends that spend your customer's money.

### 4. Authentication

- Argon2id password hashing (Spring Security's `Argon2PasswordEncoder`)
- Server-side sessions in Postgres (Spring Session JDBC) — stateless app, sessions survive
  restarts
- Cookies: `HttpOnly`, `Secure`, `SameSite=Lax`
- CSRF protection enabled for cookie auth
- Generic failure messages; hash even on unknown email to avoid timing enumeration
- Rate limit login per email+IP
- Password reset tokens stored **hashed**, single-use, 30-minute expiry, and invalidate all
  sessions on use

### 5. Authorization

- Roles within a tenant: OWNER, MEMBER
- Another tenant's resource id → **404, not 403** (403 confirms existence)
- Method-level checks on owner-only actions

### 6. Transport and headers

TLS 1.2+, HSTS, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, no `Server` header.
Cloudflare SSL mode **Full (strict)** — never Flexible.

### 7. Actuator exposure

Expose `health`, `info`, `metrics`, `prometheus`. **Never** `env`, `heapdump`, `threaddump`,
`configprops`, or `beans` publicly. An exposed heapdump hands over every secret in memory.

`show-details: never` on health.

### 8. Server hardening

- SSH: key-only, root login disabled, port 22 restricted to your IP in the Oracle Security List
- UFW: 22/80/443 only. **Port 5432 closed at both firewalls**; Postgres binds to localhost.
- `unattended-upgrades` for security patches
- `fail2ban` on SSH
- Non-root `deploy` user; the app never runs as root

### 9. Input validation

- Bean Validation on every request DTO
- Parameterised queries only (JPA/JDBC templates — never string concatenation)
- Request body size cap at Caddy (10 MB)
- **Tenant-supplied regex is untrusted input**: compile with a timeout, reject catastrophic
  patterns at save time. A pattern like `(a+)+$` evaluated against inbound messages pins a CPU
  core — half your capacity on a 2 OCPU box.
- Phone numbers normalised to E.164 and validated before reaching Meta

### 10. Logging discipline

**Never log:** access tokens · passwords or hashes · session IDs · OTPs · the encryption key ·
full message bodies · full phone numbers (mask to last 4).

Structured JSON with a request ID on every line. Sentry `send-default-pii: false` plus an
explicit scrubbing callback — and **test the scrubbing** with a deliberate error carrying a fake
token.

## Data minimisation

Deliberate choices that reduce breach impact:

| Data | Decision |
|---|---|
| End-customer phone in `message_ledger` | **Hash + last 4 only.** You don't need the full number for billing reconciliation. |
| End-customer phone in `contacts` | Full number — required to reply |
| Message bodies | Stored (needed for the inbox), never logged |
| Card details | Never touch them. Razorpay-hosted checkout only. |
| Meta app secret | Server-side only. Never in the SPA bundle. |

Everything in a Vite bundle is public. `VITE_META_APP_ID` and `VITE_META_CONFIG_ID` are public
by design; the app **secret** never goes near it.

## What you don't need at MVP

Penetration testing (revisit ~100 customers or first enterprise deal) · bug bounty · SOC 2 ·
ISO 27001 · a WAF beyond Cloudflare defaults · HSM/KMS key management · SIEM.

## Pre-launch — the five that matter most

1. [ ] App connects as non-superuser (`rolsuper = false`)
2. [ ] Token encryption key enforced at startup; token appears in no log
3. [ ] Webhook signature verification tested against a **tampered body**
4. [ ] `/actuator/env` and `/actuator/heapdump` unreachable from the internet
5. [ ] No secret in Git history (`git log -p | grep -iE 'app_secret|access_token|rzp_live'`)

Full list: `../09-TESTING/PRE-PRODUCTION-CHECKLIST.md`.
