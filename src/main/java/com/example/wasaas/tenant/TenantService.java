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
        String slug = command.slug().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new DomainException(HttpStatus.CONFLICT, "An account already exists for this email");
        }
        if (tenantRepository.existsBySlug(slug)) {
            throw new DomainException(HttpStatus.CONFLICT, "This business URL is already in use");
        }

        Tenant tenant = tenantRepository.save(Tenant.active(command.businessName().trim(), slug));
        User user = userRepository.save(User.active(email, passwordEncoder.encode(command.password()), command.fullName().trim()));
        tenantUserRepository.save(TenantUser.owner(tenant, user));
        return new RegistrationResult(tenant.getBusinessName(), tenant.getSlug(), user.getFullName(), user.getEmail());
    }
}
