# Tables — Column Reference

Every table: `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`, `created_at TIMESTAMPTZ NOT NULL
DEFAULT now()`, and `updated_at TIMESTAMPTZ` where mutable. All timestamps `timestamptz`, UTC.

---

## `tenants`
The business using our product.

| Column | Type | Notes |
|---|---|---|
| `business_name` | text NOT NULL | Display name |
| `slug` | text NOT NULL UNIQUE | URL-safe |
| `status` | text NOT NULL | `ACTIVE` / `SUSPENDED` |
| `timezone` | text NOT NULL DEFAULT 'Asia/Kolkata' | For scheduling |
| `gstin` | text NULL | B2B customers need GST invoices |
| `legal_name`, `billing_address` | text NULL | GST invoice fields |

No `tenant_id` (exception 1).

## `users`
| Column | Type | Notes |
|---|---|---|
| `email` | text NOT NULL UNIQUE | Store lowercased |
| `password_hash` | text NOT NULL | Argon2id |
| `full_name` | text NOT NULL | |
| `status` | text NOT NULL | `ACTIVE` / `DISABLED` |
| `last_login_at` | timestamptz NULL | |

No `tenant_id` (exception 2).

## `tenant_users`
| Column | Type | Notes |
|---|---|---|
| `tenant_id` | uuid NOT NULL FK | |
| `user_id` | uuid NOT NULL FK | |
| `role` | text NOT NULL | `OWNER` / `MEMBER` |

PK `(tenant_id, user_id)`.

## `password_reset_tokens`
| Column | Type | Notes |
|---|---|---|
| `user_id` | uuid NOT NULL FK | |
| `token_hash` | text NOT NULL | **SHA-256 of the token. Never the raw token.** |
| `expires_at` | timestamptz NOT NULL | 30 minutes |
| `used_at` | timestamptz NULL | Single use |

## `whatsapp_accounts`
The customer's own WABA. **The most sensitive table.**

| Column | Type | Notes |
|---|---|---|
| `tenant_id` | uuid NOT NULL FK | |
| `waba_id` | text NOT NULL | Meta's WABA id |
| `phone_number_id` | text NOT NULL | Used for every send |
| `display_phone_number` | text NOT NULL | Display only |
| `verified_name` | text NULL | Display only |
| `quality_rating` | text NULL | `GREEN`/`YELLOW`/`RED` — surface in UI |
| `messaging_limit_tier` | text NULL | e.g. `TIER_1K` |
| `access_token_encrypted` | bytea NOT NULL | **AES-256-GCM. Nonce prefixed.** |
| `token_encrypted_at` | timestamptz NOT NULL | |
| `payment_method_attached` | boolean NULL | Best-effort; drives the F18 warning |
| `status` | text NOT NULL | `CONNECTED`/`TOKEN_EXPIRED`/`DISCONNECTED` |
| `connected_at` | timestamptz NOT NULL | |

UNIQUE `(tenant_id, phone_number_id)`.

## `jobs`
See `03-ARCHITECTURE/BACKGROUND-JOBS.md` for the full DDL and claim query.

`tenant_id` **nullable** — system jobs have none. Document in the migration.

## `webhook_events`
| Column | Type | Notes |
|---|---|---|
| `event_id` | text NULL UNIQUE (partial) | Meta's id where present — dedupe |
| `waba_id`, `phone_number_id` | text NULL | Tenant resolution happens in the worker |
| `raw_payload` | jsonb NOT NULL | **Keep this. It's your only replay source.** |
| `signature_valid` | boolean NOT NULL | |
| `status` | text NOT NULL | `RECEIVED`/`PROCESSED`/`IGNORED`/`FAILED` |
| `ignore_reason` | text NULL | For unknown event types |
| `received_at`, `processed_at` | timestamptz | |

No `tenant_id` (exception 3). **Never expose via a tenant-facing API.**

## `message_ledger` and `message_ledger_status_events`
See `03-ARCHITECTURE/MESSAGE-LEDGER.md` for full DDL and rationale. Append-only.

## `whatsapp_rates`
Global reference data — Meta's rate card. No `tenant_id`.

| Column | Type | Notes |
|---|---|---|
| `country_code` | text NOT NULL | `IN` |
| `category` | text NOT NULL | |
| `rate_minor` | bigint NOT NULL | **Paise. Integer.** |
| `currency` | text NOT NULL DEFAULT 'INR' | |
| `effective_from`, `effective_to` | date | Rates change quarterly |
| `source_note` | text | e.g. "Meta list rate, verified 2026-08-18" |

