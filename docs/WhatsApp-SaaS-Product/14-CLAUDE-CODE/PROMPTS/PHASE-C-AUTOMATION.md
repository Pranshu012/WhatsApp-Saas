# Phase C Prompts — Automation (F12–F16)

> **⚠️ BLOCKED until D-01 is closed.** The exact automation feature set is **not** specified
> in the source architecture document. Open
> `docs/WhatsApp-SaaS-Product/13-DECISIONS/DECISIONS.md`, answer D-01 (core feature scope),
> and write the answer down before starting F12. Everything below is the most defensible
> default given the Tech Provider model and ADR-007 — not a settled requirement.

---

## F12 — Templates

```text
Increment F12. Read docs/WhatsApp-SaaS-Product/08-META-WHATSAPP/MESSAGE-TEMPLATES.md,
MESSAGE-CATEGORIES.md and docs/WhatsApp-SaaS-Product/05-BACKEND/TEMPLATE-SERVICE.md.

Goal: manage a tenant's WhatsApp message templates, mirroring Meta as source of truth.

Requirements:
- Migration V11__templates.sql: id, tenant_id NOT NULL, whatsapp_account_id,
  meta_template_id, name, language, category (MARKETING/UTILITY/AUTHENTICATION),
  status (PENDING/APPROVED/REJECTED/PAUSED/DISABLED), rejection_reason, body_text,
  header_type, variable_count, components jsonb, synced_at, created_at, updated_at
  - unique (tenant_id, name, language); RLS per V3 pattern
- TemplateSyncService: pull templates from Meta for a WABA and upsert. Meta is authoritative
  for status and category — never let a local edit override what Meta says.
- TemplateService.submitForApproval(...) → create via Graph API, store as PENDING
- Sync via a job (job_type SYNC_TEMPLATES), enqueued on connect and on a daily schedule
- Category matters commercially: a template Meta classifies as MARKETING costs roughly 7.5x
  a UTILITY one in India. Surface Meta's assigned category prominently and warn when a
  submitted category differs from what Meta assigned.
- Template status change webhooks (if subscribed) update local status via F10's pipeline

Tests: sync upserts without duplicating; Meta's category wins over the locally requested one;
rejected templates store the reason; sending with a non-APPROVED template is rejected before
any API call.

Do NOT build: a template designer UI (F19), template analytics.

Plan first.

Finally: write docs/IMPLEMENTATION/F12-<slug>.md following
docs/WhatsApp-SaaS-Product/14-CLAUDE-CODE/IMPLEMENTATION-DOC-TEMPLATE.md, and add a row to
docs/IMPLEMENTATION/INDEX.md. Document the key decisions, any divergence from the architecture
docs, and the gotchas someone would trip over. This is part of the increment, not optional.
```

**DoD:** sync mirrors Meta · category conflicts surfaced · non-approved templates unusable

---

## F13 — Keyword automation rules

```text
Increment F13.

Goal: when an inbound message arrives, match it against per-tenant rules and reply.

Requirements:
- Migration V12__automation_rules.sql: id, tenant_id NOT NULL, name, enabled,
  match_type (EXACT/CONTAINS/STARTS_WITH/REGEX), match_value, case_sensitive, priority,
  action_type (SEND_TEXT/SEND_TEMPLATE/SEND_INTERACTIVE/ESCALATE), action_payload jsonb,
  created_at, updated_at; index (tenant_id, enabled, priority); RLS per V3
- AutomationEngine listening to F11's InboundMessageReceived event:
  1. load enabled rules for the tenant, ordered by priority
  2. first match wins — do not fire multiple rules for one message
  3. enqueue the reply via F09's MessagingService (never send directly)
  4. no match → publish UnmatchedMessage event and log it for analysis
- REGEX rules: compile with a timeout and reject catastrophic patterns at save time.
  A tenant-supplied regex is untrusted input — treat it as such.
- Rate-limit automated replies per contact (e.g. max N auto-replies per contact per hour)
  so a loop can never spam someone or run up their Meta bill.
- Log every unmatched message. This dataset is the only honest input to "do we need AI?"
  (see ADR-007). Do not add an LLM.

Tests: exact/contains/starts-with/regex all match correctly; priority respected and only one
rule fires; no match → unmatched logged, no reply sent; a catastrophic regex is rejected at
save; per-contact rate limit prevents a reply storm; tenant A's rules never apply to tenant B.

Do NOT build: FAQ matching (F14), a visual flow builder, multi-step conversation state.

Plan first.

Finally: write docs/IMPLEMENTATION/F13-<slug>.md following
docs/WhatsApp-SaaS-Product/14-CLAUDE-CODE/IMPLEMENTATION-DOC-TEMPLATE.md, and add a row to
docs/IMPLEMENTATION/INDEX.md. Document the key decisions, any divergence from the architecture
docs, and the gotchas someone would trip over. This is part of the increment, not optional.
```

**DoD:** one rule fires per message · regex sandboxed · reply storm impossible · unmatched logged

---

## F14 — FAQ matching

