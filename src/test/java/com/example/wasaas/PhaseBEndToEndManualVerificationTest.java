package com.example.wasaas;

import com.example.wasaas.contact.Contact;
import com.example.wasaas.contact.ContactRepository;
import com.example.wasaas.contact.Conversation;
import com.example.wasaas.contact.ConversationRepository;
import com.example.wasaas.contact.ConversationStatus;
import com.example.wasaas.job.Job;
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
import com.example.wasaas.whatsapp.WhatsAppAccount;
import com.example.wasaas.whatsapp.WhatsAppAccountRepository;
import com.example.wasaas.whatsapp.WhatsAppAccountStatus;
import com.example.wasaas.whatsapp.client.WhatsAppCloudClient;
import com.example.wasaas.whatsapp.meta.MetaGraphClient;
import com.example.wasaas.whatsapp.meta.MetaProperties;
import com.example.wasaas.whatsapp.send.MessagingService;
import com.example.wasaas.whatsapp.webhook.WebhookEvent;
import com.example.wasaas.whatsapp.webhook.WebhookEventRepository;
import com.example.wasaas.whatsapp.webhook.WebhookEventStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "app.jobs.poll-interval-ms=1000000",
    "app.jobs.batch-size=10",
    "app.jobs.lock-timeout-secs=300"
})
@AutoConfigureMockMvc
@ActiveProfiles({"local", "worker"})
public class PhaseBEndToEndManualVerificationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TenantService tenantService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private WhatsAppAccountRepository accountRepository;
    @Autowired private com.example.wasaas.whatsapp.WhatsAppConnectService connectService;
    @Autowired private MetaGraphClient metaGraphClient;
    @Autowired private WhatsAppCloudClient whatsAppCloudClient;
    @Autowired private MetaProperties metaProperties;
    @Autowired private MessagingService messagingService;
    @Autowired private JobWorker jobWorker;
    @Autowired private JobRepository jobRepository;
    @Autowired private WebhookEventRepository webhookEventRepository;
    @Autowired private ContactRepository contactRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private MessageLedgerRepository ledgerRepository;
    @Autowired private MessageLedgerStatusEventRepository statusEventRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;

    private MockRestServiceServer mockGraphServer;
    private MockRestServiceServer mockCloudServer;

    private static final String WABA_ID = "10987654321";
    private static final String PHONE_NUMBER_ID = "20987654321";
    private static final String GRAPH_ACCESS_TOKEN = "EAAB_TEST_ACCESS_TOKEN_XYZ123";

    @BeforeEach
    void setup() {
        cleanup();

        // Bind Mock servers for Graph Client and Cloud Client
        RestClient.Builder graphBuilder = metaGraphClient.createClientBuilder();
        mockGraphServer = MockRestServiceServer.bindTo(graphBuilder).build();
        metaGraphClient.setRestClient(graphBuilder.build());

        RestClient.Builder cloudBuilder = whatsAppCloudClient.createClientBuilder();
        mockCloudServer = MockRestServiceServer.bindTo(cloudBuilder).build();
        whatsAppCloudClient.setRestClient(cloudBuilder.build());
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        jdbcTemplate.execute("TRUNCATE TABLE message_ledger_status_events, message_ledger, conversations, contacts, webhook_events, jobs, whatsapp_accounts, spring_session_attributes, spring_session, password_reset_tokens, login_attempts, tenant_users, users, tenants CASCADE");
    }

    private String computeHmacSignature(byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(metaProperties.getAppSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
    }

    @Test
    @DisplayName("Complete Phase B E2E Journey: Onboarding -> Handshake -> Inbound Msg -> Outbound Reply -> Status Callback -> Immutability & Privacy Audit")
    void testCompletePhaseBEndToEndJourney() throws Exception {

        // =========================================================================
        // STEP 1: Tenant Registration & Login
        // =========================================================================
        tenantService.registerTenant(new RegistrationCommand(
                "Mega Retailers Inc",
                "mega-retailers",
                "Priya Sharma",
                "priya@megaretailers.com",
                "Password123!"
        ));
        UUID tenantId = tenantRepository.findBySlug("mega-retailers").orElseThrow().getId();
        TenantContext.set(tenantId);

        // =========================================================================
        // STEP 2: WhatsApp Embedded Signup Connection Flow
        // =========================================================================
        // Mock 1: Token exchange
        mockGraphServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/oauth/access_token?client_id="
                        + metaProperties.getAppId() + "&client_secret=" + metaProperties.getAppSecret() + "&code=TEST_EMBEDDED_CODE_123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"access_token\":\"" + GRAPH_ACCESS_TOKEN + "\",\"token_type\":\"bearer\"}", MediaType.APPLICATION_JSON));

        // Mock 2: Get WABA details
        mockGraphServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/" + WABA_ID + "?fields=id,name,timezone_id,message_template_namespace"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + GRAPH_ACCESS_TOKEN))
                .andRespond(withSuccess("{\"id\":\"" + WABA_ID + "\",\"name\":\"Mega Retailers Official\",\"timezone_id\":\"Asia/Kolkata\",\"message_template_namespace\":\"mega_ns\"}", MediaType.APPLICATION_JSON));

        // Mock 3: Get Phone Number details
        mockGraphServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/" + PHONE_NUMBER_ID + "?fields=id,display_phone_number,verified_name,quality_rating,messaging_limit_tier"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + GRAPH_ACCESS_TOKEN))
                .andRespond(withSuccess("{\"id\":\"" + PHONE_NUMBER_ID + "\",\"display_phone_number\":\"+91 98765 00000\",\"verified_name\":\"Mega Retailers Support\",\"quality_rating\":\"GREEN\",\"messaging_limit_tier\":\"TIER_10K\"}", MediaType.APPLICATION_JSON));

        // Mock 4: Subscribe App to WABA Webhooks
        mockGraphServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/" + WABA_ID + "/subscribed_apps"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + GRAPH_ACCESS_TOKEN))
                .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

        // Mock 5: Initial template sync
        mockGraphServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/" + WABA_ID + "/message_templates?limit=100"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + GRAPH_ACCESS_TOKEN))
                .andRespond(withSuccess("{\"data\":[]}", MediaType.APPLICATION_JSON));

        connectService.connect(new com.example.wasaas.whatsapp.ConnectWhatsAppRequest(
                "TEST_EMBEDDED_CODE_123",
                WABA_ID,
                PHONE_NUMBER_ID
        ));

        // Process initial sync job
        jobWorker.poll();

        mockGraphServer.verify();

        WhatsAppAccount connectedAccount = accountRepository.findByPhoneNumberId(PHONE_NUMBER_ID).orElseThrow();
        assertThat(connectedAccount.getStatus()).isEqualTo(WhatsAppAccountStatus.CONNECTED);
        assertThat(connectedAccount.getVerifiedName()).isEqualTo("Mega Retailers Support");

        // =========================================================================
        // STEP 3: Meta Webhook GET Handshake
        // =========================================================================
        mockMvc.perform(get("/api/webhooks/whatsapp")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", metaProperties.getWebhookVerifyToken())
                        .param("hub.challenge", "HANDSHAKE_CHALLENGE_CODE_987"))
                .andExpect(status().isOk())
                .andExpect(content().string("HANDSHAKE_CHALLENGE_CODE_987"));

        // =========================================================================
        // STEP 4: Customer Sends Inbound WhatsApp Message
        // =========================================================================
        long inboundTimeSec = 1700000000L;
        Instant inboundTime = Instant.ofEpochSecond(inboundTimeSec);
        String customerPhone = "+919876543210";
        String customerRawFrom = "919876543210";
        String inboundWamid = "wamid.INBOUND_MSG_001";

        String inboundJson = """
                {
                  "object": "whatsapp_business_account",
                  "entry": [{
                    "id": "%s",
                    "changes": [{
                      "value": {
                        "messaging_product": "whatsapp",
                        "metadata": {
                          "display_phone_number": "+91 98765 00000",
                          "phone_number_id": "%s"
                        },
                        "contacts": [{
                          "profile": { "name": "Rohan Sharma" },
                          "wa_id": "%s"
                        }],
                        "messages": [{
                          "from": "%s",
                          "id": "%s",
                          "timestamp": "%d",
                          "text": { "body": "Hi, is order #4589 shipped?" },
                          "type": "text"
                        }]
                      },
                      "field": "messages"
                    }]
                  }]
                }
                """.formatted(WABA_ID, PHONE_NUMBER_ID, customerRawFrom, customerRawFrom, inboundWamid, inboundTimeSec);

        byte[] inboundBytes = inboundJson.getBytes(StandardCharsets.UTF_8);
        String inboundSig = computeHmacSignature(inboundBytes);

        // Fast Ingest (POST -> 200 OK)
        mockMvc.perform(post("/api/webhooks/whatsapp")
                        .header("X-Hub-Signature-256", inboundSig)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inboundBytes))
                .andExpect(status().isOk());

        // Assert Webhook Event saved
        List<WebhookEvent> webhookEvents = webhookEventRepository.findAll();
        assertThat(webhookEvents).hasSize(1);

        // Worker Processes Webhook Event
        jobWorker.poll();

        TenantContext.set(tenantId);

        // Assert Contact Created
        Contact contact = contactRepository.findByTenantIdAndPhoneE164(tenantId, customerPhone).orElseThrow();
        assertThat(contact.getDisplayName()).isEqualTo("Rohan Sharma");
        assertThat(contact.getPhoneE164()).isEqualTo(customerPhone);
        assertThat(contact.getPhoneHash()).isEqualTo(PhonePrivacyUtils.hashPhoneNumber(customerPhone));

        // Assert Conversation Created & 24h Window Accurately Set
        Conversation conversation = conversationRepository
                .findByTenantIdAndContactIdAndWhatsappAccountId(tenantId, contact.getId(), connectedAccount.getId())
                .orElseThrow();
        assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.OPEN);
        assertThat(conversation.getLastInboundAt()).isEqualTo(inboundTime);
        assertThat(conversation.getServiceWindowExpiresAt()).isEqualTo(inboundTime.plus(24, ChronoUnit.HOURS));

        // Assert Inbound Message in Ledger
        MessageLedger inboundLedger = ledgerRepository.findByWamid(inboundWamid).orElseThrow();
        assertThat(inboundLedger.getDirection()).isEqualTo(MessageDirection.INBOUND);
        assertThat(inboundLedger.getBillingCategory()).isEqualTo(BillingCategory.INBOUND_FREE);
        assertThat(inboundLedger.getStatus()).isEqualTo(MessageLedgerStatus.DELIVERED);
        assertThat(inboundLedger.getRecipientPhoneLast4()).isEqualTo("3210");

        // =========================================================================
        // STEP 5: Agent Enqueues Outbound Reply
        // =========================================================================
        String outboundReplyWamid = "wamid.OUTBOUND_REPLY_002";
        String replyText = "Hello Rohan, your order #4589 has been dispatched via BlueDart!";

        // Mock Meta Cloud API Send
        mockCloudServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/" + PHONE_NUMBER_ID + "/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + GRAPH_ACCESS_TOKEN))
                .andRespond(withSuccess("{\"messaging_product\":\"whatsapp\",\"messages\":[{\"id\":\"" + outboundReplyWamid + "\"}]}", MediaType.APPLICATION_JSON));

        // Agent triggers send
        messagingService.sendText(connectedAccount.getId(), customerPhone, replyText, "reply-key-4589");

        // Verify send job in queue
        List<Job> sendJobs = jobRepository.findAll();
        Job sendJob = sendJobs.stream()
                .filter(j -> "SEND_WHATSAPP_MESSAGE".equals(j.getJobType()) && j.getIdempotencyKey() != null && j.getIdempotencyKey().contains("reply-key-4589"))
                .findFirst()
                .orElseThrow();
        assertThat(sendJob.getStatus()).isEqualTo(JobStatus.PENDING);

        // Worker Processes Send Job
        jobWorker.poll();

        mockCloudServer.verify();

        TenantContext.set(tenantId);

        // Verify Outbound Ledger
        MessageLedger outboundLedger = ledgerRepository.findByWamid(outboundReplyWamid).orElseThrow();
        assertThat(outboundLedger.getDirection()).isEqualTo(MessageDirection.OUTBOUND);
        assertThat(outboundLedger.getBillingCategory()).isEqualTo(BillingCategory.SERVICE);
        assertThat(outboundLedger.getStatus()).isEqualTo(MessageLedgerStatus.SENT);
        assertThat(outboundLedger.getRecipientPhoneLast4()).isEqualTo("3210");

        // =========================================================================
        // STEP 6: Meta Sends Delivery Status Callback
        // =========================================================================
        String statusCallbackJson = """
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
                          "timestamp": "1700000050"
                        }]
                      },
                      "field": "messages"
                    }]
                  }]
                }
                """.formatted(WABA_ID, PHONE_NUMBER_ID, outboundReplyWamid);

        byte[] statusBytes = statusCallbackJson.getBytes(StandardCharsets.UTF_8);
        String statusSig = computeHmacSignature(statusBytes);

        mockMvc.perform(post("/api/webhooks/whatsapp")
                        .header("X-Hub-Signature-256", statusSig)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBytes))
                .andExpect(status().isOk());

        // Process status update job
        jobWorker.poll();

        TenantContext.set(tenantId);

        // Assert Ledger updated to DELIVERED and StatusEvent appended
        MessageLedger updatedOutboundLedger = ledgerRepository.findByWamid(outboundReplyWamid).orElseThrow();
        assertThat(updatedOutboundLedger.getStatus()).isEqualTo(MessageLedgerStatus.DELIVERED);

        List<MessageLedgerStatusEvent> statusEvents = statusEventRepository.findByLedgerIdOrderByOccurredAtAsc(updatedOutboundLedger.getId());
        assertThat(statusEvents).hasSize(1);
        assertThat(statusEvents.get(0).getStatus()).isEqualTo(MessageLedgerStatus.DELIVERED);

        // =========================================================================
        // STEP 7: DPDP Privacy & Database Immutability Trigger Audit
        // =========================================================================
        // 1. Full phone number exists only in contacts
        assertThat(contact.getPhoneE164()).isEqualTo("+919876543210");

        // 2. Full phone number is NOT present in any message_ledger column
        Map<String, Object> ledgerRow = jdbcTemplate.queryForMap("SELECT * FROM message_ledger WHERE id = ?", outboundLedger.getId());
        for (Object colValue : ledgerRow.values()) {
            if (colValue != null) {
                assertThat(colValue.toString()).doesNotContain("9876543210");
                assertThat(colValue.toString()).doesNotContain("+91");
            }
        }

        // 3. PostgreSQL Immutability Trigger Guard prevents financial tampering
        assertThatThrownBy(() ->
                jdbcTemplate.update("UPDATE message_ledger SET billing_category = 'MARKETING' WHERE id = ?", outboundLedger.getId())
        ).hasMessageContaining("Cannot modify billing_category");

        assertThatThrownBy(() ->
                jdbcTemplate.update("UPDATE message_ledger SET direction = 'INBOUND' WHERE id = ?", outboundLedger.getId())
        ).hasMessageContaining("Cannot modify direction");

        // =========================================================================
        // STEP 8: Multi-Tenant Cross-Isolation Audit
        // =========================================================================
        tenantService.registerTenant(new RegistrationCommand(
                "Competitor Retailers Ltd",
                "competitor-retailers",
                "Vijay Kumar",
                "vijay@competitor.com",
                "Password123!"
        ));
        UUID competitorTenantId = tenantRepository.findBySlug("competitor-retailers").orElseThrow().getId();

        // Switch to Competitor Context
        TenantContext.set(competitorTenantId);

        // Competitor must see 0 contacts, 0 conversations, and 0 ledger records from Mega Retailers
        assertThat(contactRepository.findAll()).isEmpty();
        assertThat(conversationRepository.findAll()).isEmpty();
        assertThat(ledgerRepository.findAll()).isEmpty();
    }
}
