# F06 — Embedded Signup Callback & Meta Graph API Client

## Status
Complete — verified with code exchange, asset verification, webhook subscription, Error Code 200 Advanced Access handling, and idempotent upserts across 38 automated tests.

## Summary
Implemented the Meta Embedded Signup onboarding pipeline:
- `MetaProperties` & `MetaGraphClient`: RestClient implementation interacting with Meta's Graph API (`/oauth/access_token`, `/{waba_id}`, `/{phone_number_id}`, and `/{waba_id}/subscribed_apps`).
- `WhatsAppConnectService`:
  1. Exchanges the frontend authorization code for a WABA-scoped business access token.
  2. Verifies ownership of `waba_id` and `phone_number_id` against Meta Graph API using the token.
  3. Subscribes our app to that customer's WABA webhooks (`POST /{waba_id}/subscribed_apps`).
  4. Encrypts the token via AES-256-GCM and persists the account under tenant context.
- Meta Error Code 200 Handling: Maps missing Advanced Access permissions directly to a diagnostic message explaining the need for Meta App Review approval.
- Token Confidentiality: Access tokens and raw auth codes are never logged, never returned in DTOs, and never exposed in JSON responses.

## Key Files
- `MetaProperties.java`: Graph API configuration.
- `MetaGraphClient.java`: HTTP client for Graph API calls with typed error handling.
- `ConnectWhatsAppRequest.java` & `WhatsAppAccountResponse.java`: Safe DTOs.
- `WhatsAppConnectService.java`: 4-step onboarding orchestrator.
- `WhatsAppController.java`: `POST /api/whatsapp/connect` and accounts endpoints.
- `WhatsAppConnectTest.java`: 4 comprehensive integration tests with MockRestServiceServer.
