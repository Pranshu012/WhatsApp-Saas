# Customer Validation

**Do this before Phase C. It closes D-01, D-02, D-03, and D-07 — four decisions that block
development.**

## The goal

Not to pitch. To find out:
1. Which single job they'd pay for (**D-01**)
2. Whether two separate bills is acceptable (**D-02**)
3. Whether ₹1,999/month is credible (**D-03**)
4. Whether they're already on the WhatsApp Business app (**D-07**)

## Method: ten conversations, one vertical

Pick a vertical from `TARGET-CUSTOMER.md`. Walk in, or call. Ten businesses. Twenty minutes each.

**Walking in beats calling.** You see their actual workflow — the phone on the counter, the
notebook, the person interrupted mid-task. You learn more in one visit than in ten calls.

## The script

Open honestly:

> "I'm building a tool for [vertical] and I'm trying to understand how you handle WhatsApp before
> I build the wrong thing. Twenty minutes, and I'm not selling you anything today."

Then **listen**. Questions in order:

**About their current reality**
1. How do customers usually reach you?
2. Roughly how many WhatsApp messages a day?
3. Who answers them? On what device?
4. What do people ask most often?
5. What happens to a message that arrives at 10pm?
6. Has an enquiry ever been missed? What did it cost?

**About the pain — where D-01 gets answered**
7. Which part of this is most annoying?
8. If you could automate one thing about WhatsApp, what would it be?
9. Have you tried any tool for this? What happened?

**About willingness to pay — D-03**
10. What do you currently spend on software each month? On what?
11. If something answered your repeat questions automatically, what would that be worth?

**The two-bill test — D-02. Ask this exactly.**
12. > "The way this works: you'd pay me ₹1,999 a month for the software, and Meta charges you
    > separately for the messages — about ₹100 a month at your volume, on your own card. Two
    > bills. How does that sound?"

**Write their exact words.** This is the single most important answer in the whole exercise.

**Setup reality — D-07**
13. Are you using WhatsApp Business app, or the normal one?
14. Do you have a card you could attach to a Meta account?

**Close**
15. If I build this, would you be my first paying customer?

That last question separates polite interest from actual demand. "Sounds great" is not a yes.

## What to record

One page per conversation. Verbatim quotes, not summaries.

```text
Business:            Date:
Vertical:            Msgs/day:
Who answers:         Device:

Top 3 questions they get:

"If you could automate one thing..." (their words):

Currently pays for software: ₹        for:

Reaction to two bills (VERBATIM):

On WhatsApp Business app? Y/N     Has a card? Y/N

Would be a paying customer? Yes / Maybe / No

Most surprising thing they said:
```

## Reading the results

**D-01 — feature set.** Count answers to Q8. If 7+ of 10 name the same thing, build that. If
answers scatter, either the vertical is wrong or you need ten more conversations.

**D-02 — two bills.** 
- 7+ fine with it → proceed as planned
- 4–6 hesitant → proceed, but make onboarding hand-hold the payment step hard
- Mostly rejected → **stop and reconsider.** A wallet model needs a Meta credit line and is Stage-3
  scope. Better to know now than after ten weeks of building.

**D-03 — price.** If nobody blinks at ₹1,999, you're too cheap. If everyone does, either the pain
is too small or the vertical is wrong.

**D-07 — Business app.** If most are already on it, existing-number onboarding becomes a Phase B
requirement, not an edge case.

## Signals you're onto something

- They interrupt you to describe the problem in more detail
- They ask when it'll be ready
- They offer to pay before you ask
- They mention someone else who needs it
- They show you their phone

## Signals to stop and rethink

- Polite agreement with no specifics
- "Send me some information" (means no)
- They can't name a cost of the current problem
- They already tried three tools and abandoned all of them
- Nobody will name a number when asked what it's worth

## After the ten conversations

1. Write the answers into `13-DECISIONS/DECISIONS.md` with the date
2. Pick the vertical and the single job
3. **Get 2–3 of them to commit to paying** before Phase C
4. Only then start building the automation engine

## Keep validating

Validation isn't a phase you complete. Every onboarding call is a validation call. Every support
ticket is a signal about what's missing or confusing.

Keep a running list:
```text
Feature requests — with a count of how many customers asked unprompted:
  [ ] ...                 (asked by: 1)
  [ ] ...                 (asked by: 3)  ← build this
```

**Three unprompted requests from paying customers is the bar.** One is noise, and it's how
roadmaps get hijacked.
