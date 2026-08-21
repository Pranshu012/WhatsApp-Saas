# Webhook Implementation

**Classification: BUILD NOW (F10).** Design rationale in `03-ARCHITECTURE/WEBHOOK-ARCHITECTURE.md`.

## The flow

```text
Meta
 ↓
WebhookController
 ↓
Signature verification    ← BEFORE parsing. Raw bytes. Constant time.
 ↓
Validate event shape      ← shallow: is this a WhatsApp business_account event?
 ↓
Persist raw → webhook_events
 ↓
Enqueue job
 ↓
ACK 200                   ← target p99 < 2 seconds
 ⋮  (asynchronously)
Worker
 ↓
Business logic
```

## Getting the raw body — the thing everyone gets wrong

The HMAC is computed over the **exact bytes Meta sent**. If you accept `@RequestBody Map` and
reserialise, Jackson may reorder keys or alter whitespace, and the signature will never match.
You'll lose an afternoon to this.

Three workable approaches:

```java
// Simplest: accept the raw bytes
@PostMapping("/api/webhooks/whatsapp")
public ResponseEntity<Void> receive(
        @RequestBody byte[] rawBody,
        @RequestHeader("X-Hub-Signature-256") String signature) { ... }
```

Or register a `ContentCachingRequestWrapper` filter, or use an `HttpMessageConverter` that
retains the original bytes. Pick one and document it — the failure mode is confusing.

## Signature verification

```java
public boolean isValid(byte[] rawBody, String header) {
    if (header == null || !header.startsWith("sha256=")) return false;
    String provided = header.substring("sha256=".length());

    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(appSecret.getBytes(UTF_8), "HmacSHA256"));
    String computed = HexFormat.of().formatHex(mac.doFinal(rawBody));

    // constant time — never String.equals for a secret comparison
    return MessageDigest.isEqual(
        computed.getBytes(UTF_8), provided.getBytes(UTF_8));
}
```

Invalid → **403**, log a warning with source IP and body length (never the body), do not persist
as valid, do not enqueue. Meta will not retry a 403, which is correct — the request wasn't from
Meta.

## Verification endpoint (GET)

```java
@GetMapping("/api/webhooks/whatsapp")
public ResponseEntity<String> verify(
        @RequestParam("hub.mode") String mode,
        @RequestParam("hub.verify_token") String token,
        @RequestParam("hub.challenge") String challenge) {

    boolean ok = "subscribe".equals(mode)
        && MessageDigest.isEqual(token.getBytes(UTF_8), verifyToken.getBytes(UTF_8));

    return ok ? ResponseEntity.ok(challenge) : ResponseEntity.status(403).build();
}
```

Returns the challenge as **plain text**, not JSON. Meta compares it literally.

## Ingest service

```java
@Transactional
public void ingest(byte[] rawBody, boolean signatureValid) {
    JsonNode root = objectMapper.readTree(rawBody);   // parse AFTER verification

    String eventId       = extractEventId(root);      // may be null
    String phoneNumberId = extractPhoneNumberId(root);
    String wabaId        = extractWabaId(root);

    // ingest-level dedupe: unique index on event_id does the work
    try {
        WebhookEvent event = webhookEventRepository.save(new WebhookEvent(
            eventId, wabaId, phoneNumberId, root, signatureValid));
        jobService.enqueue("PROCESS_WEBHOOK_EVENT",
            Map.of("webhookEventId", event.getId()),
            "wh:" + event.getId());
    } catch (DataIntegrityViolationException duplicate) {
        log.info("Duplicate webhook event ignored: {}", eventId);
        // still return 200 — Meta must not retry
    }
}
```

Note: the persist and the enqueue are in **one transaction**. Either both happen or neither.
This is exactly the transactional coupling an external broker cannot give you (ADR-002).

## Controller — deliberately dumb

```java
@PostMapping("/api/webhooks/whatsapp")
public ResponseEntity<Void> receive(@RequestBody byte[] raw,
                                    @RequestHeader(value = "X-Hub-Signature-256", required = false) String sig) {
    if (!verifier.isValid(raw, sig)) {
        log.warn("Invalid webhook signature, bodyLength={}", raw.length);
        return ResponseEntity.status(403).build();
    }
    ingestService.ingest(raw, true);
    return ResponseEntity.ok().build();
}
```

**That's the whole controller.** No automation, no Meta calls, no template lookups. If it can be
slow or throw, it belongs in the worker.

## Worker handler

```java
@Component
class ProcessWebhookEventHandler implements JobHandler {
    public String jobType() { return "PROCESS_WEBHOOK_EVENT"; }

    @Transactional
    public void handle(Job job) {
        WebhookEvent event = load(job);
        // resolve tenant from phone_number_id
        var account = accountRepo.findByPhoneNumberId(event.phoneNumberId())
            .orElseThrow(() -> new PermanentJobException("Unknown phone_number_id"));

        TenantContext.set(account.tenantId());
        try {
            switch (classify(event.rawPayload())) {
                case INBOUND_MESSAGE -> inboundProcessor.process(event);
                case STATUS_UPDATE   -> ledgerService.recordStatusEvent(event);
                case TEMPLATE_STATUS -> templateService.applyStatusUpdate(event);
                default -> markIgnored(event, "Unsupported event type");
            }
            markProcessed(event);
        } finally {
            TenantContext.clear();     // MUST be in finally
        }
    }
}
```

Unknown `phone_number_id` is a **permanent** failure, not retryable — retrying won't make the
account appear. Unknown event *types* are `IGNORED` with a reason, never an exception: Meta adds
event types over time and you don't want a stack trace every time they do.

## Logging

```java
// GOOD
log.info("Webhook received eventId={} type={} phoneNumberId={} latencyMs={}", ...);

// BAD — message bodies contain end-customer PII
log.info("Webhook payload: {}", rawBody);
```

## Test cases (increment F10)

| Test | Expect |
|---|---|
| Valid signature | 200, event persisted, job enqueued |
| Tampered body | 403, not persisted as valid, no job |
| Missing signature header | 403 |
| Malformed JSON with a valid signature | Persisted, job enqueued, handler marks `IGNORED` |
| Same `event_id` twice | 200 both times, one event row, one job |
| Verification handshake, correct token | 200 with the challenge as plain text |
| Verification handshake, wrong token | 403 |
| Latency benchmark | p99 under 2s |
| Controller does not touch handlers | Assert via ArchUnit or a mock that is never called |

## Common mistakes

| Mistake | Consequence |
|---|---|
| HMAC over reserialised JSON | Signature never matches; hours lost |
| Verifying after parsing | Parsing attacker-controlled input |
| `String.equals` for the signature | Timing attack on your app secret |
| Business logic in the controller | Slow ACK → Meta retries → duplicates → eventually disabled |
| Returning 500 on a duplicate | Meta retries forever |
| No dedupe | Duplicate replies, which cost your customer money |
| Forgetting `TenantContext.clear()` | Cross-tenant leak via thread reuse |
| Logging the payload | End-customer PII in your logs and in Sentry |
