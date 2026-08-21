# F12 — Meta Message Template Management & Category Synchronization

**Status:** Complete  
**Completed:** 2026-08-21  
**Spec:** ../WhatsApp-SaaS-Product/14-CLAUDE-CODE/PROMPTS/PHASE-C-AUTOMATION.md#f12

## What this does
Manages a tenant's WhatsApp message templates, mirroring Meta Graph API as the absolute source of truth for template approval statuses, variable counts, and billing categories. Automatically detects category conflicts when Meta assigns a higher-cost category (e.g. `MARKETING` instead of `UTILITY`) and provides pre-flight assertion guards that reject unusable templates before making any outbound API calls.

## Files
| File | Purpose |
|---|---|
| `src/main/resources/db/migration/V13__templates.sql` | `whatsapp_templates` table with tenant isolation RLS |
| `src/main/java/com/example/wasaas/template/TemplateCategory.java` | Template category enum (`MARKETING`, `UTILITY`, `AUTHENTICATION`) |
| `src/main/java/com/example/wasaas/template/TemplateStatus.java` | Template status enum (`PENDING`, `APPROVED`, `REJECTED`, `PAUSED`, `DISABLED`) |
| `src/main/java/com/example/wasaas/template/WhatsAppTemplate.java` | Template entity with variable count parser and category conflict tracking |
| `src/main/java/com/example/wasaas/template/WhatsAppTemplateRepository.java` | Multi-tenant repository for template lookup |
| `src/main/java/com/example/wasaas/template/TemplateSyncService.java` | Syncs templates from Meta Graph API, updates statuses and flags category conflicts |
| `src/main/java/com/example/wasaas/template/TemplateService.java` | Submits templates for approval & executes `assertSendable` pre-flight safety check |
| `src/main/java/com/example/wasaas/template/SyncTemplatesJobHandler.java` | Background job handler for `SYNC_TEMPLATES` |
| `src/main/java/com/example/wasaas/whatsapp/meta/MetaGraphClient.java` | Added `listTemplates` and `createTemplate` |
| `src/main/java/com/example/wasaas/whatsapp/send/SendMessageJobHandler.java` | Integrated `templateService.assertSendable(...)` pre-flight validation |
| `src/main/java/com/example/wasaas/whatsapp/inbound/ProcessWebhookEventHandler.java` | Added `message_template_status_update` webhook real-time processing |
| `src/test/java/com/example/wasaas/template/TemplateManagementTest.java` | 6 integration tests for sync, conflict detection, rejection reasons, and pre-send guards |

## Database changes
- Table `whatsapp_templates`:
  - `id`: UUID Primary Key
  - `tenant_id`: UUID NOT NULL referencing `tenants(id)`
  - `whatsapp_account_id`: UUID NOT NULL referencing `whatsapp_accounts(id)`
  - `meta_template_id`: VARCHAR(100)
  - `name`: VARCHAR(255) NOT NULL
  - `language`: VARCHAR(50) NOT NULL
  - `requested_category`: VARCHAR(50) (Requested on creation)
  - `category`: VARCHAR(50) (Authoritatively assigned by Meta)
  - `category_conflict`: BOOLEAN NOT NULL DEFAULT false
  - `status`: VARCHAR(50) NOT NULL DEFAULT 'PENDING'
  - `rejection_reason`: TEXT
  - `body_text`: TEXT NOT NULL
  - `header_type`: VARCHAR(50)
  - `variable_count`: INT NOT NULL DEFAULT 0
  - `components`: JSONB
  - `synced_at`: TIMESTAMPTZ
  - Unique Constraint: `(tenant_id, name, language)`
- Indexes:
  - `idx_whatsapp_templates_tenant_lookup` on `(tenant_id, name, language)`
  - `idx_whatsapp_templates_status` on `(tenant_id, status)`
- RLS Policy:
  - `whatsapp_templates_tenant_isolation` enforces `tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid`
- Migration Version: `V13`

## Key decisions and why
- **Meta is the Absolute Source of Truth:** Local template edits never override Meta's assigned `category` or `status`. When Meta classifies a template submitted as `UTILITY` as `MARKETING`, `category_conflict` is set to `true` to visibly warn the customer of the 7.5x price difference.
- **Pre-Send Safety Check:** `TemplateService.assertSendable(...)` verifies that the template exists, is `APPROVED`, and has the exact required parameter count *before* making any HTTP calls to Meta. Non-approved templates throw `PermanentJobException` immediately, avoiding failed API calls and preserving Meta quality scores.
- **Positional Variable Calculation:** Variable placeholders (`{{1}}`, `{{2}}`) are parsed automatically at ingestion/sync time and validated against outgoing payload parameter count.

## Divergence from the architecture docs
None. Follows `05-BACKEND/TEMPLATE-SERVICE.md` and `08-META-WHATSAPP/MESSAGE-TEMPLATES.md`.

## Gotchas and edge cases
- **Template Status Webhooks vs Security Webhooks:** Webhook payloads containing generic fields like `"event"` must check for `message_template_id` or `message_template_name` to prevent misidentifying non-template system notifications.
- **Pre-flight Assertion Transactions:** `assertSendable` runs inside the caller's transaction context and must not be marked `@Transactional(readOnly = true)` to avoid marking the entire parent transaction rollback-only upon validation failures.

## Test coverage
- `testSyncTemplatesUpsertsWithoutDuplicating`: Multiple sync runs update records without duplicating.
- `testMetaCategoryWinsOverRequestedCategoryWithConflictFlag`: Meta's assigned category overrides requested category and triggers `category_conflict = true`.
- `testRejectedTemplateStoresRejectionReason`: Rejected status and reasons from Meta are captured and persisted.
- `testSendingNonApprovedTemplateFailsBeforeApiCall`: `PENDING` template is rejected with `PermanentJobException` before outbound dispatch.
- `testVariableCountMismatchFailsBeforeApiCall`: Parameter count mismatch throws exception locally.
- `testMultiTenantTemplateIsolation`: RLS ensures templates are strictly partitioned per tenant.
