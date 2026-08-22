package com.example.wasaas.tenant;

import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.wasaas.common.exception.DomainException;
import com.example.wasaas.user.User;
import com.example.wasaas.user.UserRepository;

@Service
public class TenantService {
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final TenantUserRepository tenantUserRepository;
    private final PasswordEncoder passwordEncoder;

    public TenantService(TenantRepository tenantRepository, UserRepository userRepository,
            TenantUserRepository tenantUserRepository, PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.tenantUserRepository = tenantUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegistrationResult registerTenant(RegistrationCommand command) {
        String email = command.email().trim().toLowerCase(Locale.ROOT);
        String slug = resolveAvailableSlug(command.slug(), command.businessName());
        if (userRepository.existsByEmail(email)) {
            throw new DomainException(HttpStatus.CONFLICT, "An account already exists for this email");
        }
        Tenant tenant = tenantRepository.save(Tenant.active(command.businessName().trim(), slug));
        User user = userRepository.save(User.active(email, passwordEncoder.encode(command.password()), command.fullName().trim()));
        try {
            com.example.wasaas.tenant.context.TenantContext.set(tenant.getId());
            tenantUserRepository.save(TenantUser.owner(tenant, user));
        } finally {
            com.example.wasaas.tenant.context.TenantContext.clear();
        }
        return new RegistrationResult(tenant.getBusinessName(), tenant.getSlug(), user.getFullName(), user.getEmail());
    }

    private String resolveAvailableSlug(String requestedSlug, String businessName) {
        if (requestedSlug != null && !requestedSlug.isBlank()) {
            String requested = requestedSlug.toLowerCase(Locale.ROOT).trim();
            if (tenantRepository.existsBySlug(requested)) {
                throw new DomainException(HttpStatus.CONFLICT, "This business URL is already in use");
            }
            return requested;
        }

        String raw = businessName;
        String base = raw.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (base.isBlank()) {
            base = "business";
        }
        base = base.substring(0, Math.min(base.length(), 70)).replaceAll("-+$", "");

        String candidate = base;
        int suffix = 2;
        while (tenantRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }
}
