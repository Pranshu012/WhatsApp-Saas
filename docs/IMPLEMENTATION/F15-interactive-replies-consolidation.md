# F15 — Interactive Replies and Reply Consolidation

**Status:** Complete  
**Completed:** 2026-08-21  
**Spec:** ../WhatsApp-SaaS-Product/14-CLAUDE-CODE/PROMPTS/PHASE-C-AUTOMATION.md#f15

## What this does
Enables WhatsApp interactive messaging (quick reply buttons up to 3 buttons, and interactive list menus with sections and rows) via Meta Cloud API. Extracts button and list selections from inbound webhooks and pipes them into the keyword automation and FAQ matching engines. Introduces `ReplyBuilder` to consolidate multi-part responses into single outbound messages, saving up to 3x in customer Meta billing fees, and provides `ConsolidationValidator` to warn when multi-message actions would increase message costs.

## Files
| File | Purpose |
|---|---|
| `src/main/java/com/example/wasaas/whatsapp/client/ReplyButton.java` | DTO representing quick reply buttons for Meta Cloud API |
| `src/main/java/com/example/wasaas/whatsapp/client/ListRow.java` | DTO representing an item in an interactive list |
| `src/main/java/com/example/wasaas/whatsapp/client/ListSection.java` | DTO representing a section of list rows |
| `src/main/java/com/example/wasaas/whatsapp/client/WhatsAppCloudClient.java` | Added `sendInteractiveButtons` and `sendInteractiveList` |
| `src/main/java/com/example/wasaas/whatsapp/inbound/ProcessWebhookEventHandler.java` | Parses inbound `interactive` (`button_reply`, `list_reply`, `button`) messages |
| `src/main/java/com/example/wasaas/automation/ReplyBuilder.java` | Consolidates multi-line response fragments into a single message |
| `src/main/java/com/example/wasaas/automation/ConsolidationValidator.java` | Warns against unconsolidated multi-message actions to reduce billing costs |
| `src/test/java/com/example/wasaas/whatsapp/interactive/InteractiveMessagingTest.java` | 5 integration tests covering buttons, lists, webhook parsing, ReplyBuilder, and consolidation warning |

## Key decisions and why
- **Reply Consolidation for Cost Optimization:** Under Meta's per-message billing (effective July 2025 and October 2026), sending three short sequential messages costs ~₹0.345 vs ~₹0.115 for one consolidated message. `ReplyBuilder` consolidates header, body snippets, and footer into one payload.
- **Inbound Interactive Attribution:** Inbound `interactive` webhooks containing `button_reply` (or `list_reply`) extract the selected title/id and inject it into `InboundMessageReceivedEvent.text()`. This allows existing keyword rules (F13) and FAQ search (F14) to seamlessly match button clicks without duplicating engine logic.

## Divergence from the architecture docs
None.

## Test coverage
- `testSendInteractiveButtonsPayloadFormat`: Outbound 2-button interactive message JSON payload formatted according to Meta spec.
- `testSendInteractiveListPayloadFormat`: Outbound sections & rows interactive list message JSON payload formatted according to Meta spec.
- `testInboundButtonReplyProcessedAndMatchesAutomationRule`: Inbound `button_reply` webhook parsed and mapped to matching automation rule.
- `testReplyBuilderConsolidatesMultipleSnippetsIntoSingleSend`: Multiple text snippets joined into a single formatted multi-line message.
- `testMultiMessageWarningGeneratedOnUnconsolidatedActions`: Validation warning emitted when rule defines multiple disjoint messages.
