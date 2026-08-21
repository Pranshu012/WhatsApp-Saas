# Pre-Development Checklist

Everything to have in place before increment F00. Nothing here is code.

## Decisions (blocking)
- [ ] **D-04** Company name and domain
- [ ] **D-05** Business entity type (talk to a CA)
- [ ] **D-01** Core feature set — **after** 10 validation conversations
- [ ] **D-02** Two-bill model acceptance — tested in those conversations
- [ ] **D-03** Price confirmed at ₹1,999
- [ ] **D-07** Are prospects already on the WhatsApp Business app?

Write each answer, with a date, into `13-DECISIONS/DECISIONS.md`.

## Legal and financial
- [ ] Business entity registered
- [ ] PAN (business or proprietor)
- [ ] GST registration (B2B customers will want GST invoices for input credit)
- [ ] Business bank account
- [ ] CA engaged (₹1,000–2,500/month — often the largest fixed cost)
- [ ] Privacy policy published, **mentioning WhatsApp data handling**
- [ ] Terms of service published
- [ ] Data retention period decided (**D-09**)

## Domain and web presence
- [ ] Domain registered (~₹700–1,200/year)
- [ ] DNS on Cloudflare
- [ ] A real one-page site describing the product (App Review looks at this)
- [ ] Business email on your own domain — not Gmail

## Meta (start first, longest wait)
- [ ] Meta Business Portfolio created
- [ ] **Business Verification submitted**
- [ ] Business Verification **Verified**
- [ ] Meta App created (Business type)
- [ ] App ID and Secret recorded in `.env` — **never** Git
- [ ] WhatsApp product added; test number noted
- [ ] Tech Provider onboarding started, "without a partner"
- [ ] **App Review submitted: both permissions, Advanced Access**
- [ ] Embedded Signup config created; config ID recorded

## Infrastructure accounts
- [ ] Oracle Cloud — **home region Mumbai or Hyderabad (irreversible)**
- [ ] A 2 OCPU / 12 GB ARM instance successfully provisioned
- [ ] Cloudflare (DNS, Pages, R2)
- [ ] GitHub (private repo)
- [ ] Backblaze B2 (backups — **a different vendor from Oracle**)
- [ ] Sentry
- [ ] Better Stack (**not** UptimeRobot — its free plan is non-commercial only)
- [ ] Brevo or Resend
- [ ] Razorpay (needs PAN, bank account, GST cert)

## Secrets — generate and store OUTSIDE the VM
- [ ] `TOKEN_ENCRYPTION_KEY` — `openssl rand -base64 32`
- [ ] `META_WEBHOOK_VERIFY_TOKEN` — any random string
- [ ] Backup encryption key
- [ ] DB password
- [ ] Stored in a password manager **and** one offline copy
- [ ] You have verified you can retrieve them

**If the backup encryption key lives only on the VM being backed up, your backups are
decorative.**

## Development environment
- [ ] JDK 21 (Temurin)
- [ ] Docker (local Postgres only)
- [ ] `psql` 17 client
- [ ] Git
- [ ] Node 20+ (for the frontend, later)
- [ ] Claude Code installed (`14-CLAUDE-CODE/CLAUDE-CODE-SETUP.md`)
- [ ] A tunnel tool for local webhook testing (cloudflared or ngrok)

## Claude Code setup
- [ ] Repo created and `git init` run **before** the first session
- [ ] Docs workspace copied into `docs/`
- [ ] `CLAUDE.md` at repo root, from the template
- [ ] `.claude/settings.json` with allow/deny lists
- [ ] `.env` gitignored **and** deny-listed
- [ ] `/memory` confirms `CLAUDE.md` is loaded
- [ ] You've read `14-CLAUDE-CODE/WORKFLOW.md`

## Validation
- [ ] 10 conversations completed
- [ ] Notes written up, verbatim quotes captured
- [ ] Vertical chosen
- [ ] **2–3 people have said they'd pay**

## Understanding check

Can you answer these without looking?

- [ ] Why are we a Tech Provider and not a Solution Partner?
- [ ] Who pays Meta for messages, and why does that matter?
- [ ] What happens on 1 October 2026?
- [ ] Why does every table need `tenant_id` **and** an RLS policy?
- [ ] Why must the webhook ACK in under 2 seconds?
- [ ] Why do we write the ledger row **before** calling Meta?
- [ ] What's the trigger for moving to managed Postgres?
- [ ] Why no Redis, Kafka, or Kubernetes?

If any answer is fuzzy, re-read the relevant doc now. It's cheaper than discovering the gap in
week six.

## Ready when
- [ ] All decisions above answered
- [ ] Meta App Review submitted
- [ ] Oracle instance running
- [ ] Claude Code configured
- [ ] 2–3 committed prospects

**Then:** `14-CLAUDE-CODE/PROMPTS/PHASE-A-FOUNDATION.md`, increment F00.
