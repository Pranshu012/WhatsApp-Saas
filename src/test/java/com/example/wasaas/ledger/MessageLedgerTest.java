package com.example.wasaas.ledger;

import com.example.wasaas.tenant.RegistrationCommand;
import com.example.wasaas.tenant.TenantRepository;
import com.example.wasaas.tenant.TenantService;
import com.example.wasaas.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local")
public class MessageLedgerTest {

    @Autowired private LedgerService ledgerService;
    @Autowired private MessageLedgerRepository ledgerRepository;
    @Autowired private MessageLedgerStatusEventRepository statusEventRepository;
    @Autowired private TenantService tenantService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID tenantId;

    @BeforeEach
    void setup() {
        cleanup();
        tenantService.registerTenant(new RegistrationCommand(
                "Ledger Test Biz",
                "ledger-test-biz",
                "Ledger Admin",
                "admin.ledger@example.com",
                "Password123!"
        ));
        tenantId = tenantRepository.findBySlug("ledger-test-biz").orElseThrow().getId();
        TenantContext.set(tenantId);
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        jdbcTemplate.execute("TRUNCATE TABLE message_ledger_status_events, message_ledger, whatsapp_accounts, spring_session_attributes, spring_session, password_reset_tokens, login_attempts, tenant_users, users, tenants CASCADE");
    }

    @Test
    void testRecordOutboundIntentAndPhoneMasking() {
        String rawPhone = "+1 (555) 234-5678";
        RecordOutboundIntentCommand cmd = new RecordOutboundIntentCommand(
                null,
                rawPhone,
                BillingCategory.MARKETING,
                "summer_promo",
                ConversationWindow.OUT_OF_WINDOW,
                "idem-key-1",
                UUID.randomUUID()
        );

        UUID ledgerId = ledgerService.recordOutboundIntent(cmd);

        MessageLedger ledger = ledgerRepository.findById(ledgerId).orElseThrow();
        assertThat(ledger.getTenantId()).isEqualTo(tenantId);
        assertThat(ledger.getDirection()).isEqualTo(MessageDirection.OUTBOUND);
        assertThat(ledger.getBillingCategory()).isEqualTo(BillingCategory.MARKETING);
        assertThat(ledger.getTemplateName()).isEqualTo("summer_promo");
        assertThat(ledger.getConversationWindow()).isEqualTo(ConversationWindow.OUT_OF_WINDOW);
        assertThat(ledger.getStatus()).isEqualTo(MessageLedgerStatus.INTENT);
        assertThat(ledger.getRecipientPhoneLast4()).isEqualTo("5678");
        assertThat(ledger.getRecipientPhoneHash()).isEqualTo(PhonePrivacyUtils.hashPhoneNumber(rawPhone));

        // DB level verification: raw phone number does not exist in any text column
        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM message_ledger WHERE id = ?", ledgerId);
        for (Object value : row.values()) {
            if (value != null) {
                assertThat(value.toString()).doesNotContain("2345678");
                assertThat(value.toString()).doesNotContain("+1 (555)");
            }
        }
    }

    @Test
    void testAttachWamidUpdatesStatusToSent() {
        RecordOutboundIntentCommand cmd = new RecordOutboundIntentCommand(
                null,
                "+919876543210",
                BillingCategory.UTILITY,
                "order_update",
                ConversationWindow.IN_WINDOW,
                "idem-key-2",
                null
        );
        UUID ledgerId = ledgerService.recordOutboundIntent(cmd);

        String wamid = "wamid.HBgLMTA5ODc2NTQzMjEVAgARGBI0";
        ledgerService.attachWamid(ledgerId, wamid);

        MessageLedger ledger = ledgerRepository.findById(ledgerId).orElseThrow();
        assertThat(ledger.getWamid()).isEqualTo(wamid);
        assertThat(ledger.getStatus()).isEqualTo(MessageLedgerStatus.SENT);
    }

    @Test
    void testRecordFailureUpdatesErrorCodeAndMessage() {
        RecordOutboundIntentCommand cmd = new RecordOutboundIntentCommand(
                null,
                "+919876543210",
                BillingCategory.AUTHENTICATION,
                "otp_template",
                ConversationWindow.OUT_OF_WINDOW,
                "idem-key-3",
                null
        );
        UUID ledgerId = ledgerService.recordOutboundIntent(cmd);

        ledgerService.recordFailure(ledgerId, 131026, "Message undeliverable to recipient");

        MessageLedger ledger = ledgerRepository.findById(ledgerId).orElseThrow();
        assertThat(ledger.getStatus()).isEqualTo(MessageLedgerStatus.FAILED);
        assertThat(ledger.getErrorCode()).isEqualTo(131026);
        assertThat(ledger.getErrorMessage()).isEqualTo("Message undeliverable to recipient");
    }

