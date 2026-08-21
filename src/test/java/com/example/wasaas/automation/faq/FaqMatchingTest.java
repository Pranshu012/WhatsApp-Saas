package com.example.wasaas.automation.faq;

import com.example.wasaas.automation.ActionType;
import com.example.wasaas.automation.AutomationEngine;
import com.example.wasaas.automation.AutomationRuleService;
import com.example.wasaas.automation.AutoReplyRateLimiter;
import com.example.wasaas.automation.CreateRuleCommand;
import com.example.wasaas.automation.MatchType;
import com.example.wasaas.automation.UnmatchedMessage;
import com.example.wasaas.automation.UnmatchedMessageRepository;
import com.example.wasaas.job.Job;
import com.example.wasaas.job.JobRepository;
import com.example.wasaas.tenant.RegistrationCommand;
import com.example.wasaas.tenant.TenantRepository;
import com.example.wasaas.tenant.TenantService;
import com.example.wasaas.tenant.context.TenantContext;
import com.example.wasaas.whatsapp.SaveWhatsAppAccountCommand;
import com.example.wasaas.whatsapp.WhatsAppAccount;
import com.example.wasaas.whatsapp.WhatsAppAccountService;
import com.example.wasaas.whatsapp.inbound.InboundMessageReceivedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "app.jobs.poll-interval-ms=1000000"
})
@ActiveProfiles({"local", "worker"})
public class FaqMatchingTest {

    @Autowired private FaqMatchService faqMatchService;
    @Autowired private FaqRepository faqRepository;
    @Autowired private AutomationEngine automationEngine;
    @Autowired private AutomationRuleService ruleService;
    @Autowired private UnmatchedMessageRepository unmatchedMessageRepository;
    @Autowired private AutoReplyRateLimiter rateLimiter;
    @Autowired private JobRepository jobRepository;
    @Autowired private WhatsAppAccountService accountService;
    @Autowired private TenantService tenantService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID tenantAId;
    private WhatsAppAccount accountA;

    @BeforeEach
    void setup() {
        cleanup();
        rateLimiter.reset();
        faqMatchService.setConfidenceThreshold(0.35);

        tenantService.registerTenant(new RegistrationCommand(
                "FAQ Store A",
                "faq-store-a",
                "FAQ Admin",
                "admin.faq@example.com",
                "Password123!"
        ));
        tenantAId = tenantRepository.findBySlug("faq-store-a").orElseThrow().getId();
        TenantContext.set(tenantAId);

        accountA = accountService.saveOrUpdateAccount(new SaveWhatsAppAccountCommand(
                "waba_faq_1001",
                "phone_id_faq_1001",
                "+1 555-7771",
                "FAQ Support Bot",
                "GREEN",
                "TIER_10K",
                "TEST_TOKEN_FAQ_123"
        ));
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        rateLimiter.reset();
        jdbcTemplate.execute("TRUNCATE TABLE faqs, unmatched_messages, automation_rules, whatsapp_templates, message_ledger_status_events, message_ledger, conversations, contacts, webhook_events, jobs, whatsapp_accounts, spring_session_attributes, spring_session, password_reset_tokens, login_attempts, tenant_users, users, tenants CASCADE");
    }

