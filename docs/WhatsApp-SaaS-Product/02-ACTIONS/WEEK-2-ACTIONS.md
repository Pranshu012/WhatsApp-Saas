# Week 2

**Theme: Meta App Review submitted, and the foundation built while waiting.**

## Monday
- [ ] Check Business Verification status
- [ ] Create the Meta App (Business type)
- [ ] Record App ID + Secret into `.env` (**never** Git)
- [ ] Add the WhatsApp product; note the test number
- [ ] Start Tech Provider onboarding, "without a partner"

## Tuesday
- [ ] Prepare App Review materials: description, privacy policy URL, terms URL
- [ ] Build a throwaway Embedded Signup page against the **test WABA** for the screencast
- [ ] Record the screencast
- [ ] **Submit App Review: `whatsapp_business_management` + `whatsapp_business_messaging`, Advanced Access**

## Wednesday — Claude Code setup + F00
- [ ] Complete `14-CLAUDE-CODE/CLAUDE-CODE-SETUP.md` end to end
- [ ] `CLAUDE.md` in place; `/memory` confirms it's loaded
- [ ] `.claude/settings.json` with deny for `git push` and `.env`
- [ ] Run the **F00** prompt (`PROMPTS/PHASE-A-FOUNDATION.md`)
- [ ] Review the plan before letting it write code
- [ ] `./mvnw clean verify` green; commit yourself

## Thursday — F01
- [ ] Run the **F01** prompt: tenants, users, tenant_users, registration
- [ ] Verify the transaction rolls back on failure
- [ ] Confirm Argon2id hashing
- [ ] Commit; update `CURRENT-STATUS.md`

## Friday — F02, the important one
- [ ] Read `03-ARCHITECTURE/MULTI-TENANCY.md` fully first
- [ ] Run the **F02** prompt: TenantContext + RLS
- [ ] Verify all four isolation tests
- [ ] **Manually confirm test 2 fails with RLS disabled** — don't take it on trust
- [ ] Confirm tests connect as the non-superuser role
- [ ] Commit

## Also this week
- [ ] Open the Razorpay account (documents take time)
- [ ] Create Cloudflare, GitHub, B2, Sentry, Better Stack accounts
- [ ] Generate secrets and store them **outside** the VM

## End of week 2

- [ ] App Review **submitted**
- [ ] F00, F01, F02 complete and committed
- [ ] Multi-tenancy provably works
- [ ] `./mvnw clean verify` green

## If App Review is rejected

Common causes: unclear screencast, privacy policy silent on WhatsApp data, or a description that
doesn't explain the Tech Provider use case. Fix and resubmit. Meanwhile continue — Phases 1–3 and
6 don't need Meta.

## Watch out for

- Letting Claude Code skip the RLS tests. It will sometimes suggest they're "covered" by
  application filtering. They are not.
- Accepting a diff you don't understand. Ask for an explanation instead.
- Testcontainers connecting as superuser — silently makes every isolation test meaningless.
