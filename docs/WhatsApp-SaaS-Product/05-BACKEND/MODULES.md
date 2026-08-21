# Module Contracts

**Classification: BUILD NOW (F03).** Layout in `03-ARCHITECTURE/APPLICATION-STRUCTURE.md`.

For each module: what it owns, what it exposes, what it may depend on.

---

## `common`
**Owns:** exceptions, `ApiError`, `GlobalExceptionHandler`, `RequestIdFilter`, log scrubbing,
phone-number utilities, `Clock`.
**Exposes:** everything (it's the shared base).
**Depends on:** **nothing.** If `common` imports a feature package, the design has broken.

## `tenant`
**Owns:** `tenants`, `TenantContext`, `TenantContextFilter`, the transaction-local
`app.tenant_id` hook.
**Exposes:** `TenantService`, `TenantContext.require()`.
**Depends on:** `common`.
**Never:** reads business data from other modules.

## `user`
**Owns:** `users`, `tenant_users`, roles.
**Exposes:** `UserService`, `TenantUserService`.
**Depends on:** `tenant`, `common`.

## `auth`
**Owns:** `SecurityConfig`, login/logout, password reset, login rate limiting.
**Exposes:** REST endpoints only — no service consumed by other modules.
**Depends on:** `user`, `tenant`, `common`.

## `job`
**Owns:** `jobs`, `JobWorker`, `JobService`, the `JobHandler` interface.
**Exposes:** `JobService.enqueue(type, payload, idempotencyKey)`, the `JobHandler` interface.
**Depends on:** `common`.
**Critical:** `job` must not depend on `whatsapp` or `messaging` — handlers live in
`job.handler` and depend *downward* on those modules. This keeps the queue generic.

## `ledger`
**Owns:** `message_ledger`, `message_ledger_status_events`, `whatsapp_rates`, `BillingCategory`.
**Exposes:** `LedgerService` — `recordOutboundIntent`, `attachWamid`, `recordFailure`,
`recordStatusEvent`, `countByCategoryForMonth`.
**Depends on:** `tenant`, `common`.
**Rule:** append-only. No public method mutates a ledger row except the documented status update.

## `whatsapp`
**Owns:** `whatsapp_accounts`, token encryption, Embedded Signup, `MetaGraphClient`,
`WhatsAppCloudClient`, the webhook receiver.
**Exposes:** `WhatsAppAccountService`, `WhatsAppCloudClient` (**restricted — see below**).
**Depends on:** `tenant`, `job`, `ledger`, `common`.

> **Hard rule: only classes in `job.handler` may call `WhatsAppCloudClient`.**
> Everything else goes through `MessagingService`, which enqueues. Enforce with an ArchUnit test —
> this is the rule most likely to be violated by accident, and the consequence is a slow
> webhook or a duplicate charge.

## `messaging`
**Owns:** `contacts`, `conversations`, inbound processing.
**Exposes:** `MessagingService.send(...)` — **the only public send entry point.** It enqueues,
never calls Meta.
**Depends on:** `whatsapp`, `job`, `ledger`, `tenant`, `common`.
**Publishes:** `InboundMessageReceived`.

## `template`
**Owns:** `templates`, Meta sync.
**Exposes:** `TemplateService`, `TemplateSyncService`.
**Depends on:** `whatsapp`, `tenant`, `common`.
**Rule:** Meta is authoritative for status and category. A local edit never overrides Meta.

## `automation`
**Owns:** `automation_rules`, `faqs`, `unmatched_messages`, `AutomationEngine`, `RuleMatcher`,
`FaqMatchService`.
**Exposes:** REST endpoints for configuration.
**Depends on:** `messaging`, `template`, `tenant`, `common`.
**Consumes:** `InboundMessageReceived`.
**Rule:** no LLM calls (ADR-007). Below the confidence threshold → escalate, never guess.

## `scheduling`
**Owns:** `scheduled_messages`.
**Exposes:** `SchedulingService`.
**Depends on:** `messaging`, `job`, `template`, `tenant`, `common`.

## `billing`
**Owns:** `subscriptions`, `payment_events`, Razorpay integration.
**Exposes:** `SubscriptionService.isActive(tenantId)` — used for feature gating.
**Depends on:** `tenant`, `job`, `common`.
**Rule:** state changes only from **verified** webhooks. Never from a client callback.

## `inbox`
**Owns:** nothing. Read-only queries over `messaging` + `ledger`.
**Depends on:** `messaging`, `ledger`, `tenant`, `common`.

## `analytics`
**Owns:** nothing. Read-only queries over `ledger`.
**Depends on:** `ledger`, `tenant`, `common`.

---

## Cross-module communication

**Allowed:**
```java
// public service interface
messagingService.send(new SendCommand(...));

// Spring event, for decoupling
eventPublisher.publishEvent(new InboundMessageReceived(tenantId, wamid, text));
```

**Forbidden:**
```java
contactRepository.findByPhone(...);          // another module's repository
Contact c = someOtherModule.getEntity();    // another module's entity across a boundary
```

## Events

| Event | Published by | Consumed by |
|---|---|---|
| `InboundMessageReceived` | `messaging` | `automation` |
| `WhatsAppAccountConnected` | `whatsapp` | `template` (trigger sync), `audit` |
| `MessageDeliveryFailed` | `ledger` | `inbox` (surface to user) |
| `SubscriptionStatusChanged` | `billing` | gating checks, `audit` |

Use events where a cycle would otherwise form. `automation → messaging` is a direct call;
`messaging → automation` must be an event, or you get a dependency cycle.

## Enforcing this

Add **one** of these in F03 — an hour of work that prevents months of decay:

- **Spring Modulith** — declare modules, verify boundaries in a test
- **ArchUnit** — a handful of rules covering the dependency table and the
  `WhatsAppCloudClient` restriction

Without enforcement, these contracts are decoration. With it, a violation fails the build.