    @Test
    void testExactQuestionMatchScoresHighAndReplies() {
        TenantContext.set(tenantAId);
        faqMatchService.createFaq(
                "What are your business hours?",
                "We are open Monday to Saturday 9am to 7pm."
        );

        InboundMessageReceivedEvent event = new InboundMessageReceivedEvent(
                tenantAId, null, null, accountA.getId(),
                "wamid.IN_FAQ_01", "+919876543210",
                "What are your business hours?",
                Instant.now(), Instant.now().plusSeconds(86400)
        );

        automationEngine.onInboundMessage(event);

        List<Job> jobs = jobRepository.findAll();
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).getPayload()).contains("We are open Monday to Saturday 9am to 7pm.");
    }

    @Test
    void testTypoTolerantQuestionMatchSucceeds() {
        TenantContext.set(tenantAId);
        faqMatchService.createFaq(
                "What is the pricing plan for WhatsApp SaaS?",
                "Pricing starts at Rs 1999 per month for the standard plan."
        );

        // Query with typos ("wats the prcing plan for whatsapp saas?")
        InboundMessageReceivedEvent event = new InboundMessageReceivedEvent(
                tenantAId, null, null, accountA.getId(),
                "wamid.IN_FAQ_TYPO", "+919876543210",
                "wats the prcing plan for whatsapp saas?",
                Instant.now(), Instant.now().plusSeconds(86400)
        );

        automationEngine.onInboundMessage(event);

        List<Job> jobs = jobRepository.findAll();
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).getPayload()).contains("Pricing starts at Rs 1999 per month");
    }

    @Test
    void testLowConfidenceUnrelatedQueryDoesNotGuessAndLogsUnmatched() {
        TenantContext.set(tenantAId);
        faqMatchService.createFaq(
                "How do I track my order shipment?",
                "Visit our live tracking portal at track.example.com."
        );

        // Completely unrelated query
        InboundMessageReceivedEvent event = new InboundMessageReceivedEvent(
                tenantAId, null, null, accountA.getId(),
                "wamid.IN_UNRELATED", "+919876543210",
                "Can you deliver bananas in wholesale?",
                Instant.now(), Instant.now().plusSeconds(86400)
        );

        automationEngine.onInboundMessage(event);

        // No reply jobs enqueued (Prevents dangerous low-confidence hallucinations)
        assertThat(jobRepository.findAll()).isEmpty();

        // Stored in unmatched dataset
        TenantContext.set(tenantAId);
        List<UnmatchedMessage> unmatched = unmatchedMessageRepository.findAll();
        assertThat(unmatched).hasSize(1);
        assertThat(unmatched.get(0).getMessageText()).isEqualTo("Can you deliver bananas in wholesale?");
    }

    @Test
    void testKeywordRuleTakesPrecedenceOverFaq() {
        TenantContext.set(tenantAId);

        // 1. Keyword Rule for PRICING
        ruleService.createRule(new CreateRuleCommand(
                "Keyword Pricing",
                true,
                MatchType.EXACT,
                "PRICING",
                false,
                10,
                ActionType.SEND_TEXT,
                "{\"text\": \"Keyword rule response for pricing.\"}"
        ));

        // 2. FAQ for Pricing
        faqMatchService.createFaq(
                "What is your pricing?",
                "FAQ response for pricing."
        );

        InboundMessageReceivedEvent event = new InboundMessageReceivedEvent(
                tenantAId, null, null, accountA.getId(),
                "wamid.IN_PRECEDENCE", "+919876543210",
                "pricing",
                Instant.now(), Instant.now().plusSeconds(86400)
        );

        automationEngine.onInboundMessage(event);

        List<Job> jobs = jobRepository.findAll();
        assertThat(jobs).hasSize(1);
        // Keyword rule MUST win over FAQ!
        assertThat(jobs.get(0).getPayload()).contains("Keyword rule response for pricing.");
    }

    @Test
    void testConfigurableConfidenceThreshold() {
        TenantContext.set(tenantAId);
        faqMatchService.createFaq(
                "Do you provide refund within 7 days?",
                "Yes, we offer full refunds within 7 days of purchase."
        );

        String query = "refund policy please";

        // 1. Strict threshold (0.95) -> Fails match and logs unmatched
        faqMatchService.setConfidenceThreshold(0.95);
        automationEngine.onInboundMessage(new InboundMessageReceivedEvent(
                tenantAId, null, null, accountA.getId(),
                "wamid.IN_STRICT", "+919876543210", query, Instant.now(), Instant.now().plusSeconds(86400)
        ));
        assertThat(jobRepository.findAll()).isEmpty();

        // 2. Relaxed threshold (0.05) -> Matches and sends reply
        faqMatchService.setConfidenceThreshold(0.05);
        automationEngine.onInboundMessage(new InboundMessageReceivedEvent(
                tenantAId, null, null, accountA.getId(),
                "wamid.IN_RELAXED", "+919876543210", query, Instant.now(), Instant.now().plusSeconds(86400)
        ));
        assertThat(jobRepository.findAll()).hasSize(1);
        assertThat(jobRepository.findAll().get(0).getPayload()).contains("Yes, we offer full refunds");
    }

    @Test
    void testMultiTenantFaqIsolation() {
        TenantContext.set(tenantAId);
        faqMatchService.createFaq(
                "Secret Tenant A FAQ question",
                "Secret Tenant A answer"
        );

        // Create Tenant B
        tenantService.registerTenant(new RegistrationCommand(
                "FAQ Store B",
                "faq-store-b",
                "Admin B",
                "admin.b@faq.com",
                "Password123!"
        ));
        UUID tenantBId = tenantRepository.findBySlug("faq-store-b").orElseThrow().getId();
        TenantContext.set(tenantBId);

        WhatsAppAccount accountB = accountService.saveOrUpdateAccount(new SaveWhatsAppAccountCommand(
                "waba_faq_2002",
                "phone_id_faq_2002",
                "+1 555-7772",
                "Tenant B Support",
                "GREEN",
                "TIER_10K",
                "TEST_TOKEN_FAQ_B"
        ));

        // Message to Tenant B matching Tenant A's FAQ
        automationEngine.onInboundMessage(new InboundMessageReceivedEvent(
                tenantBId, null, null, accountB.getId(),
                "wamid.IN_ISOLATION", "+919876543210",
                "Secret Tenant A FAQ question",
                Instant.now(), Instant.now().plusSeconds(86400)
        ));

        // Must NOT match Tenant A's FAQ
        assertThat(jobRepository.findAll()).isEmpty();

        // Must be logged as unmatched under Tenant B
        TenantContext.set(tenantBId);
        List<UnmatchedMessage> unmatchedB = unmatchedMessageRepository.findAll();
        assertThat(unmatchedB).hasSize(1);
    }
}
