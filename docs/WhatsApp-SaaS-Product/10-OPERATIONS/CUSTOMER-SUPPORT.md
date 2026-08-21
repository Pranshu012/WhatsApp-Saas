# Customer Support

You are the support team. Design for that honestly rather than pretending otherwise.

## Channel

**WhatsApp.** Your customers are WhatsApp-native SMB owners; email gets ignored. Use a separate
business number from your product's test numbers.

Publish hours and hold to them: **10am–7pm IST, Mon–Sat.** Reply outside them if you like, but
don't promise it. A promise you break is worse than a narrower promise you keep.

Do not build a ticketing system. A WhatsApp label system and a spreadsheet handle 20 customers.

## Response targets

| Severity | Example | Target |
|---|---|---|
| P1 — nothing works | Messages not sending, can't log in, data wrong | 1 hour in hours, 4 hours out |
| P2 — degraded | One rule broken, delivery failures, template rejected | Same day |
| P3 — question | "How do I add an FAQ?" | 24 hours |
| P4 — feature request | "Can it do X?" | Acknowledge in 24h, log it, no promise |

## The eight tickets you will actually get

Have answers ready. These are ~80% of volume.

**1. "Messages aren't sending."**
First check: payment method on Meta. Second: quality rating / messaging limit. Third: token
expiry. Fourth: your own dead-jobs count. In that order — the first one is most of them.

**2. "Why did Meta charge me ₹X?"**
Show the dashboard category breakdown. Explain per-message billing, and that from
**1 October 2026** service messages and in-window utility templates became billable in India
with no volume discounts. Meta's invoice is authoritative; your counts are for reference.

**3. "It's not replying to my customers."**
Check the unmatched list. Usually no rule matches, or FAQ confidence is below threshold. Add
the FAQ with them on the call rather than telling them how.

**4. "My template was rejected."**
Show Meta's verbatim reason. Almost always promotional wording in a utility template. Reword,
resubmit, explain the 7.5× cost difference so they care.

**5. "Quality rating went RED."**
Their customers are blocking or reporting. Reduce marketing volume, improve targeting, make sure
opt-in is real. Meta can restrict sending — this is urgent for them.

**6. "Can I use my existing WhatsApp Business number?"**
Only via number migration, which deletes the app-based account. Explain the tradeoff; most SMBs
should use a fresh number.

**7. "I want a refund."**
Follow the published policy. Don't argue. Ask one question: what would have kept you. Log it.

**8. "Can it do [X]?"**
If it's in `../14-CLAUDE-CODE/FEATURE-BREAKDOWN.md` as not-in-MVP, say so plainly with no date.
Log the request with the customer's name. Three named customers asking for the same thing is a
roadmap signal; one is not.

## Triage before replying

For any "it's broken" report, in this order:

1. Better Stack — is the platform actually up?
2. Sentry — any new error for this tenant?
3. `jobs` table — any `DEAD` rows for this tenant?
4. `message_ledger` — what does the failure reason say?
5. `whatsapp_accounts` — token expired? quality rating?
6. Meta WhatsApp Manager (with them, screen-shared) — payment method?

Five minutes of this beats twenty minutes of guessing over chat.

## Rules for yourself

- **Never blame the customer.** "Meta needs a payment method added" not "you didn't add one."
- **Never expose internals.** No error codes, no stack traces, no "the RLS policy".
- **Say when you don't know**, then give a time you'll come back. Then come back.
- **Log every ticket** in a spreadsheet: date, customer, category, resolution, minutes spent.
  When one category exceeds 20% of your time, that's a product fix, not a support problem.
- **Never make a roadmap promise** to close a ticket. You will regret it.

## Escalating to yourself

Some tickets are platform bugs, not support. When you find one:

1. Tell the customer honestly: "this is a bug on our side, I'm fixing it"
2. Log it as an increment-sized task, not a hotfix, unless it's P1
3. Add a test that would have caught it — this is the only way support volume goes down

## When to hire

At ~50 customers, or when support exceeds 10 hours/week, or when P1 response targets slip
repeatedly. Before that, the support load is your product research and outsourcing it loses you
the signal.
