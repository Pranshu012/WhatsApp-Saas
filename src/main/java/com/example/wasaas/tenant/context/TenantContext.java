package com.example.wasaas.tenant.context;

import java.util.UUID;

/**
 * Thread-bound context holder for the currently authenticated tenant ID.
 * <p>
 * This context is automatically populated by web filters or background job execution
 * hooks and correlates directly with PostgreSQL Row-Level Security (RLS) via
 * {@code SET LOCAL app.tenant_id = '...'} executed on checked-out database connections.
 * Always cleared in a {@code finally} block to prevent thread leakage across requests.
 */
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
