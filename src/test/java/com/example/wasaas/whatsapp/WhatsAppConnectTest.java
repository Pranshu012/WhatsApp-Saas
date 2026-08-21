package com.example.wasaas.whatsapp;

import com.example.wasaas.common.exception.DomainException;
import com.example.wasaas.tenant.RegistrationCommand;
import com.example.wasaas.tenant.TenantRepository;
import com.example.wasaas.tenant.TenantService;
import com.example.wasaas.tenant.context.TenantContext;
import com.example.wasaas.whatsapp.meta.MetaGraphClient;
import com.example.wasaas.whatsapp.meta.MetaProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
public class WhatsAppConnectTest {

    @Autowired private WhatsAppConnectService connectService;
    @Autowired private WhatsAppAccountService accountService;
    @Autowired private WhatsAppAccountRepository accountRepository;
    @Autowired private MetaGraphClient metaGraphClient;
    @Autowired private MetaProperties metaProperties;
    @Autowired private TenantService tenantService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MockMvc mockMvc;

    private MockRestServiceServer mockServer;
    private UUID tenantId;

    private static final String WABA_ID = "10987654321";
    private static final String PHONE_ID = "20987654321";
    private static final String ACCESS_TOKEN = "EAAB_TEST_BUSINESS_SCOPED_TOKEN_123456789";

    @BeforeEach
    void setup() {
        cleanup();
        org.springframework.web.client.RestClient.Builder builder = metaGraphClient.createClientBuilder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        metaGraphClient.setRestClient(builder.build());

        tenantService.registerTenant(new RegistrationCommand(
                "Connect Test Biz",
                "connect-test-biz",
                "Connect Admin",
                "admin.connect@example.com",
                "Password123!"
        ));
        tenantId = tenantRepository.findBySlug("connect-test-biz").orElseThrow().getId();
        TenantContext.set(tenantId);
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        jdbcTemplate.execute("TRUNCATE TABLE whatsapp_accounts, spring_session_attributes, spring_session, password_reset_tokens, login_attempts, tenant_users, users, tenants CASCADE");
    }

