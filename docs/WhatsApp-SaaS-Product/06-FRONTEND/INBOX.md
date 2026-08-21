# Inbox

Increment **F20**. Routes `/inbox`, `/inbox/:id`.

## Purpose

See what the automation is doing, and step in when it can't cope. Not a full helpdesk — no
assignment, no tags, no SLAs, no canned responses in the MVP.

## Conversation list — `/inbox`

```text
┌────────────────────────────────────────┐
│ Inbox            [All ▾] [Needs reply] │
├────────────────────────────────────────┤
│ Rahul Sharma          ●●●●●● 4321      │
│ "do you have size 10?"                 │
│ 4 min ago        🟢 Window 23h 56m     │
├────────────────────────────────────────┤
│ ●●●●●● 8877                            │
│ You: "Our timings are 10am-8pm"  ✓✓    │
│ 2 hours ago      🟢 Window 21h 12m     │
├────────────────────────────────────────┤
│ Priya M.              ●●●●●● 1122      │
│ "thanks!"                              │
│ 2 days ago       ⚪ Window closed       │
└────────────────────────────────────────┘
```

Filters: All / Needs reply (escalated or unmatched) / Window closing soon. Nothing more.

Sort by last activity. Paginate; don't load everything.

## The 24-hour service window

This is the one concept you must get right, because Meta enforces it and customers don't
understand it.

- Window opens when the end customer messages the business
- Window lasts **24 hours** from that message
- **Inside** the window: free-text replies allowed
- **Outside** the window: only approved templates

Show a live countdown per conversation. Colour it: green >6h, amber <6h, grey closed.

## Conversation thread — `/inbox/:id`

```text
┌────────────────────────────────────────┐
│ ← Rahul Sharma  ●●●●●● 4321            │
│ 🟢 Window closes in 23h 56m            │
├────────────────────────────────────────┤
│                    "hi"          10:02 │
│  Auto-reply 🤖                         │
│  "Hello! How can we help?"       10:02 │
│                                     ✓✓ │
│           "do you have size 10?" 10:05 │
│  ⚠️ No confident match — handed to you │
├────────────────────────────────────────┤
│ [ Type a reply...            ] [Send]  │
└────────────────────────────────────────┘
```

- Mark automated messages distinctly (🤖). The owner needs to know what was said in their name.
- Show delivery status: ✓ sent, ✓✓ delivered, ✓✓ blue read, ⚠️ failed.
- Failed messages show the reason **in plain language**, not the Meta error code:
  `131026` → "This number isn't on WhatsApp"
  `132000` → "Template variable count doesn't match"
  Keep a small code→message map; fall back to the raw code with a support link.

### When the window is closed

Disable the text input. Replace it with:

> **You can't send a free message now.** WhatsApp only allows free-text replies within 24
> hours of the customer's last message. Send an approved template instead.
> [ Choose a template ]

Do not let the user type something, hit send, and get a Meta error. That's a support ticket
you designed in.

## New messages

Polling on an interval (e.g. 15s while the tab is focused, paused when hidden) is fine.
**Do not add WebSockets or SSE in the MVP** — you have one instance, a handful of users, and
polling costs you nothing. Revisit when concurrent users make it hurt.

## Privacy in the UI

Mask phone numbers to the last 4 digits by default with a reveal action. The full number is
in `contacts` because you need it to reply; the ledger stores only hash + last4. Reflect that
care in the UI — it's a DPDP posture, and screenshots get shared.

## Not in the MVP

Agent assignment · internal notes · tags · canned responses · search across all messages ·
media gallery · export · read receipts for the owner · typing indicators.
