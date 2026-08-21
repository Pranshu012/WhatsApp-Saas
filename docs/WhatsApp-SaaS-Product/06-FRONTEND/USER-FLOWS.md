# User Flows

## Flow 1 — Signup to first automated reply (the only flow that matters)

This is your activation funnel. Every extra step loses customers.

```text
1. Register (business name, email, password)          [F17]
2. Land on dashboard → big "Connect WhatsApp" card    [F20]
3. Click Connect → Meta Embedded Signup popup         [F18]
   3a. Customer logs into Facebook
   3b. Selects or creates a Meta Business Account
   3c. Creates/selects a WABA + phone number
   3d. Verifies the phone number via OTP
   3e. Grants our app permission
4. Popup closes → we exchange the code, subscribe webhooks, store token
5. Connection card shows: number, verified name, quality rating
   ⚠ If no payment method on Meta → prominent warning, link to WhatsApp Manager
6. Prompt: "Add your first auto-reply"                [F19]
7. Customer adds one keyword rule (e.g. "price" → price list text)
8. Customer messages their own business number from a personal phone
9. Auto-reply arrives
10. ✅ Activated
```

**Target: under 15 minutes.** Time it with a real person. Whatever step they get stuck on is
your highest-value fix — not whatever feature you feel like building.

Step 3 is Meta's UI, not yours. You cannot simplify it. You *can* prepare the customer for
it: before launching the popup, tell them plainly what they'll need (a Facebook account, a
phone number not currently on WhatsApp, and access to that number for OTP).

## Flow 2 — Inbound message, automated reply

```text
Customer's customer sends "what are your timings?"
  → Meta webhook → our receiver (verify, persist, ACK <2s)   [F10]
  → job → contact upsert, conversation upsert, ledger entry  [F11]
  → automation engine: keyword rules first                   [F13]
      match?  → enqueue reply
      no match → FAQ full-text + trigram match               [F14]
          confidence above threshold → enqueue answer
          below threshold → log to unmatched + escalate
  → send job → ledger row → Cloud API → wamid stored         [F09]
  → status webhooks → status events appended                 [F11]
```

Every arrow crosses a durable boundary (DB row or job). Nothing is held in memory. That's why
a crash anywhere loses nothing.

## Flow 3 — Manual reply from the inbox

```text
Owner opens /inbox → sees conversation with a 24h window countdown  [F20]
  Window open   → free-text reply allowed
  Window closed → free-text disabled, explanation shown, template required
Reply → enqueued send job → same path as Flow 2
```

Do not let the UI offer a free-text reply when the window has closed. Meta will reject it and
the customer will blame you.

## Flow 4 — Subscription lifecycle

```text
Register → TRIALING (14 days, full features)              [F21]
Trial ending → email at day 10 and day 13
Pay via UPI AutoPay (default) or card → ACTIVE
Payment fails → PAST_DUE
  → email immediately, retry per Razorpay schedule
  → 7-day grace: outbound sends blocked, login and export STILL WORK
  → recovered → ACTIVE
  → not recovered → EXPIRED (data retained 90 days)
```

**Never lock a customer out of their own conversation history over a failed card.** Block
sending; keep reading and exporting. Anything else earns a chargeback and a review that costs
you more than the ₹1,999.

## Flow 5 — Customer wants to leave

```text
Settings → Billing → Cancel
  → confirm screen: what stops, what's retained, how to export
  → cancel at period end (not immediately — they paid for the month)
  → offer a data export
  → one question: "what would have kept you?" (optional, free text)
```

Make this easy and honest. In the Tech Provider model the customer owns their WABA, so they
can walk away regardless — a hostile cancellation flow just costs you the referral.
