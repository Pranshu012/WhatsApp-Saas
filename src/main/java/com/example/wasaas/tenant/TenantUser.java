package com.example.wasaas.tenant;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import com.example.wasaas.user.User;

@Entity
@Table(name = "tenant_users")
public class TenantUser {
    @EmbeddedId private TenantUserId id;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TenantRole role;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected TenantUser() { }
    private TenantUser(Tenant tenant, User user) { this.id = new TenantUserId(tenant.getId(), user.getId()); this.role = TenantRole.OWNER; }
    public static TenantUser owner(Tenant tenant, User user) { return new TenantUser(tenant, user); }
    @PrePersist void onCreate() { createdAt = Instant.now(); }
}
