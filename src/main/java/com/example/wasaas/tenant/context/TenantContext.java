package com.example.wasaas.tenant.context;

import java.util.UUID;

public final class TenantContext {
    private static final ThreadLocal<UUID> CONTEXT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        CONTEXT.set(tenantId);
    }

    public static UUID require() {
        UUID tenantId = CONTEXT.get();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is not set");
        }
        return tenantId;
    }

    public static UUID get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
