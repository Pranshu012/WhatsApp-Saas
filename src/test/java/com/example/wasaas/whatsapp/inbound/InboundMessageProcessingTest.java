package com.example.wasaas.whatsapp.inbound;

import com.example.wasaas.contact.Contact;
import com.example.wasaas.contact.ContactRepository;
import com.example.wasaas.contact.Conversation;
import com.example.wasaas.contact.ConversationRepository;
import com.example.wasaas.contact.ConversationStatus;
import com.example.wasaas.job.JobRepository;
import com.example.wasaas.job.JobStatus;
import com.example.wasaas.job.JobWorker;
import com.example.wasaas.ledger.BillingCategory;
import com.example.wasaas.ledger.MessageDirection;
import com.example.wasaas.ledger.MessageLedger;
import com.example.wasaas.ledger.MessageLedgerRepository;
import com.example.wasaas.ledger.MessageLedgerStatus;
import com.example.wasaas.ledger.MessageLedgerStatusEvent;
import com.example.wasaas.ledger.MessageLedgerStatusEventRepository;
import com.example.wasaas.ledger.PhonePrivacyUtils;
import com.example.wasaas.tenant.RegistrationCommand;
import com.example.wasaas.tenant.TenantRepository;
import com.example.wasaas.tenant.TenantService;
import com.example.wasaas.tenant.context.TenantContext;
import com.example.wasaas.whatsapp.SaveWhatsAppAccountCommand;
import com.example.wasaas.whatsapp.WhatsAppAccount;
import com.example.wasaas.whatsapp.WhatsAppAccountService;
import com.example.wasaas.whatsapp.meta.MetaProperties;
import com.example.wasaas.whatsapp.webhook.WebhookEvent;
import com.example.wasaas.whatsapp.webhook.WebhookEventRepository;
import com.example.wasaas.whatsapp.webhook.WebhookEventStatus;
import com.example.wasaas.whatsapp.webhook.WebhookIngestService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "app.jobs.poll-interval-ms=1000000",
    "app.jobs.batch-size=10",
    "app.jobs.lock-timeout-secs=300"
})
@ActiveProfiles({"local", "worker"})
@org.springframework.context.annotation.Import(InboundMessageProcessingTest.TestEventListener.class)
public class InboundMessageProcessingTest {

    @Autowired private WebhookIngestService ingestService;
    @Autowired private JobWorker jobWorker;
    @Autowired private JobRepository jobRepository;
    @Autowired private WebhookEventRepository webhookEventRepository;
    @Autowired private ContactRepository contactRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private MessageLedgerRepository ledgerRepository;
    @Autowired private MessageLedgerStatusEventRepository statusEventRepository;
    @Autowired private com.example.wasaas.ledger.LedgerService ledgerService;
    @Autowired private WhatsAppAccountService accountService;
    @Autowired private TenantService tenantService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private TestEventListener testEventListener;

    private UUID tenantAId;
    private WhatsAppAccount accountA;

    private static final String PHONE_ID_A = "phone_id_tenant_a_1001";
    private static final String WABA_ID_A = "waba_id_tenant_a_2001";

    @BeforeEach
    void setup() {
        cleanup();
        testEventListener.events.clear();

        tenantService.registerTenant(new RegistrationCommand(
                "Tenant A Business",
                "tenant-a-biz",
                "Tenant A Admin",
                "admin.a@example.com",
                "Password123!"
        ));
        tenantAId = tenantRepository.findBySlug("tenant-a-biz").orElseThrow().getId();
        TenantContext.set(tenantAId);

        accountA = accountService.saveOrUpdateAccount(new SaveWhatsAppAccountCommand(
                WABA_ID_A,
                PHONE_ID_A,
                "+1 555-0101",
                "Tenant A Store",
                "GREEN",
                "TIER_10K",
                "VALID_TOKEN_A"
        ));
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        jdbcTemplate.execute("TRUNCATE TABLE message_ledger_status_events, message_ledger, conversations, contacts, webhook_events, jobs, whatsapp_accounts, spring_session_attributes, spring_session, password_reset_tokens, login_attempts, tenant_users, users, tenants CASCADE");
    }

