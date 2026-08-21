# Application Screens

Complete MVP screen inventory. If a screen isn't here, it isn't in the MVP.

## Public

| Screen | Route | Increment | Notes |
|---|---|---|---|
| Login | `/login` | F17 | Generic failure message; never reveal if email exists |
| Register | `/register` | F17 | Business name + email + password. Nothing else. |
| Forgot password | `/forgot-password` | F17 | Always shows success |
| Reset password | `/reset-password?token=` | F17 | Single-use token |

## Authenticated

| Screen | Route | Increment | Purpose |
|---|---|---|---|
| Dashboard | `/` | F20 | Message counts by category, delivery outcomes, health |
| WhatsApp Connection | `/whatsapp` | F18 | Embedded Signup, connection health, payment warning |
| Automation Rules | `/automation` | F19 | List, create, edit, reorder, enable/disable |
| Rule editor | `/automation/:id` | F19 | Match + action + live preview |
| FAQ | `/faq` | F19 | Q&A pairs + "test a question" box |
| Templates | `/templates` | F19 | Meta status/category badges, submit new |
| Inbox | `/inbox` | F20 | Conversation list |
| Conversation | `/inbox/:id` | F20 | Thread + manual reply + window countdown |
| Scheduled messages | `/scheduled` | F20 | List, cancel |
| Settings — business | `/settings` | F19 | Business name, timezone, GSTIN |
| Settings — billing | `/settings/billing` | F21 | Plan, status, invoices, payment method |
| Unmatched messages | `/unmatched` | F19 | Questions the bot couldn't answer |

**`/unmatched` is quietly the most valuable screen.** It tells the customer exactly which
FAQ to add next, and it tells you whether deterministic matching is actually enough (ADR-007).

## Screen anatomy — the standard every screen follows

```text
┌──────────────────────────────────────────┐
│ Header: page title + primary action      │
├──────────────────────────────────────────┤
│ Warning banner (if any) — e.g. no        │
│ payment method on Meta, subscription     │
│ past due                                 │
├──────────────────────────────────────────┤
│ Content:                                 │
│   loading → skeleton                     │
│   empty   → what this is + first action  │
│   error   → plain message + Retry        │
│   loaded  → the thing                    │
└──────────────────────────────────────────┘
```

## Deliberately not in the MVP

Team/user management · agent assignment · tags and segments · CSV import/export ·
campaign builder · analytics charts beyond counts · notification preferences ·
audit-log viewer · API keys for customers · white-labelling · dark mode.

Each of these is a real feature someone will eventually want. None of them is why a customer
would pay you in month one.
