-- V19: Tighten tenant_users RLS policy & add secure user membership function (Remediation Task T-001 / Finding F-01)
-- Drops the fail-open clause (OR NULLIF(current_setting('app.tenant_id', true), '') IS NULL)
-- so that tenant_users strictly isolates rows per tenant when tenant context is set.

DROP POLICY IF EXISTS tenant_users_tenant_isolation ON tenant_users;

-- 1. Tenant-isolated RLS policy: strictly checks app.tenant_id
CREATE POLICY tenant_users_tenant_isolation ON tenant_users
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

-- 2. SECURITY DEFINER function to allow authentication resolution of a specific user's memberships
-- without exposing whole-table access to un-scoped queries.
CREATE OR REPLACE FUNCTION get_user_tenant_memberships(p_user_id UUID)
RETURNS TABLE (
    tenant_id UUID,
    user_id UUID,
    role TEXT,
    created_at TIMESTAMPTZ
)
SECURITY DEFINER
SET search_path = public, pg_temp
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT tu.tenant_id, tu.user_id, tu.role, tu.created_at
    FROM tenant_users tu
    WHERE tu.user_id = p_user_id;
END;
$$;

-- Grant execution permissions
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'wasaas_app') THEN
        GRANT EXECUTE ON FUNCTION get_user_tenant_memberships(UUID) TO wasaas_app;
    END IF;
END $$;
