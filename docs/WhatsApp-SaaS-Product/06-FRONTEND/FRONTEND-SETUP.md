# Frontend Setup

## Stack

| Choice | Why |
|---|---|
| React 18 + TypeScript | You know Java; TS's type discipline transfers well |
| Vite | Fast dev server, tiny config, first-class static output |
| React Router | Standard routing |
| TanStack Query | Server state, caching, retries — removes most `useEffect` bugs |
| Tailwind CSS | No CSS architecture decisions to make alone |
| Cloudflare Pages | Free, unlimited bandwidth, global CDN, 500 builds/month |

**Nothing else.** No Redux (TanStack Query covers server state; `useState` covers the rest),
no component library, no form library until a form actually hurts.

## Why a separate SPA and not Thymeleaf

Cloudflare Pages serves the frontend free, on a global CDN, with unlimited bandwidth. That
takes all static-asset traffic off your 2 OCPU Oracle box, which then only serves JSON. On a
free-tier VM that is a real win, not a stylistic preference.

Cost: you now handle CORS and CSRF across origins. Worth it.

## Project layout

```text
frontend/
├── src/
│   ├── main.tsx
│   ├── App.tsx                 # router + providers
│   ├── api/
│   │   ├── client.ts           # fetch wrapper, credentials, CSRF, error mapping
│   │   └── hooks/              # useAuth, useRules, useFaqs, useConversations...
│   ├── components/             # shared dumb components
│   │   ├── EmptyState.tsx
│   │   ├── ErrorState.tsx
│   │   ├── Skeleton.tsx
│   │   └── Layout/
│   ├── features/               # one folder per screen group — mirrors backend modules
│   │   ├── auth/
│   │   ├── whatsapp/
│   │   ├── automation/
│   │   ├── faq/
│   │   ├── templates/
│   │   ├── inbox/
│   │   └── dashboard/
│   ├── lib/                    # formatting, phone masking, date/IST helpers
│   └── types/                  # types mirroring backend DTOs
├── .env.example
└── vite.config.ts
```

## Environment variables

Only `VITE_`-prefixed vars reach the browser. **Everything in a Vite bundle is public.**

```bash
VITE_API_BASE_URL=http://localhost:8080
VITE_META_APP_ID=              # public by design
VITE_META_CONFIG_ID=           # Embedded Signup config id, public by design
VITE_META_GRAPH_VERSION=v21.0
```

Never put the Meta **app secret**, the token encryption key, or any DB credential here.
The app secret lives only on the server.

## Cross-origin auth

Frontend on `app.yourdomain.com`, API on `api.yourdomain.com`.

- Every request: `credentials: 'include'`
- Backend `Access-Control-Allow-Credentials: true`, explicit origin allowlist (never `*`)
- Session cookie: `HttpOnly`, `Secure`, `SameSite=Lax`, `Domain=.yourdomain.com`
- CSRF: read the token from the cookie, send it in the header on every mutating request

Get this working in F17. Debugging CORS while also debugging Embedded Signup in F18 is
miserable.

## Design constraints (not negotiable)

Your users are SMB owners on mid-range Android phones, often on 4G that drops.

- **Mobile-first.** Design at 360px, then widen. Test at 360px before calling anything done.
- **Touch targets ≥ 44px.**
- **Every list screen needs four states**: loading skeleton, empty (with a concrete first
  action), error (with retry), loaded. An empty state saying "No data" is a support ticket.
- **Plain language.** "Message contains", not `CONTAINS`. "Not delivered — number not on
  WhatsApp", not `error 131026`.
- **Show money in ₹** with the GST position stated.
- **No decorative animation.** It costs battery and looks slow on mid-range hardware.

## Deploy

Cloudflare Pages, connected to the repo, build command `npm run build`, output `dist`.
Free tier: unlimited bandwidth, 500 builds/month. Set `VITE_` vars in the Pages dashboard
per environment.

Preview deployments per branch come free — use them to show a customer a screen before you
finish it.
