package com.example.wasaas.whatsapp.interactive;

import com.example.wasaas.automation.ActionType;
import com.example.wasaas.automation.AutomationEngine;
import com.example.wasaas.automation.AutomationRuleService;
import com.example.wasaas.automation.AutoReplyRateLimiter;
import com.example.wasaas.automation.ConsolidationValidator;
import com.example.wasaas.automation.CreateRuleCommand;
import com.example.wasaas.automation.MatchType;
import com.example.wasaas.automation.ReplyBuilder;
import com.example.wasaas.job.Job;
import com.example.wasaas.job.JobRepository;
import com.example.wasaas.job.JobWorker;
import com.example.wasaas.tenant.RegistrationCommand;
import com.example.wasaas.tenant.TenantRepository;
import com.example.wasaas.tenant.TenantService;
import com.example.wasaas.tenant.context.TenantContext;
import com.example.wasaas.whatsapp.SaveWhatsAppAccountCommand;
import com.example.wasaas.whatsapp.WhatsAppAccount;
import com.example.wasaas.whatsapp.WhatsAppAccountService;
import com.example.wasaas.whatsapp.client.ListRow;
import com.example.wasaas.whatsapp.client.ListSection;
import com.example.wasaas.whatsapp.client.ReplyButton;
import com.example.wasaas.whatsapp.client.SendResult;
import com.example.wasaas.whatsapp.client.WhatsAppCloudClient;
import com.example.wasaas.whatsapp.inbound.InboundMessageReceivedEvent;
import com.example.wasaas.whatsapp.webhook.WebhookIngestService;
import com.example.wasaas.whatsapp.meta.MetaProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest(properties = {
    "app.jobs.poll-interval-ms=1000000",
    "app.jobs.batch-size=10",
    "app.jobs.lock-timeout-secs=300"
})
@ActiveProfiles({"local", "worker"})
public class InteractiveMessagingTest {

    @Autowired private WhatsAppCloudClient whatsAppCloudClient;
    @Autowired private MetaProperties metaProperties;
    @Autowired private WebhookIngestService webhookIngestService;
    @Autowired private AutomationEngine automationEngine;
    @Autowired private AutomationRuleService ruleService;
    @Autowired private AutoReplyRateLimiter rateLimiter;
    @Autowired private ConsolidationValidator consolidationValidator;
    @Autowired private JobWorker jobWorker;
    @Autowired private JobRepository jobRepository;
    @Autowired private WhatsAppAccountService accountService;
    @Autowired private TenantService tenantService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    private MockRestServiceServer mockServer;
    private UUID tenantId;
    private WhatsAppAccount account;

    private static final String PHONE_ID = "phone_interactive_101";
    private static final String ACCESS_TOKEN = "TEST_TOKEN_INTERACTIVE";

    @BeforeEach
    void setup() {
        cleanup();
        rateLimiter.reset();

        RestClient.Builder builder = whatsAppCloudClient.createClientBuilder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        whatsAppCloudClient.setRestClient(builder.build());

        tenantService.registerTenant(new RegistrationCommand(
                "Interactive Store",
                "interactive-store",
                "Interactive Admin",
                "admin.interactive@example.com",
                "Password123!"
        ));
        tenantId = tenantRepository.findBySlug("interactive-store").orElseThrow().getId();
        TenantContext.set(tenantId);

        account = accountService.saveOrUpdateAccount(new SaveWhatsAppAccountCommand(
                "waba_interactive_101",
                PHONE_ID,
                "+1 555-4321",
                "Interactive Bot",
                "GREEN",
                "TIER_10K",
                ACCESS_TOKEN
        ));
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        rateLimiter.reset();
        jdbcTemplate.execute("TRUNCATE TABLE faqs, unmatched_messages, automation_rules, whatsapp_templates, message_ledger_status_events, message_ledger, conversations, contacts, webhook_events, jobs, whatsapp_accounts, spring_session_attributes, spring_session, password_reset_tokens, login_attempts, tenant_users, users, tenants CASCADE");
    }

    @Test
    void testSendInteractiveButtonsPayloadFormat() {
        String recipient = "+919876543210";
        String bodyText = "Please confirm your appointment for tomorrow:";

        List<ReplyButton> buttons = List.of(
                new ReplyButton("btn_yes", "Confirm"),
                new ReplyButton("btn_no", "Reschedule")
        );

        mockServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/" + PHONE_ID + "/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"type\":\"interactive\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"type\":\"button\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Confirm")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Reschedule")))
                .andRespond(withSuccess(
                        "{\"messaging_product\":\"whatsapp\",\"messages\":[{\"id\":\"wamid.HBgLBTN123\"}]}",
                        MediaType.APPLICATION_JSON
                ));

