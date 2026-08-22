package com.example.wasaas.subscription;

import com.example.wasaas.auth.TenantPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Filter that verifies whether an authenticated tenant has an active subscription or valid free trial.
 * Excludes read-only / auth endpoints and super_admin requests.
 */
@Component
public class SubscriptionEnforcementFilter extends OncePerRequestFilter {

    private final SubscriptionService subscriptionService;

    // Endpoints that are always accessible even if trial is expired
    private static final Set<String> EXEMPT_PREFIXES = Set.of(
            "/api/auth/",
            "/api/admin/",
            "/api/subscription",
            "/api/settings/",
            "/api/webhooks/",
            "/actuator/"
    );

    public SubscriptionEnforcementFilter(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Check if path is exempt
        for (String exempt : EXEMPT_PREFIXES) {
            if (path.startsWith(exempt)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof TenantPrincipal principal) {
            // Super admins always bypass
            if (principal.isSuperAdmin()) {
                filterChain.doFilter(request, response);
                return;
            }

            UUID tenantId = principal.getTenantId();
            if (tenantId != null) {
                boolean valid = subscriptionService.isSubscriptionValid(tenantId);
                if (!valid) {
                    response.setStatus(HttpStatus.PAYMENT_REQUIRED.value()); // 402
                    response.setContentType("application/json");
                    response.getWriter().write("""
                            {
                                "status": 402,
                                "error": "Payment Required",
                                "message": "Your 14-day free trial has expired or your account is suspended. Please contact admin to activate your plan."
                            }
                            """);
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