```text
Increment F14.

Goal: answer free-text questions from a per-tenant FAQ using Postgres only. No LLM (ADR-007).

Requirements:
- Migration V13__faqs.sql: id, tenant_id NOT NULL, question, answer, enabled,
  search_vector tsvector (generated or trigger-maintained), created_at, updated_at
  - GIN index on search_vector; pg_trgm extension + trigram index on question
  - RLS per V3
- FaqMatchService: combine full-text rank (ts_rank) with trigram similarity for typo
  tolerance. Return the best match plus a normalised confidence score.
- Confidence threshold in config (not hardcoded). Above → reply with the answer.
  Below → ESCALATE. Never reply with a low-confidence guess: a wrong answer about pricing
  or availability is worse than no answer.
- Hook into AutomationEngine AFTER keyword rules: keyword rules win, FAQ is the fallback,
  escalation is the final fallback.
- Log every below-threshold query with its best candidate and score. This is how the tenant
  improves their own FAQ, and it is a genuinely useful product surface later.

Tests: exact question matches with high confidence; typo'd question still matches (trigram);
unrelated question falls below threshold and escalates; threshold is configurable; keyword
rule takes precedence over FAQ; tenant isolation on search.

Do NOT build: embeddings, a vector database, any LLM call, multi-language stemming beyond
English + simple config.

Plan first, and show me the actual SQL for the combined ranking query.

Finally: write docs/IMPLEMENTATION/F14-<slug>.md following
docs/WhatsApp-SaaS-Product/14-CLAUDE-CODE/IMPLEMENTATION-DOC-TEMPLATE.md, and add a row to
docs/IMPLEMENTATION/INDEX.md. Document the key decisions, any divergence from the architecture
docs, and the gotchas someone would trip over. This is part of the increment, not optional.
```

**DoD:** typo tolerance works · low confidence escalates rather than guessing · threshold configurable · precedence correct

---

## F15 — Interactive replies and consolidation

> **PILOT TRACK: skip this increment.** Build it after the pilot succeeds. See
> `../../00-START-HERE/PILOT-FIRST-PLAN.md`.

```text
Increment F15. Read docs/WhatsApp-SaaS-Product/08-META-WHATSAPP/MESSAGE-PRICING.md.

Goal: use WhatsApp interactive messages, and stop sending three messages where one will do.

Commercial context: since July 2025 Meta bills PER MESSAGE. From 1 Oct 2026 even in-window
service replies are billable at ~Rs 0.115 each in India. Three short replies cost 3x one
consolidated reply — and our customer pays that bill. Consolidation is a feature.

Requirements:
- Extend WhatsAppCloudClient for interactive reply-button messages (up to 3 buttons) and
  list messages (sections/rows), per the Cloud API shape
- Handle inbound button/list replies in F11's processor: map the reply id back to the rule or
  FAQ that offered it, so a button press can drive the next action
- A ReplyBuilder that accumulates reply parts within one automation evaluation and emits ONE
  message where the content allows, rather than N sends
- Add a warning in the automation config validation when a configured action would produce
  more than one outbound message, explaining the cost implication

Tests: button message sends with correct payload shape; a button press is attributed to the
offering rule; three logical reply parts produce one send; the multi-message warning fires.

Do NOT build: WhatsApp Flows (bigger scope, revisit after 20 customers), carousels, catalogs.

Plan first.

Finally: write docs/IMPLEMENTATION/F15-<slug>.md following
docs/WhatsApp-SaaS-Product/14-CLAUDE-CODE/IMPLEMENTATION-DOC-TEMPLATE.md, and add a row to
docs/IMPLEMENTATION/INDEX.md. Document the key decisions, any divergence from the architecture
docs, and the gotchas someone would trip over. This is part of the increment, not optional.
```

**DoD:** buttons and lists send and are attributed on reply · consolidation proven by test · cost warning shown

---

## F16 — Scheduled messages

> **PILOT TRACK: skip this increment.** Build it after the pilot succeeds.

```text
Increment F16. Read docs/WhatsApp-SaaS-Product/05-BACKEND/SCHEDULING.md.

Goal: schedule a template message for a future time, reliably.

Requirements:
- Migration V14__scheduled_messages.sql: id, tenant_id NOT NULL, contact_id, template_id,
  variables jsonb, scheduled_for timestamptz, timezone, status
  (SCHEDULED/ENQUEUED/SENT/FAILED/CANCELLED), job_id, created_at, updated_at
  - index (status, scheduled_for); RLS per V3
- Store scheduled_for in UTC (timestamptz), store the tenant's timezone separately, render
  IST in the UI. India has no DST so this is simpler than usual, but do NOT rely on that —
  store the timezone explicitly so a future non-IST tenant doesn't break.
- A scheduler job (job_type ENQUEUE_DUE_SCHEDULED_MESSAGES) runs every minute, claims due
  rows, and enqueues SEND_WHATSAPP_MESSAGE jobs with a deterministic idempotency key derived
  from the scheduled_message id. A missed run or a double run must not double-send.
- Cancellation: only while SCHEDULED. Once ENQUEUED, cancellation must fail clearly rather
  than pretend.
- Sending outside a service window requires a template — reject a scheduled free-text
  message at creation time with a clear explanation, since Meta will reject it anyway.

Tests: a message scheduled for +2 min sends once; running the scheduler twice in the same
minute does not double-send; cancellation works while SCHEDULED and fails after ENQUEUED;
scheduling free text (non-template) is rejected at creation; timezone conversion correct.

Do NOT build: recurring schedules, campaigns/bulk sends, timezone-per-contact.

Plan first.

Finally: write docs/IMPLEMENTATION/F16-<slug>.md following
docs/WhatsApp-SaaS-Product/14-CLAUDE-CODE/IMPLEMENTATION-DOC-TEMPLATE.md, and add a row to
docs/IMPLEMENTATION/INDEX.md. Document the key decisions, any divergence from the architecture
docs, and the gotchas someone would trip over. This is part of the increment, not optional.
```

**DoD:** exactly-once send under double-run · cancellation semantics honest · non-template rejected early
