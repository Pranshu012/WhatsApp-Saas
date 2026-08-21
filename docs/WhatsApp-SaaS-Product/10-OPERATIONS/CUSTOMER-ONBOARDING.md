# Customer Onboarding

**Do this manually for your first 20 customers.** Not because you can't automate it, but
because the 20 conversations tell you what to build. Automating first means automating your
guesses.

## ⚠️ Meta's onboarding rate limit

You can onboard **10 new business customers per rolling 7 days** by default. That rises to
**200 per week** only after all three of: Business Verification, App Review, and Access
Verification.

At 10/week you cannot exceed ~40 customers/month even if demand exists. Plan launch around
this — it also means the manual approach costs you nothing in throughput early on.

## Before the call — send this

> Hi [name], looking forward to setting up WhatsApp automation for [business].
>
> Please have ready (10 min):
> 1. A Facebook account you can log into
> 2. A phone number **not currently on WhatsApp or WhatsApp Business**
> 3. Access to that phone for an OTP
> 4. Business name, address, website
> 5. A credit/debit card or UPI for Meta's message charges
>
> Two separate bills: our software is ₹1,999/month. Meta charges you directly for messages
> (~₹0.115 per utility/service message, ~₹0.86 per marketing message, plus GST). You control
> that and see it in Meta's WhatsApp Manager.

Point 2 is the most common blocker. Point 5 is the most common cause of "your product doesn't
work" tickets a week later.

## The onboarding call — 45 minutes

| # | Step | Notes |
|---|---|---|
| 1 | Create their account (or watch them) | 2 min |
| 2 | Explain the two-bill model **again**, verbally | Do not skip. Repetition here prevents the month-2 argument. |
| 3 | Walk through Embedded Signup on a screen share | Meta's UI, not yours — you can't simplify it, only guide |
| 4 | **Verify a payment method is attached in WhatsApp Manager** | The single biggest failure point |
| 5 | Confirm connection health: quality rating, messaging tier | |
| 6 | Ask: "what 5 questions do customers ask you most?" | This is the real work of the call |
| 7 | Build those 5 FAQs together, live | Let them type at least two themselves |
| 8 | Add 1–2 keyword rules (price list, timings) | |
| 9 | Test from **their own phone**, not yours | They must see it work with their own eyes |
| 10 | Show the inbox and the unmatched list | "Check this weekly — it tells you what to add" |
| 11 | Show the dashboard counts and explain Meta's invoice | |
| 12 | Give them your WhatsApp number for support | You are the support channel at this stage |

**Target: automated reply working, from their own phone, before the call ends.** A customer who
hasn't seen it work will not come back to try.

## Activation definition

A customer is activated when: WhatsApp connected + payment method attached + at least 3 FAQs or
rules + at least one real automated reply sent to a genuine customer.

Track this per customer in a spreadsheet. Not activated within 7 days → call them. Activation
is the number that predicts retention; everything else is noise at this stage.

## Follow-up cadence (first 30 days)

| When | Action |
|---|---|
| Day 1 | WhatsApp: "everything working?" |
| Day 3 | Check their unmatched list yourself. Suggest 2 specific FAQs. |
| Day 7 | Call. What's working, what isn't. Check message counts. |
| Day 14 | Check quality rating and delivery failures |
| Day 21 | Ask for the one thing they'd change |
| Day 30 | Renewal conversation + ask for a referral if they're happy |

Day 3 is high-leverage: reviewing their unmatched list *for* them and proposing exact FAQ text
takes you 5 minutes and feels like a service they're paying for.

## Common problems and the fix

| Problem | Cause | Fix |
|---|---|---|
| "Messages aren't sending" | No payment method on Meta | WhatsApp Manager → Billing |
| Signup fails at phone step | Number already on WhatsApp | Different number, or delete the WhatsApp account first (takes time to propagate) |
| "It's not replying" | No rules configured, or all matches below threshold | Check unmatched list |
| Every API call fails, error 200 | **Your** app lacks Advanced Access | Your problem, not theirs — App Review |
| Quality rating dropped to RED | Their customers blocking/reporting | Review what's being sent; reduce marketing sends |
| "Why is Meta charging me?" | Two-bill model not absorbed | Show the dashboard breakdown; this is why that screen exists |
| Template rejected | Promotional wording in a utility template | Reword, resubmit |

## Do not automate yet

Self-serve onboarding · onboarding email sequences · in-app product tours · video tutorials ·
a help centre.

Build these after 20 manual onboardings, when you know exactly which three steps people get
stuck on. Before that you'd be writing documentation for a flow you're about to change.
