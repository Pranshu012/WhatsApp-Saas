# F16 — Scheduled Messages

**Status:** Complete  
**Completed:** 2026-08-21  
**Spec:** ../WhatsApp-SaaS-Product/14-CLAUDE-CODE/PROMPTS/PHASE-C-AUTOMATION.md#f16

## What this does
Enables tenants to schedule future WhatsApp template messages reliably. Stores scheduled timestamps in UTC with explicit tenant timezone metadata, prevents invalid free-text scheduling early at creation time, executes due dispatches using a two-level worker architecture (`ENQUEUE_DUE_SCHEDULED_MESSAGES` with `FOR UPDATE SKIP LOCKED` claiming and deterministic idempotency key `sched:{id}`), and enforces honest cancellation semantics (409 Conflict if already enqueued/sent).

## Files
| File | Purpose |
|---|---|
| `src/main/resources/db/migration/V16__scheduled_messages.sql` | `scheduled_messages` table, indexes on `(status, scheduled_for)` and tenant, and RLS policy |
| `src/main/java/com/example/wasaas/scheduling/ScheduledMessageStatus.java` | Enum with lifecycle states (`SCHEDULED`, `ENQUEUED`, `SENT`, `FAILED`, `CANCELLED`) |
| `src/main/java/com/example/wasaas/scheduling/ScheduledMessage.java` | JPA entity with state transitions and honest cancellation guards |
| `src/main/java/com/example/wasaas/scheduling/ScheduledMessageRepository.java` | Repository with `claimDue` query using `FOR UPDATE SKIP LOCKED` |
| `src/main/java/com/example/wasaas/scheduling/ScheduleMessageCommand.java` | DTO for scheduling messages |
| `src/main/java/com/example/wasaas/scheduling/SchedulingService.java` | Handles template validation, timezone conversion to UTC, and honest cancellations |
| `src/main/java/com/example/wasaas/scheduling/EnqueueDueScheduledMessagesHandler.java` | Worker `JobHandler` claiming due messages and enqueuing sends with key `sched:{id}` |
| `src/main/java/com/example/wasaas/scheduling/ScheduledMessageScanner.java` | Minute-based cron scanner triggering due message dispatches |
| `src/test/java/com/example/wasaas/scheduling/SchedulingServiceTest.java` | 7 integration tests covering future scheduling, due claiming, double-run idempotency, honest cancellations, free-text rejection, and RLS |

## Database changes
- Migration: `V16__scheduled_messages.sql`
- Table `scheduled_messages`:
  - `id`: UUID Primary Key
  - `tenant_id`: UUID NOT NULL REFERENCES `tenants(id)`
  - `contact_id`: UUID NOT NULL REFERENCES `contacts(id)`
  - `template_id`: UUID NOT NULL REFERENCES `whatsapp_templates(id)`
  - `whatsapp_account_id`: UUID NOT NULL REFERENCES `whatsapp_accounts(id)`
  - `variables`: JSONB
  - `scheduled_for`: TIMESTAMPTZ NOT NULL (stored in UTC)
  - `timezone`: VARCHAR(100) NOT NULL DEFAULT 'Asia/Kolkata'
  - `status`: VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED'
  - `job_id`: UUID REFERENCES `jobs(id)`
  - `failure_reason`: TEXT
  - `created_at`, `updated_at`: TIMESTAMPTZ
- Indexes:
  - `idx_scheduled_messages_due`: on `(status, scheduled_for)` WHERE `status = 'SCHEDULED'`
  - `idx_scheduled_messages_tenant`: on `(tenant_id, created_at DESC)`
- RLS Policy: `scheduled_messages_tenant_isolation`

## Key decisions and why
- **Two-Level Job Queue Architecture:** Separating the finder (`ENQUEUE_DUE_SCHEDULED_MESSAGES`) from the individual sends (`SEND_WHATSAPP_MESSAGE`) keeps due polling light and allows individual send retries without blocking the batch.
- **Deterministic Idempotency Key (`sched:{id}`):** The send job uses `sched:{scheduled_message_id}` as its idempotency key. If the poller runs twice in the same minute or crashes after claiming, the unique index on `jobs.idempotency_key` rejects duplicate sends, protecting the customer from double-billing.
- **Early Rejection of Free-Text Messages:** Because the 24-hour service window will have expired by the time the future message triggers, the API rejects free-text scheduling immediately at creation rather than failing silently days later.
- **Honest Cancellation Semantics:** Cancellation succeeds only when `status == SCHEDULED`. Once `ENQUEUED` or `SENT`, it returns HTTP 409 Conflict because WhatsApp messages cannot be recalled once in flight.

## Divergence from the architecture docs
None.

## Test coverage
- `testScheduleMessageInFutureWithTimezone`: Timezone converted to UTC instant and stored with `SCHEDULED` status.
- `testEnqueueDueMessagesHandlerDispatchesSendJob`: Claims due messages and enqueues send job with deterministic key.
- `testDoubleRunInSameMinuteDoesNotDoubleSend`: Running due finder twice produces exactly 1 send job.
- `testCancelWhileScheduledSucceeds`: Message cancelled while in `SCHEDULED` status.
- `testCancelWhileEnqueuedThrowsConflictException`: Cancellation after `ENQUEUED` throws HTTP 409 Conflict.
- `testScheduleFreeTextRejectedAtCreation`: Non-template scheduling rejected at creation time.
- `testMultiTenantScheduledMessageIsolation`: RLS ensures tenant messages cannot be viewed or claimed across tenants.
