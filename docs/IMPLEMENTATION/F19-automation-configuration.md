# F19 — Automation Configuration Screens

**Status:** Complete
**Completed:** 2026-08-22
**Commit:** Pending
**Spec:** `docs/WhatsApp-SaaS-Product/14-CLAUDE-CODE/PROMPTS/PHASE-D-FRONTEND.md#f19`

## What this does
Provides self-serve configuration interfaces for SMB business owners across four distinct areas: Keyword Automation Rules (`/automation`), FAQ Knowledge Base with Live Typo Tester (`/faq`), Meta WhatsApp Templates with Status/Category badges, Sync, and Cost Warnings (`/templates`), and Unmatched Inquiries (`/unmatched`).

## Files
| File | Purpose |
|---|---|
| `src/main/java/com/example/wasaas/automation/UnmatchedMessageController.java` | REST endpoints for listing and dismissing unmatched inquiries (`/api/unmatched-messages`) |
| `frontend/src/api/types.ts` | TypeScript interfaces for Rules, FAQs, Templates, and Unmatched inquiries |
| `frontend/src/features/automation/AutomationRulesScreen.tsx` | Keyword rule manager with priority ranking, regex sandboxing toggle, and live rule tester |
| `frontend/src/features/faq/FaqScreen.tsx` | FAQ knowledge base manager with interactive typo and full-text confidence score tester |
| `frontend/src/features/templates/TemplatesScreen.tsx` | Meta template catalog with status badges, sync trigger, new template modal, and 7.5× marketing cost warning |
| `frontend/src/features/unmatched/UnmatchedMessagesScreen.tsx` | Unhandled customer inquiry viewer with one-click "Turn into FAQ" workflow |
| `frontend/src/App.tsx` | Routes mounted for `/automation`, `/faq`, `/templates`, and `/unmatched` |

## Database changes
- None. (Consumes existing `automation_rules`, `faqs`, `whatsapp_templates`, `unmatched_messages` tables from F12–F14).

## API surface
| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/automation-rules` | Session | List keyword automation rules |
| POST | `/api/automation-rules` | Session | Create new automation rule |
| POST | `/api/automation-rules/test` | Session | Test live rule matching with 50ms ReDoS sandbox |
| DELETE | `/api/automation-rules/{id}` | Session | Delete automation rule |
| GET | `/api/faqs` | Session | List FAQ entries |
| POST | `/api/faqs` | Session | Create FAQ entry |
| POST | `/api/faqs/test` | Session | Test live query against PostgreSQL FTS + Trigram index |
| DELETE | `/api/faqs/{id}` | Session | Delete FAQ entry |
| GET | `/api/templates` | Session | List synced Meta templates |
| POST | `/api/templates` | Session | Submit new template to Meta Graph API |
| POST | `/api/templates/sync` | Session | Trigger Meta category sync background job |
| GET | `/api/unmatched-messages` | Session | List unhandled customer inquiries |
| DELETE | `/api/unmatched-messages/{id}` | Session | Dismiss unhandled inquiry |

## Key decisions and why
- **Plain-Language Match Labels:** Replaces raw enum values (`EXACT`, `CONTAINS`, `STARTS_WITH`) with natural language phrasing ("When the message is exactly...", "When the message contains...").
- **Regex Shielding:** Regex matching is tucked behind an "Advanced" toggle to prevent confusing non-technical users while preserving power-user capabilities.
- **Interactive Live Typo Tester:** Enables business owners to test customer typo queries against PostgreSQL trigrams before saving, displaying exact match percentage and threshold warnings.
- **India Marketing Cost Alert:** Emphasizes Meta's ~₹0.86/msg marketing vs ~₹0.115/msg utility pricing to avoid surprising merchants with high bills.
- **Four States on Every Screen:** Loading skeleton, actionable empty state, error banner with retry button, and loaded view.

## Divergence from the architecture docs
- None. Implemented in strict accordance with `AUTOMATION-CONFIGURATION.md`.
