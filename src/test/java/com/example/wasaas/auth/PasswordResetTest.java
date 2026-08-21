package com.example.wasaas.auth;

import com.example.wasaas.common.email.EmailSender;
import com.example.wasaas.tenant.RegistrationCommand;
import com.example.wasaas.tenant.TenantRepository;
import com.example.wasaas.tenant.TenantService;
import com.example.wasaas.tenant.TenantUserRepository;
import com.example.wasaas.tenant.context.TenantContext;
import com.example.wasaas.user.User;
import com.example.wasaas.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
public class PasswordResetTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantService tenantService;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private TenantUserRepository tenantUserRepository;
    @Autowired private PasswordResetTokenRepository tokenRepository;
    @Autowired private PasswordResetService passwordResetService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockBean private EmailSender emailSender;

    private static final String TEST_EMAIL = "reset.test@example.com";
    private static final String INITIAL_PASSWORD = "InitialPassword123!";
    private static final String NEW_PASSWORD = "NewUpdatedPassword123!";

    @BeforeEach
    void setup() {
        cleanup();
        tenantService.registerTenant(new RegistrationCommand(
                "Reset Test Business",
                "reset-test-biz",
                "Reset Tester",
                TEST_EMAIL,
                INITIAL_PASSWORD
        ));
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        jdbcTemplate.execute("TRUNCATE TABLE spring_session_attributes, spring_session, password_reset_tokens, login_attempts, tenant_users, users, tenants CASCADE");
    }

    @Test
    void testValidPasswordResetFlow() throws Exception {
        // 1. Request password reset
        ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest(TEST_EMAIL);
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());

        // 2. Capture raw token sent in the email
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq(TEST_EMAIL), anyString(), bodyCaptor.capture());

        String body = bodyCaptor.getValue();
        assertThat(body).contains("token=");
        String rawToken = body.substring(body.indexOf("token=") + 6).trim();

        // 3. Verify DB has the SHA-256 hash, not the raw token
        PasswordResetToken storedToken = tokenRepository.findAll().get(0);
        assertThat(storedToken.getTokenHash()).isEqualTo(passwordResetService.hashToken(rawToken));
        assertThat(storedToken.getTokenHash()).isNotEqualTo(rawToken);

        // 4. Perform password reset with valid token
        ResetPasswordRequest resetRequest = new ResetPasswordRequest(rawToken, NEW_PASSWORD);
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password has been successfully reset. Please log in with your new password."));

        // 5. Old password no longer works
        LoginRequest oldLogin = new LoginRequest(TEST_EMAIL, INITIAL_PASSWORD);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldLogin)))
                .andExpect(status().isUnauthorized());

        // 6. New password successfully authenticates
        LoginRequest newLogin = new LoginRequest(TEST_EMAIL, NEW_PASSWORD);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL));
    }

    @Test
    void testTokenCannotBeReused() throws Exception {
        // Request password reset
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(TEST_EMAIL))))
                .andExpect(status().isOk());

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq(TEST_EMAIL), anyString(), bodyCaptor.capture());
        String rawToken = bodyCaptor.getValue().substring(bodyCaptor.getValue().indexOf("token=") + 6).trim();

        // First reset succeeds
        ResetPasswordRequest resetRequest = new ResetPasswordRequest(rawToken, NEW_PASSWORD);
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk());

        // Second reset with the same token MUST be rejected with 400 Bad Request
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(rawToken, "YetAnotherPassword123!"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired password reset token"));
    }

    @Test
    void testExpiredTokenIsRejected() throws Exception {
        User user = userRepository.findByEmail(TEST_EMAIL).orElseThrow();
        String rawToken = "expired-raw-token-12345";
        String tokenHash = passwordResetService.hashToken(rawToken);

        // Manually insert an expired token (expiresAt in past)
        PasswordResetToken expiredToken = new PasswordResetToken(user.getId(), tokenHash, Instant.now().minus(5, ChronoUnit.MINUTES));
        tokenRepository.save(expiredToken);

        ResetPasswordRequest resetRequest = new ResetPasswordRequest(rawToken, NEW_PASSWORD);
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired password reset token"));
    }

    @Test
    void testUnknownEmailReturns200WithNoEmailSent() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("nonexistent.user@example.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());

        verify(emailSender, never()).send(eq("nonexistent.user@example.com"), anyString(), anyString());
    }

    @Test
    void testExistingSessionsInvalidatedAfterReset() throws Exception {
        // 1. Log in to establish an active session
        LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, INITIAL_PASSWORD);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie sessionCookie = loginResult.getResponse().getCookie("SESSION");
        assertThat(sessionCookie).isNotNull();

        // 2. Verify active session works on /me
        mockMvc.perform(get("/api/auth/me")
                        .cookie(sessionCookie))
                .andExpect(status().isOk());

        // 3. Request reset and execute password update
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(TEST_EMAIL))))
                .andExpect(status().isOk());

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq(TEST_EMAIL), anyString(), bodyCaptor.capture());
        String rawToken = bodyCaptor.getValue().substring(bodyCaptor.getValue().indexOf("token=") + 6).trim();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(rawToken, NEW_PASSWORD))))
                .andExpect(status().isOk());

        // 4. Existing session MUST now be invalidated and rejected with 401 Unauthorized
        mockMvc.perform(get("/api/auth/me")
                        .cookie(sessionCookie))
                .andExpect(status().isUnauthorized());
    }
}
