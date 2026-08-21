package com.example.wasaas.auth;

import com.example.wasaas.common.exception.DomainException;
import com.example.wasaas.tenant.Tenant;
import com.example.wasaas.tenant.TenantRepository;
import com.example.wasaas.tenant.TenantStatus;
import com.example.wasaas.tenant.TenantUser;
import com.example.wasaas.tenant.TenantUserRepository;
import com.example.wasaas.user.User;
import com.example.wasaas.user.UserRepository;
import com.example.wasaas.user.UserStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

import java.util.List;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final TenantUserRepository tenantUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final String dummyPasswordHash;
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public AuthService(UserRepository userRepository,
                       TenantRepository tenantRepository,
                       TenantUserRepository tenantUserRepository,
                       PasswordEncoder passwordEncoder,
                       LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.tenantUserRepository = tenantUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
        // Pre-compute dummy hash for timing attack mitigation on unknown email
        this.dummyPasswordHash = passwordEncoder.encode("dummy-timing-defense-string-not-real");
    }

    @Transactional
    public AuthUserResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String email = request.email().toLowerCase().trim();
        String ip = getClientIp(httpRequest);

        if (loginAttemptService.isBlocked(email, ip)) {
            throw new DomainException(HttpStatus.TOO_MANY_REQUESTS, "Too many failed login attempts. Please try again later.");
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // Timing attack defense: perform dummy password verification
            passwordEncoder.matches(request.password(), dummyPasswordHash);
            loginAttemptService.recordFailure(email, ip);
            throw new DomainException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        User user = userOpt.get();
        if (user.getStatus() != UserStatus.ACTIVE) {
            // Inactive / disabled user treated with generic error
            passwordEncoder.matches(request.password(), dummyPasswordHash);
            loginAttemptService.recordFailure(email, ip);
            throw new DomainException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttemptService.recordFailure(email, ip);
            throw new DomainException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        TenantUser membership = resolveTenantMembership(user, request.tenantSlug());
        if (membership == null) {
            loginAttemptService.recordFailure(email, ip);
            throw new DomainException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        // Successful authentication
        loginAttemptService.clearAttempts(email, ip);

        TenantPrincipal principal = new TenantPrincipal(
                user.getId(),
                membership.getId().getTenantId(),
                user.getEmail(),
                user.getFullName(),
                user.getPasswordHash(),
                membership.getRole()
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

        return new AuthUserResponse(
                principal.getUserId(),
                principal.getTenantId(),
                principal.getUsername(),
                principal.getFullName(),
                principal.getRole().name()
        );
    }

    private TenantUser resolveTenantMembership(User user, String requestedSlug) {
        if (requestedSlug != null && !requestedSlug.isBlank()) {
            Optional<Tenant> tenantOpt = tenantRepository.findBySlug(requestedSlug.toLowerCase().trim());
            if (tenantOpt.isEmpty() || tenantOpt.get().getStatus() != TenantStatus.ACTIVE) {
                return null;
            }
            Tenant tenant = tenantOpt.get();
            List<TenantUser> memberships = tenantUserRepository.findByIdUserId(user.getId());
            return memberships.stream()
                    .filter(m -> m.getId().getTenantId().equals(tenant.getId()))
                    .findFirst()
                    .orElse(null);
        }

        List<TenantUser> memberships = tenantUserRepository.findByIdUserId(user.getId());
        if (memberships.isEmpty()) {
            return null;
        }

        // Filter for active tenants
        for (TenantUser m : memberships) {
            Optional<Tenant> tOpt = tenantRepository.findById(m.getId().getTenantId());
            if (tOpt.isPresent() && tOpt.get().getStatus() == TenantStatus.ACTIVE) {
                return m;
            }
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
