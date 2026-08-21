# Pilot Playbook

How to find, run, and learn from 5–10 pilot customers.

Read [../00-START-HERE/PILOT-FIRST-PLAN.md](../00-START-HERE/PILOT-FIRST-PLAN.md) first.

---

## Timeline

| Week | Activity |
|---|---|
| 1–2 | Talk to 15–20 businesses. Close decision D-01. Line up 10 candidates. |
| 1–7 | Build (in parallel — increments F00–F14, F17, F18, F20, F22, F23) |
| 5 | Onboard **customer #1 only**. Fix what breaks. |
| 6 | Onboard 2–3 more |
| 7–8 | Onboard the rest, up to 10 |
| 8–12 | Run, observe, weekly calls |
| 12 | Decide: register the business, pivot, or stop |

**Onboard the first customer while you're still building.** You will find three things wrong
that no amount of testing would have shown you, and fixing them for one person is far cheaper
than for ten.

---

## Finding pilot customers

### What good looks like

| Want | Avoid |
|---|---|
| Gets 20+ WhatsApp enquiries/day | Barely uses WhatsApp |
| Answers the same questions repeatedly | Every enquiry is bespoke |
| Owner is reachable and will talk to you | You'd deal with a gatekeeper |
| Near you, or will video call | Cannot get 45 minutes |
| Not a close friend | Close friends lie to be kind |

**The friend problem is real.** Friends will use it once, say it's great, and stop. You'll learn
nothing and lose the ability to ask honestly. Use acquaintances, referrals, and strangers.

### Where to look

1. **Your own suppliers and service providers.** Your tailor, gym, clinic, coaching centre,
   pharmacy. They already know you and there's a real relationship, but not friendship.
2. **Referrals.** Ask each conversation: "who else gets a lot of WhatsApp enquiries?" This is by
   far the highest-yield channel.
3. **Local business WhatsApp/Facebook groups.** Ask a question, don't pitch.
4. **Walk in.** Genuinely effective in India. Ten shops in a market, 45 minutes.

### The approach message

Short, no pitch, asks for help rather than selling:

> Hi [name], I'm building a small tool that answers common WhatsApp questions automatically for
> businesses like yours — timings, prices, that kind of thing.
>
> I'm looking for a few businesses to try it early and tell me honestly if it's useful. It's
> ₹500/month during the pilot. WhatsApp charges separately for messages (probably ₹100–500 for
> you).
>
> Could I take 20 minutes to show you and hear what you'd actually want?

Rate of reply will be low. Message 30 to get 10.

---

## The validation conversation (before you build)

Do 15–20 of these in weeks 1–2. **They also close decision D-01.**

Ask, and shut up:

1. How many WhatsApp messages from customers do you get in a day?
2. What are the five questions you get most often?
3. Who answers them? When you're closed, what happens?
4. What happens if you reply an hour late? A day late?
5. Have you tried anything to fix this?
6. *(after showing a mock-up)* What would you use this for, in your own words?
7. What would stop you using it?

**Do not ask "would you pay for this?"** People say yes to be nice. Ask what they currently
spend, and whether they'd join the pilot at ₹500 — an actual commitment, right now.

Write down their **exact words** for question 2. That list becomes their FAQ configuration and
tells you whether F13/F14 as scoped is what people actually need.

---

## Onboarding a pilot customer

Full script in [../10-OPERATIONS/CUSTOMER-ONBOARDING.md](../10-OPERATIONS/CUSTOMER-ONBOARDING.md).
Pilot-specific additions:

**Before the call**
- [ ] Get their Facebook account name/ID
- [ ] Add them as **Tester** on your Meta app (App Roles → Roles → Add People)
- [ ] Send them the acceptance link: `https://developers.facebook.com/settings/developer/requests/`
- [ ] Send the "what you'll need" message from CUSTOMER-ONBOARDING.md

**On the call (45–60 min)**
1. They accept the Tester invitation
2. Explain what it means: *"this only lets our software connect to the WhatsApp account you
   choose — no access to your personal Facebook, and you can remove it any time"*
