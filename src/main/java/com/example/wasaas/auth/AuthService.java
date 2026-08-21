package com.example.wasaas.auth;

import com.example.wasaas.common.exception.DomainException;
import com.example.wasaas.tenant.TenantUser;
import com.example.wasaas.tenant.TenantUserRepository;
import com.example.wasaas.user.User;
import com.example.wasaas.user.UserRepository;
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
    private final TenantUserRepository tenantUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final String dummyPasswordHash;
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public AuthService(UserRepository userRepository,
                       TenantUserRepository tenantUserRepository,
                       PasswordEncoder passwordEncoder,
                       LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
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
            // Timing attack defense: perform a dummy password verification so latency matches real account check
            passwordEncoder.matches(request.password(), dummyPasswordHash);
            loginAttemptService.recordFailure(email, ip);
            throw new DomainException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttemptService.recordFailure(email, ip);
            throw new DomainException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        // Successful authentication
        loginAttemptService.clearAttempts(email, ip);

        List<TenantUser> memberships = tenantUserRepository.findByIdUserId(user.getId());
        if (memberships.isEmpty()) {
            throw new DomainException(HttpStatus.FORBIDDEN, "No active tenant membership found");
        }

        TenantUser membership = memberships.get(0);
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

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
