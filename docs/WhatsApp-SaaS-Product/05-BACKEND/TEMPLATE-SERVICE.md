# Template Service

**Classification: BUILD NOW (F12).** Meta concepts in `08-META-WHATSAPP/MESSAGE-TEMPLATES.md`.

## Why templates exist

Outside the 24-hour customer service window you **cannot** send free-form text. You must use a
template that Meta has pre-approved. Templates are also where the billing category is decided —
and category drives cost by a factor of 7.5 in India.

## Meta is the source of truth

```text
Meta owns:  status (PENDING/APPROVED/REJECTED/PAUSED/DISABLED)
            category (MARKETING/UTILITY/AUTHENTICATION)
            rejection reason

We own:     which template a rule or schedule uses
            variable mapping
```

**Meta's category assignment overrides what you requested.** You may submit a template as
UTILITY and Meta may classify it MARKETING — at 7.5× the cost, charged to your customer. So:

1. Store both `requested_category` and Meta's assigned `category`
2. **Warn visibly when they differ** — this is real money
3. Never let a local edit overwrite Meta's values on sync

## Sync

```java
@Component
class SyncTemplatesHandler implements JobHandler {
    public String jobType() { return "SYNC_TEMPLATES"; }

    public void handle(Job job) {
        var account = accountService.require(tenantOf(job));
        List<MetaTemplate> remote = graphClient.listTemplates(account);
        // upsert on (tenant_id, name, language); Meta's status and category win
        templateService.upsertAll(account, remote);
    }
}
```

Triggered:
- On `WhatsAppAccountConnected`
- Daily (idempotency key `tplsync:{wabaId}:{yyyy-MM-dd}` so a duplicate trigger is a no-op)
- Manually from the UI
- On a template status webhook, if subscribed

## Submitting

```java
public WhatsAppTemplate submit(CreateTemplateCommand cmd) {
    var account = accountService.require(TenantContext.require());
    String metaId = graphClient.createTemplate(account, cmd);
    return repository.save(new WhatsAppTemplate(
        TenantContext.require(), account.id(), metaId,
        cmd.name(), cmd.language(),
        cmd.requestedCategory(),        // what we asked for
        null,                           // Meta's assignment — arrives on sync
        TemplateStatus.PENDING, cmd.bodyText(), cmd.components()));
}
```

Approval is asynchronous — minutes to hours. The UI must show `PENDING` honestly rather than
implying the template is usable.

## Guard before sending

```java
public void assertSendable(WhatsAppTemplate t) {
    if (t.status() != APPROVED) {
        throw new PermanentJobException(
            "Template '" + t.name() + "' is " + t.status() + ", not APPROVED");
    }
}
```

Check **before** the API call. Meta would reject it anyway, but a local check gives a clear
message, avoids a wasted API call, and doesn't count against quality metrics.

Also validate variable count locally: a template with 3 placeholders sent with 2 values fails
with Meta error 132000. Catching that locally is a much better experience.

## Variable substitution

Meta templates use positional placeholders (`{{1}}`, `{{2}}`). Store `variable_count` on sync and
validate at send time. Names would be friendlier but Meta is positional, so don't invent an
abstraction that has to be unwound later.

## Rejection handling

Store and **display** `rejection_reason`. Common causes worth documenting for customers:

| Reason | Fix |
|---|---|
| Promotional content in a UTILITY template | Rewrite, or accept MARKETING pricing |
| Placeholder at the start or end of the body | Add surrounding text |
| Unclear or generic content | Be specific about the transaction |
| Policy violation | Read Meta's commerce and messaging policies |

## Test cases

| Test | Expect |
|---|---|
| Sync upserts | No duplicates on repeated sync |
| Meta category differs from requested | Both stored, warning surfaced |
| Sync does not overwrite with local values | Meta wins |
| Non-APPROVED template send | Rejected before any API call |
| Variable count mismatch | Rejected locally with a clear message |
| Rejected template | Reason stored and returned by the API |
| Duplicate sync same day | Idempotency key → one job |

## DO NOT BUILD YET

A visual template designer · template version history · A/B testing · automatic
re-categorisation appeals · multi-language variants of the same template beyond storage.
