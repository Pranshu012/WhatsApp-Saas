# Scheduling

**Classification: BUILD NOW (F16).**

## Design

```text
User schedules → scheduled_messages (status=SCHEDULED, scheduled_for UTC)
                        │
Every minute: ENQUEUE_DUE_SCHEDULED_MESSAGES job
                        │
                        ├── claim due rows (SKIP LOCKED)
                        ├── mark ENQUEUED
                        └── enqueue SEND_WHATSAPP_MESSAGE
                                 with key = sched:{scheduled_message_id}
                        │
                 normal send path (F09)
                        │
                 mark SENT / FAILED
```

Two levels of jobs: one that finds due messages, one that sends each. This keeps the finder
cheap and lets each send retry independently.

## Time handling

```text
Store:  scheduled_for  timestamptz  (UTC, always)
        timezone       text         (the tenant's zone, explicitly)
Render: IST in the frontend
```

India has no DST, so this is simpler than in most markets — but **store the timezone anyway**.
The cost is one column; the cost of not doing it is a rewrite when your first non-IST customer
appears.

```java
// user picks "18 Aug 2026, 9:00 AM" in their tenant timezone
ZonedDateTime local = LocalDateTime.of(2026, 8, 18, 9, 0).atZone(tenantZone);
Instant scheduledFor = local.toInstant();      // store this
```

## The finder

```java
@Component
class EnqueueDueScheduledMessagesHandler implements JobHandler {
    public String jobType() { return "ENQUEUE_DUE_SCHEDULED_MESSAGES"; }

    public void handle(Job job) {
        List<ScheduledMessage> due = repository.claimDue(200);   // SKIP LOCKED
        for (var sm : due) {
            messagingService.send(new SendCommand(
                sm.tenantId(), sm.contactId(), sm.templateId(), sm.variables(),
                "sched:" + sm.id()                              // deterministic key
            ));
            sm.markEnqueued();
        }
    }
}
```

**The deterministic idempotency key is the whole safety mechanism.** Two scheduler runs in the
same minute, or a crash and retry, cannot double-send — the unique index on
`jobs.idempotency_key` rejects the second enqueue. This matters because a duplicate costs your
customer real money.

Re-enqueue this finder job on a recurring basis. Simplest approach: a `@Scheduled` method under
the `worker` profile that enqueues it every minute with key
`due-scan:{yyyy-MM-dd'T'HH:mm}` — so even overlapping timer fires produce one job per minute.

## Free-text vs template — reject early

Outside the 24-hour service window only templates are permitted. So:

```java
if (cmd.templateId() == null && !conversationService.isWindowOpen(cmd.contactId())) {
    throw new ConflictException("SCHEDULED_FREE_TEXT_NOT_ALLOWED",
        "Scheduled messages must use an approved template, because the 24-hour reply " +
        "window will likely have closed by the send time.");
}
```

Reject at **creation** time, not send time. A user who schedules something for next Tuesday
should learn immediately, not discover a failure next Tuesday.

## Cancellation — honest semantics

| Current status | Cancel? |
|---|---|
| `SCHEDULED` | ✅ Yes → `CANCELLED` |
| `ENQUEUED` | ❌ 409 — the send job is already queued |
| `SENT` | ❌ 409 — WhatsApp has no unsend |
| `FAILED`/`CANCELLED` | ❌ 409 — terminal |

Do not pretend to cancel something already in flight. A UI that says "cancelled" while the
message goes out is worse than an honest error.

## Test cases

| Test | Expect |
|---|---|
| Scheduled +2 min | Sends once, around the right time |
| Scheduler runs twice in a minute | **One** send (idempotency key) |
| Cancel while `SCHEDULED` | Succeeds |
| Cancel while `ENQUEUED` | 409 |
| Free-text scheduled | Rejected at creation |
| Timezone conversion | 9 AM IST → correct UTC instant |
| Worker crash after claim | Message re-enqueued, still one send |

## DO NOT BUILD YET

Recurring schedules (daily/weekly) · bulk campaigns · per-contact timezone · send-time
optimisation · quiet hours · calendar integration.

**Recurrence trigger:** three customers asking for it. Not one, not a guess.
