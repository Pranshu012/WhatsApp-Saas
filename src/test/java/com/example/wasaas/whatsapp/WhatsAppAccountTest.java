package com.example.wasaas.whatsapp;

import com.example.wasaas.common.exception.NotFoundException;
import com.example.wasaas.tenant.RegistrationCommand;
import com.example.wasaas.tenant.Tenant;
import com.example.wasaas.tenant.TenantRepository;
import com.example.wasaas.tenant.TenantService;
import com.example.wasaas.tenant.context.TenantContext;
import com.example.wasaas.whatsapp.crypto.TokenCipher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local")
public class WhatsAppAccountTest {

    @Autowired private WhatsAppAccountService accountService;
    @Autowired private WhatsAppAccountRepository accountRepository;
    @Autowired private TenantService tenantService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private TokenCipher tokenCipher;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID tenantAId;
    private UUID tenantBId;

    private static final String RAW_TOKEN = "EAAB1234567890abcdef_SECRET_ACCESS_TOKEN_FOR_WABA";

    @BeforeEach
    void setup() {
        cleanup();

        tenantService.registerTenant(new RegistrationCommand(
                "Tenant A Business",
                "tenant-a-biz",
                "Owner A",
                "owner.a@example.com",
                "Password123!"
        ));
        tenantAId = tenantRepository.findBySlug("tenant-a-biz").orElseThrow().getId();

        tenantService.registerTenant(new RegistrationCommand(
                "Tenant B Business",
                "tenant-b-biz",
                "Owner B",
                "owner.b@example.com",
                "Password123!"
        ));
        tenantBId = tenantRepository.findBySlug("tenant-b-biz").orElseThrow().getId();
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        jdbcTemplate.execute("TRUNCATE TABLE whatsapp_accounts, spring_session_attributes, spring_session, password_reset_tokens, login_attempts, tenant_users, users, tenants CASCADE");
    }

    @Test
    void testSaveAndRetrieveWhatsAppAccountWithDecryption() {
        TenantContext.set(tenantAId);

        SaveWhatsAppAccountCommand command = new SaveWhatsAppAccountCommand(
                "waba_1001",
                "phone_2001",
                "+1 (555) 123-4567",
                "Tenant A Support",
                "GREEN",
                "TIER_10K",
                RAW_TOKEN
        );

        WhatsAppAccount saved = accountService.saveOrUpdateAccount(command);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTenantId()).isEqualTo(tenantAId);
        assertThat(saved.getVerifiedName()).isEqualTo("Tenant A Support");
        assertThat(saved.getQualityRating()).isEqualTo("GREEN");
        assertThat(saved.getStatus()).isEqualTo(WhatsAppAccountStatus.CONNECTED);

        // Explicit service decryption returns plaintext token
        String decrypted = accountService.getDecryptedToken(saved.getId());
        assertThat(decrypted).isEqualTo(RAW_TOKEN);
    }

    @Test
    void testTokenIsOpaqueInDatabaseRow() {
        TenantContext.set(tenantAId);

        SaveWhatsAppAccountCommand command = new SaveWhatsAppAccountCommand(
                "waba_1002",
                "phone_2002",
                "+1 (555) 987-6543",
                "Tenant A Orders",
                "UNKNOWN",
                "TIER_250",
                RAW_TOKEN
        );
        WhatsAppAccount saved = accountService.saveOrUpdateAccount(command);

        // Query raw database row
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT access_token_encrypted FROM whatsapp_accounts WHERE id = ?",
                saved.getId()
        );

        byte[] dbBytes = (byte[]) row.get("access_token_encrypted");
        assertThat(dbBytes).isNotNull();
        assertThat(dbBytes.length).isGreaterThan(RAW_TOKEN.length());

        // Verify the database row does NOT contain the plaintext string
        String dbBytesAsString = new String(dbBytes, StandardCharsets.UTF_8);
        assertThat(dbBytesAsString).doesNotContain(RAW_TOKEN);
        assertThat(dbBytesAsString).doesNotContain("EAAB");

        // Verify it decrypts cleanly with the cipher
        assertThat(tokenCipher.decrypt(dbBytes)).isEqualTo(RAW_TOKEN);
    }

    @Test
    void testJsonSerializationNeverContainsPlaintextToken() throws Exception {
        TenantContext.set(tenantAId);

        SaveWhatsAppAccountCommand command = new SaveWhatsAppAccountCommand(
                "waba_1003",
                "phone_2003",
                "+1 (555) 333-4444",
                "Tenant A Sales",
                "GREEN",
                "TIER_1K",
                RAW_TOKEN
        );
        WhatsAppAccount saved = accountService.saveOrUpdateAccount(command);

        String json = objectMapper.writeValueAsString(saved);

        // Verify JSON contains metadata but NO token fields
        assertThat(json).contains("waba_1003");
        assertThat(json).contains("phone_2003");
        assertThat(json).contains("Tenant A Sales");
        assertThat(json).doesNotContain(RAW_TOKEN);
        assertThat(json).doesNotContain("EAAB");
        assertThat(json).doesNotContain("accessTokenEncrypted");
    }

    @Test
    void testTenantIsolationOnWhatsAppAccounts() {
        // Tenant A creates account
        TenantContext.set(tenantAId);
        WhatsAppAccount accountA = accountService.saveOrUpdateAccount(new SaveWhatsAppAccountCommand(
                "waba_A",
                "phone_A",
                "+1 (111) 111-1111",
                "Tenant A Name",
                "GREEN",
                "TIER_250",
                "TOKEN_TENANT_A"
        ));

        // Tenant B creates account
        TenantContext.set(tenantBId);
        WhatsAppAccount accountB = accountService.saveOrUpdateAccount(new SaveWhatsAppAccountCommand(
                "waba_B",
                "phone_B",
                "+1 (222) 222-2222",
                "Tenant B Name",
                "GREEN",
                "TIER_250",
                "TOKEN_TENANT_B"
        ));

        // Under Tenant A context: cannot see Tenant B account
        TenantContext.set(tenantAId);
        List<WhatsAppAccount> accountsForA = accountRepository.findAll();
        assertThat(accountsForA).hasSize(1);
        assertThat(accountsForA.get(0).getId()).isEqualTo(accountA.getId());

        // Attempting to query Tenant B's ID under Tenant A context fails with NotFound
        assertThatThrownBy(() -> accountService.getAccount(accountB.getId()))
                .isInstanceOf(NotFoundException.class);

        assertThatThrownBy(() -> accountService.getDecryptedToken(accountB.getId()))
                .isInstanceOf(NotFoundException.class);
    }
}