    @Test
    void testInboundMessageCreatesContactConversationLedgerAndEmitsEvent() {
        long epochSec = 1700000000L;
        Instant msgTime = Instant.ofEpochSecond(epochSec);

        String payload = """
                {
                  "object": "whatsapp_business_account",
                  "entry": [{
                    "id": "%s",
                    "changes": [{
                      "value": {
                        "messaging_product": "whatsapp",
                        "metadata": {
                          "display_phone_number": "15550101",
                          "phone_number_id": "%s"
                        },
                        "contacts": [{
                          "profile": { "name": "Alice" },
                          "wa_id": "15551234567"
                        }],
                        "messages": [{
                          "from": "15551234567",
                          "id": "wamid.INBOUND001",
                          "timestamp": "%d",
                          "text": { "body": "Need order help" },
                          "type": "text"
                        }]
                      },
                      "field": "messages"
                    }]
                  }]
                }
                """.formatted(WABA_ID_A, PHONE_ID_A, epochSec);

        // Ingest webhook
        ingestService.ingest(payload.getBytes(StandardCharsets.UTF_8), true);

        // Process queue
        jobWorker.poll();

        TenantContext.set(tenantAId);

        // 1. Verify Contact created with full phone number
        Contact contact = contactRepository.findByTenantIdAndPhoneE164(tenantAId, "+15551234567").orElseThrow();
        assertThat(contact.getDisplayName()).isEqualTo("Alice");
        assertThat(contact.getPhoneHash()).isEqualTo(PhonePrivacyUtils.hashPhoneNumber("+15551234567"));
        assertThat(contact.getLastSeenAt()).isEqualTo(msgTime);

        // 2. Verify Conversation created with 24-hour service window expiry
        Conversation conversation = conversationRepository
                .findByTenantIdAndContactIdAndWhatsappAccountId(tenantAId, contact.getId(), accountA.getId())
                .orElseThrow();
        assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.OPEN);
        assertThat(conversation.getLastInboundAt()).isEqualTo(msgTime);
        assertThat(conversation.getServiceWindowExpiresAt()).isEqualTo(msgTime.plus(24, ChronoUnit.HOURS));

        // 3. Verify Message Ledger recorded with INBOUND_FREE
        MessageLedger ledger = ledgerRepository.findByWamid("wamid.INBOUND001").orElseThrow();
        assertThat(ledger.getTenantId()).isEqualTo(tenantAId);
        assertThat(ledger.getDirection()).isEqualTo(MessageDirection.INBOUND);
        assertThat(ledger.getBillingCategory()).isEqualTo(BillingCategory.INBOUND_FREE);
        assertThat(ledger.getRecipientPhoneLast4()).isEqualTo("4567");

        // 4. Verify Spring domain event received
        assertThat(testEventListener.events).hasSize(1);
        InboundMessageReceivedEvent evt = testEventListener.events.get(0);
        assertThat(evt.wamid()).isEqualTo("wamid.INBOUND001");
        assertThat(evt.fromE164()).isEqualTo("+15551234567");
        assertThat(evt.text()).isEqualTo("Need order help");
        assertThat(evt.serviceWindowExpiresAt()).isEqualTo(msgTime.plus(24, ChronoUnit.HOURS));

