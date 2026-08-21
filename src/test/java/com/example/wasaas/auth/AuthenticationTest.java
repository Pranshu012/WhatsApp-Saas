package com.example.wasaas.auth;

import com.example.wasaas.tenant.RegistrationCommand;
import com.example.wasaas.tenant.Tenant;
import com.example.wasaas.tenant.TenantRepository;
import com.example.wasaas.tenant.TenantRole;
import com.example.wasaas.tenant.TenantService;
import com.example.wasaas.tenant.TenantUser;
import com.example.wasaas.tenant.TenantUserRepository;
import com.example.wasaas.tenant.context.TenantContext;
import com.example.wasaas.user.User;
import com.example.wasaas.user.UserRepository;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
public class AuthenticationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantService tenantService;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private TenantUserRepository tenantUserRepository;
    @Autowired private LoginAttemptRepository loginAttemptRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final String TEST_EMAIL = "auth.test@example.com";
    private static final String TEST_PASSWORD = "CorrectPassword123!";

    @BeforeEach
    void setup() {
        cleanup();
        tenantService.registerTenant(new RegistrationCommand(
                "Auth Test Business",
                "auth-test-biz",
                "Auth Tester",
                TEST_EMAIL,
                TEST_PASSWORD
        ));
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        jdbcTemplate.execute("TRUNCATE TABLE spring_session_attributes, spring_session, login_attempts, tenant_users, users, tenants CASCADE");
    }

    @Test
    void testSuccessfulLoginAndMeEndpoint() throws Exception {
        LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.fullName").value("Auth Tester"))
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.tenantId").isNotEmpty())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andReturn();

        Cookie sessionCookie = loginResult.getResponse().getCookie("SESSION");
        assertThat(sessionCookie).isNotNull();
        assertThat(sessionCookie.isHttpOnly()).isTrue();

        // Access /api/auth/me with session cookie
        mockMvc.perform(get("/api/auth/me")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.fullName").value("Auth Tester"))
                .andExpect(jsonPath("$.role").value("OWNER"));

        // Verify session is persisted in PostgreSQL SPRING_SESSION table
        Integer sessionCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM spring_session", Integer.class);
        assertThat(sessionCount).isGreaterThan(0);
    }

    @Test
    void testLogoutRequiresCsrfAndInvalidatesSession() throws Exception {
        LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie sessionCookie = loginResult.getResponse().getCookie("SESSION");
        assertThat(sessionCookie).isNotNull();

        // 1. Logout without CSRF token MUST be rejected with 403 Forbidden (prevents forced logout attacks)
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(sessionCookie))
                .andExpect(status().isForbidden());

        // 2. Logout with valid CSRF token succeeds
        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .cookie(sessionCookie))
                .andExpect(status().isOk());

        // 3. Subsequent call to /me should now fail with 401 Unauthorized
        mockMvc.perform(get("/api/auth/me")
                        .cookie(sessionCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testWrongPasswordReturnsGeneric401() throws Exception {
        LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, "WrongPassword123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void testUnknownEmailReturnsIdentical401() throws Exception {
        LoginRequest loginRequest = new LoginRequest("unknown.user@example.com", "SomePassword123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void testDisabledUserCannotLogin() throws Exception {
        User user = userRepository.findByEmail(TEST_EMAIL).orElseThrow();
        user.disable();
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void testXTenantIdHeaderGrantsNothing() throws Exception {
        // Attempting to access protected endpoint using only X-Tenant-Id header without session
        mockMvc.perform(get("/api/auth/me")
                        .header("X-Tenant-Id", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testExplicitTenantSelectionWhenUserBelongsToMultipleTenants() throws Exception {
        User user = userRepository.findByEmail(TEST_EMAIL).orElseThrow();

        // Create second tenant and add user as MEMBER
        Tenant secondTenant = tenantRepository.save(Tenant.active("Second Workspace", "second-workspace"));
        try {
            TenantContext.set(secondTenant.getId());
            tenantUserRepository.save(TenantUser.owner(secondTenant, user));
        } finally {
            TenantContext.clear();
        }

        // Login explicitly specifying second-workspace slug
        LoginRequest loginSecond = new LoginRequest(TEST_EMAIL, TEST_PASSWORD, "second-workspace");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginSecond)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(secondTenant.getId().toString()));

        // Login explicitly specifying first workspace slug
        Tenant firstTenant = tenantRepository.findBySlug("auth-test-biz").orElseThrow();
        LoginRequest loginFirst = new LoginRequest(TEST_EMAIL, TEST_PASSWORD, "auth-test-biz");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginFirst)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(firstTenant.getId().toString()));
    }

    @Test
    void testRateLimitTripsAfterMaxFailedAttempts() throws Exception {
        String testIp = "192.168.1.100";
        LoginRequest badRequest = new LoginRequest(TEST_EMAIL, "BadPassword!");

        // Perform 5 failed attempts
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .with(request -> {
                                request.setRemoteAddr(testIp);
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
                    .andExpect(status().isUnauthorized());
        }

        // 6th attempt should be blocked with 429 Too Many Requests
        mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr(testIp);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Too many failed login attempts. Please try again later."));
    }

    @Test
    void testSessionSurvivesPersistenceAcrossRequests() throws Exception {
        LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie sessionCookie = loginResult.getResponse().getCookie("SESSION");
        assertThat(sessionCookie).isNotNull();

        // Verify session attributes are present in spring_session_attributes table
        Integer attrCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM spring_session_attributes", Integer.class);
        assertThat(attrCount).isGreaterThan(0);

        // Make subsequent authenticated call ensuring session state is read from DB
        mockMvc.perform(get("/api/auth/me")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL));
    }

    @Test
    void testCsrfEndpointReturnsToken() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").isNotEmpty())
                .andExpect(jsonPath("$.parameterName").isNotEmpty());
    }
}
