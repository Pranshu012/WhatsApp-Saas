# F17 — React Setup, Auth, and App Shell

**Status:** Complete
**Completed:** 2026-08-22
**Commit:** Pending
**Spec:** `docs/WhatsApp-SaaS-Product/14-CLAUDE-CODE/PROMPTS/PHASE-D-FRONTEND.md#f17`

## What this does
Establishes the React 18 + Vite + TypeScript single-page application (SPA) with a responsive mobile-first App Shell and complete authentication workflows (Login, Register, Forgot Password, Reset Password). Includes a centralized fetch API client with HttpOnly session cookie credential inclusion, CSRF token attachment, session bootstrap with zero flash of login screen, and empty-state placeholders for all MVP routes.

## Files
| File | Purpose |
|---|---|
| `frontend/package.json` | Project dependencies (React 18, React Router 6, TanStack Query 5, Tailwind CSS, Lucide icons) |
| `frontend/vite.config.ts` | Vite configuration with React plugin and dev proxy to `/api` |
| `frontend/tailwind.config.js` | Tailwind theme with brand colors and 44px minimum touch targets |
| `frontend/src/api/client.ts` | Centralized fetch wrapper with `credentials: 'include'`, CSRF token extraction, and `ApiError` mapping |
| `frontend/src/api/types.ts` | TypeScript DTO interfaces matching backend request and response models |
| `frontend/src/context/AuthContext.tsx` | Global authentication state, session bootstrap via `/api/auth/me`, login, register, and logout handlers |
| `frontend/src/components/ProtectedRoute.tsx` | Route guard redirecting unauthenticated users to `/login` with full-screen loading state |
| `frontend/src/components/Layout/AppLayout.tsx` | Main application shell with responsive header, user profile, workspace name, and mobile menu toggle |
| `frontend/src/components/Layout/Sidebar.tsx` | Sidebar navigation drawer with active route highlighting |
| `frontend/src/components/EmptyState.tsx` | Reusable empty state component with concrete action CTA |
| `frontend/src/components/ErrorState.tsx` | Reusable error banner with retry button |
| `frontend/src/components/Skeleton.tsx` | Pulse-animated content placeholder for loading states |
| `frontend/src/components/AlertBanner.tsx` | Reusable status alert banners (warning, error, success, info) |
| `frontend/src/features/auth/LoginScreen.tsx` | Login screen with generic error messaging |
| `frontend/src/features/auth/RegisterScreen.tsx` | Registration screen with automatic slug derivation and min-12 char password validation |
| `frontend/src/features/auth/ForgotPasswordScreen.tsx` | Forgot password screen with honest success message to prevent user enumeration |
| `frontend/src/features/auth/ResetPasswordScreen.tsx` | Reset password screen with token verification and confirmation |
| `frontend/src/features/placeholders/FeaturePlaceholder.tsx` | Standardized placeholder screen for upcoming MVP routes |
| `frontend/src/App.tsx` | Route definitions with QueryClient and AuthProvider wrappers |
| `src/main/java/com/example/wasaas/common/config/SecurityConfig.java` | Updated Spring Security CORS configuration to allow credentials and local frontend development origins |

## Database changes
- None. (Frontend increment consuming existing F01–F16 database models and endpoints).

## API surface
| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register new tenant and owner user |
| POST | `/api/auth/login` | Public | Authenticate user and issue Spring Session cookie |
| GET | `/api/auth/me` | Session | Bootstrap current user session on page load |
| GET | `/api/auth/csrf` | Session | Fetch CSRF token for mutating requests |
| POST | `/api/auth/forgot-password` | Public | Request password reset email |
| POST | `/api/auth/reset-password` | Public | Complete password reset with one-time token |
| POST | `/api/auth/logout` | Session | Invalidate session |

## Key decisions and why
- **Session Bootstrap Loading State:** `AuthContext` starts with `loading = true` and queries `/api/auth/me` before rendering protected routes, preventing momentary flickering of the login screen for logged-in users on page refresh.
- **CSRF Cookie and Header Synchronization:** `apiClient` checks for the `XSRF-TOKEN` cookie or queries `/api/auth/csrf` to attach `X-XSRF-TOKEN` on all state-mutating HTTP methods (POST, PUT, DELETE, PATCH), complying with Spring Security CSRF requirements.
- **Mobile-First Touch Target Sizing:** All interactive inputs, buttons, and nav links use `min-h-[44px]` and `min-w-[44px]` for effortless tapping on mid-range Android mobile devices at 360px viewport width.
- **Automated Slug Generation:** On the registration screen, typing the business name automatically creates a sanitized URL-safe workspace slug with live visual preview (`wasaas.in/patel-electronics`), while allowing manual override if needed.
- **Zero Secrets in Vite Bundle:** Only `VITE_` prefixed public variables are exposed; no backend secrets or private keys are bundled.

## Divergence from the architecture docs
- None. Followed `FRONTEND-SETUP.md` and `APPLICATION-SCREENS.md` exact specifications.
