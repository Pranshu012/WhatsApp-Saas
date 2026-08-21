# F18 — WhatsApp Onboarding and Connection Screen

**Status:** Complete
**Completed:** 2026-08-22
**Commit:** Pending
**Spec:** `docs/WhatsApp-SaaS-Product/14-CLAUDE-CODE/PROMPTS/PHASE-D-FRONTEND.md#f18`

## What this does
Implements the WhatsApp Onboarding and Connection interface (`/whatsapp`), allowing business owners to connect their Meta WhatsApp Business Account (WABA) via Meta's official Embedded Signup flow. Handles all 6 connection states, gracefully handles popup abandonment without errors, displays live connection health metrics (Quality Rating and Messaging Limit Tier), features an unmissable missing payment method warning banner, and permanently explains the Two-Bill pricing model.

## Files
| File | Purpose |
|---|---|
| `frontend/src/features/whatsapp/useMetaEmbeddedSignup.ts` | Custom hook for Meta JS SDK loading, window message listeners, popup launch, abandonment detection, and `/api/whatsapp/connect` POST |
| `frontend/src/features/whatsapp/WhatsAppConnectionScreen.tsx` | Main connection management screen handling all 6 UI states and disconnect modal |
| `frontend/src/App.tsx` | Route integration for `/whatsapp` replacing placeholder |

## Database changes
- None. (Consumes existing `whatsapp_accounts` table and F05/F06 endpoints).

## API surface
| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/whatsapp/connect` | Session | Exchange OAuth code and register verified WABA |
| GET | `/api/whatsapp/account` | Session | Fetch tenant primary connected WhatsApp account |
| POST | `/api/whatsapp/accounts/{id}/disconnect` | Session | Disconnect WhatsApp number |

## Key decisions and why
- **Dual Flow Synchronization:** Captures the `code` returned by `FB.login()` as well as `waba_id` and `phone_number_id` broadcast by Meta's iframe via `window.addEventListener('message')`.
- **Graceful Abandonment Recovery:** When a user closes the Embedded Signup popup before completing the flow, the hook catches the cancellation and resets the UI to the ready state without displaying alarming error messages or getting stuck in loading spinners.
- **Prominent Payment Warning:** Highlights the single most common onboarding failure mode (missing card on Meta's WhatsApp Manager) via an unmissable amber banner with direct navigation to Meta's phone numbers management dashboard.
- **Two-Bill Model Clarity:** Dedicated explanation card outlining the distinction between our ₹1,999/mo software platform subscription and Meta's direct per-message utility/marketing charges.
- **Touch-Friendly & Mobile-First:** Designed and validated for mid-range mobile viewports (360px width) with minimum 44px touch targets.

## Divergence from the architecture docs
- None. Implemented in strict accordance with `WHATSAPP-ONBOARDING.md` and `EMBEDDED-SIGNUP.md`.
