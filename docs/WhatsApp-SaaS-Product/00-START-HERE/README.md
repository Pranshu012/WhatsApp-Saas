# WhatsApp SaaS for Indian SMBs — Documentation Home

> **If you are starting development today, read this file first, then go straight to
> [`DEVELOPMENT-ROADMAP.md`](DEVELOPMENT-ROADMAP.md).**

This workspace is your execution system. It is deliberately modular: you should never
need to read more than two documents to know what to do next.

**Source of truth:** [`../SOURCE-architecture-and-cost-strategy.md`](../SOURCE-architecture-and-cost-strategy.md)
(researched and verified 18 August 2026). Every architectural and cost decision in this
workspace traces back to that document. Where something is *not* covered by it, it is
marked `[DECISION REQUIRED]` and logged in [`../13-DECISIONS/DECISIONS.md`](../13-DECISIONS/DECISIONS.md).

---

## Reading order

```text
START HERE (you are here)
     ↓
BUSINESS PLAN            01-BUSINESS/BUSINESS-PLAN.md
     ↓
MASTER ACTION PLAN       02-ACTIONS/MASTER-ACTION-PLAN.md      ← your daily driver
     ↓
SYSTEM ARCHITECTURE      03-ARCHITECTURE/SYSTEM-ARCHITECTURE.md
     ↓
APPLICATION STRUCTURE    05-BACKEND/SPRING-BOOT-STRUCTURE.md
     ↓
DATABASE                 04-DATABASE/DATABASE-DESIGN.md
     ↓
BACKEND                  05-BACKEND/BACKEND-SETUP.md
     ↓
FRONTEND                 06-FRONTEND/FRONTEND-SETUP.md
     ↓
WHATSAPP                 08-META-WHATSAPP/TECH-PROVIDER-SETUP.md
     ↓
TESTING                  09-TESTING/TESTING-STRATEGY.md
     ↓
DEPLOYMENT               07-INFRASTRUCTURE/PRODUCTION-DEPLOYMENT.md
     ↓
FIRST CUSTOMER           10-OPERATIONS/CUSTOMER-ONBOARDING.md
```

---

## The three documents you will use most

| Document | When |
|---|---|
| [`02-ACTIONS/MASTER-ACTION-PLAN.md`](../02-ACTIONS/MASTER-ACTION-PLAN.md) | Every working session. Phases 0–14 with checkboxes. |
| [`00-START-HERE/CURRENT-STATUS.md`](CURRENT-STATUS.md) | Update at the end of every session. Read after any break. |
| [`13-DECISIONS/DECISIONS.md`](../13-DECISIONS/DECISIONS.md) | Whenever you are tempted to decide something quietly. |

---

## Full index

### 00 — Start here
- [README.md](README.md) — this file
- [DEVELOPMENT-ROADMAP.md](DEVELOPMENT-ROADMAP.md) — the 15-step build sequence
- [CURRENT-STATUS.md](CURRENT-STATUS.md) — where you are right now
- [ASSUMPTIONS-AND-EXPIRY-DATES.md](ASSUMPTIONS-AND-EXPIRY-DATES.md) — dated facts needing re-verification

### 01 — Business
- [BUSINESS-PLAN.md](../01-BUSINESS/BUSINESS-PLAN.md)
- [PRODUCT-VISION.md](../01-BUSINESS/PRODUCT-VISION.md)
- [TARGET-CUSTOMER.md](../01-BUSINESS/TARGET-CUSTOMER.md)
- [VALUE-PROPOSITION.md](../01-BUSINESS/VALUE-PROPOSITION.md)
- [PRICING-AND-MONETIZATION.md](../01-BUSINESS/PRICING-AND-MONETIZATION.md)
- [CUSTOMER-VALIDATION.md](../01-BUSINESS/CUSTOMER-VALIDATION.md)

### 02 — Actions
- [MASTER-ACTION-PLAN.md](../02-ACTIONS/MASTER-ACTION-PLAN.md)
- [PRE-DEVELOPMENT-CHECKLIST.md](../02-ACTIONS/PRE-DEVELOPMENT-CHECKLIST.md)
- [WEEK-1-ACTIONS.md](../02-ACTIONS/WEEK-1-ACTIONS.md)
- [WEEK-2-ACTIONS.md](../02-ACTIONS/WEEK-2-ACTIONS.md)
- [WEEK-3-ACTIONS.md](../02-ACTIONS/WEEK-3-ACTIONS.md)
- [WEEK-4-ACTIONS.md](../02-ACTIONS/WEEK-4-ACTIONS.md)
- [FIRST-10-CUSTOMERS.md](../02-ACTIONS/FIRST-10-CUSTOMERS.md)
- [FIRST-20-CUSTOMERS.md](../02-ACTIONS/FIRST-20-CUSTOMERS.md)