## `contacts`
| Column | Type | Notes |
|---|---|---|
| `tenant_id` | uuid NOT NULL FK | |
| `phone_e164` | text NOT NULL | **Full number — needed to send** |
| `phone_hash` | text NOT NULL | Matches the ledger's hash |
| `display_name` | text NULL | From the WhatsApp profile |
| `opt_in_status` | text NOT NULL | `OPTED_IN`/`OPTED_OUT`/`UNKNOWN` |
| `last_seen_at` | timestamptz NULL | |

UNIQUE `(tenant_id, phone_e164)`.

## `conversations`
| Column | Type | Notes |
|---|---|---|
| `tenant_id`, `contact_id`, `whatsapp_account_id` | uuid NOT NULL FK | |
| `last_inbound_at`, `last_outbound_at` | timestamptz NULL | |
| `service_window_expires_at` | timestamptz NULL | **Commercially significant after 1 Oct 2026** |
| `status` | text NOT NULL | `OPEN`/`ESCALATED`/`CLOSED` |

UNIQUE `(tenant_id, contact_id, whatsapp_account_id)`.

## `templates`
| Column | Type | Notes |
|---|---|---|
| `tenant_id`, `whatsapp_account_id` | uuid NOT NULL FK | |
| `meta_template_id` | text NULL | |
| `name`, `language` | text NOT NULL | |
| `category` | text NOT NULL | **Meta's assignment wins over what you requested** |
| `status` | text NOT NULL | `PENDING`/`APPROVED`/`REJECTED`/`PAUSED`/`DISABLED` |
| `rejection_reason` | text NULL | Show this to the customer |
| `body_text`, `header_type`, `variable_count` | | |
| `components` | jsonb | Meta's structure |
| `synced_at` | timestamptz | |

UNIQUE `(tenant_id, name, language)`.

## `automation_rules`
| Column | Type | Notes |
|---|---|---|
| `tenant_id` | uuid NOT NULL FK | |
| `name`, `enabled` | | |
| `match_type` | text NOT NULL | `EXACT`/`CONTAINS`/`STARTS_WITH`/`REGEX` |
| `match_value` | text NOT NULL | **Untrusted input if REGEX** |
| `case_sensitive` | boolean NOT NULL DEFAULT false | |
| `priority` | int NOT NULL | Lower runs first; first match wins |
| `action_type` | text NOT NULL | `SEND_TEXT`/`SEND_TEMPLATE`/`SEND_INTERACTIVE`/`ESCALATE` |
| `action_payload` | jsonb NOT NULL | |

## `faqs`
| Column | Type | Notes |
|---|---|---|
| `tenant_id` | uuid NOT NULL FK | |
| `question`, `answer` | text NOT NULL | |
| `search_vector` | tsvector | Generated column or trigger-maintained |
| `enabled` | boolean NOT NULL DEFAULT true | |

GIN on `search_vector`; trigram GIN on `question`.

## `unmatched_messages`
**The dataset behind ADR-007.** Log every message no rule and no FAQ matched.

| Column | Type | Notes |
|---|---|---|
| `tenant_id` | uuid NOT NULL FK | |
| `message_text` | text NOT NULL | Retention applies (D-09) |
| `best_candidate_faq_id` | uuid NULL | |
| `best_score` | numeric NULL | |
| `escalated` | boolean NOT NULL | |

## `scheduled_messages`
| Column | Type | Notes |
|---|---|---|
| `tenant_id`, `contact_id`, `template_id` | uuid NOT NULL FK | |
| `variables` | jsonb | |
| `scheduled_for` | timestamptz NOT NULL | **UTC** |
| `timezone` | text NOT NULL | Tenant's tz, stored explicitly |
| `status` | text NOT NULL | `SCHEDULED`/`ENQUEUED`/`SENT`/`FAILED`/`CANCELLED` |
| `job_id` | uuid NULL | |

## `subscriptions`
| Column | Type | Notes |
|---|---|---|
| `tenant_id` | uuid NOT NULL FK | |
| `plan_code` | text NOT NULL | |
| `status` | text NOT NULL | `TRIALING`/`ACTIVE`/`PAST_DUE`/`CANCELLED`/`EXPIRED` |
| `razorpay_subscription_id` | text NULL UNIQUE | |
| `current_period_start`, `current_period_end`, `trial_ends_at`, `cancelled_at` | timestamptz | |

## `payment_events`
Append-only raw Razorpay webhooks. Same discipline as `webhook_events`.

## `audit_log`
Append-only. `tenant_id`, `user_id`, `action`, `resource_type`, `resource_id`, `ip`, `metadata`
jsonb, `created_at`.
