# F13 — Keyword Automation Rules

**Status:** Complete  
**Completed:** 2026-08-21  
**Spec:** ../WhatsApp-SaaS-Product/14-CLAUDE-CODE/PROMPTS/PHASE-C-AUTOMATION.md#f13

## What this does
Matches inbound WhatsApp messages against tenant-configured automation rules using `EXACT`, `CONTAINS`, `STARTS_WITH`, and `REGEX` match conditions. Enforces priority ordering with a "first match wins" strategy, dispatches auto-replies through `MessagingService`'s durable queue, throttles per-contact replies using a sliding window to prevent reply loops, validates untrusted tenant regexes with a timeout to prevent ReDoS attacks, and logs all unmatched messages as an empirical dataset (per ADR-007).

## Files
| File | Purpose |
|---|---|
| `src/main/resources/db/migration/V14__automation_rules.sql` | `automation_rules` and `unmatched_messages` tables with RLS |
| `src/main/java/com/example/wasaas/automation/MatchType.java` | Match types enum (`EXACT`, `CONTAINS`, `STARTS_WITH`, `REGEX`) |
| `src/main/java/com/example/wasaas/automation/ActionType.java` | Action types enum (`SEND_TEXT`, `SEND_TEMPLATE`, `SEND_INTERACTIVE`, `ESCALATE`) |
| `src/main/java/com/example/wasaas/automation/AutomationRule.java` | Rule entity with priority ordering and JSONB action payloads |
| `src/main/java/com/example/wasaas/automation/AutomationRuleRepository.java` | Repository querying enabled rules ordered by priority ASC |
| `src/main/java/com/example/wasaas/automation/UnmatchedMessage.java` | Entity logging unmatched inbound messages for analysis |
| `src/main/java/com/example/wasaas/automation/UnmatchedMessageRepository.java` | Repository for unmatched messages |
| `src/main/java/com/example/wasaas/automation/UnmatchedMessageEvent.java` | Spring application event published on unmatched messages |
| `src/main/java/com/example/wasaas/automation/RegexValidator.java` | ReDoS pattern detector and 50ms timeout compilation sandbox |
| `src/main/java/com/example/wasaas/automation/AutoReplyRateLimiter.java` | Per-contact sliding window rate limiter (max 5 auto-replies/hour) |
| `src/main/java/com/example/wasaas/automation/RuleMatcher.java` | Evaluates rule conditions against inbound message texts |
| `src/main/java/com/example/wasaas/automation/AutomationRuleService.java` | CRUD service for rules with regex safety validation |
| `src/main/java/com/example/wasaas/automation/AutomationEngine.java` | `@EventListener` consuming `InboundMessageReceivedEvent` and firing actions |
| `src/test/java/com/example/wasaas/automation/AutomationEngineTest.java` | 7 integration tests covering matchers, priority, ReDoS rejection, loop throttling, and multi-tenant isolation |

## Database changes
- Table `automation_rules`:
  - `id`: UUID Primary Key
  - `tenant_id`: UUID NOT NULL referencing `tenants(id)`
  - `name`: VARCHAR(255) NOT NULL
  - `enabled`: BOOLEAN NOT NULL DEFAULT true
  - `match_type`: VARCHAR(50) NOT NULL
  - `match_value`: TEXT NOT NULL
  - `case_sensitive`: BOOLEAN NOT NULL DEFAULT false
  - `priority`: INT NOT NULL DEFAULT 100
  - `action_type`: VARCHAR(50) NOT NULL
  - `action_payload`: JSONB NOT NULL
  - Index: `idx_automation_rules_tenant_priority` on `(tenant_id, enabled, priority ASC)`
  - RLS Policy: `automation_rules_tenant_isolation`
- Table `unmatched_messages`:
  - `id`: UUID Primary Key
  - `tenant_id`: UUID NOT NULL referencing `tenants(id)`
  - `whatsapp_account_id`: UUID REFERENCES `whatsapp_accounts(id)`
  - `contact_id`: UUID REFERENCES `contacts(id)`
  - `sender_phone`: VARCHAR(50) NOT NULL
  - `message_text`: TEXT NOT NULL
  - `wamid`: VARCHAR(128)
  - `received_at`: TIMESTAMPTZ NOT NULL DEFAULT now()
  - Index: `idx_unmatched_messages_tenant` on `(tenant_id, received_at DESC)`
  - RLS Policy: `unmatched_messages_tenant_isolation`
- Migration Version: `V14`

## Key decisions and why
- **First Match Wins:** Rules are ordered by `priority ASC` (`priority=1` executes before `priority=10`). As soon as the first rule matches, its action is triggered and rule evaluation terminates immediately.
- **Untrusted Regex Sandboxing:** Tenant-supplied regular expressions are checked for nested quantifiers (`(a+)+`, `(a*)*`, etc.) and tested against adversarial benchmark strings with a 50ms timeout. Dangerous patterns are rejected at rule save time with HTTP 400.
- **Per-Contact Auto-Reply Limiting:** To prevent infinite ping-pong loops between bots or runaway billing, auto-replies are capped at 5 per hour per contact using a sliding window limiter.
- **No LLM Fallback (ADR-007):** Unmatched messages are stored in `unmatched_messages` as an empirical dataset for future analysis rather than sending unpredictable or ungrounded generative AI guesses.

## Divergence from the architecture docs
None.

## Test coverage
- `testExactMatchTriggersAutoReply`: `EXACT` case-insensitive matching triggers auto-reply.
- `testContainsAndStartsWithAndRegexMatchRules`: `CONTAINS`, `STARTS_WITH`, and `REGEX` patterns match and dispatch jobs.
- `testPriorityOrderingFirstMatchWins`: When multiple rules match, only the lowest priority number rule fires.
- `testNoMatchLogsUnmatchedMessage`: Messages matching no rules are saved to `unmatched_messages` and publish `UnmatchedMessageEvent`.
- `testCatastrophicRegexRejectedAtSave`: ReDoS patterns like `(a+)+$` are rejected with `DomainException` at save time.
- `testPerContactRateLimitPreventsReplyStorm`: Rapid incoming messages from the same sender are capped at 5 auto-replies per hour.
- `testMultiTenantRuleIsolation`: Tenant A rules never trigger for Tenant B's incoming messages.