### 03 — Architecture
- [SYSTEM-ARCHITECTURE.md](../03-ARCHITECTURE/SYSTEM-ARCHITECTURE.md)
- [APPLICATION-STRUCTURE.md](../03-ARCHITECTURE/APPLICATION-STRUCTURE.md)
- [DATA-FLOW.md](../03-ARCHITECTURE/DATA-FLOW.md)
- [WHATSAPP-INTEGRATION.md](../03-ARCHITECTURE/WHATSAPP-INTEGRATION.md)
- [WEBHOOK-ARCHITECTURE.md](../03-ARCHITECTURE/WEBHOOK-ARCHITECTURE.md)
- [BACKGROUND-JOBS.md](../03-ARCHITECTURE/BACKGROUND-JOBS.md)
- [MULTI-TENANCY.md](../03-ARCHITECTURE/MULTI-TENANCY.md)
- [MESSAGE-LEDGER.md](../03-ARCHITECTURE/MESSAGE-LEDGER.md)
- [AUTHENTICATION-AND-AUTHORIZATION.md](../03-ARCHITECTURE/AUTHENTICATION-AND-AUTHORIZATION.md)
- [SECURITY.md](../03-ARCHITECTURE/SECURITY.md)
- [BACKUP-AND-RECOVERY.md](../03-ARCHITECTURE/BACKUP-AND-RECOVERY.md)
- [SCALING-STRATEGY.md](../03-ARCHITECTURE/SCALING-STRATEGY.md)

### 04 — Database
- [DATABASE-DESIGN.md](../04-DATABASE/DATABASE-DESIGN.md)
- [ER-DIAGRAM.md](../04-DATABASE/ER-DIAGRAM.md)
- [TABLES.md](../04-DATABASE/TABLES.md)
- [INDEXES.md](../04-DATABASE/INDEXES.md)
- [MULTI-TENANT-DATABASE-RULES.md](../04-DATABASE/MULTI-TENANT-DATABASE-RULES.md)
- [DATABASE-MIGRATIONS.md](../04-DATABASE/DATABASE-MIGRATIONS.md)

### 05 — Backend
- [BACKEND-SETUP.md](../05-BACKEND/BACKEND-SETUP.md)
- [SPRING-BOOT-STRUCTURE.md](../05-BACKEND/SPRING-BOOT-STRUCTURE.md)
- [MODULES.md](../05-BACKEND/MODULES.md)
- [API-DESIGN.md](../05-BACKEND/API-DESIGN.md)
- [WEBHOOK-IMPLEMENTATION.md](../05-BACKEND/WEBHOOK-IMPLEMENTATION.md)
- [JOB-PROCESSING.md](../05-BACKEND/JOB-PROCESSING.md)
- [WHATSAPP-SERVICE.md](../05-BACKEND/WHATSAPP-SERVICE.md)
- [TEMPLATE-SERVICE.md](../05-BACKEND/TEMPLATE-SERVICE.md)
- [SCHEDULING.md](../05-BACKEND/SCHEDULING.md)
- [BILLING-LEDGER.md](../05-BACKEND/BILLING-LEDGER.md)
- [ERROR-HANDLING-AND-RETRY.md](../05-BACKEND/ERROR-HANDLING-AND-RETRY.md)

### 06 — Frontend
- [FRONTEND-SETUP.md](../06-FRONTEND/FRONTEND-SETUP.md)
- [APPLICATION-SCREENS.md](../06-FRONTEND/APPLICATION-SCREENS.md)
- [USER-FLOWS.md](../06-FRONTEND/USER-FLOWS.md)
- [DASHBOARD.md](../06-FRONTEND/DASHBOARD.md)
- [WHATSAPP-ONBOARDING.md](../06-FRONTEND/WHATSAPP-ONBOARDING.md)
- [AUTOMATION-CONFIGURATION.md](../06-FRONTEND/AUTOMATION-CONFIGURATION.md)
- [INBOX.md](../06-FRONTEND/INBOX.md)
- [ANALYTICS.md](../06-FRONTEND/ANALYTICS.md)

