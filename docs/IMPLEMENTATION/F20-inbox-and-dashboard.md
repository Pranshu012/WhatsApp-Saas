# F20 — Inbox and Dashboard Screens

**Status:** Complete
**Completed:** 2026-08-22
**Commit:** Pending
**Spec:** `docs/WhatsApp-SaaS-Product/14-CLAUDE-CODE/PROMPTS/PHASE-D-FRONTEND.md#f20`

## What this does
Implements the customer Inbox with 24-hour service window tracking and manual free-text replies, the Workspace Dashboard reporting per-tenant monthly message usage across all Meta billing categories and delivery outcomes directly from the immutable message ledger, and the Scheduled Broadcasts management screen.

## Files
| File | Purpose |
|---|---|
| `src/main/java/com/example/wasaas/dashboard/DashboardController.java` | REST endpoint for monthly billing category counts and delivery outcome analytics (`/api/dashboard/stats`) |
| `src/main/java/com/example/wasaas/ledger/StatusOutcomeCount.java` | Projection interface for delivery status breakdown aggregation |
| `src/main/java/com/example/wasaas/ledger/MessageLedgerRepository.java` | Added query `countByStatusForDateRange` for delivery outcomes |
| `frontend/src/api/types.ts` | Added DTO types for Dashboard, Conversations, Ledger messages, and Scheduled messages |
| `frontend/src/features/dashboard/DashboardScreen.tsx` | Analytics dashboard displaying current month volume, category counts, delivery rates, and Meta billing clarity |
| `frontend/src/features/inbox/InboxScreen.tsx` | Mobile-first inbox thread view with 24-hour service window countdown, status ticks, and manual replies |
| `frontend/src/features/scheduled/ScheduledMessagesScreen.tsx` | Scheduled broadcasts manager with timezone preservation and honest cancellation |
| `frontend/src/App.tsx` | Mounted routes for `/`, `/inbox`, and `/scheduled` |

## Database changes
- None. (Consumes existing `message_ledger`, `conversations`, `contacts`, and `scheduled_messages` tables from F08, F11, and F16).

## API surface
| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/dashboard/stats` | Session | Aggregate monthly message volume by category and delivery status |
| GET | `/api/conversations` | Session | List conversations with 24-hour service window status |
| GET | `/api/conversations/{id}/messages` | Session | Retrieve conversation message history from ledger |
| POST | `/api/conversations/{id}/reply` | Session | Send manual free-text reply inside active 24h window |
| GET | `/api/scheduled-messages` | Session | List scheduled message broadcasts |
| POST | `/api/scheduled-messages` | Session | Schedule a new message broadcast |
| DELETE | `/api/scheduled-messages/{id}` | Session | Cancel a pending scheduled broadcast |

## Key decisions and why
- **24-Hour Service Window Safeguard:** The inbox prominently tracks the countdown (`serviceWindowExpiresAt`). When the 24-hour window is closed, free-text inputs are disabled and replaced with a clear banner explaining that Meta requires an approved template, preventing confusing delivery rejections.
- **Authoritative Meta Billing Notice:** Dashboard displays a permanent notice emphasizing that our platform counts reflect internal immutable ledger logs for merchant reference, whereas Meta's official invoice in WhatsApp Manager is authoritative.
- **Interval Polling Without WebSockets:** Real-time updates use lightweight interval polling (10s on conversation list, 5s on active thread, 15s on dashboard) avoiding premature WebSocket complexity.
- **One-Handed 360px Mobile Usability:** On mobile devices (<768px), the inbox smoothly transitions between the conversation list and message thread with a single tap, with touch targets $\ge 44\text{px}$.

## Divergence from the architecture docs
- None. Followed specifications from `PHASE-D-FRONTEND.md#f20`.
