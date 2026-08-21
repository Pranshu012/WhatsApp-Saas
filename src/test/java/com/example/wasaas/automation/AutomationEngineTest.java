package com.example.wasaas.automation;

import com.example.wasaas.common.exception.DomainException;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
    "app.jobs.poll-interval-ms=1000000"
})
@ActiveProfiles({"local", "worker"})
public class AutomationEngineTest {

    @Autowired private AutomationEngine automationEngine;
    @Autowired private AutomationRuleService ruleService;
    @Autowired private AutomationRuleRepository ruleRepository;
    @Autowired private UnmatchedMessageRepository unmatchedMessageRepository;
    @Autowired private AutoReplyRateLimiter rateLimiter;
    @Autowired private JobRepository jobRepository;
    @Autowired private WhatsAppAccountService accountService;
    @Autowired private TenantService tenantService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID tenantAId;
    private WhatsAppAccount accountA;

    @BeforeEach
    void setup() {
        cleanup();
        rateLimiter.reset();

        tenantService.registerTenant(new RegistrationCommand(
                "Automation Store A",
                "automation-store-a",
                "Rule Admin",
                "admin.rules@example.com",
                "Password123!"
        ));
        tenantAId = tenantRepository.findBySlug("automation-store-a").orElseThrow().getId();
        TenantContext.set(tenantAId);

        accountA = accountService.saveOrUpdateAccount(new SaveWhatsAppAccountCommand(
                "waba_auto_1001",
                "phone_id_auto_1001",
                "+1 555-8888",
                "Automation Bot",
                "GREEN",
                "TIER_10K",
                "TEST_TOKEN_AUTO_123"
        ));
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        rateLimiter.reset();
        jdbcTemplate.execute("TRUNCATE TABLE unmatched_messages, automation_rules, whatsapp_templates, message_ledger_status_events, message_ledger, conversations, contacts, webhook_events, jobs, whatsapp_accounts, spring_session_attributes, spring_session, password_reset_tokens, login_attempts, tenant_users, users, tenants CASCADE");
    }

    @Test
    void testExactMatchTriggersAutoReply() {
        TenantContext.set(tenantAId);
        ruleService.createRule(new CreateRuleCommand(
                "Stop Rule",
                true,
                MatchType.EXACT,
                "STOP",
                false,
                10,
                ActionType.SEND_TEXT,
                "{\"text\": \"You have been unsubscribed from notifications.\"}"
        ));

        // Inbound STOP message
        InboundMessageReceivedEvent event = new InboundMessageReceivedEvent(
                tenantAId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                accountA.getId(),
                "wamid.INBOUND_STOP_01",
                "+919876543210",
                "stop",
                Instant.now(),
                Instant.now().plusSeconds(86400)
        );

        automationEngine.onInboundMessage(event);

        List<Job> jobs = jobRepository.findAll();
        assertThat(jobs).hasSize(1);
        Job replyJob = jobs.get(0);
        assertThat(replyJob.getJobType()).isEqualTo("SEND_WHATSAPP_MESSAGE");
        assertThat(replyJob.getPayload()).contains("You have been unsubscribed");
    }

    @Test
    void testContainsAndStartsWithAndRegexMatchRules() {
        TenantContext.set(tenantAId);

        ruleService.createRule(new CreateRuleCommand(
                "Pricing Query",
                true,
                MatchType.CONTAINS,
                "pricing",
                false,
                20,
                ActionType.SEND_TEXT,
                "{\"text\": \"Our plans start at Rs 1999/month.\"}"
        ));

        ruleService.createRule(new CreateRuleCommand(
                "Track Prefix",
                true,
                MatchType.STARTS_WITH,
                "TRACK",
                false,
                30,
                ActionType.SEND_TEXT,
                "{\"text\": \"Tracking your order details...\"}"
        ));

        ruleService.createRule(new CreateRuleCommand(
                "Order Regex",
                true,
                MatchType.REGEX,
                "^ORD-\\d{4}$",
                false,
                40,
                ActionType.SEND_TEXT,
                "{\"text\": \"Order format recognized.\"}"
        ));

        // 1. CONTAINS match
        automationEngine.onInboundMessage(new InboundMessageReceivedEvent(
                tenantAId, UUID.randomUUID(), UUID.randomUUID(), accountA.getId(),
                "wamid.IN_01", "+919876543211", "Can you send the pricing list?", Instant.now(), Instant.now().plusSeconds(86400)
        ));

        // 2. STARTS_WITH match
        automationEngine.onInboundMessage(new InboundMessageReceivedEvent(
                tenantAId, UUID.randomUUID(), UUID.randomUUID(), accountA.getId(),
                "wamid.IN_02", "+919876543212", "track 448822", Instant.now(), Instant.now().plusSeconds(86400)
        ));

        // 3. REGEX match
        automationEngine.onInboundMessage(new InboundMessageReceivedEvent(
                tenantAId, UUID.randomUUID(), UUID.randomUUID(), accountA.getId(),
                "wamid.IN_03", "+919876543213", "ORD-5566", Instant.now(), Instant.now().plusSeconds(86400)
        ));

        List<Job> jobs = jobRepository.findAll();
        assertThat(jobs).hasSize(3);
        assertThat(jobs.get(0).getPayload()).contains("Our plans start at Rs 1999/month.");
        assertThat(jobs.get(1).getPayload()).contains("Tracking your order details...");
        assertThat(jobs.get(2).getPayload()).contains("Order format recognized.");
    }

