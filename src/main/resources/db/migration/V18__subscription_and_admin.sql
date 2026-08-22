-- V18__subscription_and_admin.sql
-- Create subscriptions table and add super_admin flag to users

-- 1. Add super_admin flag to users
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS is_super_admin BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Create subscriptions table
CREATE TABLE IF NOT EXISTS subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    plan_type TEXT NOT NULL,
    status TEXT NOT NULL,
    trial_start_date TIMESTAMPTZ,
    trial_expires_at TIMESTAMPTZ,
    current_period_start TIMESTAMPTZ,
    current_period_end TIMESTAMPTZ,
    monthly_price_paise INTEGER NOT NULL DEFAULT 49900,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT subscriptions_plan_type_check CHECK (plan_type IN ('FREE_TRIAL', 'BUSINESS_499', 'CUSTOM')),
    CONSTRAINT subscriptions_status_check CHECK (status IN ('TRIALING', 'ACTIVE', 'PAST_DUE', 'EXPIRED', 'SUSPENDED', 'CANCELLED')),
    CONSTRAINT uq_subscriptions_tenant UNIQUE (tenant_id)
);

-- Index for subscription lookups by tenant
CREATE INDEX IF NOT EXISTS idx_subscriptions_tenant ON subscriptions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_status ON subscriptions(status);

-- 3. Enable RLS on subscriptions
ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE subscriptions FORCE ROW LEVEL SECURITY;

CREATE POLICY subscriptions_tenant_isolation ON subscriptions
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid OR NULLIF(current_setting('app.tenant_id', true), '') IS NULL)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

-- 4. Grant permissions to wasaas_app if role exists
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'wasaas_app') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON subscriptions TO wasaas_app;
    END IF;
END $$;

-- 5. Backfill existing active tenants with 14-day trials or active subscriptions if missing
INSERT INTO subscriptions (tenant_id, plan_type, status, trial_start_date, trial_expires_at, current_period_start, current_period_end, monthly_price_paise, currency)
SELECT 
    t.id,
    'FREE_TRIAL',
    'TRIALING',
    t.created_at,
    t.created_at + INTERVAL '14 days',
    t.created_at,
    t.created_at + INTERVAL '14 days',
    49900,
    'INR'
FROM tenants t
LEFT JOIN subscriptions s ON t.id = s.tenant_id
WHERE s.id IS NULL;