        // 5. Verify WebhookEvent marked PROCESSED
        WebhookEvent webhookEvent = webhookEventRepository.findAll().get(0);
        assertThat(webhookEvent.getStatus()).isEqualTo(WebhookEventStatus.PROCESSED);
    }

    @Test
    void testRepeatInboundUpdatesRatherThanDuplicating() {
        long t1 = 1700000000L;
        long t2 = 1700007200L; // 2 hours later

        String payload1 = createMessagePayload(WABA_ID_A, PHONE_ID_A, "15551234567", "Alice", "wamid.REPEAT01", t1, "Msg 1");
        String payload2 = createMessagePayload(WABA_ID_A, PHONE_ID_A, "15551234567", "Alice Smith", "wamid.REPEAT02", t2, "Msg 2");

        ingestService.ingest(payload1.getBytes(StandardCharsets.UTF_8), true);
        jobWorker.poll();

        ingestService.ingest(payload2.getBytes(StandardCharsets.UTF_8), true);
        jobWorker.poll();

        TenantContext.set(tenantAId);

        // Single contact updated
        List<Contact> contacts = contactRepository.findAll();
        assertThat(contacts).hasSize(1);
        assertThat(contacts.get(0).getDisplayName()).isEqualTo("Alice Smith");
        assertThat(contacts.get(0).getLastSeenAt()).isEqualTo(Instant.ofEpochSecond(t2));

        // Single conversation refreshed
        List<Conversation> conversations = conversationRepository.findAll();
        assertThat(conversations).hasSize(1);
        assertThat(conversations.get(0).getLastInboundAt()).isEqualTo(Instant.ofEpochSecond(t2));
        assertThat(conversations.get(0).getServiceWindowExpiresAt()).isEqualTo(Instant.ofEpochSecond(t2).plus(24, ChronoUnit.HOURS));

        // Two ledger records
        List<MessageLedger> ledgers = ledgerRepository.findAll();
        assertThat(ledgers).hasSize(2);
    }

    @Test
    void testServiceWindowAcrossTimezoneBoundary() {
        // 2026-10-01 23:59:00 UTC
        Instant edgeTimestamp = Instant.parse("2026-10-01T23:59:00Z");
        long epochSec = edgeTimestamp.getEpochSecond();

        String payload = createMessagePayload(WABA_ID_A, PHONE_ID_A, "15559998888", "Timezone User", "wamid.TZ001", epochSec, "Boundary test");
        ingestService.ingest(payload.getBytes(StandardCharsets.UTF_8), true);
        jobWorker.poll();

        TenantContext.set(tenantAId);
        Conversation conversation = conversationRepository.findAll().get(0);

        // Must expire exactly 2026-10-02 23:59:00 UTC
        assertThat(conversation.getServiceWindowExpiresAt()).isEqualTo(Instant.parse("2026-10-02T23:59:00Z"));
    }

    @Test
    void testStatusCallbackAppendsToLedger() {
        // Pre-create outbound message in ledger
        String outboundWamid = "wamid.OUTBOUND_STATUS_TEST";
        ledgerService.recordInboundMessage(accountA.getId(), "+15553334444", outboundWamid, Instant.now());

        String statusPayload = """
                {
                  "object": "whatsapp_business_account",
                  "entry": [{
                    "id": "%s",
                    "changes": [{
                      "value": {
                        "messaging_product": "whatsapp",
                        "metadata": { "phone_number_id": "%s" },
                        "statuses": [{
                          "id": "%s",
                          "status": "delivered",
                          "timestamp": "1700001000"
                        }]
                      },
                      "field": "messages"
                    }]
                  }]
                }
                """.formatted(WABA_ID_A, PHONE_ID_A, outboundWamid);

        ingestService.ingest(statusPayload.getBytes(StandardCharsets.UTF_8), true);
        jobWorker.poll();

        TenantContext.set(tenantAId);

        MessageLedger ledger = ledgerRepository.findByWamid(outboundWamid).orElseThrow();
        assertThat(ledger.getStatus()).isEqualTo(MessageLedgerStatus.DELIVERED);

        List<MessageLedgerStatusEvent> events = statusEventRepository.findByLedgerIdOrderByOccurredAtAsc(ledger.getId());
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getStatus()).isEqualTo(MessageLedgerStatus.DELIVERED);
    }

    @Test
    void testUnknownEventTypeMarkedIgnored() {
        String unknownPayload = """
                {
                  "object": "whatsapp_business_account",
                  "entry": [{
                    "id": "%s",
                    "changes": [{
                      "field": "security",
                      "value": {
                        "metadata": { "phone_number_id": "%s" },
                        "event": "pin_change"
                      }
                    }]
                  }]
                }
                """.formatted(WABA_ID_A, PHONE_ID_A);

        ingestService.ingest(unknownPayload.getBytes(StandardCharsets.UTF_8), true);
        jobWorker.poll();

        WebhookEvent event = webhookEventRepository.findAll().get(0);
        assertThat(event.getStatus()).isEqualTo(WebhookEventStatus.IGNORED);
        assertThat(jobRepository.findAll().get(0).getStatus()).isEqualTo(JobStatus.SUCCEEDED);
    }

    @Test
    void testInboundForTenantANeverVisibleToTenantB() {
        // Register Tenant B
        tenantService.registerTenant(new RegistrationCommand(
                "Tenant B Business",
                "tenant-b-biz",
                "Tenant B Admin",
                "admin.b@example.com",
                "Password123!"
        ));
        UUID tenantBId = tenantRepository.findBySlug("tenant-b-biz").orElseThrow().getId();

        // Inbound message for Tenant A
        String payload = createMessagePayload(WABA_ID_A, PHONE_ID_A, "15551112222", "Tenant A Customer", "wamid.A_ONLY", 1700000000L, "Tenant A message");
        ingestService.ingest(payload.getBytes(StandardCharsets.UTF_8), true);
        jobWorker.poll();

        // Switch to Tenant B context
        TenantContext.set(tenantBId);

        // Tenant B should see nothing
        assertThat(contactRepository.findAll()).isEmpty();
        assertThat(conversationRepository.findAll()).isEmpty();
        assertThat(ledgerRepository.findAll()).isEmpty();
    }

    private String createMessagePayload(String wabaId, String phoneId, String from, String name, String wamid, long timestamp, String text) {
        return """
                {
                  "object": "whatsapp_business_account",
                  "entry": [{
                    "id": "%s",
                    "changes": [{
                      "value": {
                        "messaging_product": "whatsapp",
                        "metadata": {
                          "display_phone_number": "15550100",
                          "phone_number_id": "%s"
                        },
                        "contacts": [{
                          "profile": { "name": "%s" },
                          "wa_id": "%s"
                        }],
                        "messages": [{
                          "from": "%s",
                          "id": "%s",
                          "timestamp": "%d",
                          "text": { "body": "%s" },
                          "type": "text"
                        }]
                      },
                      "field": "messages"
                    }]
                  }]
                }
                """.formatted(wabaId, phoneId, name, from, from, wamid, timestamp, text);
    }

    @Component
    static class TestEventListener {
        final List<InboundMessageReceivedEvent> events = new CopyOnWriteArrayList<>();

        @EventListener
        public void onInboundMessage(InboundMessageReceivedEvent event) {
            events.add(event);
        }
    }
}
