# Caddy Setup

Caddy over Nginx for one reason: **automatic TLS certificates with zero configuration**. No
certbot, no renewal cron, no expired-certificate outage at 3am.

## Install

```bash
sudo apt install -y debian-keyring debian-archive-keyring apt-transport-https curl
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' \
  | sudo gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' \
  | sudo tee /etc/apt/sources.list.d/caddy-stable.list
sudo apt update && sudo apt install -y caddy
```

## `/etc/caddy/Caddyfile`

```caddyfile
{
    email you@yourdomain.com          # Let's Encrypt notifications
}

api.yourdomain.com {
    encode zstd gzip

    header {
        Strict-Transport-Security "max-age=31536000; includeSubDomains"
        X-Content-Type-Options "nosniff"
        X-Frame-Options "DENY"
        Referrer-Policy "strict-origin-when-cross-origin"
        -Server                        # don't advertise the stack
    }

    # Webhooks: generous timeouts, no rate limiting, no body buffering games.
    # Meta retries on failure but losing messages is worse than serving a burst.
    handle /api/webhooks/* {
        reverse_proxy 127.0.0.1:8080 {
            transport http {
                read_timeout 30s
                write_timeout 30s
            }
        }
    }

    handle {
        reverse_proxy 127.0.0.1:8080 {
            transport http {
                read_timeout 60s
                write_timeout 60s
            }
            health_uri /actuator/health/readiness
            health_interval 10s
        }
    }

    request_body {
        max_size 10MB              # WhatsApp media metadata is small; cap abuse
    }

    log {
        output file /var/log/caddy/api.log {
            roll_size 50MB
            roll_keep 5
        }
        format json
    }
}
```

Reload without dropping connections:
```bash
sudo caddy validate --config /etc/caddy/Caddyfile
sudo systemctl reload caddy
```

## ⚠️ Certificate issuance and Cloudflare

Caddy obtains its certificate via an HTTP-01 challenge on **port 80**. If Cloudflare is
proxying `api.yourdomain.com` (orange cloud), the challenge is intercepted and issuance fails.

Order of operations:
1. Cloudflare DNS record for `api` set to **DNS only** (grey cloud)
2. Port 80 and 443 open in the Oracle Security List **and** UFW
3. Start Caddy, confirm the certificate is issued (`sudo journalctl -u caddy -f`)
4. *Optionally* switch Cloudflare to proxied, with SSL mode **Full (strict)**

If you skip step 1 you'll lose an hour to an error message that doesn't mention Cloudflare.

## Raw request body and HMAC verification

Meta signs the **raw bytes** of the webhook body. Any proxy that re-encodes or re-serialises
the body breaks signature verification. Caddy's `reverse_proxy` passes bodies through byte-for-byte,
so this works — but do not add body-rewriting middleware here, and do not enable compression
on the request path.

The application side of this (getting the raw bytes in Spring before Jackson parses them) is in
`../05-BACKEND/WEBHOOK-IMPLEMENTATION.md`.

## CORS

Handle CORS in Spring, not Caddy. One place, with the origin allowlist next to the security
config, is easier to reason about than rules split across two systems.

## Definition of Done

- [ ] `https://api.yourdomain.com/actuator/health` returns 200 with a valid certificate
- [ ] HTTP redirects to HTTPS automatically
- [ ] Security headers present (check with `curl -I`)
- [ ] `caddy validate` passes and reload doesn't drop connections
- [ ] Webhook path has its own longer timeouts
- [ ] Certificate auto-renewal confirmed working (Caddy logs show the renewal schedule)
- [ ] Caddy logs rotating, not filling the disk