    @Test
    void testPriorityOrderingFirstMatchWins() {
        TenantContext.set(tenantAId);

        // High priority rule (priority 5)
        ruleService.createRule(new CreateRuleCommand(
                "High Priority Offer",
                true,
                MatchType.CONTAINS,
                "discount",
                false,
                5,
                ActionType.SEND_TEXT,
                "{\"text\": \"High priority special discount response.\"}"
        ));

        // Low priority rule (priority 50)
        ruleService.createRule(new CreateRuleCommand(
                "Low Priority Offer",
                true,
                MatchType.CONTAINS,
                "discount",
                false,
                50,
                ActionType.SEND_TEXT,
                "{\"text\": \"Low priority generic response.\"}"
        ));

        automationEngine.onInboundMessage(new InboundMessageReceivedEvent(
                tenantAId, UUID.randomUUID(), UUID.randomUUID(), accountA.getId(),
                "wamid.IN_PRIORITY", "+919876543210", "Do you have any discount?", Instant.now(), Instant.now().plusSeconds(86400)
        ));

        List<Job> jobs = jobRepository.findAll();
        // First match wins: Only 1 job enqueued with high priority text
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).getPayload()).contains("High priority special discount response.");
    }

    @Test
    void testNoMatchLogsUnmatchedMessage() {
        TenantContext.set(tenantAId);

        ruleService.createRule(new CreateRuleCommand(
                "Support Rule",
                true,
                MatchType.EXACT,
                "HELP",
                false,
                10,
                ActionType.SEND_TEXT,
                "{\"text\": \"How can we assist you?\"}"
        ));

        // Inbound message that matches nothing
        automationEngine.onInboundMessage(new InboundMessageReceivedEvent(
                tenantAId, null, null, accountA.getId(),
                "wamid.IN_UNMATCHED_01", "+919876543210", "Is tomorrow a bank holiday?", Instant.now(), Instant.now().plusSeconds(86400)
        ));

        // No reply jobs enqueued
        assertThat(jobRepository.findAll()).isEmpty();

        // Recorded in unmatched_messages dataset (ADR-007)
        TenantContext.set(tenantAId);
        List<UnmatchedMessage> unmatched = unmatchedMessageRepository.findAll();
        assertThat(unmatched).hasSize(1);
        assertThat(unmatched.get(0).getMessageText()).isEqualTo("Is tomorrow a bank holiday?");
        assertThat(unmatched.get(0).getSenderPhone()).isEqualTo("+919876543210");
        assertThat(unmatched.get(0).getWamid()).isEqualTo("wamid.IN_UNMATCHED_01");
    }

    @Test
    void testCatastrophicRegexRejectedAtSave() {
        TenantContext.set(tenantAId);

        // Nested quantifier catastrophic regex (ReDoS attack vector)
        assertThatThrownBy(() -> ruleService.createRule(new CreateRuleCommand(
                "Evil Regex Rule",
                true,
                MatchType.REGEX,
                "(a+)+$",
                false,
                10,
                ActionType.SEND_TEXT,
                "{\"text\": \"Evil\"}"
        )))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Unsafe regex pattern detected");
    }

    @Test
    void testPerContactRateLimitPreventsReplyStorm() {
        TenantContext.set(tenantAId);

        ruleService.createRule(new CreateRuleCommand(
                "Echo Rule",
                true,
                MatchType.CONTAINS,
                "hello",
                false,
                10,
                ActionType.SEND_TEXT,
                "{\"text\": \"Hello there!\"}"
        ));

        String senderPhone = "+919876543299";

        // Send 7 rapid messages (Limit is 5 per hour)
        for (int i = 1; i <= 7; i++) {
            automationEngine.onInboundMessage(new InboundMessageReceivedEvent(
                    tenantAId, UUID.randomUUID(), UUID.randomUUID(), accountA.getId(),
                    "wamid.IN_STORM_" + i, senderPhone, "hello " + i, Instant.now(), Instant.now().plusSeconds(86400)
            ));
        }

        // Exactly 5 replies enqueued, remaining 2 throttled
        List<Job> jobs = jobRepository.findAll();
        assertThat(jobs).hasSize(5);
    }

    @Test
    void testMultiTenantRuleIsolation() {
        TenantContext.set(tenantAId);

        ruleService.createRule(new CreateRuleCommand(
                "Tenant A Keyword",
                true,
                MatchType.EXACT,
                "SPECIAL_A",
                false,
                10,
                ActionType.SEND_TEXT,
                "{\"text\": \"Response for A only.\"}"
        ));

        // Create Tenant B
        tenantService.registerTenant(new RegistrationCommand(
                "Automation Store B",
                "automation-store-b",
                "Admin B",
                "admin.b@rules.com",
                "Password123!"
        ));
        UUID tenantBId = tenantRepository.findBySlug("automation-store-b").orElseThrow().getId();
        TenantContext.set(tenantBId);

        WhatsAppAccount accountB = accountService.saveOrUpdateAccount(new SaveWhatsAppAccountCommand(
                "waba_auto_2002",
                "phone_id_auto_2002",
                "+1 555-7777",
                "Tenant B Bot",
                "GREEN",
                "TIER_10K",
                "TEST_TOKEN_AUTO_B"
        ));

        // Inbound message to Tenant B with Tenant A's keyword
        automationEngine.onInboundMessage(new InboundMessageReceivedEvent(
                tenantBId, null, null, accountB.getId(),
                "wamid.IN_TENANT_B", "+919876543210", "SPECIAL_A", Instant.now(), Instant.now().plusSeconds(86400)
        ));

        // Tenant A's rule must NOT fire for Tenant B!
        assertThat(jobRepository.findAll()).isEmpty();

        // Message should be logged as unmatched under Tenant B
        TenantContext.set(tenantBId);
        List<UnmatchedMessage> unmatchedB = unmatchedMessageRepository.findAll();
        assertThat(unmatchedB).hasSize(1);
        assertThat(unmatchedB.get(0).getMessageText()).isEqualTo("SPECIAL_A");
    }
}
