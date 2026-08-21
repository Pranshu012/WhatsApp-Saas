# Product Vision

## Now (MVP, 0–20 customers)

**The simplest product that answers WhatsApp enquiries automatically on a business's own number.**

In scope:
- Connect your own WhatsApp Business Account (Embedded Signup)
- Keyword-based automatic replies
- FAQ matching with escalation when unsure
- Message templates, synced from Meta
- Scheduled reminders
- Shared inbox with conversation history
- Message counts by billing category

That's it. Eight screens, sixteen tables, one deployable JAR.

## Deliberately not now

| Not building | Revisit when |
|---|---|
| Bulk marketing campaigns | Customers ask, and understand the ₹0.8631/message cost |
| Visual flow builder | Rules become genuinely insufficient |
| AI chatbot | The unmatched-message log proves the need (ADR-007) |
| Multi-channel (Instagram, SMS) | WhatsApp is validated and saturated |
| CRM features | Customers ask for the specific field, not the category |
| Agent assignment / team inbox | A tenant has more than 2 users (D-08) |
| Mobile app | The web app is unusable on a phone — it shouldn't be |
| Multi-language UI | English is validated (D-06) |
| Self-serve onboarding | You've onboarded 20 people manually |
| Wallet-based message billing | Stage 3, ADR-005 |
| Integrations (Tally, Zoho, Google Calendar) | Three customers name the same one |

**The rule:** three unprompted requests from paying customers, or it isn't a feature.

## Next (20–100 customers)

Whatever the first 20 actually asked for. Not what's on this page — this page is a guess, and
their feedback is data.

Likely candidates, based on the shape of the problem:
- Vertical-specific template libraries (the fastest path to a customer's first send)
- Better FAQ authoring, driven by the unmatched-message log
- A simple integration with whatever their booking or billing system is
- Multi-user access, once tenants have real teams

## Later — the actual strategic bet

**Own one vertical completely.**

Become the default WhatsApp layer for, say, Indian dental clinics: the templates, the reminder
workflows, the integration with the practice-management software they already use, the language
their patients expect.

Why vertical depth rather than horizontal features:
- Defensible against funded horizontal competitors who must serve everyone
- Referrals compound inside a vertical — clinic owners know clinic owners
- The same feature is worth more when it fits exactly
- You can genuinely be the best option for one trade; you cannot be the best for all

The alternative — a generic WhatsApp inbox for every SMB in India — puts you in direct feature
competition with Wati, AiSensy, and Gupshup. That's a fight you lose.

## What success looks like

**Not** revenue, initially. These, in order:

1. **10 businesses paying**
2. **Still using it after 3 months** — retention is the only honest validation
3. **One customer refers another without being asked**
4. **A customer says "I couldn't run my business without this"**
5. **20 businesses paying**

Then, and only then, does revenue growth become the metric.

## What would make us stop

Being willing to name this in advance is what stops you from spending two years on something that
isn't working:

- Fewer than 5 paying customers after 3 months of active selling
- Customers sign up and stop using it within 6 weeks
- Nobody will pay ₹1,999 (they'd pay ₹299, which doesn't work at this cost of support)
- Meta restricts the Tech Provider model in a way that breaks the economics
- The two-bill model is a consistent dealbreaker (D-02) and the wallet alternative is out of reach

If two or more of these are true at month three, the honest move is to change the product or the
market — not to build more features.

## Guiding principles

1. **The customer's problem is time, not technology.** Nobody wants software; they want their
   evenings back.
2. **Boring beats clever.** Every time.
3. **Their WhatsApp account is their business.** Never risk it. Never lock it in.
4. **Transparency is the moat.** The moment we mark up Meta silently, we're just a smaller Wati.
5. **Ship less.** The failure mode for a solo founder is building, not selling.