### 07 — Infrastructure
- [INFRASTRUCTURE-OVERVIEW.md](../07-INFRASTRUCTURE/INFRASTRUCTURE-OVERVIEW.md)
- [ORACLE-CLOUD-SETUP.md](../07-INFRASTRUCTURE/ORACLE-CLOUD-SETUP.md)
- [CLOUDFLARE-SETUP.md](../07-INFRASTRUCTURE/CLOUDFLARE-SETUP.md)
- [POSTGRES-SETUP.md](../07-INFRASTRUCTURE/POSTGRES-SETUP.md)
- [CADDY-SETUP.md](../07-INFRASTRUCTURE/CADDY-SETUP.md)
- [BACKUP-SETUP.md](../07-INFRASTRUCTURE/BACKUP-SETUP.md)
- [MONITORING.md](../07-INFRASTRUCTURE/MONITORING.md)
- [CI-CD.md](../07-INFRASTRUCTURE/CI-CD.md)
- [PRODUCTION-DEPLOYMENT.md](../07-INFRASTRUCTURE/PRODUCTION-DEPLOYMENT.md)

### 08 — Meta / WhatsApp
- [META-BUSINESS-SETUP.md](../08-META-WHATSAPP/META-BUSINESS-SETUP.md)
- [TECH-PROVIDER-SETUP.md](../08-META-WHATSAPP/TECH-PROVIDER-SETUP.md)
- [EMBEDDED-SIGNUP.md](../08-META-WHATSAPP/EMBEDDED-SIGNUP.md)
- [WHATSAPP-CLOUD-API.md](../08-META-WHATSAPP/WHATSAPP-CLOUD-API.md)
- [WEBHOOKS.md](../08-META-WHATSAPP/WEBHOOKS.md)
- [MESSAGE-TEMPLATES.md](../08-META-WHATSAPP/MESSAGE-TEMPLATES.md)
- [MESSAGE-CATEGORIES.md](../08-META-WHATSAPP/MESSAGE-CATEGORIES.md)
- [MESSAGE-PRICING.md](../08-META-WHATSAPP/MESSAGE-PRICING.md)
- [OCTOBER-2026-BILLING-CHANGE.md](../08-META-WHATSAPP/OCTOBER-2026-BILLING-CHANGE.md) ← **time-critical**

### 09 — Testing
- [TESTING-STRATEGY.md](../09-TESTING/TESTING-STRATEGY.md)
- [UNIT-TESTS.md](../09-TESTING/UNIT-TESTS.md)
- [INTEGRATION-TESTS.md](../09-TESTING/INTEGRATION-TESTS.md)
- [WHATSAPP-TESTING.md](../09-TESTING/WHATSAPP-TESTING.md)
- [SECURITY-TESTING.md](../09-TESTING/SECURITY-TESTING.md)
- [MULTI-TENANCY-TESTING.md](../09-TESTING/MULTI-TENANCY-TESTING.md)
- [PRE-PRODUCTION-CHECKLIST.md](../09-TESTING/PRE-PRODUCTION-CHECKLIST.md)

### 10 — Operations
- [CUSTOMER-ONBOARDING.md](../10-OPERATIONS/CUSTOMER-ONBOARDING.md)
- [CUSTOMER-SUPPORT.md](../10-OPERATIONS/CUSTOMER-SUPPORT.md)
- [INCIDENT-RESPONSE.md](../10-OPERATIONS/INCIDENT-RESPONSE.md)
- [MONITORING-AND-ALERTS.md](../10-OPERATIONS/MONITORING-AND-ALERTS.md)
- [BACKUP-RESTORE-PROCEDURE.md](../10-OPERATIONS/BACKUP-RESTORE-PROCEDURE.md)
- [PRODUCTION-RUNBOOK.md](../10-OPERATIONS/PRODUCTION-RUNBOOK.md)

### 11 — Security & compliance
- [SECURITY-REQUIREMENTS.md](../11-SECURITY-COMPLIANCE/SECURITY-REQUIREMENTS.md)
- [SECRETS-MANAGEMENT.md](../11-SECURITY-COMPLIANCE/SECRETS-MANAGEMENT.md)
- [CUSTOMER-DATA.md](../11-SECURITY-COMPLIANCE/CUSTOMER-DATA.md)
- [PRIVACY-CONSIDERATIONS.md](../11-SECURITY-COMPLIANCE/PRIVACY-CONSIDERATIONS.md)
- [DPDP-CONSIDERATIONS.md](../11-SECURITY-COMPLIANCE/DPDP-CONSIDERATIONS.md)

