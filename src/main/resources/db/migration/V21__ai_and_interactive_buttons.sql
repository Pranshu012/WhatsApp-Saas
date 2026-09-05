-- V21: Tenant AI Configuration & Assistant Support
-- Stores per-tenant Gemini AI receptionist configuration, business context, and prompt settings with RLS.

CREATE TABLE IF NOT EXISTS tenant_ai_configs (
    tenant_id           UUID PRIMARY KEY REFERENCES tenants(id) ON DELETE CASCADE,
    enabled             BOOLEAN NOT NULL DEFAULT true,
    api_key             TEXT,
    model               VARCHAR(100) NOT NULL DEFAULT 'gemini-1.5-flash',
    business_context    TEXT NOT NULL DEFAULT '',
    system_prompt       TEXT NOT NULL DEFAULT 'You are a warm, helpful, and concise WhatsApp receptionist for this business. Reply in 1-2 sentences. Match the customer''s language (Hindi, English, or Hinglish). Never make up fake prices, discounts, or medical advice not provided in the business context. If unsure, politely ask the customer to speak with a human or visit the store.',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE tenant_ai_configs ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_ai_configs FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_ai_configs_isolation ON tenant_ai_configs
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

-- Grant permissions if wasaas_app role exists
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'wasaas_app') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON tenant_ai_configs TO wasaas_app;
    END IF;
END $$;
