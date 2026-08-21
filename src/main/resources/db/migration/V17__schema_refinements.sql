-- V17__schema_refinements.sql
-- Schema refinements: tenant billing/profile columns, missing indexes, and role grants

-- 1. Tenant Business Profile & Invoicing Details
ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Kolkata',
    ADD COLUMN IF NOT EXISTS gstin VARCHAR(15),
    ADD COLUMN IF NOT EXISTS legal_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS billing_address TEXT;

-- 2. WhatsApp Account payment method indicator
ALTER TABLE whatsapp_accounts
    ADD COLUMN IF NOT EXISTS payment_method_attached BOOLEAN NOT NULL DEFAULT FALSE;

-- 3. User last login tracking
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMPTZ;

-- 4. Critical Performance Indexes
CREATE INDEX IF NOT EXISTS idx_conv_recent
    ON conversations (tenant_id, last_inbound_at DESC);

CREATE INDEX IF NOT EXISTS idx_jobs_claim_partial
    ON jobs (run_after)
    WHERE status IN ('PENDING', 'RUNNING');

CREATE INDEX IF NOT EXISTS idx_webhook_events_status_partial
    ON webhook_events (status, received_at)
    WHERE status IN ('PENDING', 'FAILED');

CREATE INDEX IF NOT EXISTS idx_conversations_window_expiry_partial
    ON conversations (service_window_expires_at)
    WHERE status = 'OPEN';

CREATE INDEX IF NOT EXISTS idx_ledger_recipient
    ON message_ledger (tenant_id, recipient_phone_hash, created_at);

CREATE INDEX IF NOT EXISTS idx_ledger_status
    ON message_ledger (tenant_id, status, created_at);

-- 5. Role Grants for non-superuser wasaas_app
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'wasaas_app') THEN
        GRANT ALL ON jobs, login_attempts, SPRING_SESSION, SPRING_SESSION_ATTRIBUTES TO wasaas_app;
    END IF;
END $$;