### 12 — Scaling
- [SCALING-0-20-CUSTOMERS.md](../12-SCALING/SCALING-0-20-CUSTOMERS.md)
- [SCALING-20-100-CUSTOMERS.md](../12-SCALING/SCALING-20-100-CUSTOMERS.md)
- [SCALING-100-1000-CUSTOMERS.md](../12-SCALING/SCALING-100-1000-CUSTOMERS.md)
- [WHEN-TO-INTRODUCE-REDIS.md](../12-SCALING/WHEN-TO-INTRODUCE-REDIS.md)
- [WHEN-TO-INTRODUCE-MESSAGE-BROKER.md](../12-SCALING/WHEN-TO-INTRODUCE-MESSAGE-BROKER.md)
- [WHEN-TO-INTRODUCE-MICROSERVICES.md](../12-SCALING/WHEN-TO-INTRODUCE-MICROSERVICES.md)
- [REVENUE-FUNDED-INFRASTRUCTURE.md](../12-SCALING/REVENUE-FUNDED-INFRASTRUCTURE.md)

### 13 — Decisions
- [DECISIONS.md](../13-DECISIONS/DECISIONS.md) — open questions & `[DECISION REQUIRED]` log
- [ADR-001-MODULAR-MONOLITH.md](../13-DECISIONS/ADR-001-MODULAR-MONOLITH.md)
- [ADR-002-POSTGRES-JOB-QUEUE.md](../13-DECISIONS/ADR-002-POSTGRES-JOB-QUEUE.md)
- [ADR-003-TECH-PROVIDER-MODEL.md](../13-DECISIONS/ADR-003-TECH-PROVIDER-MODEL.md)
- [ADR-004-MULTI-TENANCY.md](../13-DECISIONS/ADR-004-MULTI-TENANCY.md)
- [ADR-005-CUSTOMER-PAID-WHATSAPP-MESSAGING.md](../13-DECISIONS/ADR-005-CUSTOMER-PAID-WHATSAPP-MESSAGING.md)
- [ADR-006-SELF-HOSTED-POSTGRES.md](../13-DECISIONS/ADR-006-SELF-HOSTED-POSTGRES.md)
- [ADR-007-NO-AI-DEPENDENCY-FOR-CORE-AUTOMATION.md](../13-DECISIONS/ADR-007-NO-AI-DEPENDENCY-FOR-CORE-AUTOMATION.md)

---

## Golden Rules

Print these. Violating any one of them costs more to undo than to obey.

1. **Don't build what customers haven't asked for.** 20 paying customers with a simple product beats 0 customers with a beautiful one.
2. **Don't introduce infrastructure before it's needed.** No Redis, no Kafka, no Kubernetes, no microservices until a *specific* documented trigger fires. See `12-SCALING/`.
3. **Every database table must have `tenant_id`.** No exceptions, no "we'll add it later" tables. A single missing `tenant_id` is a cross-customer data leak and, in B2B, terminal.
4. **Never call the WhatsApp API synchronously inside a webhook request.** ACK in under 2 seconds, then enqueue.
5. **Never store WhatsApp access tokens in plaintext.** Encrypt at rest, never log, never return from an API.
6. **Never promise unlimited WhatsApp messaging.** Every outbound message maps to a real per-unit Meta cost billed to your customer.
7. **Don't send customer data to a free-tier AI API.** Google's free Gemini tier permits training on prompts.
8. **Keep the application stateless.** No in-memory sessions, no local disk writes. Horizontal scaling should be a config change, not a rewrite.
9. **Use Flyway from commit #1.** Never hand-edit production schema.
10. **Every important architectural decision gets an ADR.** If you can't write down why, you don't know why.
11. **Idempotency on every outbound send.** A duplicate message spends your customer's real money.
12. **Test the restore, not the backup.** An untested backup is not a backup.

---

## What this product is, in one paragraph

A WhatsApp automation SaaS for Indian SMBs. Each customer connects their **own**
WhatsApp Business Account through Meta's Embedded Signup inside our app. We operate as
a Meta **Tech Provider**, which means the customer owns their WABA and attaches their own
payment method to Meta — **Meta bills them directly for messages, and we bill them only a
flat software subscription.** Our margin is therefore immune to message volume and to
Meta's quarterly rate changes. We run on a single Oracle Always Free VM with a Spring Boot
modular monolith, self-hosted PostgreSQL, and a Postgres-backed job queue. Target: 10–20
paying customers before spending meaningful money on infrastructure.

