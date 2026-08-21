# API Design

**Classification: BUILD NOW.** REST, JSON, session-cookie authenticated.

## Conventions

- Base path `/api`
- Plural nouns: `/api/contacts`, `/api/automation-rules`
- Tenant is **never** in the URL or a parameter — it comes from the session
- `snake_case` in JSON? No — `camelCase`, matching the JS client
- All timestamps ISO-8601 UTC (`2026-08-18T12:34:56Z`); the frontend renders IST
- Money as integer minor units plus an explicit currency, never a float

## Endpoints (MVP)

### Auth — public
```text
POST   /api/auth/register          { businessName, email, password, fullName }
POST   /api/auth/login             { email, password }           → Set-Cookie
POST   /api/auth/logout
GET    /api/auth/me                                              → user + tenant + role
POST   /api/auth/forgot-password   { email }                     → always 200
POST   /api/auth/reset-password    { token, newPassword }
GET    /api/auth/csrf                                            → CSRF token for the SPA
```

### WhatsApp
```text
POST   /api/whatsapp/connect       { code, wabaId, phoneNumberId }   OWNER only
GET    /api/whatsapp/account                                          → status + quality + payment warning
DELETE /api/whatsapp/account                                          OWNER only
POST   /api/whatsapp/account/refresh                                  → re-fetch details from Meta
```

### Templates
```text
GET    /api/templates
POST   /api/templates              submit for Meta approval
POST   /api/templates/sync         trigger a sync job
GET    /api/templates/{id}
```

### Automation
```text
GET    /api/automation-rules
POST   /api/automation-rules
PUT    /api/automation-rules/{id}
DELETE /api/automation-rules/{id}
POST   /api/automation-rules/reorder    { orderedIds: [...] }
POST   /api/automation-rules/test       { messageText }  → which rule would match
```

### FAQ
```text
GET    /api/faqs
POST   /api/faqs
PUT    /api/faqs/{id}
DELETE /api/faqs/{id}
POST   /api/faqs/test              { question } → best match + confidence
```

`/test` endpoints are small but disproportionately valuable — they're how a non-technical user
learns what the matcher actually does.

### Inbox
```text
GET    /api/conversations?cursor=&limit=
GET    /api/conversations/{id}/messages?cursor=&limit=
POST   /api/conversations/{id}/reply    { text }   → 409 if the service window has closed
```

### Scheduling
```text
GET    /api/scheduled-messages
POST   /api/scheduled-messages
DELETE /api/scheduled-messages/{id}    → 409 if already ENQUEUED
```

### Analytics
```text
GET    /api/analytics/message-counts?month=2026-08   → per-category counts
GET    /api/analytics/delivery-summary?month=2026-08
```

### Billing
```text
GET    /api/billing/subscription
POST   /api/billing/checkout       → Razorpay order/subscription
GET    /api/billing/invoices
```

### Webhooks — public, signature-verified
```text
GET    /api/webhooks/whatsapp      Meta verification handshake
POST   /api/webhooks/whatsapp      Meta events
POST   /api/webhooks/razorpay      payment events
```

### Health
```text
GET    /actuator/health            includes DB connectivity
```

## Pagination — cursor, not offset

```json
{
  "items": [ ... ],
  "nextCursor": "eyJpZCI6...",
  "hasMore": true
}
```

Offset pagination drifts when rows are inserted while the user pages — and in an inbox, rows are
constantly inserted. Cursor pagination on `(created_at, id)` avoids duplicates and skips.

## Error shape

```json
{
  "code": "SERVICE_WINDOW_CLOSED",
  "message": "This conversation's 24-hour window has closed. Send an approved template instead.",
  "requestId": "a3f2...",
  "timestamp": "2026-08-18T12:34:56Z",
  "fieldErrors": {}
}
```

| Status | When |
|---|---|
| 400 | Validation failure (`fieldErrors` populated) |
| 401 | Not authenticated |
| 403 | Authenticated but not permitted; invalid webhook signature |
| 404 | Not found **or** belongs to another tenant — never distinguish these |
| 409 | Conflict: duplicate email, window closed, already enqueued |
| 422 | Semantically invalid (e.g. template not approved) |
| 429 | Rate limited |
| 500 | Unexpected — generic message, details only in logs and Sentry |

**404 vs 403 for another tenant's resource:** always 404. A 403 confirms the resource exists,
which leaks information across tenants.

Messages must be **customer-readable**. "The recipient's number is not on WhatsApp" beats
"Meta error 131026" — though log the code.

## Security

- Everything authenticated except auth endpoints, webhooks, health
- CSRF enabled (cookie auth), token from `/api/auth/csrf`
- Bean validation on every request record
- Reject unknown JSON properties (`FAIL_ON_UNKNOWN_PROPERTIES`) — catches client/server drift
- Body size limits in Caddy
- Per-tenant rate limits on `/test` endpoints and sends

## DO NOT BUILD YET

GraphQL · public API for customers · API keys · webhooks *to* customers · bulk endpoints ·
OpenAPI-generated client SDKs · API versioning (add `/v2` when you actually break something).
