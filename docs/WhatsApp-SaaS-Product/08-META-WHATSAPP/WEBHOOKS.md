# Meta Webhooks — Payload Reference

**Classification: BUILD NOW (F10).** Implementation in `05-BACKEND/WEBHOOK-IMPLEMENTATION.md`.

## Two separate things are required

1. **Configure the webhook URL on your App** (App Dashboard → WhatsApp → Configuration)
2. **Subscribe your app to each customer's WABA** — `POST /{waba-id}/subscribed_apps`, during
   onboarding

Doing only (1) is the most common integration bug in this model: everything looks configured and
no messages ever arrive.

## Verification handshake (GET)

```http
GET /api/webhooks/whatsapp?hub.mode=subscribe
                          &hub.verify_token=YOUR_TOKEN
                          &hub.challenge=1158201444
```
Return `1158201444` as **plain text**, 200, if the token matches (constant-time compare).
Otherwise 403.

Your app must already be **deployed and responding** when you configure the webhook, or this
fails.

## Signature (POST)

```
X-Hub-Signature-256: sha256=<HMAC-SHA256 of the RAW body, keyed with your App Secret>
```
Verify over the **exact raw bytes**, before parsing. See the implementation doc — computing the
HMAC over reserialised JSON is the classic time-waster.

## Inbound text message

```json
{
  "object": "whatsapp_business_account",
  "entry": [{
    "id": "WABA_ID",
    "changes": [{
      "field": "messages",
      "value": {
        "messaging_product": "whatsapp",
        "metadata": { "display_phone_number": "919876500000", "phone_number_id": "PHONE_NUMBER_ID" },
        "contacts": [{ "profile": { "name": "Rahul" }, "wa_id": "919876543210" }],
        "messages": [{
          "from": "919876543210",
          "id": "wamid.HBgL...",
          "timestamp": "1755500000",
          "type": "text",
          "text": { "body": "what are your timings?" }
        }]
      }
    }]
  }]
}
```

**Where to read what:**

| Need | Path |
|---|---|
| Tenant resolution | `entry[].changes[].value.metadata.phone_number_id` → `whatsapp_accounts` |
| Sender | `...value.messages[].from` (E.164, no `+`) |
| Profile name | `...value.contacts[].profile.name` |
| Message id (dedupe) | `...value.messages[].id` |
| Timestamp | `...value.messages[].timestamp` (**Unix seconds, string**) |
| Text | `...value.messages[].text.body` |

`entry` and `changes` are **arrays** — Meta may batch. Iterate; don't index `[0]`.

## Button reply

```json
"messages": [{
  "from": "919876543210",
  "id": "wamid...",
  "type": "interactive",
  "interactive": { "type": "button_reply", "button_reply": { "id": "hours", "title": "Timings" } }
}]
```
The `id` is what you set when sending (F15) — map it back to the offering rule.

## Status update

```json
"value": {
  "metadata": { "phone_number_id": "..." },
  "statuses": [{
    "id": "wamid.HBgL...",
    "status": "delivered",
    "timestamp": "1755500050",
    "recipient_id": "919876543210",
    "conversation": { "id": "...", "origin": { "type": "service" } },
    "pricing": { "billable": true, "pricing_model": "PMP", "category": "service" }
  }]
}
```

**The `pricing` object is gold.** It tells you what Meta actually billed — `billable` and
`category` — which is the authoritative answer for your ledger, better than your own inference.
Store it. After 1 October 2026 this becomes the cleanest way to reconcile a customer's bill.

Status sequence: `sent` → `delivered` → `read`, or `failed`. They can arrive out of order, and
`read` may never arrive. Append each as an event; never assume ordering.

## Failed status

```json
"statuses": [{
  "id": "wamid...",
  "status": "failed",
  "errors": [{ "code": 131026, "title": "Message undeliverable" }]
}]
```
Translate the code into plain language for the inbox.

## Template status update

```json
{ "field": "message_template_status_update",
  "value": { "message_template_id": "...", "message_template_name": "appointment_reminder",
             "event": "APPROVED", "reason": null } }
```
Subscribe to this field so approvals and rejections update without waiting for a daily sync.

## Fields to subscribe to (MVP)

| Field | Why |
|---|---|
| `messages` | Inbound messages **and** status updates |
| `message_template_status_update` | Approval/rejection notifications |

**Not yet:** `account_update`, `phone_number_quality_update`, `business_capability_update`. Add
`phone_number_quality_update` when you want proactive quality-rating alerts — useful, not
essential.

## Handling rules

1. **Iterate all arrays.** `entry[]`, `changes[]`, `messages[]`, `statuses[]` can all have
   multiple items.
2. **Unknown types → `IGNORED` with a reason.** Never throw; Meta adds types over time.
3. **Dedupe by `messages[].id` / `statuses[].id`.** Redelivery is expected behaviour.
4. **Timestamps are Unix seconds as strings.** Parse, don't assume.
5. **Never log the payload at INFO.** It contains end-customer PII.
6. **Return 200 even for duplicates and ignored events.** Anything else makes Meta retry.

## Local testing

Expose `localhost:8080` with a tunnel:
```bash
cloudflared tunnel --url http://localhost:8080
# or: ngrok http 8080
```
Point the App's webhook at the tunnel URL. Note the URL changes on restart with free tunnels —
reconfigure each time.

Once you have real payloads in `webhook_events`, replay them into tests as fixtures. Real
payloads are far more valuable than ones you invent, which is a large part of why we store the
raw JSON.
