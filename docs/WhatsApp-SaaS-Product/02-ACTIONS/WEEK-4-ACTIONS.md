# Week 4

**Theme: WhatsApp round trip working end to end.**

By Friday, a message sent to your test number should produce a reply, with a full audit trail.

## Monday — F06 (Embedded Signup)
- [ ] Read `08-META-WHATSAPP/EMBEDDED-SIGNUP.md` fully
- [ ] Run the **F06** prompt
- [ ] Verify the **subscribe-app-to-WABA** call exists — the classic omission
- [ ] Verify WABA/phone IDs are checked against Meta, not trusted from the client
- [ ] Verify reconnect is idempotent
- [ ] Connect your own test business end to end
- [ ] Commit

**Blocked on App Review.** If still pending, do F09 and F10 first and come back.

## Tuesday — F09 (outbound send)
- [ ] Run the **F09** prompt
- [ ] Verify ledger-first ordering
- [ ] Verify the error classification table (transient vs permanent)
- [ ] Verify nothing calls `WhatsAppCloudClient` outside `job.handler`
- [ ] **Send a real message to your own phone via the queue**
- [ ] Commit

First real message. Worth pausing on.

## Wednesday — F10 (webhooks)
- [ ] Read `05-BACKEND/WEBHOOK-IMPLEMENTATION.md` fully
- [ ] Start a tunnel: `cloudflared tunnel --url http://localhost:8080`
- [ ] Configure the webhook URL in the Meta App Dashboard
- [ ] Run the **F10** prompt
- [ ] Verify HMAC uses **raw bytes**, verified before parsing
- [ ] Verify p99 under 2 seconds
- [ ] Send yourself a WhatsApp message and watch it arrive
- [ ] Commit

## Thursday — F11 (inbound processing)
- [ ] Run the **F11** prompt
- [ ] Verify contact and conversation upsert without duplicating
- [ ] Verify `service_window_expires_at` is computed correctly
- [ ] Verify status callbacks append ledger events
- [ ] Verify unknown event types are marked `IGNORED`, not thrown
- [ ] Commit

## Friday — Phase B checkpoint
Do this properly. It's the gate to Phase C.

- [ ] **Full round trip:** message in → contact + conversation created → reply enqueued → sent →
      status callback recorded
- [ ] `./mvnw clean verify` green
- [ ] Kill the app mid-send, restart, confirm **no duplicate** reached the phone
- [ ] Search the codebase: the token appears only in `TokenCipher` and the account service
- [ ] `grep -ri token` over your logs returns nothing sensitive
- [ ] Update `CURRENT-STATUS.md`

## Before Phase C

**D-01 must be closed.** If your validation conversations didn't produce a clear answer, do more
conversations. Do **not** start the automation engine on a guess — it's the most expensive place
to be wrong.

## End of week 4

- [ ] F00–F11 complete: the engine works
- [ ] You can send and receive WhatsApp messages per tenant with a full ledger
- [ ] Crash-safety verified by hand
- [ ] D-01 answered

## Watch out for

- Configuring the webhook before the app is deployed and responding — the handshake fails
- Free tunnel URLs changing on restart; reconfigure each time
- Committing the App Secret. If you do: rotate it immediately and assume it's compromised.
