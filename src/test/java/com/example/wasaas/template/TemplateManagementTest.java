package com.example.wasaas.template;

import com.example.wasaas.job.PermanentJobException;
import com.example.wasaas.tenant.RegistrationCommand;
import com.example.wasaas.tenant.TenantRepository;
import com.example.wasaas.tenant.TenantService;
import com.example.wasaas.tenant.context.TenantContext;
import com.example.wasaas.whatsapp.SaveWhatsAppAccountCommand;
import com.example.wasaas.whatsapp.WhatsAppAccount;
import com.example.wasaas.whatsapp.WhatsAppAccountService;
import com.example.wasaas.whatsapp.meta.MetaGraphClient;
import com.example.wasaas.whatsapp.meta.MetaProperties;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest
@ActiveProfiles({"local", "worker"})
public class TemplateManagementTest {

    @Autowired private TemplateSyncService syncService;
    @Autowired private TemplateService templateService;
    @Autowired private WhatsAppTemplateRepository templateRepository;
    @Autowired private MetaGraphClient metaGraphClient;
    @Autowired private MetaProperties metaProperties;
    @Autowired private WhatsAppAccountService accountService;
    @Autowired private TenantService tenantService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private MockRestServiceServer mockServer;
    private UUID tenantAId;
    private WhatsAppAccount accountA;

    private static final String WABA_ID = "waba_tpl_test_1001";
    private static final String ACCESS_TOKEN = "TEST_TOKEN_TPL_123";

    @BeforeEach
    void setup() {
        cleanup();

        RestClient.Builder builder = metaGraphClient.createClientBuilder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        metaGraphClient.setRestClient(builder.build());

        tenantService.registerTenant(new RegistrationCommand(
                "Template Store A",
                "template-store-a",
                "Store Admin",
                "admin.store@example.com",
                "Password123!"
        ));
        tenantAId = tenantRepository.findBySlug("template-store-a").orElseThrow().getId();
        TenantContext.set(tenantAId);

        accountA = accountService.saveOrUpdateAccount(new SaveWhatsAppAccountCommand(
                WABA_ID,
                "phone_id_tpl_1001",
                "+1 555-9000",
                "Template Store Support",
                "GREEN",
                "TIER_10K",
                ACCESS_TOKEN
        ));
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        jdbcTemplate.execute("TRUNCATE TABLE whatsapp_templates, message_ledger_status_events, message_ledger, conversations, contacts, webhook_events, jobs, whatsapp_accounts, spring_session_attributes, spring_session, password_reset_tokens, login_attempts, tenant_users, users, tenants CASCADE");
    }

    @Test
    void testSyncTemplatesUpsertsWithoutDuplicating() {
        String metaResponseJson = """
                {
                  "data": [
                    {
                      "id": "meta_tpl_01",
                      "name": "order_update_v1",
                      "language": "en_US",
                      "status": "APPROVED",
                      "category": "UTILITY",
                      "components": [
                        { "type": "BODY", "text": "Hi {{1}}, your order {{2}} has been confirmed!" }
                      ]
                    },
                    {
                      "id": "meta_tpl_02",
                      "name": "summer_promo_v1",
                      "language": "en_US",
                      "status": "APPROVED",
                      "category": "MARKETING",
                      "components": [
                        { "type": "BODY", "text": "Check out our latest discount offers!" }
                      ]
                    }
                  ]
                }
                """;

        // 1st Sync
        mockServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/" + WABA_ID + "/message_templates?limit=100"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andRespond(withSuccess(metaResponseJson, MediaType.APPLICATION_JSON));

        int syncedFirst = syncService.syncTemplates(tenantAId, accountA.getId());
        mockServer.verify();
        assertThat(syncedFirst).isEqualTo(2);

        TenantContext.set(tenantAId);
        List<WhatsAppTemplate> templates = templateRepository.findAll();
        assertThat(templates).hasSize(2);

        WhatsAppTemplate orderTpl = templateRepository.findByTenantIdAndNameAndLanguage(tenantAId, "order_update_v1", "en_US").orElseThrow();
        assertThat(orderTpl.getStatus()).isEqualTo(TemplateStatus.APPROVED);
        assertThat(orderTpl.getCategory()).isEqualTo(TemplateCategory.UTILITY);
        assertThat(orderTpl.getVariableCount()).isEqualTo(2);
        assertThat(orderTpl.isCategoryConflict()).isFalse();

        // 2nd Sync (Should update without duplicating)
        mockServer.reset();
        mockServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/" + WABA_ID + "/message_templates?limit=100"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andRespond(withSuccess(metaResponseJson, MediaType.APPLICATION_JSON));

        int syncedSecond = syncService.syncTemplates(tenantAId, accountA.getId());
        mockServer.verify();
        assertThat(syncedSecond).isEqualTo(2);

        TenantContext.set(tenantAId);
        assertThat(templateRepository.findAll()).hasSize(2);
    }

