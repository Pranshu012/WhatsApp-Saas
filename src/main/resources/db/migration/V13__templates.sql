-- F12: WhatsApp Message Templates
-- Stores template definitions, components, variable counts, and Meta-assigned categories with RLS.

CREATE TABLE whatsapp_templates (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    whatsapp_account_id     UUID NOT NULL REFERENCES whatsapp_accounts(id) ON DELETE CASCADE,
    meta_template_id        VARCHAR(100),
    name                    VARCHAR(255) NOT NULL,
    language                VARCHAR(50) NOT NULL,
    requested_category      VARCHAR(50),
    category                VARCHAR(50),
    category_conflict       BOOLEAN NOT NULL DEFAULT false,
    status                  VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    rejection_reason        TEXT,
    body_text               TEXT NOT NULL,
    header_type             VARCHAR(50),
    variable_count          INT NOT NULL DEFAULT 0,
    components              JSONB,
    synced_at               TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_whatsapp_templates_tenant_name_lang UNIQUE (tenant_id, name, language)
);

CREATE INDEX idx_whatsapp_templates_tenant_lookup ON whatsapp_templates (tenant_id, name, language);
CREATE INDEX idx_whatsapp_templates_status ON whatsapp_templates (tenant_id, status);

-- Enable RLS and FORCE ROW LEVEL SECURITY
ALTER TABLE whatsapp_templates ENABLE ROW LEVEL SECURITY;
ALTER TABLE whatsapp_templates FORCE ROW LEVEL SECURITY;

CREATE POLICY whatsapp_templates_tenant_isolation ON whatsapp_templates
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

DO
$do$
BEGIN
   IF EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE  rolname = 'wasaas_app') THEN
      GRANT SELECT, INSERT, UPDATE, DELETE ON whatsapp_templates TO wasaas_app;
   END IF;
END
$do$;
