# Message Pricing

**⚠️ DATED INFORMATION — verified 18 August 2026. Meta revises rates quarterly
(1 Jan / 1 Apr / 1 Jul / 1 Oct) with about one month's notice. Re-verify before any pricing
promise. See `00-START-HERE/ASSUMPTIONS-AND-EXPIRY-DATES.md`.**

## India rate card

Meta list rates, effective 1 July 2026, INR billing, **excluding 18% GST**:

| Category | Rate | Volume tiers |
|---|---|---|
| Marketing | **₹0.8631** | **None, ever** |
| Utility | **₹0.1150** | From 25M/month |
| Authentication | **₹0.1150** | From 750,000/month |
| Authentication-International | ₹2.4971 | — |
| Service (free-form reply in the 24h window) | **₹0 → ₹0.1150 from 1 Oct 2026** | None, ever |
| Inbound (customer → business) | **Free, always** | — |

With GST, ₹0.8631 becomes about ₹1.0185 per marketing message.

**Marketing costs 7.5× utility**, with no volume discount at any scale. Template
mis-categorisation is therefore expensive — and it's your customer's money.

## How billing works

Since 1 July 2025 Meta charges **per delivered message**, not per 24-hour conversation. The old
conversation model — and the "first 1,000 conversations free" that older guides describe — is
retired.

- Only **delivered** messages are billed. Failed sends are free.
- The **recipient's** country determines the rate, not yours.
- Category is decided by the **template's Meta-assigned category** — not what you requested.
- India moved to local INR billing in January 2026; migration deadline 31 December 2026. An
  existing USD WABA **cannot** be converted; a new WABA is required.

## What's free

| Free | Conditions |
|---|---|
| Inbound messages | Always |
| Service replies in the 24h window | **Until 30 September 2026 only** |
| In-window utility templates | **Until 30 September 2026 only** |
| Everything in a Free Entry Point window | 72 hours, opened by a Click-to-WhatsApp ad or Page CTA. **Stays free after October.** |

The Free Entry Point window is genuinely valuable and under-used: 72 hours of completely free
messaging, including templates.

## Worked examples

**A clinic, 500 appointment reminders/month (utility templates):**
```
500 × ₹0.1150            = ₹57.50
+ 18% GST                = ₹67.85 per month
```
Trivial. Easy to justify against a ₹1,999 software fee.

**A boutique, one marketing blast to 2,000 contacts:**
```
2,000 × ₹0.8631          = ₹1,726.20
+ 18% GST                = ₹2,036.92 per send
```
A single blast costs more than your software fee. **Your customer needs to understand this
before they run it.**

**A support-heavy business after 1 October 2026 — 1,000 replies/month:**
```
Before: ₹0
After:  1,000 × ₹0.1150  = ₹115.00 + GST = ₹135.70 per month
```
Small in absolute terms, but it goes from zero to non-zero, which is the part customers notice.

## Meta Business Agent — not our cost

From 1 August 2026 Meta bills its **own** AI replies ("Meta Business Agent") per token, around
$2 per 1M tokens. We run our own deterministic automation (ADR-007), so this does not apply to
us. Worth knowing so you don't confuse it with your own costs.

## Implications for our product

1. **Rates live in the `whatsapp_rates` table, dated.** Never constants. The 1 October change
   then requires a new row, not a deploy.
2. **Never promise unlimited messaging.** Golden Rule 6.
3. **Consolidate replies** (F15). Each message is billed separately, so three short messages cost
   3×.
4. **Show per-category counts** (F20) so customers can reconcile their Meta bill.
5. **Warn on MARKETING templates** in the UI, with the rupee implication.
6. **Per-contact reply rate limits** — a loop spends your customer's money.
7. **Exploit the Free Entry Point window** where the customer runs Click-to-WhatsApp ads.

## Our own pricing

₹1,999/month software fee (D-03). Deliberately under ₹2,000 because **UPI carries 0% MDR below
₹2,000** under the NPCI waiver, making our payment processing cost effectively zero. Cards would
cost 2.36%.

See `01-BUSINESS/PRICING-AND-MONETIZATION.md`.

## Re-verification

| When | Do |
|---|---|
| 1 October 2026 | **Rates change AND the service-message model changes.** Highest priority. |
| Quarterly thereafter | Check Meta's rate card and pricing-updates changelog |
| Before any customer pricing promise | Always |

Log each check in `00-START-HERE/ASSUMPTIONS-AND-EXPIRY-DATES.md`.