    @Test
    void testMetaCategoryWinsOverRequestedCategoryWithConflictFlag() {
        TenantContext.set(tenantAId);

        // Pre-create local template requested as UTILITY
        WhatsAppTemplate localTpl = new WhatsAppTemplate(
                tenantAId,
                accountA.getId(),
                "meta_tpl_promo",
                "special_discount_v1",
                "en_US",
                TemplateCategory.UTILITY,
                null,
                TemplateStatus.PENDING,
                null,
                "Get 20% off your next purchase with code {{1}}",
                null,
                null
        );
        templateRepository.save(localTpl);

        // Meta classifies it as MARKETING (at ~7.5x cost)
        String metaResponseJson = """
                {
                  "data": [
                    {
                      "id": "meta_tpl_promo",
                      "name": "special_discount_v1",
                      "language": "en_US",
                      "status": "APPROVED",
                      "category": "MARKETING",
                      "components": [
                        { "type": "BODY", "text": "Get 20% off your next purchase with code {{1}}" }
                      ]
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/" + WABA_ID + "/message_templates?limit=100"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andRespond(withSuccess(metaResponseJson, MediaType.APPLICATION_JSON));

        syncService.syncTemplates(tenantAId, accountA.getId());
        mockServer.verify();

        TenantContext.set(tenantAId);
        WhatsAppTemplate syncedTpl = templateRepository.findByTenantIdAndNameAndLanguage(tenantAId, "special_discount_v1", "en_US").orElseThrow();

        assertThat(syncedTpl.getRequestedCategory()).isEqualTo(TemplateCategory.UTILITY);
        assertThat(syncedTpl.getCategory()).isEqualTo(TemplateCategory.MARKETING);
        assertThat(syncedTpl.isCategoryConflict()).isTrue(); // Prominent conflict flag active
        assertThat(syncedTpl.getStatus()).isEqualTo(TemplateStatus.APPROVED);
    }

    @Test
    void testRejectedTemplateStoresRejectionReason() {
        String metaResponseJson = """
                {
                  "data": [
                    {
                      "id": "meta_tpl_rejected",
                      "name": "invalid_utility_tpl",
                      "language": "en_US",
                      "status": "REJECTED",
                      "category": "UTILITY",
                      "rejection_reason": "PROMOTIONAL_CONTENT_IN_UTILITY",
                      "components": [
                        { "type": "BODY", "text": "Buy now and save {{1}}!" }
                      ]
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/" + WABA_ID + "/message_templates?limit=100"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andRespond(withSuccess(metaResponseJson, MediaType.APPLICATION_JSON));

        syncService.syncTemplates(tenantAId, accountA.getId());
        mockServer.verify();

        TenantContext.set(tenantAId);
        WhatsAppTemplate rejectedTpl = templateRepository.findByTenantIdAndNameAndLanguage(tenantAId, "invalid_utility_tpl", "en_US").orElseThrow();

        assertThat(rejectedTpl.getStatus()).isEqualTo(TemplateStatus.REJECTED);
        assertThat(rejectedTpl.getRejectionReason()).isEqualTo("PROMOTIONAL_CONTENT_IN_UTILITY");
    }

    @Test
    void testSendingNonApprovedTemplateFailsBeforeApiCall() {
        TenantContext.set(tenantAId);

        WhatsAppTemplate pendingTpl = new WhatsAppTemplate(
                tenantAId,
                accountA.getId(),
                "meta_pending_01",
                "pending_announcement",
                "en_US",
                TemplateCategory.UTILITY,
                null,
                TemplateStatus.PENDING,
                null,
                "Hello {{1}}",
                null,
                null
        );
        templateRepository.save(pendingTpl);

        // Pre-flight assertion must fail with PermanentJobException before any Meta call
        assertThatThrownBy(() -> templateService.assertSendable(tenantAId, "pending_announcement", "en_US", 1))
                .isInstanceOf(PermanentJobException.class)
                .hasMessageContaining("is PENDING, not APPROVED");
    }

    @Test
    void testVariableCountMismatchFailsBeforeApiCall() {
        TenantContext.set(tenantAId);

        WhatsAppTemplate approvedTpl = new WhatsAppTemplate(
                tenantAId,
                accountA.getId(),
                "meta_approved_01",
                "multi_variable_notice",
                "en_US",
                TemplateCategory.UTILITY,
                TemplateCategory.UTILITY,
                TemplateStatus.APPROVED,
                null,
                "Dear {{1}}, your booking {{2}} at {{3}} is confirmed for {{4}}.",
                null,
                null
        );
        templateRepository.save(approvedTpl);

        // Template requires 4 variables; passing 2 should fail immediately
        assertThatThrownBy(() -> templateService.assertSendable(tenantAId, "multi_variable_notice", "en_US", 2))
                .isInstanceOf(PermanentJobException.class)
                .hasMessageContaining("variable count mismatch: expected 4, got 2");
    }

    @Test
    void testMultiTenantTemplateIsolation() {
        TenantContext.set(tenantAId);

        WhatsAppTemplate tplA = new WhatsAppTemplate(
                tenantAId,
                accountA.getId(),
                "meta_tpl_tenant_a",
                "tenant_a_exclusive_tpl",
                "en_US",
                TemplateCategory.UTILITY,
                TemplateCategory.UTILITY,
                TemplateStatus.APPROVED,
                null,
                "Exclusive for Tenant A: {{1}}",
                null,
                null
        );
        templateRepository.save(tplA);

        // Register Tenant B
        tenantService.registerTenant(new RegistrationCommand(
                "Template Store B",
                "template-store-b",
                "Store B Admin",
                "admin.b@example.com",
                "Password123!"
        ));
        UUID tenantBId = tenantRepository.findBySlug("template-store-b").orElseThrow().getId();

        // Query under Tenant B context
        TenantContext.set(tenantBId);
        assertThat(templateRepository.findAll()).isEmpty();
        assertThat(templateRepository.findByTenantIdAndNameAndLanguage(tenantBId, "tenant_a_exclusive_tpl", "en_US")).isEmpty();
    }
}