        SendResult result = whatsAppCloudClient.sendInteractiveButtons(
                PHONE_ID,
                ACCESS_TOKEN,
                recipient,
                bodyText,
                buttons
        );

        mockServer.verify();
        assertThat(result.wamid()).isEqualTo("wamid.HBgLBTN123");
    }

    @Test
    void testSendInteractiveListPayloadFormat() {
        String recipient = "+919876543210";
        String bodyText = "Select your desired support department:";

        List<ListSection> sections = List.of(
                new ListSection("Departments", List.of(
                        new ListRow("row_sales", "Sales & Inquiries", "Ask about products"),
                        new ListRow("row_tech", "Technical Support", "Help with account")
                ))
        );

        mockServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/" + PHONE_ID + "/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"type\":\"interactive\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"type\":\"list\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sales & Inquiries")))
                .andRespond(withSuccess(
                        "{\"messaging_product\":\"whatsapp\",\"messages\":[{\"id\":\"wamid.HBgLLIST123\"}]}",
                        MediaType.APPLICATION_JSON
                ));

        SendResult result = whatsAppCloudClient.sendInteractiveList(
                PHONE_ID,
                ACCESS_TOKEN,
                recipient,
                bodyText,
                "View Options",
                sections
        );

        mockServer.verify();
        assertThat(result.wamid()).isEqualTo("wamid.HBgLLIST123");
    }

    @Test
    void testInboundButtonReplyProcessedAndMatchesAutomationRule() {
        TenantContext.set(tenantId);

        // Configure rule matching the button title
        ruleService.createRule(new CreateRuleCommand(
                "Confirm Appointment Rule",
                true,
                MatchType.EXACT,
                "Confirm",
                false,
                10,
                ActionType.SEND_TEXT,
                "{\"text\": \"Thank you! Your appointment is confirmed.\"}"
        ));

        // Inbound Webhook payload with button_reply
        String webhookPayload = String.format("""
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
                              "display_phone_number": "+1 555-4321",
                              "phone_number_id": "%s"
                            },
                            "contacts": [
                              { "profile": { "name": "Amit Kumar" }, "wa_id": "919876543210" }
                            ],
                            "messages": [
                              {
                                "from": "919876543210",
                                "id": "wamid.IN_BTN_001",
                                "timestamp": "1724230000",
                                "type": "interactive",
                                "interactive": {
                                  "type": "button_reply",
                                  "button_reply": {
                                    "id": "btn_yes",
                                    "title": "Confirm"
                                  }
                                }
                              }
                            ]
                          }
                        }
                      ]
                    }
                  ]
                }
                """, account.getWabaId(), PHONE_ID);

        webhookIngestService.ingest(webhookPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8), true);

        // Process webhook job
        jobWorker.poll();

        // Verify that the automation rule triggered an auto-reply job
        List<Job> jobs = jobRepository.findAll();
        assertThat(jobs).hasSize(2); // 1st was PROCESS_WEBHOOK_EVENT, 2nd is SEND_WHATSAPP_MESSAGE
        Job sendJob = jobs.stream().filter(j -> "SEND_WHATSAPP_MESSAGE".equals(j.getJobType())).findFirst().orElseThrow();
        assertThat(sendJob.getPayload()).contains("Thank you! Your appointment is confirmed.");
    }

    @Test
    void testReplyBuilderConsolidatesMultipleSnippetsIntoSingleSend() {
        String consolidated = ReplyBuilder.create()
                .header("📢 Order Notification")
                .addSnippet("Hi Rahul, your order #5589 has been packed.")
                .addSnippet("Tracking URL: https://track.example.com/5589")
                .footer("Reply HELP if you have any questions.")
                .build();

        assertThat(consolidated)
                .contains("📢 Order Notification\n\n")
                .contains("Hi Rahul, your order #5589 has been packed.\n\n")
                .contains("Tracking URL: https://track.example.com/5589\n\n")
                .contains("Reply HELP if you have any questions.");
    }

    @Test
    void testMultiMessageWarningGeneratedOnUnconsolidatedActions() throws Exception {
        String multiActionJson = """
                {
                  "messages": [
                    { "text": "Message 1" },
                    { "text": "Message 2" },
                    { "text": "Message 3" }
                  ]
                }
                """;

        JsonNode actionNode = objectMapper.readTree(multiActionJson);
        Optional<String> warning = consolidationValidator.validateActionConsolidation(actionNode);

        assertThat(warning).isPresent();
        assertThat(warning.get()).contains("Action defines 3 separate messages");
        assertThat(warning.get()).contains("billed individually (~Rs 0.115/msg in India)");
        assertThat(warning.get()).contains("Consolidating into a single message saves 2x billing cost");
    }
}
