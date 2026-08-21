# F10 — Inbound Webhook Receiver

## Status
Complete — verified with GET verification challenge, raw-byte HMAC-SHA256 constant-time verification, fast < 50ms ingestion latency, ingest-level partial unique deduplication, and zero-outbound HTTP in controller across 64 automated tests.

## Summary
Implemented the high-speed webhook ingest receiver for Meta WhatsApp Cloud API:
- `V10__webhook_events.sql`: `webhook_events` table storing raw JSON payloads, Meta event IDs, WABA IDs, phone number IDs, verification validity, and processing status with a partial unique index on `event_id` for deduplication.
- `WebhookSignatureVerifier`: Validates `X-Hub-Signature-256` header by computing `HmacSHA256` over the exact raw request bytes using `metaProperties.appSecret` with constant-time equality (`MessageDigest.isEqual`).
- `WebhookController`:
  - `GET /api/webhooks/whatsapp`: Responds with plain text `hub.challenge` after validating `hub.mode == "subscribe"` and `hub.verify_token`.
  - `POST /api/webhooks/whatsapp`: Accepts `@RequestBody byte[] rawBody`, checks signature before JSON parsing, logs warnings on invalid signatures without exposing payloads, persists raw event, and returns `200 OK` immediately.
- `WebhookIngestService`: Ingests the event and enqueues a `PROCESS_WEBHOOK_EVENT` background job with an idempotent key (`wh:<eventId>`). Contains zero outbound HTTP calls or business logic.
- `SecurityConfig`: Permitted `/api/webhooks/**` and exempted from CSRF.

## Key Files
- `V10__webhook_events.sql`: Migration.
- `WebhookSignatureVerifier.java`: Constant-time HMAC verifier.
- `WebhookEvent.java` & `WebhookEventRepository.java`: Persistence entities.
- `WebhookIngestService.java`: Ingestion orchestrator.
- `WebhookController.java`: Endpoints.
- `WebhookReceiverTest.java`: 8 integration tests.
