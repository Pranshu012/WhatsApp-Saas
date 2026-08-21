package com.example.wasaas.api;

import com.example.wasaas.automation.ActionType;
import com.example.wasaas.automation.MatchType;
import com.example.wasaas.contact.Contact;
import com.example.wasaas.contact.ContactRepository;
import com.example.wasaas.contact.Conversation;
import com.example.wasaas.contact.ConversationRepository;
import com.example.wasaas.contact.ConversationStatus;
import com.example.wasaas.ledger.ConversationWindow;
import com.example.wasaas.ledger.MessageLedger;
import com.example.wasaas.ledger.MessageLedgerRepository;
import com.example.wasaas.ledger.PhonePrivacyUtils;
import com.example.wasaas.template.TemplateCategory;
import com.example.wasaas.template.TemplateStatus;
import com.example.wasaas.template.WhatsAppTemplate;
import com.example.wasaas.template.WhatsAppTemplateRepository;
import com.example.wasaas.tenant.Tenant;
import com.example.wasaas.tenant.TenantRepository;
import com.example.wasaas.tenant.context.TenantContext;
import com.example.wasaas.whatsapp.SaveWhatsAppAccountCommand;
import com.example.wasaas.whatsapp.WhatsAppAccount;
import com.example.wasaas.whatsapp.WhatsAppAccountService;
import com.example.wasaas.whatsapp.client.WhatsAppCloudClient;
import com.example.wasaas.whatsapp.meta.MetaGraphClient;
import com.example.wasaas.whatsapp.meta.MetaProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "app.jobs.poll-interval-ms=1000000",
    "app.jobs.batch-size=10",
    "app.jobs.lock-timeout-secs=300"
})
@AutoConfigureMockMvc
@ActiveProfiles({"local", "worker"})
public class CompleteApiEndToEndTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private WhatsAppAccountService accountService;
    @Autowired private WhatsAppTemplateRepository templateRepository;
    @Autowired private ContactRepository contactRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private MessageLedgerRepository ledgerRepository;
    @Autowired private MetaProperties metaProperties;
    @Autowired private MetaGraphClient metaGraphClient;
    @Autowired private JdbcTemplate jdbcTemplate;

    private org.springframework.test.web.client.MockRestServiceServer mockMetaServer;

    @BeforeEach
    void setup() {
        cleanup();
        org.springframework.web.client.RestClient.Builder metaBuilder = metaGraphClient.createClientBuilder();
        mockMetaServer = org.springframework.test.web.client.MockRestServiceServer.bindTo(metaBuilder).ignoreExpectOrder(true).build();
        metaGraphClient.setRestClient(metaBuilder.build());
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        jdbcTemplate.execute("TRUNCATE TABLE scheduled_messages, faqs, unmatched_messages, automation_rules, whatsapp_templates, message_ledger_status_events, message_ledger, conversations, contacts, webhook_events, jobs, whatsapp_accounts, spring_session_attributes, spring_session, password_reset_tokens, login_attempts, tenant_users, users, tenants CASCADE");
    }

    private String computeHmacSignature(byte[] body) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(metaProperties.getAppSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + java.util.HexFormat.of().formatHex(mac.doFinal(body));
    }

    @Test
    void testCompleteHttpRestApiEndToEndJourney() throws Exception {
        // ==========================================
        // 1. TENANT REGISTRATION & AUTHENTICATION
        // ==========================================

        // 1.1 POST /api/auth/register
        String registerPayload = """
                {
                  "businessName": "Mega Store India",
                  "slug": "mega-store",
                  "fullName": "Rajesh Sharma",
                  "email": "rajesh@megastore.in",
                  "password": "Password123!long"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("mega-store"))
                .andExpect(jsonPath("$.ownerEmail").value("rajesh@megastore.in"));

        Tenant tenant = tenantRepository.findBySlug("mega-store").orElseThrow();
        UUID tenantId = tenant.getId();

        // 1.2 POST /api/auth/login
        String loginPayload = """
                {
                  "tenantSlug": "mega-store",
                  "email": "rajesh@megastore.in",
                  "password": "Password123!long"
                }
                """;

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("rajesh@megastore.in"))
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andReturn();

        Cookie sessionCookie = loginResult.getResponse().getCookie("SESSION");
        assertThat(sessionCookie).isNotNull();

        // 1.3 GET /api/auth/me
        mockMvc.perform(get("/api/auth/me").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("rajesh@megastore.in"))
                .andExpect(jsonPath("$.role").value("OWNER"));

        // 1.4 GET /api/auth/csrf
        mockMvc.perform(get("/api/auth/csrf").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        // 1.5 POST /api/auth/forgot-password
        mockMvc.perform(post("/api/auth/forgot-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"rajesh@megastore.in\"}"))
                .andExpect(status().isOk());

        // ==========================================
        // 2. WHATSAPP ACCOUNT MANAGEMENT APIS
        // ==========================================
        TenantContext.set(tenantId);
        WhatsAppAccount account = accountService.saveOrUpdateAccount(new SaveWhatsAppAccountCommand(
                "waba_mega_99",
                "phone_mega_99",
                "+91 9988776655",
                "Mega Store Official",
                "GREEN",
                "TIER_10K",
                "TEST_TOKEN_MEGA"
        ));

        // 2.1 GET /api/whatsapp/account
        mockMvc.perform(get("/api/whatsapp/account").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayPhoneNumber").value("+91 9988776655"))
                .andExpect(jsonPath("$.qualityRating").value("GREEN"))
                .andExpect(jsonPath("$.messagingLimitTier").value("TIER_10K"));

        // ==========================================
        // 3. WEBHOOK RECEIVER APIS
        // ==========================================

        // 3.1 GET /api/webhooks/whatsapp (Verification Handshake)
        mockMvc.perform(get("/api/webhooks/whatsapp")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", metaProperties.getWebhookVerifyToken())
                        .param("hub.challenge", "challenge_code_9876"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string("challenge_code_9876"));

        // 3.2 POST /api/webhooks/whatsapp (Inbound Webhook)
        String inboundWebhook = String.format("""
                {
                  "object": "whatsapp_business_account",
                  "entry": [
                    {
                      "id": "%s",
                      "changes": [
                        {
                          "field": "messages",
                          "value": {
                            "messaging_product": "whatsapp",
                            "metadata": {
                              "display_phone_number": "+91 9988776655",
                              "phone_number_id": "phone_mega_99"
                            },
                            "contacts": [
                              { "profile": { "name": "Deepak Patel" }, "wa_id": "919123456789" }
                            ],
                            "messages": [
                              {
                                "from": "919123456789",
                                "id": "wamid.IN_API_001",
                                "timestamp": "1724230000",
                                "type": "text",
                                "text": { "body": "Hello, when does my order arrive?" }
                              }
                            ]
                          }
                        }
                      ]
                    }
                  ]
                }
                """, account.getWabaId());

        mockMvc.perform(post("/api/webhooks/whatsapp")
                        .header("X-Hub-Signature-256", computeHmacSignature(inboundWebhook.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inboundWebhook))
                .andExpect(status().isOk());

        // ==========================================
        // 4. TEMPLATE MANAGEMENT REST APIS
        // ==========================================

        // 4.1 POST /api/templates (Submit Template)
        mockMetaServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("https://graph.facebook.com/v21.0/" + account.getWabaId() + "/message_templates"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess("{\"id\":\"meta_tpl_998877\"}", MediaType.APPLICATION_JSON));

        String submitTemplatePayload = """
                {
                  "name": "order_shipped_v1",
                  "language": "en_US",
                  "category": "UTILITY",
                  "bodyText": "Hello {{1}}, your order {{2}} is on the way!"
                }
                """;

        MvcResult createTplResult = mockMvc.perform(post("/api/templates")
                        .with(csrf())
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitTemplatePayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("order_shipped_v1"))
                .andExpect(jsonPath("$.requestedCategory").value("UTILITY"))
                .andReturn();

        JsonNode tplNode = objectMapper.readTree(createTplResult.getResponse().getContentAsString());
        UUID templateId = UUID.fromString(tplNode.get("id").asText());

        // 4.2 GET /api/templates
        mockMvc.perform(get("/api/templates").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("order_shipped_v1"));

        // 4.3 GET /api/templates/{id}
        mockMvc.perform(get("/api/templates/" + templateId).cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(templateId.toString()));

        // 4.4 POST /api/templates/sync
        mockMvc.perform(post("/api/templates/sync")
                        .with(csrf())
                        .cookie(sessionCookie))
                .andExpect(status().isAccepted());

        // ==========================================
        // 5. KEYWORD AUTOMATION RULES REST APIS
        // ==========================================

        // 5.1 POST /api/automation-rules
        String createRulePayload = """
                {
                  "name": "Pricing Keyword Rule",
                  "enabled": true,
                  "matchType": "EXACT",
                  "matchValue": "PRICING",
                  "caseSensitive": false,
                  "priority": 10,
                  "actionType": "SEND_TEXT",
                  "actionPayload": "{\\"text\\": \\"Our plans start at Rs 1999/month.\\"}"
                }
                """;

        MvcResult createRuleResult = mockMvc.perform(post("/api/automation-rules")
                        .with(csrf())
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRulePayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Pricing Keyword Rule"))
                .andReturn();

        JsonNode ruleNode = objectMapper.readTree(createRuleResult.getResponse().getContentAsString());
        UUID ruleId = UUID.fromString(ruleNode.get("id").asText());

        // 5.2 GET /api/automation-rules
        mockMvc.perform(get("/api/automation-rules").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pricing Keyword Rule"));

        // 5.3 POST /api/automation-rules/test
        mockMvc.perform(post("/api/automation-rules/test")
                        .with(csrf())
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"messageText\": \"pricing\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched").value(true))
                .andExpect(jsonPath("$.ruleName").value("Pricing Keyword Rule"));

        // ==========================================
        // 6. FAQ KNOWLEDGE BASE REST APIS
        // ==========================================

        // 6.1 POST /api/faqs
        String createFaqPayload = """
                {
                  "question": "What is the return and refund policy?",
                  "answer": "You can return items within 7 days for a 100% full refund."
                }
                """;

        MvcResult createFaqResult = mockMvc.perform(post("/api/faqs")
                        .with(csrf())
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createFaqPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.question").value("What is the return and refund policy?"))
                .andReturn();

        JsonNode faqNode = objectMapper.readTree(createFaqResult.getResponse().getContentAsString());
        UUID faqId = UUID.fromString(faqNode.get("id").asText());

        // 6.2 GET /api/faqs
        mockMvc.perform(get("/api/faqs").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].answer").value("You can return items within 7 days for a 100% full refund."));

        // 6.3 POST /api/faqs/test (PostgreSQL FTS + Trigram typo search)
        mockMvc.perform(post("/api/faqs/test")
                        .with(csrf())
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"wats the return and rfnd policy?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isConfident").value(true))
                .andExpect(jsonPath("$.answer").value("You can return items within 7 days for a 100% full refund."));

        // ==========================================
        // 7. INBOX & CONVERSATIONS REST APIS
        // ==========================================
        TenantContext.set(tenantId);
        String phoneHash = PhonePrivacyUtils.hashPhoneNumber("+919123456789");
        Contact contact = new Contact(tenantId, "+919123456789", phoneHash, "Deepak Patel");
        contact = contactRepository.save(contact);

        Conversation conv = new Conversation(tenantId, contact.getId(), account.getId(), Instant.now());
        conv = conversationRepository.save(conv);

        // Message ledger entry
        MessageLedger ledger = new MessageLedger(
                account.getId(),
                com.example.wasaas.ledger.MessageDirection.INBOUND,
                contact.getPhoneHash(),
                "6789",
                com.example.wasaas.ledger.BillingCategory.INBOUND_FREE,
                null,
                com.example.wasaas.ledger.ConversationWindow.IN_WINDOW,
                com.example.wasaas.ledger.MessageLedgerStatus.DELIVERED,
                "inbound:wamid.IN_API_001",
                null
        );
        ledger.setTenantId(tenantId);
        ledger.setWamid("wamid.IN_API_001");
        ledgerRepository.save(ledger);

        // 7.1 GET /api/conversations
        mockMvc.perform(get("/api/conversations").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contactName").value("Deepak Patel"))
                .andExpect(jsonPath("$[0].serviceWindowActive").value(true));

        // 7.2 GET /api/conversations/{id}/messages
        mockMvc.perform(get("/api/conversations/" + conv.getId() + "/messages").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].wamid").value("wamid.IN_API_001"));

        // 7.3 POST /api/conversations/{id}/reply (Free-text reply inside 24h window)
        mockMvc.perform(post("/api/conversations/" + conv.getId() + "/reply")
                        .with(csrf())
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"Your order is scheduled for delivery today!\"}"))
                .andExpect(status().isAccepted());

        // ==========================================
        // 8. SCHEDULED MESSAGES REST APIS
        // ==========================================
        TenantContext.set(tenantId);
        // Approve template for scheduling
        WhatsAppTemplate approvedTemplate = templateRepository.findByTenantIdAndId(tenantId, templateId).orElseThrow();
        approvedTemplate.setStatus(TemplateStatus.APPROVED);
        templateRepository.save(approvedTemplate);

        // 8.1 POST /api/scheduled-messages
        String schedulePayload = String.format("""
                {
                  "contactId": "%s",
                  "templateId": "%s",
                  "whatsappAccountId": "%s",
                  "components": [
                    {
                      "type": "body",
                      "parameters": [
                        { "type": "text", "text": "Deepak" },
                        { "type": "text", "text": "ORD-9988" }
                      ]
                    }
                  ],
                  "scheduledFor": "%s",
                  "timezone": "Asia/Kolkata"
                }
                """, contact.getId(), templateId, account.getId(), Instant.now().plus(2, ChronoUnit.HOURS));

        MvcResult scheduleResult = mockMvc.perform(post("/api/scheduled-messages")
                        .with(csrf())
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(schedulePayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.timezone").value("Asia/Kolkata"))
                .andReturn();

        JsonNode schedNode = objectMapper.readTree(scheduleResult.getResponse().getContentAsString());
        UUID scheduledMsgId = UUID.fromString(schedNode.get("id").asText());

        // 8.2 GET /api/scheduled-messages
        mockMvc.perform(get("/api/scheduled-messages").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SCHEDULED"));

        // 8.3 DELETE /api/scheduled-messages/{id} (Cancel scheduled message)
        mockMvc.perform(delete("/api/scheduled-messages/" + scheduledMsgId)
                        .with(csrf())
                        .cookie(sessionCookie))
                .andExpect(status().isNoContent());

        // Verify cancelled in DB
        TenantContext.set(tenantId);
        mockMvc.perform(get("/api/scheduled-messages").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("CANCELLED"));

        // ==========================================
        // 9. CLEANUP / DELETE ENDPOINTS
        // ==========================================
        // 9.1 DELETE /api/faqs/{id}
        mockMvc.perform(delete("/api/faqs/" + faqId)
                        .with(csrf())
                        .cookie(sessionCookie))
                .andExpect(status().isNoContent());

        // 9.2 DELETE /api/automation-rules/{id}
        mockMvc.perform(delete("/api/automation-rules/" + ruleId)
                        .with(csrf())
                        .cookie(sessionCookie))
                .andExpect(status().isNoContent());

        // 9.3 POST /api/auth/logout
        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .cookie(sessionCookie))
                .andExpect(status().isOk());
    }
}