---

## ⚠️ Read first: are you doing a pilot?

**[PILOT-FIRST-PLAN.md](PILOT-FIRST-PLAN.md)** — validate with 5–10 customers before registering
a business or waiting on Meta verification. Saves 4–5 weeks of waiting and ~₹2,000/month.
Where it conflicts with another document, it wins until the pilot is over.

Companions: [../02-ACTIONS/PILOT-PLAYBOOK.md](../02-ACTIONS/PILOT-PLAYBOOK.md) ·
[../08-META-WHATSAPP/PILOT-MODE-SETUP.md](../08-META-WHATSAPP/PILOT-MODE-SETUP.md) ·
[../13-DECISIONS/ADR-008-PILOT-BEFORE-BUSINESS-REGISTRATION.md](../13-DECISIONS/ADR-008-PILOT-BEFORE-BUSINESS-REGISTRATION.md)

---

## Navigation and progress

**[BUILD-LOG.html](../BUILD-LOG.html)** — double-click to open in a browser. Shows the one step
you're on, the exact docs for it, and a filterable index of all 124 files. Every path copies as
an `@` mention for Claude Code Desktop. Progress is saved in your browser.

---

## Newbie manual (start here if you've never done this before)

**[START-HERE-MANUAL.txt](../START-HERE-MANUAL.txt)** — plain-text, step-by-step, zero
assumptions, **now pilot-first (11 parts)**. Tells you exactly what action to take, which file to open, what to paste into
Claude Code, and every Facebook/Meta setup step at the point you need it. If you read one
thing, read that.

---

## 06 — Frontend

- [FRONTEND-SETUP.md](../06-FRONTEND/FRONTEND-SETUP.md)
- [APPLICATION-SCREENS.md](../06-FRONTEND/APPLICATION-SCREENS.md)
- [USER-FLOWS.md](../06-FRONTEND/USER-FLOWS.md)
- [DASHBOARD.md](../06-FRONTEND/DASHBOARD.md)
- [WHATSAPP-ONBOARDING.md](../06-FRONTEND/WHATSAPP-ONBOARDING.md)
- [AUTOMATION-CONFIGURATION.md](../06-FRONTEND/AUTOMATION-CONFIGURATION.md)
- [INBOX.md](../06-FRONTEND/INBOX.md)
- [ANALYTICS.md](../06-FRONTEND/ANALYTICS.md)

## 07 — Infrastructure

- [INFRASTRUCTURE-OVERVIEW.md](../07-INFRASTRUCTURE/INFRASTRUCTURE-OVERVIEW.md)
- [ORACLE-CLOUD-SETUP.md](../07-INFRASTRUCTURE/ORACLE-CLOUD-SETUP.md)
- [CLOUDFLARE-SETUP.md](../07-INFRASTRUCTURE/CLOUDFLARE-SETUP.md)
- [POSTGRES-SETUP.md](../07-INFRASTRUCTURE/POSTGRES-SETUP.md)
- [CADDY-SETUP.md](../07-INFRASTRUCTURE/CADDY-SETUP.md)
- [BACKUP-SETUP.md](../07-INFRASTRUCTURE/BACKUP-SETUP.md)
- [MONITORING.md](../07-INFRASTRUCTURE/MONITORING.md)
- [CI-CD.md](../07-INFRASTRUCTURE/CI-CD.md)
- [PRODUCTION-DEPLOYMENT.md](../07-INFRASTRUCTURE/PRODUCTION-DEPLOYMENT.md) — the full runbook

## 09 — Testing

- [TESTING-STRATEGY.md](../09-TESTING/TESTING-STRATEGY.md)
- [UNIT-TESTS.md](../09-TESTING/UNIT-TESTS.md)
- [INTEGRATION-TESTS.md](../09-TESTING/INTEGRATION-TESTS.md)
- [MULTI-TENANCY-TESTING.md](../09-TESTING/MULTI-TENANCY-TESTING.md) — **the most important one**
- [WHATSAPP-TESTING.md](../09-TESTING/WHATSAPP-TESTING.md)
- [SECURITY-TESTING.md](../09-TESTING/SECURITY-TESTING.md)
- [PRE-PRODUCTION-CHECKLIST.md](../09-TESTING/PRE-PRODUCTION-CHECKLIST.md)

## 10 — Operations