    @Test
    void testRecordStatusEventsAppendAndNeverMutateParentImmutableFields() {
        RecordOutboundIntentCommand cmd = new RecordOutboundIntentCommand(
                null,
                "+15551234567",
                BillingCategory.MARKETING,
                "promo_2",
                ConversationWindow.OUT_OF_WINDOW,
                "idem-key-4",
                null
        );
        UUID ledgerId = ledgerService.recordOutboundIntent(cmd);
        String wamid = "wamid.DELIVERY_FLOW_123";
        ledgerService.attachWamid(ledgerId, wamid);

        // Record DELIVERED event
        ledgerService.recordStatusEvent(wamid, MessageLedgerStatus.DELIVERED, "{\"pricing\":{\"category\":\"marketing\"}}");
        // Record READ event
        ledgerService.recordStatusEvent(wamid, MessageLedgerStatus.READ, "{\"timestamp\":\"1700000000\"}");

        // Verify status events appended
        List<MessageLedgerStatusEvent> events = statusEventRepository.findByLedgerIdOrderByOccurredAtAsc(ledgerId);
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getStatus()).isEqualTo(MessageLedgerStatus.DELIVERED);
        assertThat(events.get(0).getRawPayload()).contains("pricing");
        assertThat(events.get(1).getStatus()).isEqualTo(MessageLedgerStatus.READ);

        // Verify parent ledger immutable billing semantics remained intact
        MessageLedger parent = ledgerRepository.findById(ledgerId).orElseThrow();
        assertThat(parent.getStatus()).isEqualTo(MessageLedgerStatus.READ);
        assertThat(parent.getBillingCategory()).isEqualTo(BillingCategory.MARKETING);
        assertThat(parent.getDirection()).isEqualTo(MessageDirection.OUTBOUND);
    }

    @Test
    void testMonthlyCountByCategory() {
        YearMonth currentMonth = YearMonth.now();

        // Marketing: 2
        ledgerService.recordOutboundIntent(new RecordOutboundIntentCommand(null, "+15551111111", BillingCategory.MARKETING, "t1", null, "m1", null));
        ledgerService.recordOutboundIntent(new RecordOutboundIntentCommand(null, "+15552222222", BillingCategory.MARKETING, "t2", null, "m2", null));

        // Utility: 1
        ledgerService.recordOutboundIntent(new RecordOutboundIntentCommand(null, "+15553333333", BillingCategory.UTILITY, "t3", null, "u1", null));

        // Authentication: 3
        ledgerService.recordOutboundIntent(new RecordOutboundIntentCommand(null, "+15554444444", BillingCategory.AUTHENTICATION, "t4", null, "a1", null));
        ledgerService.recordOutboundIntent(new RecordOutboundIntentCommand(null, "+15555555555", BillingCategory.AUTHENTICATION, "t5", null, "a2", null));
        ledgerService.recordOutboundIntent(new RecordOutboundIntentCommand(null, "+15556666666", BillingCategory.AUTHENTICATION, "t6", null, "a3", null));

        Map<BillingCategory, Long> counts = ledgerService.countByCategoryForMonth(tenantId, currentMonth);

        assertThat(counts.get(BillingCategory.MARKETING)).isEqualTo(2L);
        assertThat(counts.get(BillingCategory.UTILITY)).isEqualTo(1L);
        assertThat(counts.get(BillingCategory.AUTHENTICATION)).isEqualTo(3L);
        assertThat(counts.get(BillingCategory.SERVICE)).isEqualTo(0L);
        assertThat(counts.get(BillingCategory.INBOUND_FREE)).isEqualTo(0L);
    }

    @Test
    void testDuplicateIdempotencyKeyProducesSingleRow() {
        RecordOutboundIntentCommand cmd1 = new RecordOutboundIntentCommand(null, "+15557778888", BillingCategory.UTILITY, "invoice_receipt", null, "idem-dup-key", null);
        RecordOutboundIntentCommand cmd2 = new RecordOutboundIntentCommand(null, "+15557778888", BillingCategory.UTILITY, "invoice_receipt", null, "idem-dup-key", null);

        UUID firstId = ledgerService.recordOutboundIntent(cmd1);
        UUID secondId = ledgerService.recordOutboundIntent(cmd2);

        assertThat(secondId).isEqualTo(firstId);
        assertThat(ledgerRepository.findAll()).hasSize(1);
    }

    @Test
    void testDatabaseTriggerRejectsMutationOfImmutableColumns() {
        RecordOutboundIntentCommand cmd = new RecordOutboundIntentCommand(null, "+15559990000", BillingCategory.MARKETING, "promo", null, "idem-guard-key", null);
        UUID ledgerId = ledgerService.recordOutboundIntent(cmd);

        // Attempt to illegally modify billing_category
        assertThatThrownBy(() ->
                jdbcTemplate.update("UPDATE message_ledger SET billing_category = 'SERVICE' WHERE id = ?", ledgerId)
        ).hasMessageContaining("Cannot modify billing_category");

        // Attempt to illegally modify direction
        assertThatThrownBy(() ->
                jdbcTemplate.update("UPDATE message_ledger SET direction = 'INBOUND' WHERE id = ?", ledgerId)
        ).hasMessageContaining("Cannot modify direction");

        // Attempt to illegally modify recipient_phone_hash
        assertThatThrownBy(() ->
                jdbcTemplate.update("UPDATE message_ledger SET recipient_phone_hash = 'tampered' WHERE id = ?", ledgerId)
        ).hasMessageContaining("Cannot modify recipient_phone_hash");
    }
}
