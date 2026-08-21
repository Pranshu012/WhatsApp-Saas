# Cloudflare Setup

Three things: DNS, SPA hosting (Pages), and media storage (R2). All free at your scale.

## Domain and DNS

Register a `.com` or `.in` (~₹1,000/year). Then add the domain to Cloudflare (free plan) and
point your registrar's nameservers at Cloudflare.

### Records

| Type | Name | Content | Proxy | Notes |
|---|---|---|---|---|
| A | `api` | `<Oracle reserved IP>` | **DNS only (grey)** at first | See below |
| CNAME | `app` | `<project>.pages.dev` | Proxied | Cloudflare Pages |
| CNAME | `www` | `yourdomain.com` | Proxied | |
| A | `@` | `<Oracle IP>` or Pages | Proxied | Marketing page |

**Start `api` as DNS-only (grey cloud).** Caddy needs to complete an HTTP-01 ACME challenge on
port 80 to obtain its TLS certificate. Cloudflare proxying interferes with that. Once Caddy has
a valid certificate, you may switch to proxied (orange) with SSL mode **Full (strict)**.

If you leave it proxied from the start you'll spend an hour debugging certificate failures.

### SSL/TLS settings

- Mode: **Full (strict)** — never "Flexible" (that leaves the origin leg unencrypted)
- Always Use HTTPS: on
- Minimum TLS: 1.2

## Cloudflare Pages (the React SPA)

**Workers & Pages → Create → Pages → Connect to Git**

| Setting | Value |
|---|---|
| Build command | `npm run build` |
| Output directory | `dist` |
| Root directory | `frontend` |
| Node version | 20 or 22 |

Environment variables (Production and Preview separately):
```
VITE_API_BASE_URL=https://api.yourdomain.com
VITE_META_APP_ID=...
VITE_META_CONFIG_ID=...
VITE_META_GRAPH_VERSION=v21.0
```

Free tier: **unlimited bandwidth**, 500 builds/month, unlimited requests. Every branch gets a
preview URL — genuinely useful for showing a customer a screen before it's finished.

### SPA routing

Client-side routes need a catch-all. Add `frontend/public/_redirects`:
```
/*    /index.html   200
```
Without this, refreshing on `/inbox` returns 404.

## Cloudflare R2 (media storage)

Needed because WhatsApp media URLs from Meta expire — you must download and store anything you
want to keep.

**R2 → Create bucket** → `wasaas-media`, location hint Asia-Pacific.

Free tier: 10 GB storage, and — the important part — **zero egress fees**. S3 charges for
egress; R2 doesn't. At 10 GB you'll comfortably serve MVP media volumes for ₹0.

Create an R2 API token (Object Read & Write, scoped to that bucket). R2 is S3-compatible, so
the AWS SDK v2 for Java works unchanged:

```
R2_ACCOUNT_ID=...
R2_ACCESS_KEY_ID=...
R2_SECRET_ACCESS_KEY=...
R2_BUCKET=wasaas-media
R2_ENDPOINT=https://<account-id>.r2.cloudflarestorage.com
```

**Keep the bucket private.** Serve media through your own authenticated endpoint or presigned
URLs. A public bucket of customers' WhatsApp media is a DPDP incident waiting to happen.

Store media under a tenant-scoped key prefix: `{tenantId}/{yyyy}/{mm}/{mediaId}`. It makes
per-tenant deletion (a DPDP requirement) a prefix delete rather than a scan.

## Optional hardening (free plan)

- **Rate limiting** on `/api/auth/login` — cheap brute-force protection at the edge
- **WAF managed rules** — on by default, leave them on
- **Do not** rate-limit `/api/webhooks/*`. Meta's webhook traffic is legitimate and bursty;
  blocking it loses messages.

## Definition of Done

- [ ] Domain on Cloudflare nameservers, DNS resolving
- [ ] `api` A record → Oracle reserved IP, DNS-only until Caddy has a certificate
- [ ] SSL mode Full (strict); Always Use HTTPS on
- [ ] Pages deploys the SPA; `app.yourdomain.com` loads
- [ ] `_redirects` present — refresh on a deep route works
- [ ] R2 bucket created, private, credentials stored in the server env file only
- [ ] Rate limit on the login endpoint; webhooks exempt
