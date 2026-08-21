# WhatsApp Service — Implementation

**Classification: BUILD NOW (F06, F09).** Concepts in `03-ARCHITECTURE/WHATSAPP-INTEGRATION.md`.

## Two clients, separate concerns

| Client | Purpose |
|---|---|
| `MetaGraphClient` | Onboarding and management: token exchange, WABA details, webhook subscription, template CRUD |
| `WhatsAppCloudClient` | Sending messages |

Both go through one configured `RestClient`/`WebClient` with the Graph version from config
(`app.meta.graph-version`), explicit timeouts, and typed error mapping.

**Pin the Graph version in config, not in call sites.** Meta deprecates versions; you want to
change one property, not thirty URLs.

## Embedded Signup — the onboarding sequence

```java
@Transactional
public WhatsAppAccount connect(ConnectRequest req) {
    UUID tenantId = TenantContext.require();

    // 1. exchange the code for a business token scoped to the customer's WABA
    String token = graphClient.exchangeCodeForToken(req.code());

    // 2. verify the IDs against Meta — do NOT trust the client's values
    WabaDetails details = graphClient.getWabaDetails(req.wabaId(), token);
    PhoneNumberDetails phone = graphClient.getPhoneNumber(req.phoneNumberId(), token);

    // 3. subscribe OUR app to THIS WABA's webhooks
    //    Forget this and nothing ever arrives. Most common onboarding bug.
    graphClient.subscribeAppToWaba(req.wabaId(), token);

    // 4. persist with the token encrypted
    return accountService.upsert(tenantId, details, phone, cipher.encrypt(token));
}
```

Step 2 matters: the client supplies `wabaId` and `phoneNumberId`, and a malicious client could
supply someone else's. Verifying against Meta with the token you just obtained proves the token
actually grants access to those assets.

Step 4 is `upsert`, keyed on `(tenant_id, phone_number_id)`, so reconnecting updates rather than
duplicating.

## Sending

```java
public interface WhatsAppCloudClient {
    SendResult sendText(WhatsAppAccount acct, String toE164, String body);
    SendResult sendTemplate(WhatsAppAccount acct, String toE164, TemplateSend send);
    SendResult sendInteractive(WhatsAppAccount acct, String toE164, InteractiveSend send);
}

public record SendResult(String wamid) {}
```

**Only `job.handler` classes may call this.** Everything else uses `MessagingService`, which
enqueues. Enforce with ArchUnit — this is the rule most likely to be broken by accident, and the
consequence is either a slow webhook ACK or a duplicate charge to your customer.

```java
// MessagingService — the ONLY public send entry point
public void send(SendCommand cmd) {
    jobService.enqueue("SEND_WHATSAPP_MESSAGE", cmd, cmd.idempotencyKey());
}
```

## Error mapping

```java
sealed interface MetaError permits MetaTransientException, MetaPermanentException {}
```

| Meta code | Meaning | Class | Action |
|---|---|---|---|
| 200 | App lacks Advanced Access | Permanent | Alert **you** — a deployment problem, not the customer's |
| 190 | Token invalid/expired | Permanent | Mark account `TOKEN_EXPIRED`, notify customer, stop sending |
| 131026 | Recipient not on WhatsApp | Permanent | Show plainly in the inbox |
| 131047 | Outside the service window | Permanent | Requires a template — surface that |
| 131048 | Spam rate limit | Transient | Back off hard; check quality rating |
| 130429 | Rate limit | Transient | Back off |
| 132000/132001 | Template param mismatch / missing | Permanent | Fix the template config |
| 1, 2 | Meta internal | Transient | Retry |
| HTTP 5xx, timeout | — | Transient | Retry |

Getting this table right is what stops the system from retrying a permanently invalid number
five times, and from giving up on a transient Meta outage.

## Rate limiting

Meta enforces per-number send limits, and the messaging limit tier starts low for new WABAs.

MVP: a simple in-process per-account throttle (a token bucket in a `ConcurrentHashMap`).
We are single-instance, so this is sufficient and correct.

```java
// TODO(scale): in-process only — needs Redis when we run multiple workers.
// See 12-SCALING/WHEN-TO-INTRODUCE-REDIS.md. Do NOT add Redis now.
```

Leave that comment. It's the honest state of the code and it tells the next reader (or Claude
Code) not to "fix" it prematurely.

## Token lifecycle

Business tokens from Embedded Signup are long-lived, but can be revoked by the customer at any
time from Meta Business Suite — we cannot prevent that, and shouldn't try.

```text
On Meta error 190:
  1. mark whatsapp_accounts.status = TOKEN_EXPIRED
  2. stop enqueuing sends for that tenant (fail fast, clear message)
  3. surface prominently in the UI: "Reconnect your WhatsApp account"
  4. email the owner
  5. do NOT retry the job — no amount of retrying fixes a revoked token
```

## Payment-method detection

Meta does not offer a clean "does this WABA have a payment method?" flag. Practical approach:

1. Attempt a send; a payment-related failure marks `payment_method_attached = false`
2. Warn prominently at onboarding regardless, before any send is attempted
3. Ask the customer to confirm during the manual onboarding call (first 20 customers)

Treat the flag as best-effort. **The prominent warning at onboarding is what actually prevents
the problem** — this is the single most common failure in the Tech Provider model.

## Test cases

| Test | Expect |
|---|---|
| Connect happy path | Token encrypted, webhooks subscribed, account persisted |
| Reconnect same number | Updates, does not duplicate |
| Client supplies a WABA the token can't access | Rejected |
| Error 200 | Clear actionable message, not a generic 500 |
| Error 190 | Account marked `TOKEN_EXPIRED`, no retry |
| Send text/template/interactive | Correct payload shape (assert against Meta's documented JSON) |
| Transient error | Job retries |
| Permanent error | Job → DEAD immediately |
| Token never logged | Assert against captured log output |
| `WhatsAppCloudClient` called only from `job.handler` | ArchUnit |

Mock Meta with WireMock or MockWebServer. Never hit the real API in tests.
