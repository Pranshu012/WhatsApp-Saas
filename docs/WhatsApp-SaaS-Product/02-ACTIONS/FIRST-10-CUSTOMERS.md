# First 10 Customers

**Objective: 10 businesses paying, and still using it after a month.**

Not sign-ups. Not trials. Paying, active customers.

## Rules

1. **One vertical.** The one you validated. Resist the plumber who calls after your third clinic.
2. **Manual onboarding, on a video call, every time.** All ten.
3. **Everyone pays.** No free pilots — a free user teaches you nothing about willingness to pay
   and generates support load anyway.
4. **No new features until all ten are live.** Fix what breaks; build nothing new.
5. **Write down every confusion.** Every one. That list is your roadmap.

## Why manual onboarding for all ten

It feels inefficient. It is the highest-value work available to you.

- You see exactly where people get stuck (usually the Meta payment method)
- You hear their actual words for their problems — that's your marketing copy
- You catch the payment-method failure before it becomes a silent support ticket
- You learn which templates they actually need
- Ten repetitions tell you what to automate. Automating before that means automating your guess.

Budget 60–90 minutes per customer, plus follow-up.

## The sequence per customer

See `10-OPERATIONS/CUSTOMER-ONBOARDING.md` for the full script.

```text
1. Sales conversation — state the two-bill model explicitly
2. They agree to pay
3. Schedule a 60-minute onboarding call
4. On the call:
   - create account
   - Embedded Signup → their own WABA
   - ⚠️ WATCH THEM ATTACH A PAYMENT METHOD ON META
   - submit 1–2 templates
   - configure 3–5 automation rules and FAQs together
   - send a test message to their own phone
5. Take payment (UPI)
6. Follow up on day 2, day 7, day 30
```

## Where they'll get stuck

| Stuck at | What to do |
|---|---|
| Facebook login | Many owners don't remember their password. Warn them beforehand to have it ready. |
| Business Portfolio creation | Walk them through it; needs their business details |
| Phone number verification | OTP by SMS or voice. If the number is on the WhatsApp Business app, see D-07. |
| **Attaching a payment method** | **The big one. Do not end the call until you've seen it done.** |
| Template rejection | Rewrite together. Explain utility vs marketing categories. |
| Understanding two bills | Show them the dashboard counts and a rupee estimate |

## What to measure

| Metric | Target | Why |
|---|---|---|
| Onboarding call duration | <90 min | If it's growing, something regressed |
| Time to first real message sent | <24 h | Sooner is better |
| Customers active at day 30 | 8/10 | **The number that matters most** |
| Automated replies in month 1 | >100/customer | Below that, is it doing anything? |
| Support messages per customer per week | <3 | Above that, something is unclear |
| Unmatched inbound messages | Tracked | Feeds the FAQ and ADR-007 |

**Day-30 retention is the real validation.** Ten sign-ups who all stop using it in three weeks
means the product doesn't work, regardless of revenue.

## What to fix, in order

1. **Anything that stopped a customer sending their first message**
2. **Anything three or more customers asked about**
3. **Anything that generated repeat support**
4. Everything else — later, or never

## What NOT to do

| Don't | Why |
|---|---|
| Build a feature one customer asked for | Wait for three unprompted requests |
| Build self-serve onboarding | You haven't learned the manual version yet |
| Upgrade infrastructure | ₹10,000 MRR means ₹500 of infra. Nothing else. |
| Take a customer outside your vertical | You cannot serve two well while learning either |
| Discount to close | You'll anchor your entire price list |
| Add a second vertical | Focus is the only advantage you have |
| Set up analytics dashboards for yourself | Ten customers is a spreadsheet |

## Revenue at 10 customers

```text
10 × ₹1,999            = ₹19,990 MRR
Infrastructure         ≈ ₹100
GST / CA               ≈ ₹1,500
Domain                 ≈ ₹80
                       ─────────
Net                    ≈ ₹18,310/month
```

**Spend on infrastructure: ₹500 maximum.** Verify your restore works. Bank the rest. See
`12-SCALING/REVENUE-FUNDED-INFRASTRUCTURE.md`.

## Definition of Done

- [ ] 10 businesses paying via Razorpay
- [ ] 8+ still actively using it at day 30
- [ ] Each has sent at least 100 automated replies
- [ ] You have a written list of every confusion from every call
- [ ] You know your top 3 support questions
- [ ] At least one customer has referred someone unprompted
- [ ] At least one usable quote with a name and a business

Then: `FIRST-20-CUSTOMERS.md`.
