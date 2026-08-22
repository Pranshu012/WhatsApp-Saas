# Product readiness audit — 22 August 2026

Scope: repository code, documented plan, API/UI contract, build checks, and the unauthenticated first-use experience. This is a code review, not proof that Meta, Razorpay, a production VM, or a backup restore has been run in a real account.

## Executive result

The application contains a substantial working pilot product: authentication, tenant isolation, WhatsApp connection plumbing, durable jobs, message ledger, inbound/outbound processing, automation, FAQs, templates, scheduling, inbox, and dashboard are represented in the codebase. Both backend verification (`./mvnw clean verify`) and frontend production build (`npm run build`) pass locally.

It is **not yet a complete production SaaS**. F21, the paid subscription/payment feature, is absent. F22/F23 scripts are present but have not been proven on a fresh production VM or through a restore drill. The first-visit UI also needed a product explanation; the corresponding improvement is included with this audit.

## Planned features compared with code

| Area | Result | Evidence / limitation |
|---|---|---|
| F00–F14 core backend | Present | Migrations V1–V15 and corresponding Spring modules/tests exist. |
| F15 interactive replies | Present | Interactive messaging and reply-consolidation code/tests are present. |
| F16 scheduled messages | Present | Migration V16, API, UI and worker flow exist. |
| F17–F20 web app | Present | Protected React routes and matching backend endpoints exist; production frontend build passes. |
| F21 billing subscription | **Missing** | No subscription migration, `payment_events`, Razorpay client/webhook/controller, checkout, dunning, plan state, or billing screen. Razorpay values only appear as environment placeholders/docs. |
| F22 infrastructure scripts | Present, unproven | Caddy, provisioning, deploy, systemd and CI/CD files exist. Need a clean-server rehearsal. |
| F23 backups/monitoring | Partial, unproven | Backup/restore/WAL scripts and a backup heartbeat exist. No demonstrated scratch-DB restore, and no application Sentry/worker heartbeat integration was found. |

## Highest-priority gaps before taking paid customers

1. Build F21 before advertising or collecting subscription payments. Current registration has no plan/trial enforcement, checkout, verified payment activation, cancellation, or failed-payment handling.
2. Run F22 on a new VM and execute a documented F23 restore into a scratch database. The plan requires tested recovery, not just scripts in Git.
3. Make encrypted off-site backups mandatory. `infra/backup.sh` currently continues with an unencrypted local dump when the Age key is missing; a production backup should fail loudly and alert instead.
4. Replace production role passwords safely. `infra/provision.sh` and the historical RLS migration contain known placeholder/default role-password values; deployment must set unique secrets before any data is created.
5. Perform real Meta sandbox checks: Embedded Signup, webhook delivery, template sync, a sent message, a received message, and a missing-Meta-payment-method warning. These need external Meta access and cannot be certified from source code alone.

## Product and usability findings

1. **First screen previously did not explain the product.** A visitor landed on sign-in with no plain explanation of the outcome or setup sequence. Fixed in this change with a clear three-step introduction on desktop authentication pages.
2. **Terminology was too technical.** “Workspace slug/identifier” was renamed in the user-facing login form to “Business code,” with a plain explanation that it is optional.
3. **Dashboard language is still operator-oriented.** Phrases such as “immutable ledger,” “billing category,” and “typo-tolerant” should be simplified in a follow-up pass and paired with a first-time checklist: connect WhatsApp → add 3 FAQs → test a message.
4. **Navigation is feature-oriented rather than task-oriented.** “Unmatched” and “Templates” are meaningful to an operator, but should have short descriptions or be grouped under “Set up replies” for a small-business owner.

## Items intentionally not called missing

The original plan explicitly defers F21 in a pilot. Redis, microservices, mobile apps, AI automation, advanced analytics, and multilingual UI are also deliberately out of MVP scope. They are not defects at the current scale.

## Recommended next implementation order

1. Finish the first-time dashboard checklist and simplify navigation copy.
2. Build F21 end-to-end, beginning with database state and verified Razorpay webhook handling before checkout UI.
3. Harden and rehearse F22/F23 in a non-production VM, including an encrypted backup and scratch restore.
4. Run the real Meta acceptance flow with a test business account.
