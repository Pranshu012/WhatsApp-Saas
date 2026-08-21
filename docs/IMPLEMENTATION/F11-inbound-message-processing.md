# F11 — Inbound Message Processing & 24-Hour Service Window Tracking

## Status
Complete — verified with inbound message entity creation (Contacts, Conversations, Ledger entries), 24-hour service window expiry calculation across timezone boundaries, repeat message idempotent updates, status callback ledger transitions, graceful unsupported event handling, and multi-tenant isolation across 70 automated tests.

## Summary
Implemented the domain processing layer that converts raw webhook events into business entities:
- `V11__contacts_conversations.sql`:
  - `contacts`: Stores full E.164 phone numbers (`phone_e164`), SHA-256 hashes, display names, and activity timestamps with a unique constraint on `(tenant_id, phone_e164)`.
  - `conversations`: Stores conversation status, message activity timestamps, and tracks `service_window_expires_at` with a unique constraint on `(tenant_id, contact_id, whatsapp_account_id)`.
  - RLS policies applied to both tables.
- `LedgerService.recordInboundMessage`: Records `INBOUND_FREE` ledger entries with direction `INBOUND` and masked phone numbers (hash + last 4 digits only).
- `ProcessWebhookEventHandler` (`PROCESS_WEBHOOK_EVENT`):
  - Resolves `WhatsAppAccount` from `phone_number_id` and establishes `TenantContext`.
  - Inbound messages: Upserts `Contact`, upserts/refreshes `Conversation` (`service_window_expires_at = timestamp + 24 hours`), writes `INBOUND_FREE` ledger row, emits `InboundMessageReceivedEvent` Spring domain event, and marks webhook event `PROCESSED`.
  - Status updates: Calls `ledgerService.recordStatusEvent(wamid, status, payload)` and marks webhook event `PROCESSED`.
  - Unsupported shapes: Marks webhook event `IGNORED` without throwing exceptions or silently dropping data.
- `TenantFilterAspect`: Enhanced to explicitly disable Hibernate's `tenantFilter` when `TenantContext.get() == null` to support cross-tenant webhook routing.

## Key Files
- `V11__contacts_conversations.sql`: Migration.
- `Contact.java` & `ContactRepository.java`: Contact persistence.
- `Conversation.java` & `ConversationRepository.java`: 24h window tracking.
- `InboundMessageReceivedEvent.java`: Domain event.
- `ProcessWebhookEventHandler.java`: Job processor.
- `InboundMessageProcessingTest.java`: 6 integration tests.
