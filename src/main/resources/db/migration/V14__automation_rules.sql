-- F13: Keyword Automation Rules & Unmatched Message Logging
-- Tables for tenant automation rules and empirical unmatched message dataset with RLS.

CREATE TABLE automation_rules (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name                VARCHAR(255) NOT NULL,
    enabled             BOOLEAN NOT NULL DEFAULT true,
    match_type          VARCHAR(50) NOT NULL,
    match_value         TEXT NOT NULL,
    case_sensitive      BOOLEAN NOT NULL DEFAULT false,
    priority            INT NOT NULL DEFAULT 100,
    action_type         VARCHAR(50) NOT NULL,
    action_payload      JSONB NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_automation_rules_tenant_priority ON automation_rules (tenant_id, enabled, priority ASC);

ALTER TABLE automation_rules ENABLE ROW LEVEL SECURITY;
ALTER TABLE automation_rules FORCE ROW LEVEL SECURITY;

CREATE POLICY automation_rules_tenant_isolation ON automation_rules
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

CREATE TABLE unmatched_messages (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    whatsapp_account_id UUID REFERENCES whatsapp_accounts(id) ON DELETE CASCADE,
    contact_id         UUID REFERENCES contacts(id) ON DELETE SET NULL,
    sender_phone        VARCHAR(50) NOT NULL,
    message_text        TEXT NOT NULL,
    wamid               VARCHAR(128),
    received_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_unmatched_messages_tenant ON unmatched_messages (tenant_id, received_at DESC);

ALTER TABLE unmatched_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE unmatched_messages FORCE ROW LEVEL SECURITY;

CREATE POLICY unmatched_messages_tenant_isolation ON unmatched_messages
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

DO
$do$
BEGIN
   IF EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE  rolname = 'wasaas_app') THEN
      GRANT SELECT, INSERT, UPDATE, DELETE ON automation_rules TO wasaas_app;
      GRANT SELECT, INSERT, UPDATE, DELETE ON unmatched_messages TO wasaas_app;
   END IF;
END
$do$;
