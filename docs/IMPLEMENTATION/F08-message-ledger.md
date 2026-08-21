# F08 — Message Ledger

## Status
Complete — verified with phone number SHA-256 masking, wamid attachment, append-only status event history, monthly billing category aggregation, idempotency deduplication, and PostgreSQL immutability trigger guard across 50 automated tests.

## Summary
Implemented the append-only billing and message evidence ledger:
- `V9__message_ledger.sql`:
  - `message_ledger`: Records each message attempt (INBOUND/OUTBOUND), WABA ID, wamid, template, conversation window, status, and billing category.
  - `message_ledger_status_events`: Child audit table where status transitions (SENT -> DELIVERED -> READ -> FAILED) are appended as immutable events without modifying parent billing semantics.
  - PostgreSQL DB trigger guard `prevent_immutable_ledger_updates()`: Rejects any attempts to mutate `tenant_id`, `direction`, `billing_category`, `recipient_phone_hash`, `recipient_phone_last4`, or `created_at` on `message_ledger`.
  - Row Level Security (RLS) on both ledger tables enforcing multi-tenant isolation.
- `PhonePrivacyUtils`: Masks recipient phone numbers using SHA-256 hex digest + last 4 digits only, minimizing DPDP exposure.
- `LedgerService`:
  - `recordOutboundIntent`: Records intent before dispatching Meta API call.
  - `attachWamid`: Updates `wamid` and marks `SENT` after Meta returns message ID.
  - `recordFailure`: Records error code and failure reason.
  - `recordStatusEvent`: Appends status event by `wamid`.
  - `countByCategoryForMonth`: Aggregates message counts grouped by `BillingCategory` for invoice generation.

## Key Files
- `V9__message_ledger.sql`: Migration with tables, triggers, and RLS.
- `PhonePrivacyUtils.java`: SHA-256 + last4 hashing.
- `MessageLedger.java` & `MessageLedgerStatusEvent.java`: JPA entities.
- `MessageLedgerRepository.java` & `MessageLedgerStatusEventRepository.java`: Spring Data repositories.
- `LedgerService.java`: Transactional orchestrator.
- `MessageLedgerTest.java`: 7 comprehensive tests.
