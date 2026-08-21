package com.example.wasaas.tenant;

import java.io.Serializable;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class TenantUserId implements Serializable {
    @Column(name = "tenant_id") private UUID tenantId;
    @Column(name = "user_id") private UUID userId;
    public UUID getTenantId() { return tenantId; }
    protected TenantUserId() { }
    public TenantUserId(UUID tenantId, UUID userId) { this.tenantId = tenantId; this.userId = userId; }
    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TenantUserId that)) return false;
        return tenantId.equals(that.tenantId) && userId.equals(that.userId);
    }
    @Override public int hashCode() { return java.util.Objects.hash(tenantId, userId); }
}
