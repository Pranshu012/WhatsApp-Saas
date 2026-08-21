# Security Testing

Read alongside `../11-SECURITY-COMPLIANCE/SECURITY-REQUIREMENTS.md`.

## Secrets never leak — automated

```java
@Test
void whatsappAccountJsonNeverContainsToken() throws Exception {
    var account = whatsappAccountService.connect(tenantId, "REAL_LOOKING_TOKEN_abc123", ...);
    var json = objectMapper.writeValueAsString(accountMapper.toDto(account));
    assertThat(json).doesNotContain("REAL_LOOKING_TOKEN_abc123");
    assertThat(json.toLowerCase()).doesNotContain("access_token");
}
```

```java
@Test
void exceptionMessagesNeverContainTokens() {
    // force a Meta 401 with a known token value; assert the propagated exception,
    // the log output, and the API error response all exclude it
}
```

**Log assertion test.** Capture the appender output during a full send flow and assert the token
string appears nowhere. Tokens leak into logs via exception messages and debug statements more
often than via deliberate logging.

## Authentication

| Test | Expect |
|---|---|
| Wrong password | 401, generic message |
| Unknown email | 401, **identical** response body and status to wrong password |
| Timing | No significant difference between the two above (hash even on unknown email) |
| Session after logout | Rejected |
| Session after password reset | All sessions invalidated |
| Login rate limit | Trips after N attempts per email+IP |
| Password stored | Argon2id hash; plaintext nowhere in the DB |
| Reset token | Stored hashed; single use; expires |

Account enumeration is the easy one to get wrong: returning 404 for unknown email and 401 for
wrong password tells an attacker which emails are registered.

## Authorization

| Test | Expect |
|---|---|
| MEMBER attempts an OWNER-only action | 403 |
| Authenticated user requests another tenant's resource by id | 404 (not 403 — don't confirm existence) |
| Unauthenticated request to any `/api/**` except auth/webhooks | 401 |
| Tampered session cookie | 401 |

**Return 404, not 403, for another tenant's resource id.** A 403 confirms the resource exists,
which is itself an information leak.

## Webhook security

| Test | Expect |
|---|---|
| Valid HMAC | 200 |
| Tampered body, original signature | 403 |
| Valid body, wrong signature | 403 |
| Missing signature header | 403 |
| Signature compared with `==` on strings | **Must not** — assert constant-time comparison is used |
| Verify token comparison | Constant-time |
| Razorpay webhook, invalid signature | 403, and no subscription state change |

The Razorpay one matters commercially: if an unverified webhook can activate a subscription,
anyone can get your product free.

## Input validation

- Oversized request body → 413, not an OOM
- Deeply nested JSON → rejected (Jackson depth limit)
- SQL injection attempts in every text field → parameterised queries; nothing executes
- XSS payloads stored and rendered → escaped by React; verify no `dangerouslySetInnerHTML`
- **Catastrophic regex** in an automation rule → rejected at save with a compile timeout
- Path traversal in media keys → rejected
- Invalid phone formats → rejected before reaching Meta

The regex one is a genuine ReDoS vector: a tenant-supplied pattern like `(a+)+$` evaluated
against inbound messages can pin a CPU core. On a 2 OCPU box that's half your capacity.

## Transport and headers

```bash
curl -I https://api.yourdomain.com/actuator/health
```

- [ ] HSTS present
- [ ] `X-Content-Type-Options: nosniff`
- [ ] `X-Frame-Options: DENY`
- [ ] No `Server` header advertising the stack
- [ ] HTTP redirects to HTTPS
- [ ] TLS 1.2 minimum
- [ ] `/actuator/health` shows no details; `/actuator/env` and `/actuator/heapdump` not exposed

An exposed `/actuator/heapdump` hands over every secret in memory. Check this explicitly.

## Secrets in the repository

Add to CI:

```bash
# fail the build if likely secrets are committed
grep -rIn --exclude-dir=.git -E \
  '(EAA[A-Za-z0-9]{20,}|rzp_live_|-----BEGIN .* PRIVATE KEY)' . && exit 1 || true
```

Also scan history once: `git log -p | grep -iE 'app_secret|access_token|rzp_live'`. If you find
something, rotate it — removing it from history doesn't un-leak it.

## Dependency scanning

`./mvnw org.owasp:dependency-check-maven:check` monthly, plus GitHub Dependabot alerts on.

Don't fail the build on every CVSS 7 — you'll disable it within a week. Review monthly, patch
what's reachable from your code paths.

## What you don't need at MVP

Penetration testing (revisit at ~100 customers or first enterprise customer) · a bug bounty ·
SOC 2 · WAF tuning beyond Cloudflare defaults · SAST beyond your IDE's inspections.

## Before launch — the five that actually matter

1. [ ] App connects to Postgres as a **non-superuser** (`SELECT rolsuper` → false)
2. [ ] Sentry scrubbing verified with a real test error carrying a fake token
3. [ ] No secret in Git history
4. [ ] `/actuator/env` and `/actuator/heapdump` unreachable from the internet
5. [ ] Webhook signature verification tested against a tampered body
