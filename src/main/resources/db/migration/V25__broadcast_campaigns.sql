-- V25: WhatsApp Bulk Broadcast & Marketing Campaigns
-- Tables for managing broadcast campaigns, target audiences, and recipient delivery logs.

CREATE TABLE broadcast_campaigns (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    whatsapp_account_id UUID REFERENCES whatsapp_accounts(id) ON DELETE CASCADE,
    name                VARCHAR(255) NOT NULL,
    target_type         VARCHAR(50) NOT NULL DEFAULT 'ALL_CONTACTS',
    target_stage        VARCHAR(50),
    template_name       VARCHAR(255),
    template_language   VARCHAR(50) NOT NULL DEFAULT 'en_US',
    template_id         UUID REFERENCES whatsapp_templates(id) ON DELETE SET NULL,
    template_params     JSONB,
    message_preview     TEXT,
    status              VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    total_recipients    INT NOT NULL DEFAULT 0,
    sent_count          INT NOT NULL DEFAULT 0,
    failed_count        INT NOT NULL DEFAULT 0,
    scheduled_for       TIMESTAMPTZ,
    started_at          TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE broadcast_recipients (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    broadcast_id        UUID NOT NULL REFERENCES broadcast_campaigns(id) ON DELETE CASCADE,
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    contact_id          UUID REFERENCES contacts(id) ON DELETE SET NULL,
    phone_e164          VARCHAR(30) NOT NULL,
    contact_name        VARCHAR(255),
    custom_params       JSONB,
    status              VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    wamid               VARCHAR(255),
    error_message       TEXT,
    sent_at             TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes
CREATE INDEX idx_broadcast_campaigns_tenant ON broadcast_campaigns (tenant_id, created_at DESC);
CREATE INDEX idx_broadcast_campaigns_due ON broadcast_campaigns (status, scheduled_for)
    WHERE status = 'SCHEDULED';
CREATE INDEX idx_broadcast_recipients_broadcast ON broadcast_recipients (broadcast_id, status);
CREATE INDEX idx_broadcast_recipients_tenant ON broadcast_recipients (tenant_id);

-- RLS Enforcement
ALTER TABLE broadcast_campaigns ENABLE ROW LEVEL SECURITY;
ALTER TABLE broadcast_campaigns FORCE ROW LEVEL SECURITY;

CREATE POLICY broadcast_campaigns_tenant_isolation ON broadcast_campaigns
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid OR NULLIF(current_setting('app.tenant_id', true), '') IS NULL)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE broadcast_recipients ENABLE ROW LEVEL SECURITY;
ALTER TABLE broadcast_recipients FORCE ROW LEVEL SECURITY;

CREATE POLICY broadcast_recipients_tenant_isolation ON broadcast_recipients
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid OR NULLIF(current_setting('app.tenant_id', true), '') IS NULL)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

DO
$do$
BEGIN
   IF EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE rolname = 'wasaas_app') THEN
      GRANT SELECT, INSERT, UPDATE, DELETE ON broadcast_campaigns TO wasaas_app;
      GRANT SELECT, INSERT, UPDATE, DELETE ON broadcast_recipients TO wasaas_app;
   END IF;
END
$do$;