    @Test
    void testConnectSuccessPersistsEncryptedAccountAndSubscribesWebhooks() throws Exception {
        String code = "valid_auth_code_123";

        // 1. Code Exchange Mock
        mockServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/oauth/access_token?client_id="
                        + metaProperties.getAppId() + "&client_secret=" + metaProperties.getAppSecret() + "&code=" + code))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"access_token\":\"" + ACCESS_TOKEN + "\",\"token_type\":\"bearer\"}",
                        MediaType.APPLICATION_JSON
                ));

        // 2. WABA Details Mock
        mockServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/" + WABA_ID + "?fields=id,name,timezone_id,message_template_namespace"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andRespond(withSuccess(
                        "{\"id\":\"" + WABA_ID + "\",\"name\":\"Acme Corp\",\"timezone_id\":\"Asia/Kolkata\",\"message_template_namespace\":\"acme_ns\"}",
                        MediaType.APPLICATION_JSON
                ));

        // 3. Phone Number Details Mock
        mockServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/" + PHONE_ID + "?fields=id,display_phone_number,verified_name,quality_rating,messaging_limit_tier"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andRespond(withSuccess(
                        "{\"id\":\"" + PHONE_ID + "\",\"display_phone_number\":\"+1 555-0199\",\"verified_name\":\"Acme Support\",\"quality_rating\":\"GREEN\",\"messaging_limit_tier\":\"TIER_10K\"}",
                        MediaType.APPLICATION_JSON
                ));

        // 4. Subscribed Apps Mock
        mockServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/" + WABA_ID + "/subscribed_apps"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

        // Execute Connect
        ConnectWhatsAppRequest request = new ConnectWhatsAppRequest(code, WABA_ID, PHONE_ID);
        WhatsAppAccountResponse response = connectService.connect(request);

        mockServer.verify();

        // Assert response attributes
        assertThat(response.id()).isNotNull();
        assertThat(response.wabaId()).isEqualTo(WABA_ID);
        assertThat(response.phoneNumberId()).isEqualTo(PHONE_ID);
        assertThat(response.displayPhoneNumber()).isEqualTo("+1 555-0199");
        assertThat(response.verifiedName()).isEqualTo("Acme Support");
        assertThat(response.qualityRating()).isEqualTo("GREEN");
        assertThat(response.messagingLimitTier()).isEqualTo("TIER_10K");
        assertThat(response.status()).isEqualTo("CONNECTED");

        // Assert JSON representation has NO token
        String jsonResponse = objectMapper.writeValueAsString(response);
        assertThat(jsonResponse).doesNotContain(ACCESS_TOKEN);
        assertThat(jsonResponse).doesNotContain("accessToken");
        assertThat(jsonResponse).doesNotContain("accessTokenEncrypted");

        // Verify account is stored with encrypted token in database
        WhatsAppAccount savedAccount = accountRepository.findById(response.id()).orElseThrow();
        assertThat(savedAccount.getTenantId()).isEqualTo(tenantId);
        assertThat(accountService.getDecryptedToken(savedAccount.getId())).isEqualTo(ACCESS_TOKEN);
    }

    @Test
    void testReconnectIsIdempotent() {
        String code1 = "auth_code_first";
        String code2 = "auth_code_second";
        String updatedToken = "EAAB_UPDATED_TOKEN_987654321";

        // First connect expectations
        mockServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/oauth/access_token?client_id=" + metaProperties.getAppId() + "&client_secret=" + metaProperties.getAppSecret() + "&code=" + code1))
                .andRespond(withSuccess("{\"access_token\":\"" + ACCESS_TOKEN + "\",\"token_type\":\"bearer\"}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/" + WABA_ID + "?fields=id,name,timezone_id,message_template_namespace"))
                .andRespond(withSuccess("{\"id\":\"" + WABA_ID + "\",\"name\":\"Acme Corp\"}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/" + PHONE_ID + "?fields=id,display_phone_number,verified_name,quality_rating,messaging_limit_tier"))
                .andRespond(withSuccess("{\"id\":\"" + PHONE_ID + "\",\"display_phone_number\":\"+1 555-0199\",\"verified_name\":\"Acme Support\",\"quality_rating\":\"GREEN\",\"messaging_limit_tier\":\"TIER_1K\"}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/" + WABA_ID + "/subscribed_apps"))
                .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

        // Second connect expectations (same phone ID, updated tier and token)
        mockServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/oauth/access_token?client_id=" + metaProperties.getAppId() + "&client_secret=" + metaProperties.getAppSecret() + "&code=" + code2))
                .andRespond(withSuccess("{\"access_token\":\"" + updatedToken + "\",\"token_type\":\"bearer\"}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/" + WABA_ID + "?fields=id,name,timezone_id,message_template_namespace"))
                .andRespond(withSuccess("{\"id\":\"" + WABA_ID + "\",\"name\":\"Acme Corp\"}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/" + PHONE_ID + "?fields=id,display_phone_number,verified_name,quality_rating,messaging_limit_tier"))
                .andRespond(withSuccess("{\"id\":\"" + PHONE_ID + "\",\"display_phone_number\":\"+1 555-0199\",\"verified_name\":\"Acme Support VIP\",\"quality_rating\":\"GREEN\",\"messaging_limit_tier\":\"TIER_10K\"}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/" + WABA_ID + "/subscribed_apps"))
                .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

        // Connect first time
        WhatsAppAccountResponse first = connectService.connect(new ConnectWhatsAppRequest(code1, WABA_ID, PHONE_ID));

        // Connect second time
        WhatsAppAccountResponse second = connectService.connect(new ConnectWhatsAppRequest(code2, WABA_ID, PHONE_ID));

        mockServer.verify();

        // Must update existing account rather than creating duplicate
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.verifiedName()).isEqualTo("Acme Support VIP");
        assertThat(second.messagingLimitTier()).isEqualTo("TIER_10K");
        assertThat(accountRepository.findAll()).hasSize(1);
        assertThat(accountService.getDecryptedToken(second.id())).isEqualTo(updatedToken);
    }

    @Test
    void testMetaErrorCode200ProducesClearAdvancedAccessMessage() {
        String code = "auth_code_error_200";

        // OAuth succeeds
        mockServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/oauth/access_token?client_id=" + metaProperties.getAppId() + "&client_secret=" + metaProperties.getAppSecret() + "&code=" + code))
                .andRespond(withSuccess("{\"access_token\":\"" + ACCESS_TOKEN + "\"}", MediaType.APPLICATION_JSON));

        // WABA lookup fails with Meta Error Code 200
        String metaError200Json = """
                {
                  "error": {
                    "message": "(#200) Requires whatsapp_business_management permission to manage the business account",
                    "type": "OAuthException",
                    "code": 200,
                    "error_subcode": 2388040,
                    "fbtrace_id": "A1B2C3D4"
                  }
                }
                """;

        mockServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/" + WABA_ID + "?fields=id,name,timezone_id,message_template_namespace"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(metaError200Json));

        assertThatThrownBy(() -> connectService.connect(new ConnectWhatsAppRequest(code, WABA_ID, PHONE_ID)))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Meta App lacks Advanced Access for WhatsApp Business Management. Please complete App Review in the Meta App Dashboard.");

        // Assert nothing was persisted
        assertThat(accountRepository.findAll()).isEmpty();
    }

    @Test
    void testSubscriptionFailureRollsBackTransaction() {
        String code = "auth_code_sub_fail";

        mockServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/oauth/access_token?client_id=" + metaProperties.getAppId() + "&client_secret=" + metaProperties.getAppSecret() + "&code=" + code))
                .andRespond(withSuccess("{\"access_token\":\"" + ACCESS_TOKEN + "\"}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/" + WABA_ID + "?fields=id,name,timezone_id,message_template_namespace"))
                .andRespond(withSuccess("{\"id\":\"" + WABA_ID + "\",\"name\":\"Acme Corp\"}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/" + PHONE_ID + "?fields=id,display_phone_number,verified_name,quality_rating,messaging_limit_tier"))
                .andRespond(withSuccess("{\"id\":\"" + PHONE_ID + "\",\"display_phone_number\":\"+1 555-0199\",\"verified_name\":\"Acme Support\"}", MediaType.APPLICATION_JSON));

        // Subscribed apps fails with 500 error
        mockServer.expect(requestTo(metaProperties.getApiBaseUrl() + "/" + WABA_ID + "/subscribed_apps"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"message\":\"Service unavailable\",\"code\":2}}"));

        assertThatThrownBy(() -> connectService.connect(new ConnectWhatsAppRequest(code, WABA_ID, PHONE_ID)))
                .isInstanceOf(DomainException.class);

        // Assert transaction rolled back — nothing persisted
        assertThat(accountRepository.findAll()).isEmpty();
    }
}
