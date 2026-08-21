package com.example.wasaas.whatsapp.send;

import com.example.wasaas.job.Job;
import com.example.wasaas.job.JobRepository;
import com.example.wasaas.job.JobStatus;
import com.example.wasaas.job.JobWorker;
import com.example.wasaas.ledger.BillingCategory;
import com.example.wasaas.ledger.MessageLedger;
import com.example.wasaas.ledger.MessageLedgerRepository;
import com.example.wasaas.ledger.MessageLedgerStatus;
import com.example.wasaas.tenant.RegistrationCommand;
import com.example.wasaas.tenant.TenantRepository;
import com.example.wasaas.tenant.TenantService;
import com.example.wasaas.tenant.context.TenantContext;
import com.example.wasaas.whatsapp.SaveWhatsAppAccountCommand;
import com.example.wasaas.whatsapp.WhatsAppAccount;
import com.example.wasaas.whatsapp.WhatsAppAccountRepository;
import com.example.wasaas.whatsapp.WhatsAppAccountService;
import com.example.wasaas.whatsapp.WhatsAppAccountStatus;
import com.example.wasaas.whatsapp.client.TemplateComponent;
import com.example.wasaas.whatsapp.client.TemplateParameter;
import com.example.wasaas.whatsapp.client.WhatsAppCloudClient;
import com.example.wasaas.whatsapp.meta.MetaProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest(properties = {
    "app.jobs.poll-interval-ms=1000000",
    "app.jobs.batch-size=10",
    "app.jobs.lock-timeout-secs=300"
})
@ActiveProfiles({"local", "worker"})
public class MessagingServiceTest {

    @Autowired private MessagingService messagingService;
    @Autowired private JobWorker jobWorker;
    @Autowired private JobRepository jobRepository;
    @Autowired private MessageLedgerRepository ledgerRepository;
    @Autowired private WhatsAppAccountService accountService;
    @Autowired private WhatsAppAccountRepository accountRepository;
    @Autowired private WhatsAppCloudClient cloudClient;
    @Autowired private MetaProperties metaProperties;
    @Autowired private TenantService tenantService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private MockRestServiceServer mockServer;
    private UUID tenantId;
    private WhatsAppAccount account;

    private static final String WABA_ID = "1122334455";
    private static final String PHONE_ID = "9988776655";
    private static final String ACCESS_TOKEN = "EAAB_VALID_SEND_TOKEN_999";

    @BeforeEach
    void setup() {
        cleanup();

        RestClient.Builder builder = cloudClient.createClientBuilder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        cloudClient.setRestClient(builder.build());

        tenantService.registerTenant(new RegistrationCommand(
                "Messaging Biz",
                "messaging-biz",
                "Messaging Admin",
                "admin.messaging@example.com",
                "Password123!"
        ));
        tenantId = tenantRepository.findBySlug("messaging-biz").orElseThrow().getId();
        TenantContext.set(tenantId);

        account = accountService.saveOrUpdateAccount(new SaveWhatsAppAccountCommand(
                WABA_ID,
                PHONE_ID,
                "+1 555-0100",
                "Messaging Biz Verified",
                "GREEN",
                "TIER_10K",
                ACCESS_TOKEN
        ));
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        jdbcTemplate.execute("TRUNCATE TABLE message_ledger_status_events, message_ledger, jobs, whatsapp_accounts, spring_session_attributes, spring_session, password_reset_tokens, login_attempts, tenant_users, users, tenants CASCADE");
    }

