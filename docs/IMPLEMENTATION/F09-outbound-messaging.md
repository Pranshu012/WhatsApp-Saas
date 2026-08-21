# F09 — Outbound Message Dispatcher

## Status
Complete — verified with queue-driven asynchronous sending, template and text formatting, ledger-first intent recording, transient error exponential retry, permanent dead-lettering, token revocation disconnect handling, and idempotency deduplication across 56 automated tests.

## Summary
Implemented asynchronous, idempotent outbound WhatsApp messaging:
- `WhatsAppCloudClient`: Meta Cloud API client executing `POST /{phoneNumberId}/messages` with decrypted Bearer tokens, configured timeouts, and strict error classification.
  - Permanent errors (`PermanentJobException`): codes `200` (Missing permissions), `190` (Token revoked), `131026` (Recipient not on WhatsApp), `131047` (Outside service window), `132000`/`132001` (Template parameter mismatch).
  - Transient errors (`DomainException`): codes `130429` (Rate limits), `131048` (Spam rate limit), `1`, `2` (Meta internal), HTTP 5xx.
- `AccountRateLimiter`: In-process token bucket rate limiter per phone number to respect Meta message limits.
- `SendMessageJobHandler`: Background job worker (`SEND_WHATSAPP_MESSAGE`) executing the core flow:
  1. Loads tenant's `WhatsAppAccount`.
  2. Records outbound intent on `LedgerService` **before** calling Meta.
  3. Throttles via `AccountRateLimiter`.
  4. Calls `WhatsAppCloudClient`.
  5. Attaches `wamid` on success or records failure with error code.
  6. On Error 190, marks account `DISCONNECTED`.
- `MessagingService`: The **only** public send entry point. Enqueues background jobs with deterministic idempotency keys and prohibits synchronous HTTP execution.

## Key Files
- `WhatsAppCloudClient.java`: Cloud API client.
- `SendMessageJobHandler.java`: Job processor.
- `AccountRateLimiter.java`: In-memory throttle.
- `MessagingService.java`: Public send API.
- `MessagingServiceTest.java`: 6 integration tests with MockRestServiceServer.
