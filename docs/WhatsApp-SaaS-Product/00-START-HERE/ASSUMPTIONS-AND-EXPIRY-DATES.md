# Assumptions and Expiry Dates

Every fact below was verified on a specific date. **None of it is permanently true.**
Meta revises WhatsApp rates quarterly. Oracle halved its free tier in June 2026 with no
public announcement. Google cut its Gemini free quota sharply in December 2025.

Free tiers are marketing budgets, and marketing budgets get cut.

**Rule: re-verify every item marked `QUARTERLY` on 1 Jan / 1 Apr / 1 Jul / 1 Oct.**

---

## How to read this file

| Label | Meaning |
|---|---|
| `SOURCE DOCUMENT` | Stated in `SOURCE-architecture-and-cost-strategy.md`, verified 18 Aug 2026 |
| `CURRENTLY VERIFIED` | Re-checked later than the source document — note the date |
| `ASSUMPTION` | Not verified; a working assumption that needs testing |
| `[DECISION REQUIRED]` | Not covered by the source document at all |

---

## 1. WhatsApp / Meta pricing — `QUARTERLY`

`SOURCE DOCUMENT` — verified 18 August 2026, Meta list rates effective 1 July 2026,
India, INR billing, **exclusive of 18% GST**:

| Category | Rate | Volume tiers |
|---|---|---|
| Marketing | ₹0.8631 | None — never discounted |
| Utility | ₹0.1150 | From 25M/month |
| Authentication | ₹0.1150 | From 750,000/month |
| Authentication-International | ₹2.4971 | — |
| Service (free-form reply in 24h window) | ₹0 → **₹0.1150 from 1 Oct 2026** | None, ever |
| In-window utility template | ₹0 → **billable from 1 Oct 2026** | — |

**Re-verify:** 1 October 2026 (rates change AND the service-message model changes),
then quarterly.
**Where used:** `08-META-WHATSAPP/MESSAGE-PRICING.md`, `01-BUSINESS/PRICING-AND-MONETIZATION.md`
**Implementation consequence:** rates live in a **config table**, never as constants in code.

Additional dated facts:
- India moved to local INR billing January 2026; migration deadline 31 December 2026.
  An existing USD WABA **cannot** be converted — a new WABA is required.
