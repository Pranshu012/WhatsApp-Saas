# Authentication and Authorization

**Classification: BUILD NOW (increments F03, F04).**

## Decision: self-implemented, not a vendor

Spring Security + Argon2id + Spring Session JDBC. No Auth0, Clerk, Okta, or Supabase Auth.

**Why:** this is roughly 200 lines of well-trodden Spring configuration. A vendor adds a
per-MAU bill that grows with success, a runtime dependency on someone else's uptime, and a
migration project if you ever leave. We are not rolling our own crypto — Spring Security's
`Argon2PasswordEncoder` and Spring Session are standard, audited components.

**Where a vendor would win:** SSO/SAML for enterprise buyers. Revisit then, not now.

---

## Model

```text
users ──┬── tenant_users (role) ──── tenants
        │
        └── a user MAY belong to multiple tenants (schema supports it;
            the MVP UI assumes one — see D-08)
```

Roles within a tenant: `OWNER`, `MEMBER`.

| Action | OWNER | MEMBER |
|---|---|---|
| Connect/disconnect WhatsApp | ✅ | ❌ |
| Manage billing | ✅ | ❌ |
| Invite users | ✅ | ❌ (deferred, D-08) |
| Create/edit automation rules | ✅ | ✅ |
| Manage FAQ, templates | ✅ | ✅ |
| View inbox, reply | ✅ | ✅ |
| View dashboard | ✅ | ✅ |

## Sessions, not JWT

| | Server sessions (chosen) | JWT |
|---|---|---|
| Revocation | Immediate — delete the row | Hard; needs a blocklist, which is a session store again |
| Storage | Postgres (already have it) | Client |
| Stateless app | ✅ Yes — state is in Postgres, not memory | ✅ |
| Complexity | Low | Refresh tokens, rotation, expiry edge cases |

Revocation is the deciding factor. When a customer says "remove that employee's access", it
must be immediate.

Cookie: `HttpOnly`, `Secure`, `SameSite=Lax`. CSRF protection on, with a token endpoint for
the SPA.

---

## Registration flow

```text
POST /api/auth/register { businessName, email, password, fullName }
 │
 ▼ ONE @Transactional method
 ├── validate: email format, password strength, email not taken, slug free
 ├── INSERT tenants
 ├── INSERT users (password → Argon2id)
 ├── INSERT tenant_users (role = OWNER)
 └── commit  ← all or nothing; a failure must leave no orphan tenant
```

## Login flow

```text
POST /api/auth/login { email, password }
 │
 ├── look up user by lowercased email
 ├── unknown email → hash a dummy password anyway, then fail  ← prevents timing leak
 ├── verify Argon2id hash
 ├── resolve tenant membership
 ├── create session (Spring Session JDBC)
 ├── set TenantContext for subsequent requests via the session principal
 └── 200 + Set-Cookie
```

Failures are always `401 "Invalid email or password"`. Same message, same shape, same
approximate timing, whether the email exists or not.

## Password reset

```text
POST /api/auth/forgot-password { email }
 └── ALWAYS 200, regardless of whether the email exists (no enumeration)
     if it exists: create token, store the HASH, email the raw token, expire in 30 min

POST /api/auth/reset-password { token, newPassword }
 ├── hash the incoming token, look it up
 ├── check not used, not expired
 ├── update password
 ├── mark token used (single use)
 └── invalidate ALL sessions for that user
```

Store the token **hashed**. If your database leaks, raw reset tokens are live account takeovers.

---

## Tenant context binding

After authentication, `TenantContextFilter` sets `TenantContext` from the **session principal**.

**Increment F02 temporarily uses an `X-Tenant-Id` header** because auth doesn't exist yet.
**Increment F03 must delete that path entirely** — if it survives to production, anyone can set
a header and read any tenant's data. Add a test that asserts the header grants nothing.

## Rate limiting

Login attempts per `(email, ip)`: e.g. 5 failures in 15 minutes → temporary lock with a clear
message. A Postgres counter table is entirely sufficient. No Redis (nothing is multi-instance).

## DO NOT BUILD YET

2FA/TOTP · SSO/SAML/OIDC · social login · magic links · remember-me · API keys for customers ·
impersonation for support · granular permissions beyond OWNER/MEMBER.
