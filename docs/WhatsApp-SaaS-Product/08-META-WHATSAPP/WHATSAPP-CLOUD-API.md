# WhatsApp Cloud API — Reference

**Classification: BUILD NOW (F06, F09).** Meta-hosted; no servers of ours involved.

Base URL: `https://graph.facebook.com/{version}` — version pinned in
`app.meta.graph-version`, never hardcoded at call sites.

## Authentication
`Authorization: Bearer {customer's business token}` — the decrypted, per-tenant token from
`whatsapp_accounts`.

## Sending a text (inside the 24h service window only)

```http
POST /{phone-number-id}/messages
{
  "messaging_product": "whatsapp",
  "recipient_type": "individual",
  "to": "919876543210",
  "type": "text",
  "text": { "preview_url": false, "body": "Your appointment is confirmed." }
}
```

Response: `{ "messages": [ { "id": "wamid.HBg..." } ] }` — store that `wamid` in the ledger.

`to` is E.164 **without** the leading `+`.

## Sending a template (required outside the window)

```http
POST /{phone-number-id}/messages
{
  "messaging_product": "whatsapp",
  "to": "919876543210",
  "type": "template",
  "template": {
    "name": "appointment_reminder",
    "language": { "code": "en" },
    "components": [{
      "type": "body",
      "parameters": [
        { "type": "text", "text": "Rahul" },
        { "type": "text", "text": "18 Aug, 4:00 PM" }
      ]
    }]
  }
}
```

Parameters are **positional**, matching `{{1}}`, `{{2}}` in the approved body. A count mismatch
returns error 132000 — validate locally first (`variable_count` on the template row).

## Interactive messages

**Reply buttons** — max 3, 20 characters each:
```json
{
  "type": "interactive",
  "interactive": {
    "type": "button",
    "body": { "text": "What would you like to do?" },
    "action": { "buttons": [
      { "type": "reply", "reply": { "id": "book",   "title": "Book" } },
      { "type": "reply", "reply": { "id": "hours",  "title": "Timings" } },
      { "type": "reply", "reply": { "id": "human",  "title": "Talk to us" } }
    ]}
  }
}
```

**List** — up to 10 rows across sections. Use when you need more than 3 options.

Button/list replies arrive as inbound webhooks carrying the `id` you set — map it back to the rule
or FAQ that offered it (F15).

## Management endpoints

| Purpose | Call |
|---|---|
| Exchange code for token | `GET /oauth/access_token?...&code={code}` |
| WABA details | `GET /{waba-id}?fields=name,timezone_id,message_template_namespace` |
| Phone numbers | `GET /{waba-id}/phone_numbers?fields=display_phone_number,verified_name,quality_rating` |
| **Subscribe app to WABA** | `POST /{waba-id}/subscribed_apps` ← forget this and no webhooks arrive |
| List templates | `GET /{waba-id}/message_templates` |
| Create template | `POST /{waba-id}/message_templates` |
| Mark message read | `POST /{phone-number-id}/messages` with `status: read` |

## Media

Two steps: upload to get an ID, then send by ID.
```http
POST /{phone-number-id}/media        (multipart) → { "id": "..." }
POST /{phone-number-id}/messages     { "type": "image", "image": { "id": "..." } }
```
Inbound media: the webhook gives a media ID; `GET /{media-id}` returns a **short-lived** URL. If
you need to keep the file, download it promptly and store it in Cloudflare R2.

## Error codes we handle

| Code | Meaning | Retry? |
|---|---|---|
| 200 | App lacks Advanced Access | ❌ Alert **us** — configuration, not the customer |
| 190 | Token invalid/expired | ❌ Mark `TOKEN_EXPIRED`, notify customer |
| 131026 | Recipient not on WhatsApp | ❌ |
| 131047 | Outside the 24h window (template required) | ❌ |
| 131048 | Spam rate limit | ✅ Back off hard; check quality rating |
| 130429 | Rate limit | ✅ Back off |
| 132000 | Template parameter count mismatch | ❌ |
| 132001 | Template does not exist | ❌ |
| 133010 | Phone number not registered | ❌ |
| 1, 2 | Meta internal | ✅ |
| HTTP 5xx / timeout | — | ✅ |

Full classification in `05-BACKEND/ERROR-HANDLING-AND-RETRY.md`.

## Rate limits and messaging tiers

New WABAs start on a low messaging tier (e.g. 1,000 unique recipients per rolling 24 hours) and
rise automatically with sustained good-quality usage. Surface the tier and quality rating in the
UI — customers care, and a red rating means Meta will throttle them.

## Webhook payload shapes

See `WEBHOOKS.md`.

## Testing

Use WireMock or MockWebServer against Meta's documented shapes. **Never hit the real API in
tests** — you'd spend money, pollute your quality rating, and produce flaky tests.

For manual end-to-end testing, the test number from the WhatsApp product works with standard
access and can message a small allowlist of verified recipients.
