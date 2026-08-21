# Reusable Prompts

Keep these to hand. They are the ones you'll use dozens of times.

---

## Start of every session

```text
Read CLAUDE.md and docs/WhatsApp-SaaS-Product/00-START-HERE/CURRENT-STATUS.md.
I'm working on increment [F##]. Confirm you understand the constraints, especially the
"no new infrastructure" and "every table needs tenant_id" rules, then wait for my prompt.
```

---

## Force a plan

```text
Plan this before writing any code. Show me:
1. Files you'll create or modify
2. The DB migration, if any, including the RLS policy
3. Any new dependency and why it's unavoidable
4. Which tests you'll write
5. Anything you think is ambiguous or that I've got wrong

Don't write code until I approve the plan.
```

---

## Review a diff

```text
Review the current diff as a senior engineer who will maintain this for three years.
Check specifically:
- Is tenant_id present and enforced on every new table and query?
- Any WhatsApp API call outside a job handler?
- Any token, password, or phone number that could reach a log or an API response?
- Any UPDATE on message_ledger?
- Any new dependency I didn't approve?
- Anything built for a future increment that should be removed now?

List real problems only. Don't pad the list.
```

---

## Debug a failure

```text
This test/behaviour fails:

[paste the full error and stack trace]

Before changing anything: explain what you think the root cause is and how you'd confirm it.
Don't guess-and-patch. If you need to see a file or run a command, say which.
```

---

## Push back on over-engineering

```text
Stop. That's more than this increment needs.

Remove: [X, Y, Z]

Rule 9 in CLAUDE.md — no new infrastructure dependencies. Solve this with what we already
have (Postgres, Spring, the jobs table). If you genuinely believe that's impossible, explain
why in two sentences and I'll decide.
```

---

## Scope creep check

```text
Is anything in this change outside increment [F##] as defined in
docs/WhatsApp-SaaS-Product/14-CLAUDE-CODE/FEATURE-BREAKDOWN.md?
If yes, list it and revert it. I want this increment complete and nothing more.
```

---

## Write tests from requirements, not implementation

```text
Write tests for [feature] based on the Definition of Done in the increment prompt, not on
how the code currently works. If the code fails a test that correctly reflects the DoD, the
code is wrong — tell me, don't adjust the test.

Use Testcontainers Postgres. No H2.
```

---

## Tenant isolation check (run after every increment that adds a table)

```text
I've added [table(s)]. Verify tenant isolation:
1. Confirm NOT NULL tenant_id
2. Confirm an RLS policy exists and matches the V3 pattern
3. Confirm the app role is not a superuser in any test setup
4. Write a test where tenant A attempts to read tenant B's rows via the repository
5. Write a test using a raw query that OMITS tenant_id, proving RLS still blocks it
6. Temporarily disable RLS and confirm test 5 FAILS — then re-enable

If step 6 passes with RLS off, the test is worthless. Tell me.
```

---

## End of increment

```text
Increment [F##] is done. Give me:
1. A one-paragraph summary of what changed
2. The Definition of Done checklist with each item marked done or not, honestly
3. Anything you had to assume that should go into
   docs/WhatsApp-SaaS-Product/13-DECISIONS/DECISIONS.md
4. A suggested conventional-commit message
5. The three lines I should update in CURRENT-STATUS.md

Do not commit. I'll review and commit.
```

---

## Update the docs after a decision changes

```text
We changed [decision]. Update:
1. The relevant ADR in docs/WhatsApp-SaaS-Product/13-DECISIONS/ — add a
   "Revised [date]" section, don't rewrite history
2. CLAUDE.md if it contradicts a rule there
3. Any architecture doc that now states something false

Show me the diffs. Don't touch anything not affected.
```

---

## Cost/commercial sanity check (use before any messaging feature ships)

```text
Review [feature] against docs/WhatsApp-SaaS-Product/08-META-WHATSAPP/MESSAGE-PRICING.md and
OCTOBER-2026-BILLING-CHANGE.md.

For each outbound message this feature can produce:
- Which billing category does Meta assign it?
- Could it be consolidated into fewer messages?
- Can a loop or retry cause repeated sends that our customer pays for?
- Is there a per-contact cap?
- Does the ledger record it correctly for the customer's reconciliation?

Remember: our customer pays Meta directly. A bug here spends their money, not ours — which
is worse, not better.
```
