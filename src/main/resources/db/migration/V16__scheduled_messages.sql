-- F16: Scheduled WhatsApp Messages
-- Table for future template message scheduling with UTC timestamp normalization and RLS.

CREATE TABLE scheduled_messages (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    contact_id         UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    template_id        UUID NOT NULL REFERENCES whatsapp_templates(id) ON DELETE RESTRICT,
    whatsapp_account_id UUID NOT NULL REFERENCES whatsapp_accounts(id) ON DELETE CASCADE,
    variables           JSONB,
    scheduled_for       TIMESTAMPTZ NOT NULL,
    timezone            VARCHAR(100) NOT NULL DEFAULT 'Asia/Kolkata',
    status              VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',
    job_id              UUID REFERENCES jobs(id) ON DELETE SET NULL,
    failure_reason      TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Index for due message polling
CREATE INDEX idx_scheduled_messages_due ON scheduled_messages (status, scheduled_for)
    WHERE status = 'SCHEDULED';

-- Tenant index
CREATE INDEX idx_scheduled_messages_tenant ON scheduled_messages (tenant_id, created_at DESC);

-- RLS
ALTER TABLE scheduled_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE scheduled_messages FORCE ROW LEVEL SECURITY;

CREATE POLICY scheduled_messages_tenant_isolation ON scheduled_messages
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid OR NULLIF(current_setting('app.tenant_id', true), '') IS NULL)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

DO
$do$
BEGIN
   IF EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE  rolname = 'wasaas_app') THEN
      GRANT SELECT, INSERT, UPDATE, DELETE ON scheduled_messages TO wasaas_app;
   END IF;
END
$do$;