- India's marketing rate rose ~10% on 1 January 2026 (₹0.7846 → ₹0.8631).
- "Meta Business Agent" (Meta's own AI replies) billed per token from 1 Aug 2026, ~$2/1M tokens.
  **Not our cost** — we run our own logic.
- 72-hour Free Entry Point window (Click-to-WhatsApp ads / Page CTA) remains free after October.
- Inbound messages from users are always free.

---

## 2. Oracle Cloud Always Free — `QUARTERLY`

`SOURCE DOCUMENT` — verified 18 August 2026:

- Ampere A1 ARM allocation **halved** from 4 OCPU / 24 GB to **2 OCPU / 12 GB**,
  effective 15 June 2026, with no public announcement.
- Oracle emailed users that instances above the new limits **will be terminated on or
  after 18 August 2026**.
- 200 GB block storage and 2× AMD micro instances were **not** cut.
- 10 TB/month outbound bandwidth.
- Idle instances may be reclaimed. CPU activity is the primary idle metric.
- Always Free resources exist **only in the tenancy home region**, and the home region
  **cannot be changed after signup**.

`ASSUMPTION` — 2 OCPU / 12 GB is sufficient to 200–500 customers. Not load-tested.
Validate before you rely on it past ~100 customers.

**Re-verify:** quarterly, and immediately if you receive any Oracle policy email.
**Mitigation (mandatory, not optional):** off-box backups + a single idempotent
provisioning script, so migrating to any VPS is under an hour. Priced fallback:
Hetzner CX22 ~€3.79/mo ≈ ₹380/mo. See `03-ARCHITECTURE/BACKUP-AND-RECOVERY.md`.

---

## 3. Other free tiers — `QUARTERLY`

`SOURCE DOCUMENT` — verified 18 August 2026:

| Service | Free limit | Watch for |
|---|---|---|
| Cloudflare Pages | Unlimited bandwidth, 500 builds/mo | Build-minute creep |
| Cloudflare R2 | 10 GB, 1M Class A + 10M Class B ops/mo, **zero egress** | Storage growth |
| Backblaze B2 | 10 GB, 1 GB/day free egress | Backup size growth |
| GitHub Actions | 2,000 min/mo private repos | ~600 JAR builds |
| Sentry | 5,000 errors/mo | Sustained overage = a real bug |
| Better Stack | 10 monitors, 3-min checks, status page | **Commercial use allowed** |
| Grafana Cloud | 10k series, 50 GB logs, 50 GB traces | — |
| Brevo / Resend | 300/day / 3,000/mo | Notification volume |

**⚠️ UptimeRobot free plan is restricted to personal, non-commercial use** as of
1 December 2024. Do **not** use it for this product. Use Better Stack.

**Managed Postgres free tiers — deliberately rejected:**
- **Neon free:** 0.5 GB + **100 CU-hours/month per project**, scale-to-zero after 5 min idle.
  Our app is a continuously-queried webhook receiver, so the DB never idles. At Neon's
  minimum 0.25 CU, a full month is 730h × 0.25 = **182 CU-hours** — quota exhausted around
  day 16, then compute suspends. **Disqualifying.** (See ADR-006.)
- **Supabase free:** 500 MB, 2 projects, pauses after 7 days inactivity (tightened Feb 2026).
  Workable but adds latency and a second vendor for 5% of the platform's value.

---

## 4. Payments and compliance — `ANNUAL`

`SOURCE DOCUMENT` — verified 18 August 2026:

- Razorpay: **2% + 18% GST = 2.36% effective** on domestic cards/netbanking/wallets.
  ₹0 setup, ₹0 AMC. Premium instruments (Amex, EMI, corporate cards, international) ~3%.
- **UPI is 0% under ₹2,000** (NPCI MDR waiver) — this is why the ₹1,999 price point matters.
- Razorpay Subscriptions adds ~0.99%. UPI AutoPay recurring ~0.5% + GST.
- Meta Business Verification: free, but requires a registered entity + documents.
- GST compliance: CA ₹1,000–2,500/month. Often the largest *fixed* cost of an Indian
  bootstrapped SaaS, and it is not infrastructure at all.

`ASSUMPTION` — the NPCI UPI MDR waiver continues. It is a policy decision and could change.
If it does, a ₹1,999 price point gains ~₹47/month of payment cost. Not fatal, but model it.

---

## 5. Business assumptions — untested, highest risk

These are `ASSUMPTION`, not verified fact. They are the things most likely to be wrong,
and they matter far more than any infrastructure number.

| Assumption | How to test | Doc |
|---|---|---|
| Indian SMBs will pay ~₹1,999/mo for this | Sell to 5 before building fully | `01-BUSINESS/CUSTOMER-VALIDATION.md` |
| They will tolerate two separate bills (yours + Meta's) | Ask explicitly in validation calls | `13-DECISIONS/DECISIONS.md` D-02 |
| They can attach a payment method to Meta unaided | Watch customer #1 do it | `10-OPERATIONS/CUSTOMER-ONBOARDING.md` |
| Deterministic rules cover enough use cases without AI | Log unmatched inbound messages from day 1 | ADR-007 |
| The Oct 2026 change won't cause churn | Pre-emptively communicate; measure | `08-META-WHATSAPP/OCTOBER-2026-BILLING-CHANGE.md` |

---

## Re-verification log

| Date checked | Items | Changes found | By |
|---|---|---|---|
| 2026-08-18 | All | Baseline | Initial research |

---

## ⚠️ Pilot-critical assumption — verify quarterly

| Assumption | Source | Verified | Re-check |
|---|---|---|---|
| Meta apps in **development mode** show WhatsApp permissions in Embedded Signup to anyone with an **admin, developer, or tester** role; Advanced Access is required only in **live mode** | Meta Embedded Signup overview docs | 19 Aug 2026 | Quarterly, and before starting F06 |
| Business-type apps receive **Standard Access** automatically for all permissions of that app type | Meta App Review docs | 19 Aug 2026 | Quarterly |
| Without Business Verification, conversations are capped around **250 per 24 hours** | Third-party guides, Aug 2026 | 19 Aug 2026 | Before onboarding pilot customer #5 |

**This is the single-point assumption of the entire pilot plan
([PILOT-FIRST-PLAN.md](PILOT-FIRST-PLAN.md), ADR-008).** If Meta changes development-mode
access rules, the pilot path closes and you fall back to Business Verification plus App Review
before onboarding anyone.

Confirm on `developers.facebook.com` yourself and update the "Verified" date above. Do not take
this workspace's word for it.
