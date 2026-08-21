# Database Design

**Classification: BUILD NOW.** PostgreSQL 17. Full column lists in `TABLES.md`.

## Principles

1. **Every table has `tenant_id UUID NOT NULL`** except the three documented exceptions below.
2. **Every table has an RLS policy** (`USING` + `WITH CHECK`).
3. **UUID primary keys** (`gen_random_uuid()` from `pgcrypto`) — no sequential IDs leaking
   volume information to customers.
4. **`timestamptz` always, UTC always.** Render IST in the frontend.
5. **Money as integer minor units** (paise) — never `float`, never `double`.
6. **Append-only where it's evidence** — `message_ledger`, `webhook_events`, `audit_log`.
7. **No table exists "for the future".** If nothing reads it this increment, don't create it.

### The three documented exceptions to `tenant_id`

| Table | Why |
|---|---|
| `tenants` | It *is* the tenant |
| `users` | A user may belong to multiple tenants via `tenant_users` |
| `webhook_events` | At ingest we have only `phone_number_id`; the tenant is resolved by the worker. **Must never be exposed through a tenant-facing API.** |

`jobs.tenant_id` is nullable (system jobs have none). Document it in the migration.

---

## Tables, by increment

| Increment | Migration | Tables |
|---|---|---|
| F00 | `V1__baseline.sql` | extensions only (`pgcrypto`, `pg_trgm`) |
| F01 | `V2__tenants_users.sql` | `tenants`, `users`, `tenant_users` |
| F02 | `V3__rls.sql` | app role + RLS policies + the documented pattern |
| F03 | `V4__spring_session.sql` | Spring Session JDBC schema |
| F04 | `V5__password_reset.sql` | `password_reset_tokens` |
| F05 | `V6__whatsapp_accounts.sql` | `whatsapp_accounts` |
| F07 | `V7__jobs.sql` | `jobs` |
| F08 | `V8__message_ledger.sql` | `message_ledger`, `message_ledger_status_events`, `whatsapp_rates` |
| F10 | `V9__webhook_events.sql` | `webhook_events` |
| F11 | `V10__contacts_conversations.sql` | `contacts`, `conversations` |
| F12 | `V11__templates.sql` | `templates` |
| F13 | `V12__automation_rules.sql` | `automation_rules` |
| F14 | `V13__faqs.sql` | `faqs`, `unmatched_messages` |
| F16 | `V14__scheduled_messages.sql` | `scheduled_messages` |
| F21 | `V15__subscriptions.sql` | `subscriptions`, `payment_events` |
| — | `V16__audit_log.sql` | `audit_log` |

**16 tables total for the MVP.** If you find yourself at 30, something has gone wrong.

---

## Tables we deliberately do NOT create

| Not creating | Why | Instead |
|---|---|---|
| `campaigns`, `campaign_recipients` | No validated need; bulk marketing is a different product | Later, if customers ask |
| `tags`, `contact_tags` | Nobody asked | Later |
| `agents`, `assignments` | Single-user tenants for now | D-08 |
| `notifications` | Email is enough | Later |
| `feature_flags` | 1 developer, 1 deploy | Config properties |
| `roles`, `permissions` | Two roles fit in an enum | Later |
| `message_costs` | Ledger + `whatsapp_rates` computes it | — |
| `conversation_windows` | A column on `conversations` | — |
| Separate `media` table | R2 key on the message row | — |

---

## Relationships

```text
tenants ──┬── tenant_users ──── users
          │
          ├── whatsapp_accounts ──┬── templates
          │                       └── conversations
          ├── contacts ────────────── conversations
          ├── message_ledger ───────── message_ledger_status_events
          ├── automation_rules
          ├── faqs
          ├── unmatched_messages
          ├── scheduled_messages
          ├── subscriptions
          └── audit_log

jobs             (tenant_id nullable)
webhook_events   (no tenant_id — resolved by worker)
whatsapp_rates   (global reference data)
```

## Indexing philosophy

Index for the queries you actually run. Every index costs write throughput, and this is a
write-heavy application.

**Always:**
- `(tenant_id, <primary lookup column>)` on every tenant table — RLS filters on `tenant_id`, so
  it belongs first in the composite
- Unique constraints for real business rules (`(tenant_id, phone_e164)` on contacts)
- Partial indexes for the queue: `(status, run_after) WHERE status IN ('PENDING','RUNNING')`

**Not yet:** covering indexes, expression indexes beyond FTS/trigram, partitioning.
See `INDEXES.md`.

## Migration discipline

- **Never edit an applied migration.** Always a new `V{n}__`.
- `ddl-auto: validate`. Never `update` or `create`.
- Every migration that creates a table must also create its RLS policy — in the *same* file.
- Test every migration against a **fresh** database, not just an incremental apply.
- Add a comment at the top of each migration saying which increment it belongs to.

See `DATABASE-MIGRATIONS.md`.
