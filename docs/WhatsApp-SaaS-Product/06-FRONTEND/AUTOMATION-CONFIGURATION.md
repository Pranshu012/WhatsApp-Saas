# Automation Configuration Screens

Increment **F19**. Routes `/automation`, `/faq`, `/templates`.

**Design test:** a 45-year-old shop owner, on a phone, with no technical background, adds a
working auto-reply without calling you. If they'd need to call you, redesign.

## Automation rules — `/automation`

### List

Rows in priority order. Each row: name, plain-language match summary, enabled toggle, up/down
reorder, edit.

Plain-language summary examples:
- `EXACT` "price" → *When the message is exactly "price"*
- `CONTAINS` "timing" → *When the message contains "timing"*
- `STARTS_WITH` "order" → *When the message starts with "order"*

Never show the enum. Use up/down buttons rather than drag-and-drop — drag is fragile on touch
and the list is short.

### Editor

```text
Rule name        [ Price enquiry                    ]

When a message   [ contains          ▾ ]
                 [ price                            ]
                 ☐ Match uppercase/lowercase exactly

Then             ( ) Send a text message
                 ( ) Send an approved template
                 ( ) Send buttons
                 (•) Hand over to a human

Message          [ Our price list: ...              ]

┌─ Test it ─────────────────────────────────┐
│ [ what is the price?           ] [Test]   │
│ ✅ This rule would match                  │
└───────────────────────────────────────────┘
```

The test box is the feature that teaches people how matching works. Do not skip it.

### Regex

Hide behind an "Advanced" toggle with a warning. Most users should never see it. Validate
server-side at save time with a compile timeout and reject catastrophic patterns — a
tenant-supplied regex is untrusted input.

### Cost warning

If a rule's action would produce more than one outbound message, warn inline:

> This rule sends 3 messages. Meta bills your customer per message — about ₹0.35 total per
> trigger. Consider combining them into one.

## FAQ — `/faq`

Simple list of question/answer pairs: add, edit, delete, enable.

**The tester is the point:**

```text
┌─ Test a question ─────────────────────────────┐
│ [ wat r ur timings              ] [Test]      │
│                                               │
│ ✅ Matched (87% confidence)                   │
│    "What are your business hours?"            │
│    → "We're open 10am to 8pm, Mon-Sat"        │
└───────────────────────────────────────────────┘
```

Show the confidence number. When it's below threshold, say so and explain the fix:

> ⚠️ No confident match (41%). This question would be handed to a human. Add it as an FAQ, or
> reword an existing one to include words your customers actually use.

Matching is Postgres full-text + trigram similarity — deterministic, explainable, no AI
(ADR-007). The tester makes that transparency an asset rather than a limitation.

## Templates — `/templates`

List with two badges per template: **Meta status** and **Meta category**.

| Status | Badge | Meaning shown to user |
|---|---|---|
| APPROVED | green | Ready to use |
| PENDING | amber | Meta is reviewing — usually under 24 hours |
| REJECTED | red | Show Meta's rejection reason verbatim + how to fix |
| PAUSED | amber | Too many blocks/reports — Meta paused it |
| DISABLED | grey | Cannot be used |

**Category cost warning** — mandatory on MARKETING templates:

> ⚠️ Meta classified this as **Marketing**: about ₹0.86 per message versus ₹0.115 for Utility
> — roughly 7.5× the cost. Marketing has no volume discounts in India. If this message is a
> transactional update (order confirmation, delivery status, OTP), reword it to remove
> promotional language and resubmit as Utility.

Meta assigns the final category, not the customer, and Meta's assignment always wins in your
data. Show what Meta decided, and flag when it differs from what was requested.

## Every screen — the four states

| State | Requirement |
|---|---|
| Loading | Skeleton rows, never a bare spinner on a list |
| Empty | Explain what this is + one concrete button. Never "No data." |
| Error | Plain language + Retry. Never a stack trace or error code alone. |
| Success | Inline confirmation that fades. No modal. |

Good empty state:

> **No auto-replies yet.** Auto-replies answer common questions instantly, even at 2am.
> Most businesses start with their price list or opening hours.
> [ Create your first auto-reply ]