    @Test
    void testEnqueueAndWorkerSendsTextMessageSuccessfully() {
        String recipient = "+15551234567";
        String messageText = "Hello from WhatsApp SaaS!";

        // Mock Meta Cloud API Send
        mockServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/" + PHONE_ID + "/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Hello from WhatsApp SaaS!")))
                .andRespond(withSuccess(
                        "{\"messaging_product\":\"whatsapp\",\"contacts\":[{\"input\":\"15551234567\",\"wa_id\":\"15551234567\"}],\"messages\":[{\"id\":\"wamid.HBgLTEXT123\"}]}",
                        MediaType.APPLICATION_JSON
                ));

        // 1. Enqueue send via public service (synchronous HTTP does not happen here)
        messagingService.sendText(account.getId(), recipient, messageText, "msg-key-1");

        // Verify background job was queued
        List<Job> jobs = jobRepository.findAll();
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).getStatus()).isEqualTo(JobStatus.PENDING);

        // 2. Worker claims and processes job
        jobWorker.poll();

        mockServer.verify();

        // 3. Verify job finished and ledger recorded
        Job completedJob = jobRepository.findById(jobs.get(0).getId()).orElseThrow();
        assertThat(completedJob.getStatus()).isEqualTo(JobStatus.SUCCEEDED);

        List<MessageLedger> ledgers = ledgerRepository.findAll();
        assertThat(ledgers).hasSize(1);
        MessageLedger ledger = ledgers.get(0);
        assertThat(ledger.getWamid()).isEqualTo("wamid.HBgLTEXT123");
        assertThat(ledger.getStatus()).isEqualTo(MessageLedgerStatus.SENT);
        assertThat(ledger.getBillingCategory()).isEqualTo(BillingCategory.SERVICE);
    }

    @Test
    void testSendTemplateMessageFormattedCorrectly() {
        String recipient = "+15559876543";

        mockServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/" + PHONE_ID + "/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("order_receipt")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("INV-999")))
                .andRespond(withSuccess(
                        "{\"messaging_product\":\"whatsapp\",\"messages\":[{\"id\":\"wamid.HBgLTPL999\"}]}",
                        MediaType.APPLICATION_JSON
                ));

        List<TemplateComponent> components = List.of(
                TemplateComponent.body(List.of(TemplateParameter.text("INV-999")))
        );

        messagingService.sendTemplate(
                account.getId(),
                recipient,
                "order_receipt",
                "en_US",
                components,
                BillingCategory.MARKETING,
                "tpl-key-1"
        );

        jobWorker.poll();

        mockServer.verify();

        MessageLedger ledger = ledgerRepository.findAll().get(0);
        assertThat(ledger.getWamid()).isEqualTo("wamid.HBgLTPL999");
        assertThat(ledger.getBillingCategory()).isEqualTo(BillingCategory.MARKETING);
        assertThat(ledger.getTemplateName()).isEqualTo("order_receipt");
    }

    @Test
    void testTransientErrorTriggersRetry() {
        mockServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/" + PHONE_ID + "/messages"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"message\":\"Rate limit exceeded\",\"code\":130429}}"));

        messagingService.sendText(account.getId(), "+15550001111", "Retry test", "retry-key");

        jobWorker.poll();

        Job job = jobRepository.findAll().get(0);
        // Remains PENDING for next retry
        assertThat(job.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(job.getAttempts()).isEqualTo(1);
        assertThat(job.getLastError()).contains("130429");

        MessageLedger ledger = ledgerRepository.findAll().get(0);
        assertThat(ledger.getStatus()).isEqualTo(MessageLedgerStatus.FAILED);
        assertThat(ledger.getErrorCode()).isEqualTo(429);
    }

    @Test
    void testPermanentErrorMovesJobToDeadWithoutRetry() {
        mockServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/" + PHONE_ID + "/messages"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"message\":\"Recipient is not a valid WhatsApp user\",\"code\":131026}}"));

        messagingService.sendText(account.getId(), "+15550002222", "Dead letter test", "dead-key");

        jobWorker.poll();

        Job job = jobRepository.findAll().get(0);
        // Moves straight to DEAD
        assertThat(job.getStatus()).isEqualTo(JobStatus.DEAD);
        assertThat(job.getAttempts()).isEqualTo(1);
        assertThat(job.getLastError()).contains("131026");

        MessageLedger ledger = ledgerRepository.findAll().get(0);
        assertThat(ledger.getStatus()).isEqualTo(MessageLedgerStatus.FAILED);
        assertThat(ledger.getErrorCode()).isEqualTo(400);
    }

    @Test
    void testTokenRevocationError190MarksAccountDisconnected() {
        mockServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/" + PHONE_ID + "/messages"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"message\":\"Invalid OAuth access token\",\"code\":190}}"));

        messagingService.sendText(account.getId(), "+15550003333", "Revoke test", "revoke-key");

        jobWorker.poll();

        Job job = jobRepository.findAll().get(0);
        assertThat(job.getStatus()).isEqualTo(JobStatus.DEAD);

        WhatsAppAccount updatedAccount = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(updatedAccount.getStatus()).isEqualTo(WhatsAppAccountStatus.DISCONNECTED);
    }

    @Test
    void testIdempotencyPreventsDuplicateSend() {
        mockServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/" + PHONE_ID + "/messages"))
                .andRespond(withSuccess("{\"messaging_product\":\"whatsapp\",\"messages\":[{\"id\":\"wamid.IDEM123\"}]}", MediaType.APPLICATION_JSON));

        // Call twice with same idempotency key
        messagingService.sendText(account.getId(), "+15550004444", "Idempotent send", "idem-send-key");
        messagingService.sendText(account.getId(), "+15550004444", "Idempotent send", "idem-send-key");

        // Exactly 1 job created
        assertThat(jobRepository.findAll()).hasSize(1);

        jobWorker.poll();

        mockServer.verify();
        assertThat(ledgerRepository.findAll()).hasSize(1);
    }
}