- [CUSTOMER-ONBOARDING.md](../10-OPERATIONS/CUSTOMER-ONBOARDING.md)
- [CUSTOMER-SUPPORT.md](../10-OPERATIONS/CUSTOMER-SUPPORT.md)
- [INCIDENT-RESPONSE.md](../10-OPERATIONS/INCIDENT-RESPONSE.md)
- [MONITORING-AND-ALERTS.md](../10-OPERATIONS/MONITORING-AND-ALERTS.md)
- [BACKUP-RESTORE-PROCEDURE.md](../10-OPERATIONS/BACKUP-RESTORE-PROCEDURE.md) — includes the restore-test log
- [PRODUCTION-RUNBOOK.md](../10-OPERATIONS/PRODUCTION-RUNBOOK.md)

## 11 — Security and compliance

- [SECURITY-REQUIREMENTS.md](../11-SECURITY-COMPLIANCE/SECURITY-REQUIREMENTS.md)
- [SECRETS-MANAGEMENT.md](../11-SECURITY-COMPLIANCE/SECRETS-MANAGEMENT.md)
- [CUSTOMER-DATA.md](../11-SECURITY-COMPLIANCE/CUSTOMER-DATA.md)
- [PRIVACY-CONSIDERATIONS.md](../11-SECURITY-COMPLIANCE/PRIVACY-CONSIDERATIONS.md)
- [DPDP-CONSIDERATIONS.md](../11-SECURITY-COMPLIANCE/DPDP-CONSIDERATIONS.md)

## 12 — Scaling

- [SCALING-0-20-CUSTOMERS.md](../12-SCALING/SCALING-0-20-CUSTOMERS.md)
- [SCALING-20-100-CUSTOMERS.md](../12-SCALING/SCALING-20-100-CUSTOMERS.md)
- [SCALING-100-1000-CUSTOMERS.md](../12-SCALING/SCALING-100-1000-CUSTOMERS.md)
- [WHEN-TO-INTRODUCE-REDIS.md](../12-SCALING/WHEN-TO-INTRODUCE-REDIS.md)
- [WHEN-TO-INTRODUCE-MESSAGE-BROKER.md](../12-SCALING/WHEN-TO-INTRODUCE-MESSAGE-BROKER.md)
- [WHEN-TO-INTRODUCE-MICROSERVICES.md](../12-SCALING/WHEN-TO-INTRODUCE-MICROSERVICES.md)
- [REVENUE-FUNDED-INFRASTRUCTURE.md](../12-SCALING/REVENUE-FUNDED-INFRASTRUCTURE.md)

---

## 14 — Building with Claude Code

- [README.md](../14-CLAUDE-CODE/README.md) — how to use this section
- [CLAUDE-CODE-SETUP.md](../14-CLAUDE-CODE/CLAUDE-CODE-SETUP.md) — one-time setup
- [PROJECT-CLAUDE-MD-TEMPLATE.md](../14-CLAUDE-CODE/PROJECT-CLAUDE-MD-TEMPLATE.md) — copy to repo root as `CLAUDE.md`
- [FEATURE-BREAKDOWN.md](../14-CLAUDE-CODE/FEATURE-BREAKDOWN.md) — the 23 increments
- [WORKFLOW.md](../14-CLAUDE-CODE/WORKFLOW.md) — the per-increment loop
- [IMPLEMENTATION-DOC-TEMPLATE.md](../14-CLAUDE-CODE/IMPLEMENTATION-DOC-TEMPLATE.md) — every increment documents itself
- [PROMPTS/PHASE-A-FOUNDATION.md](../14-CLAUDE-CODE/PROMPTS/PHASE-A-FOUNDATION.md) — F00–F04
- [PROMPTS/PHASE-B-WHATSAPP.md](../14-CLAUDE-CODE/PROMPTS/PHASE-B-WHATSAPP.md) — F05–F11
- [PROMPTS/PHASE-C-AUTOMATION.md](../14-CLAUDE-CODE/PROMPTS/PHASE-C-AUTOMATION.md) — F12–F16
- [PROMPTS/PHASE-D-FRONTEND.md](../14-CLAUDE-CODE/PROMPTS/PHASE-D-FRONTEND.md) — F17–F20
- [PROMPTS/PHASE-E-PRODUCTION.md](../14-CLAUDE-CODE/PROMPTS/PHASE-E-PRODUCTION.md) — F21–F23
- [PROMPTS/REUSABLE-PROMPTS.md](../14-CLAUDE-CODE/PROMPTS/REUSABLE-PROMPTS.md) — review, debug, test, scope-check
