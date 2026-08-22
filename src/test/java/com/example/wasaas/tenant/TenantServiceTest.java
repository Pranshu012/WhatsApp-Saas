package com.example.wasaas.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.wasaas.common.exception.DomainException;
import com.example.wasaas.user.UserRepository;
import com.example.wasaas.user.User;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {
    @Mock private TenantRepository tenantRepository;
    @Mock private UserRepository userRepository;
    @Mock private TenantUserRepository tenantUserRepository;
    @Mock private com.example.wasaas.subscription.SubscriptionService subscriptionService;
    private TenantService service;
    private PasswordEncoder passwordEncoder;

    @BeforeEach void setUp() {
        passwordEncoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        service = new TenantService(tenantRepository, userRepository, tenantUserRepository, passwordEncoder, subscriptionService);
    }

    @Test void registersOwnerWithHashedPasswordAndNormalizedIdentifiers() {
        when(tenantRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.registerTenant(new RegistrationCommand("  My Shop  ", "my-shop", "  Priya  ", "PRIYA@EXAMPLE.COM", "a-secure-password"));
        assertThat(result.businessName()).isEqualTo("My Shop");
        assertThat(result.ownerEmail()).isEqualTo("priya@example.com");
        var user = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(user.capture());
        assertThat(user.getValue().getPasswordHash()).isNotEqualTo("a-secure-password");
        assertThat(passwordEncoder.matches("a-secure-password", user.getValue().getPasswordHash())).isTrue();
    }

    @Test void rejectsDuplicateEmail() {
        when(userRepository.existsByEmail("priya@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service.registerTenant(new RegistrationCommand("Shop", "shop", "Priya", "priya@example.com", "a-secure-password")))
                .isInstanceOf(DomainException.class).extracting(error -> ((DomainException) error).status()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test void rejectsDuplicateSlug() {
        when(tenantRepository.existsBySlug("shop")).thenReturn(true);
        assertThatThrownBy(() -> service.registerTenant(new RegistrationCommand("Shop", "shop", "Priya", "priya@example.com", "a-secure-password")))
                .isInstanceOf(DomainException.class).extracting(error -> ((DomainException) error).status()).isEqualTo(HttpStatus.CONFLICT);
    }
}
