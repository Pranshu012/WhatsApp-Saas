# Security

**Classification: BUILD NOW.** Detail in `11-SECURITY-COMPLIANCE/`.

## Threat model, briefly

You hold: other businesses' WhatsApp access tokens, and conversations between those businesses
and their end customers. The two events that end the product are **a cross-tenant data leak**
and **a leaked access token** (which lets someone message that business's entire customer list
in their name).

Everything below is ordered by how much damage it prevents.

---

## 1. Tenant isolation
See `MULTI-TENANCY.md`. Two layers: application `TenantContext` + PostgreSQL RLS. The app
connects as a **non-superuser** role. Four mandatory tests, including one that proves RLS is
doing work.

## 2. WhatsApp access token protection

- **AES-256-GCM at rest.** Key from an environment variable (base64, 32 bytes), never in code
  or Git. Store the nonce with the ciphertext.
- **Fail startup** if the key is missing or the wrong length. Running insecurely is worse than
  not running.
- The entity must have **no getter Jackson can serialise**. Decryption goes through an explicit
  service method.
- **Never logged, never in an API response, never in an exception message.** Exception messages
  end up in Sentry.
- Sentry must be configured to scrub token-shaped strings before sending.

## 3. Password handling

- **Argon2id** via Spring Security's `Argon2PasswordEncoder`. Not BCrypt (acceptable but
  weaker), never SHA/MD5.
- Login failures are **generic**: "invalid credentials". Never reveal whether an email exists.
- Watch response timing too — an early return on unknown email leaks existence via latency.
  Hash a dummy password on the unknown-email path.
- Rate-limit login by email + IP. A Postgres counter is fine; no Redis.

## 4. Sessions

- Spring Session JDBC — server-side, in Postgres. Keeps the app stateless.
- Cookie: `HttpOnly`, `Secure`, `SameSite=Lax`.
- CSRF protection enabled (we use cookie auth, so it applies), with a token endpoint the SPA reads.
- Invalidate **all** sessions for a user on password reset.
- Never log session IDs.

## 5. Webhook signature verification
`X-Hub-Signature-256`, HMAC-SHA256 over raw bytes, constant-time comparison, **before parsing**.
See `WEBHOOK-ARCHITECTURE.md`. Same discipline for the Razorpay webhook.

## 6. API security

- Every endpoint authenticated except: login, register, forgot/reset password, webhooks, health.
- Authorization checked per tenant **and** per role. Never trust a `tenantId` from the client.
- Bean validation on every request DTO. Reject unknown JSON fields.
- Request body size limits in Caddy.
- Per-tenant rate limits on expensive endpoints.

## 7. SQL injection
JPA/prepared statements only. If you write native SQL, parameters are bound — never string
concatenation. One specific risk here: **tenant-supplied regex** in automation rules. Compile
with a timeout and reject catastrophic patterns at save time; a tenant-supplied regex is
untrusted input.

## 8. XSS
The React SPA escapes by default. Never use `dangerouslySetInnerHTML` with customer content —
including message bodies, which contain end-customer text you do not control. Content Security
Policy header via Caddy.

## 9. Secrets
Environment variables from a root-owned `0600` file, loaded by systemd `EnvironmentFile`.
`.env` is gitignored **and** deny-listed in `.claude/settings.json` so it never enters a
transcript. `.env.example` holds placeholders only. See `11-SECURITY-COMPLIANCE/SECRETS-MANAGEMENT.md`.

## 10. Transport
TLS everywhere via Caddy + Let's Encrypt. HSTS. Postgres listens on localhost only. UFW allows
22, 80, 443 only. SSH keys only, no password auth. fail2ban.

---

## NEVER LOG — enforce this

| Never | Instead |
|---|---|
| WhatsApp access tokens | The account id |
| Passwords or hashes | Nothing |
| Session IDs | The user id |
| The token encryption key | Nothing, ever |
| OTPs / reset tokens | The token id |
| Full end-customer phone numbers | Last 4 digits |
| Message bodies of end-customer conversations | The `wamid` and length |
| Razorpay signatures or API secrets | The event id |
| Full webhook payloads at INFO | Event id, type, tenant id |

Configure Sentry's `beforeSend` to scrub these. Then **test it**: trigger an error containing a
fake token and confirm it's stripped before it reaches Sentry.

---

## Audit logging — BUILD NOW (minimal)

Log to a table, not just to stdout, for actions with real consequences:

- Login success/failure, password reset
- WhatsApp account connected or disconnected
- Automation rule created/modified/deleted
- Any bulk or scheduled send
- Subscription state changes

Fields: `tenant_id`, `user_id`, `action`, `resource_type`, `resource_id`, `ip`, `created_at`,
`metadata` jsonb. Append-only. This is what lets you answer "who turned that automation on?"

## DO NOT BUILD YET

2FA · SSO/SAML · penetration testing · a bug bounty · WAF rules beyond Cloudflare's free tier ·
field-level encryption beyond tokens · HSM/KMS key management · SOC 2 controls.

Revisit when an enterprise deal requires it, or at ~₹5,00,000/month revenue.