3. Embedded Signup, screen-shared
4. **Verify a payment method is attached in WhatsApp Manager** ← the #1 failure point
5. Build their top 5 FAQs together, from their own answers to question 2
6. Add 1–2 keyword rules
7. **Test from their own phone.** They must see it work with their own eyes.
8. Set expectations: pilot, things will break, message me directly
9. Give them your WhatsApp number
10. Agree the ₹500 and how they'll pay (UPI on the 1st)

**Do not end the call until an automated reply has arrived on their phone.**

---

## Running the pilot

### Weekly, per customer (15 minutes each)

Actually call. Do not send a survey.

1. Did it reply to anything useful this week?
2. Did it get anything wrong?
3. What did you want it to do that it couldn't?
4. Have you shown it to anyone else? *(referral signal — the strongest one)*

### Weekly, your side

- Review each tenant's **unmatched messages** and propose 2 specific FAQs per customer. Five
  minutes of your time, feels like a service they're paying for, and directly improves retention.
- Check dead jobs, delivery failures, quality ratings
- Check the ~250 conversations/24h app-wide cap
- Update the pilot scoreboard

### The pilot scoreboard

One spreadsheet. Update weekly.

| Customer | Onboarded | Activated | Msgs/wk auto-replied | Unmatched % | Paid? | Would pay ₹1,999? | Referred? | Notes |
|---|---|---|---|---|---|---|---|---|

**Activated** = connected + payment method attached + 3+ rules/FAQs + a real automated reply to a
genuine customer.

The two columns that decide everything are **Paid?** and **Referred?**. Usage without either is a
polite user, not a customer.

---

## What you are actually testing

Four questions. Everything else is noise.

**1. Does the onboarding work without you?**
It won't, at first. Count the steps where you had to intervene. That count must fall to zero
before you can scale — the whole self-serve model depends on it.

**2. Do the automated replies actually help?**
Measured by: unmatched rate falling over time, and customers adding FAQs themselves without
being asked.

**3. Will they pay?**
₹500 collected, twice, from 5+ people. Then: does anyone say ₹1,999 unprompted?

**4. Does the platform hold up?**
Zero cross-tenant incidents. Zero data loss. Zero duplicate sends. If any of these happen, that's
a stop-and-fix, not a note for later.

---

## Signals

### Good

- They add FAQs themselves, unprompted
- They ask when a feature is coming (means they're planning around it)
- They refer someone
- They complain about a specific limitation — engagement, not rejection
- Unmatched rate falling week over week

### Bad — and what each means

| Signal | Likely meaning |
|---|---|
| Uses it for a week, then silent | Not solving a real problem |
| "It's good" with no specifics | Being polite. Push: "what did it get wrong?" |
| Never adds FAQs after onboarding | Doesn't understand the value, or doesn't have one |
| Won't pay ₹500 | Will definitely not pay ₹1,999 |
| Turns automation off | Something is actively embarrassing them — find out today |
| Quality rating drops to RED | Their customers are blocking. Serious. |

### Stop-and-think signals

- Fewer than 3 of 10 still active at week 8
- Nobody refers anyone
- The same feature gap from 5+ customers *(that's not a gap — that's the actual product)*
- You spend more than 10 hours/week supporting 10 customers

The last one is worth taking seriously. If 10 customers take 10 hours/week, 100 customers is
impossible and the product needs to change, not the sales effort.

---

## Handling the pilot ending

**If it works** → follow the transition in
[../00-START-HERE/PILOT-FIRST-PLAN.md](../00-START-HERE/PILOT-FIRST-PLAN.md). Tell pilot
customers a month before the price moves to ₹1,999, as promised. Consider keeping them at a lower
rate permanently — they took the risk, and they're your references.

**If it doesn't** → say so honestly, give them a month's notice, help them export their data,
and ask each one the single most valuable question: *"what would have made this worth ₹1,999?"*

Then decide: pivot the feature set, change the customer segment, or stop. All three are
legitimate outcomes. **Finding this out in 12 weeks for ₹5,000 is a good result** — the failure
mode you avoided was finding out in 12 months for ₹2,00,000.
