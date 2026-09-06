package com.example.wasaas.auth.oauth;

import com.example.wasaas.auth.AuthUserResponse;
import com.example.wasaas.auth.TenantPrincipal;
import com.example.wasaas.common.exception.DomainException;
import com.example.wasaas.subscription.SubscriptionService;
import com.example.wasaas.tenant.Tenant;
import com.example.wasaas.tenant.TenantRepository;
import com.example.wasaas.tenant.TenantStatus;
import com.example.wasaas.tenant.TenantUser;
import com.example.wasaas.tenant.TenantUserRepository;
import com.example.wasaas.tenant.context.TenantContext;
import com.example.wasaas.user.User;
import com.example.wasaas.user.UserRepository;
import com.example.wasaas.user.UserStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class GoogleOAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthService.class);
    private static final String GOOGLE_TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";

    private final GoogleOAuthProperties properties;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final TenantUserRepository tenantUserRepository;
    private final SubscriptionService subscriptionService;
    private final PasswordEncoder passwordEncoder;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public GoogleOAuthService(GoogleOAuthProperties properties,
                              UserRepository userRepository,
                              TenantRepository tenantRepository,
                              TenantUserRepository tenantUserRepository,
                              SubscriptionService subscriptionService,
                              PasswordEncoder passwordEncoder,
                              ObjectMapper objectMapper) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.tenantUserRepository = tenantUserRepository;
        this.subscriptionService = subscriptionService;
        this.passwordEncoder = passwordEncoder;
        this.restClient = RestClient.builder().build();
        this.objectMapper = objectMapper;
    }

    public record GoogleUserInfo(String email, String name, String sub, String picture) {}

    @Transactional
    public AuthUserResponse authenticateWithGoogle(GoogleOAuthRequest request,
                                                  HttpServletRequest httpRequest,
                                                  HttpServletResponse httpResponse) {
        GoogleUserInfo userInfo = verifyAndExtractUserInfo(request);
        String email = userInfo.email().trim().toLowerCase(Locale.ROOT);
        String fullName = userInfo.name() != null && !userInfo.name().isBlank() ? userInfo.name().trim() : "Google User";

        Optional<User> existingUserOpt = userRepository.findByEmail(email);
        User user;
        Tenant tenant;
        TenantUser membership;

        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new DomainException(HttpStatus.FORBIDDEN, "Account is inactive. Please contact support.");
            }

            membership = resolveTenantMembership(user);
            if (membership == null) {
                // Edge case: user exists but lacks an active tenant workspace -> provision one automatically
                tenant = provisionWorkspace(user, request.businessName(), fullName);
                membership = createOwnerMembership(tenant, user);
            } else {
                tenant = tenantRepository.findById(membership.getId().getTenantId())
                        .orElseThrow(() -> new DomainException(HttpStatus.INTERNAL_SERVER_ERROR, "Tenant workspace not found"));
            }
        } else {
            // New Customer Onboarding via Google OAuth
            log.info("Onboarding brand new customer via Google OAuth: email=[{}]", email);
            String randomSecurePassword = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
            user = userRepository.save(User.active(email, passwordEncoder.encode(randomSecurePassword), fullName));

            tenant = provisionWorkspace(user, request.businessName(), fullName);
            membership = createOwnerMembership(tenant, user);
        }

        user.recordLogin();
        userRepository.save(user);

        // Establish authenticated session in Spring Security
        TenantPrincipal principal = new TenantPrincipal(
                user.getId(),
                membership.getId().getTenantId(),
                user.getEmail(),
                user.getFullName(),
                user.getPasswordHash(),
                membership.getRole(),
                user.isSuperAdmin()
        );

        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );

        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(auth);
        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        log.info("Google OAuth login successful: user=[{}], tenant=[{}], role=[{}]",
                user.getEmail(), tenant.getSlug(), membership.getRole());

        return new AuthUserResponse(
                principal.getUserId(),
                principal.getTenantId(),
                principal.getUsername(),
                principal.getFullName(),
                principal.getRole().name(),
                principal.isSuperAdmin()
        );
    }

    private GoogleUserInfo verifyAndExtractUserInfo(GoogleOAuthRequest request) {
        String credential = request.credential().trim();

        // 1. Support dev/mock mode when enabled and token starts with mock- or dev-
        if (properties.isMockEnabled() && (credential.startsWith("mock-") || credential.startsWith("dev-") || "mock-token".equals(credential))) {
            String email = request.email() != null && !request.email().isBlank()
                    ? request.email()
                    : "demo.user@example.com";
            String name = request.name() != null && !request.name().isBlank()
                    ? request.name()
                    : "Demo Google User";
            log.info("Using mock Google OAuth authentication: email=[{}], name=[{}]", email, name);
            return new GoogleUserInfo(email, name, "mock-sub-12345", "");
        }

        // 2. Real Google Identity verification via Google tokeninfo endpoint
        try {
            String jsonResponse = restClient.get()
                    .uri(GOOGLE_TOKENINFO_URL + credential)
                    .retrieve()
                    .body(String.class);

            if (jsonResponse == null || jsonResponse.isBlank()) {
                throw new DomainException(HttpStatus.UNAUTHORIZED, "Invalid Google credential response");
            }

            JsonNode node = objectMapper.readTree(jsonResponse);

            if (node.has("error_description")) {
                throw new DomainException(HttpStatus.UNAUTHORIZED, "Google token error: " + node.get("error_description").asText());
            }

            String email = node.has("email") ? node.get("email").asText() : null;
            if (email == null || email.isBlank()) {
                throw new DomainException(HttpStatus.UNAUTHORIZED, "Google account does not contain a valid email");
            }

            boolean emailVerified = node.has("email_verified") &&
                    ("true".equalsIgnoreCase(node.get("email_verified").asText()) || node.get("email_verified").asBoolean(false));

            if (!emailVerified) {
                throw new DomainException(HttpStatus.UNAUTHORIZED, "Google email address is not verified");
            }

            // Verify audience if client-id is explicitly configured
            if (properties.getClientId() != null && !properties.getClientId().isBlank()) {
                String aud = node.has("aud") ? node.get("aud").asText() : "";
                if (!properties.getClientId().equals(aud)) {
                    log.warn("Google token audience mismatch. Expected [{}] but got [{}]", properties.getClientId(), aud);
                    throw new DomainException(HttpStatus.UNAUTHORIZED, "Google token audience mismatch");
                }
            }

            String name = node.has("name") ? node.get("name").asText() : "Google User";
            String sub = node.has("sub") ? node.get("sub").asText() : UUID.randomUUID().toString();
            String picture = node.has("picture") ? node.get("picture").asText() : "";

            return new GoogleUserInfo(email, name, sub, picture);
        } catch (DomainException de) {
            throw de;
        } catch (Exception e) {
            log.error("Failed to verify Google ID token with Google tokeninfo endpoint", e);
            throw new DomainException(HttpStatus.UNAUTHORIZED, "Failed to authenticate with Google: " + e.getMessage());
        }
    }

    private Tenant provisionWorkspace(User user, String requestedBusinessName, String userFullName) {
        String businessName;
        if (requestedBusinessName != null && !requestedBusinessName.isBlank()) {
            businessName = requestedBusinessName.trim();
        } else {
            businessName = userFullName + "'s Workspace";
        }

        String slug = resolveAvailableSlug(null, businessName);
        Tenant tenant = tenantRepository.save(Tenant.active(businessName, slug));

        try {
            TenantContext.set(tenant.getId());
            subscriptionService.createTrial(tenant.getId());
        } finally {
            TenantContext.clear();
        }

        return tenant;
    }

    private TenantUser createOwnerMembership(Tenant tenant, User user) {
        try {
            TenantContext.set(tenant.getId());
            return tenantUserRepository.save(TenantUser.owner(tenant, user));
        } finally {
            TenantContext.clear();
        }
    }

    private TenantUser resolveTenantMembership(User user) {
        List<TenantUser> memberships = tenantUserRepository.findByIdUserId(user.getId());
        for (TenantUser m : memberships) {
            Optional<Tenant> tOpt = tenantRepository.findById(m.getId().getTenantId());
            if (tOpt.isPresent() && tOpt.get().getStatus() == TenantStatus.ACTIVE) {
                return m;
            }
        }
        return null;
    }

    private String resolveAvailableSlug(String requestedSlug, String businessName) {
        if (requestedSlug != null && !requestedSlug.isBlank()) {
            String requested = requestedSlug.toLowerCase(Locale.ROOT).trim();
            if (!tenantRepository.existsBySlug(requested)) {
                return requested;
            }
        }

        String base = businessName.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (base.isBlank()) {
            base = "workspace";
        }
        base = base.substring(0, Math.min(base.length(), 60)).replaceAll("-+$", "");

        String candidate = base;
        int suffix = 2;
        while (tenantRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }
}
