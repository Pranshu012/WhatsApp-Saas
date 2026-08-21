package com.example.wasaas.auth;

import com.example.wasaas.tenant.TenantRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Custom UserDetails carrying tenant context.
 * Stored in the Spring Session — must be Serializable.
 */
public class TenantPrincipal implements UserDetails, Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID userId;
    private final UUID tenantId;
    private final String email;
    private final String fullName;
    private final String passwordHash;
    private final TenantRole role;

    public TenantPrincipal(UUID userId, UUID tenantId, String email, String fullName,
                           String passwordHash, TenantRole role) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.email = email;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public UUID getUserId() { return userId; }
    public UUID getTenantId() { return tenantId; }
    public String getFullName() { return fullName; }
    public TenantRole getRole() { return role; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
