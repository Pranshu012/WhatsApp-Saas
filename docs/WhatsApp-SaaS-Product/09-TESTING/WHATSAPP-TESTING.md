# WhatsApp Testing

You cannot automate against Meta's real API in CI. Strategy: mock the HTTP boundary for
automated tests, and keep a short manual checklist for the real thing.

## Layer 1 — Mock the Graph API (automated)

WireMock or MockWebServer. Test **your** behaviour, not Meta's.

```java
@Test
void ledgerRowExistsBeforeApiCall() {
    metaMock.stubFor(post(urlPathMatching("/v[0-9.]+/.*/messages"))
        .willReturn(okJson("""
            {"messages":[{"id":"wamid.TEST123"}]}""")));

    var ledgerId = messagingService.send(tenantId, "+919876543210", "hello", "test:key:1");
    worker.pollAndProcessAll();

    assertThat(ledgerRepository.findById(ledgerId))
        .get().extracting(Ledger::getWamid).isEqualTo("wamid.TEST123");
}
```

Cases to cover:

| Scenario | Stub | Expect |
|---|---|---|
| Success | 200 + wamid | Ledger `SENT`, wamid attached |
| Rate limited | 429 / code 80007 | Job retries with backoff |
| Server error | 500 | Job retries |
| Invalid number | code 131026 | `DEAD` immediately, no retry, plain-language error |
| Template mismatch | code 132000 | `DEAD` immediately |
| **Missing Advanced Access** | **code 200** | Clear actionable message, not a generic 500 |
| Expired token | code 190 | Account marked token-expired, tenant notified |
| Timeout | delay > timeout | Retry, and **no duplicate send** |

**Meta error code 200 deserves special attention.** It means your app lacks Advanced Access for
`whatsapp_business_management` / `whatsapp_business_messaging`. Every call against a customer
WABA fails this way until App Review approves you. If it surfaces as "Internal Server Error"
you'll waste days.

## Layer 2 — Webhook payload fixtures

Save real payloads as JSON fixtures in `src/test/resources/webhooks/`. Get them from Meta's docs
and from your own dev account once connected.

```text
inbound-text.json
inbound-image.json
inbound-button-reply.json
inbound-list-reply.json
status-sent.json
status-delivered.json
status-read.json
status-failed.json
template-status-update.json
unknown-event-type.json
```

Test each through the full pipeline: receiver → persisted event → job → domain effect.

**Test the HMAC properly:** compute the signature over the fixture's exact bytes, then also send
a modified body with the original signature and assert 403. That's what proves you verify raw
bytes rather than a re-serialised object.

## Layer 3 — Manual testing with real Meta

### Dev setup

1. A Meta app in **Development** mode
2. A WABA on a **test number** (Meta provides free test numbers with limited allowances) or a
   second real SIM
3. **ngrok** (or a Cloudflare tunnel) to receive webhooks on localhost:
   ```bash
   ngrok http 8080
   # set the webhook callback to https://<id>.ngrok.io/api/webhooks/whatsapp
   ```
4. Your personal WhatsApp as the "end customer"

Meta's test numbers can send to a small allowlist of verified recipients only. Add your own
number there first.

### The manual checklist (run before each release)

- [ ] Embedded Signup completes; token stored encrypted; webhooks subscribed
- [ ] Abandoning the popup mid-flow leaves clean UI state
- [ ] Inbound text → contact + conversation + ledger row created
- [ ] Keyword rule fires; auto-reply arrives on the phone
- [ ] FAQ match answers a typo'd question
- [ ] Below-threshold question escalates instead of guessing
- [ ] Button message renders and the reply is attributed correctly
- [ ] Delivery statuses (sent → delivered → read) appear in the inbox
- [ ] Send to an invalid number → plain-language failure, no retry storm
- [ ] Manual reply inside the window works
- [ ] Window-closed state disables free text and explains why
- [ ] Scheduled message sends **once** (run the scheduler twice deliberately)
- [ ] Template sync mirrors Meta; a rejected template shows Meta's reason
- [ ] Dashboard counts match a direct ledger query

## Testing the money-loss scenarios explicitly

Your customer pays Meta directly, so a send bug spends their money. Test these on purpose:

| Risk | Test |
|---|---|
| Duplicate send after a timeout | Stub a timeout, then a success; assert exactly one wamid and one ledger row |
| Retry storm | Stub persistent 500s; assert attempts capped and the job reaches `DEAD` |
| Auto-reply loop | Two rules that could trigger each other; assert per-contact rate limit stops it |
| Fan-out mistake | A rule with 3 actions; assert the consolidation warning and the actual send count |
| Wrong category | A marketing-worded template; assert Meta's category is stored, not the requested one |

## Never in a test

- Real customer phone numbers
- Real access tokens (fixtures use `TEST_TOKEN_DO_NOT_USE`)
- Sending to numbers you don't own — you'd be spending real money and possibly spamming
- Pointing tests at production credentials
