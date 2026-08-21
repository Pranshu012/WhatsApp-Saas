# ER Diagram

```mermaid
erDiagram
    TENANTS ||--o{ TENANT_USERS : has
    USERS   ||--o{ TENANT_USERS : belongs_to
    USERS   ||--o{ PASSWORD_RESET_TOKENS : requests

    TENANTS ||--o{ WHATSAPP_ACCOUNTS : owns
    TENANTS ||--o{ CONTACTS : has
    TENANTS ||--o{ AUTOMATION_RULES : configures
    TENANTS ||--o{ FAQS : configures
    TENANTS ||--o{ UNMATCHED_MESSAGES : logs
    TENANTS ||--o{ SUBSCRIPTIONS : pays_via
    TENANTS ||--o{ AUDIT_LOG : records
    TENANTS ||--o{ MESSAGE_LEDGER : accrues

    WHATSAPP_ACCOUNTS ||--o{ TEMPLATES : holds
    WHATSAPP_ACCOUNTS ||--o{ CONVERSATIONS : hosts
    CONTACTS          ||--o{ CONVERSATIONS : participates
    CONTACTS          ||--o{ SCHEDULED_MESSAGES : targets
    TEMPLATES         ||--o{ SCHEDULED_MESSAGES : uses

    MESSAGE_LEDGER ||--o{ MESSAGE_LEDGER_STATUS_EVENTS : transitions

    SUBSCRIPTIONS ||--o{ PAYMENT_EVENTS : evidenced_by

    TENANTS {
        uuid id PK
        text business_name
        text slug UK
        text status
        text timezone
        text gstin "for GST invoices"
    }

    USERS {
        uuid id PK
        text email UK
        text password_hash "Argon2id"
        text full_name
    }

    WHATSAPP_ACCOUNTS {
        uuid id PK
        uuid tenant_id FK
        text waba_id
        text phone_number_id
        bytea access_token_encrypted "AES-256-GCM"
        text quality_rating
        boolean payment_method_attached
        text status
    }

    MESSAGE_LEDGER {
        uuid id PK
        uuid tenant_id FK
        text direction
        text wamid "Meta message id"
        text billing_category "MARKETING|UTILITY|AUTH|SERVICE|INBOUND_FREE"
        text recipient_phone_hash "no full number"
        text status
    }

    CONVERSATIONS {
        uuid id PK
        uuid tenant_id FK
        timestamptz service_window_expires_at "billable after 1 Oct 2026"
        text status
    }

    AUTOMATION_RULES {
        uuid id PK
        uuid tenant_id FK
        text match_type
        text match_value "untrusted if REGEX"
        int priority "first match wins"
        text action_type
    }
```

## Tables outside the tenant graph

These have no `tenant_id` and sit deliberately outside the diagram above:

```mermaid
erDiagram
    JOBS {
        uuid id PK
        uuid tenant_id "NULLABLE - system jobs"
        text job_type
        text status
        text idempotency_key UK
        int attempts
        timestamptz run_after
        timestamptz locked_at
    }

    WEBHOOK_EVENTS {
        uuid id PK
        text event_id UK "dedupe"
        text phone_number_id "tenant resolved by worker"
        jsonb raw_payload "replay source"
        boolean signature_valid
        text status
    }

    WHATSAPP_RATES {
        uuid id PK
        text country_code
        text category
        bigint rate_minor "paise, integer"
        date effective_from "Meta changes quarterly"
    }
```

**Why they're separate:**
- `jobs` — infrastructure, spans tenants, some jobs are system-level
- `webhook_events` — at ingest we have only a `phone_number_id`; tenant resolution is the
  worker's job. **Never expose through a tenant-facing API.**
- `whatsapp_rates` — global reference data from Meta's published rate card

See `MULTI-TENANT-DATABASE-RULES.md` for the full explanation of these three exceptions.
