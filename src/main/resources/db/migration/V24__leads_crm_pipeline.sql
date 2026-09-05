-- V24: Leads CRM Pipeline, Scoring, and Custom Qualification Rules

CREATE TABLE leads (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    contact_id              UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    stage                   VARCHAR(50) NOT NULL DEFAULT 'NEW', -- 'NEW', 'IN_DISCUSSION', 'PROPOSAL_SENT', 'WON', 'LOST'
    temperature             VARCHAR(20) NOT NULL DEFAULT 'WARM', -- 'HOT', 'WARM', 'COLD'
    score                   INTEGER NOT NULL DEFAULT 50, -- 0 to 100
    deal_value              NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    requirement_summary     TEXT,
    notes                   TEXT,
    last_interaction_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_leads_tenant_contact UNIQUE (tenant_id, contact_id)
);

CREATE INDEX idx_leads_tenant_stage ON leads (tenant_id, stage);
CREATE INDEX idx_leads_tenant_temperature ON leads (tenant_id, temperature);
CREATE INDEX idx_leads_tenant_score ON leads (tenant_id, score DESC);
CREATE INDEX idx_leads_contact ON leads (tenant_id, contact_id);

CREATE TABLE lead_qualification_rules (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    rule_name               VARCHAR(100) NOT NULL,
    match_keywords          TEXT NOT NULL,
    score_boost             INTEGER NOT NULL DEFAULT 20,
    is_active               BOOLEAN NOT NULL DEFAULT true,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_lead_rules_tenant ON lead_qualification_rules (tenant_id, is_active);

-- RLS for leads
ALTER TABLE leads ENABLE ROW LEVEL SECURITY;
ALTER TABLE leads FORCE ROW LEVEL SECURITY;

CREATE POLICY leads_tenant_isolation ON leads
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

-- RLS for lead_qualification_rules
ALTER TABLE lead_qualification_rules ENABLE ROW LEVEL SECURITY;
ALTER TABLE lead_qualification_rules FORCE ROW LEVEL SECURITY;

CREATE POLICY lead_rules_tenant_isolation ON lead_qualification_rules
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

DO
$do$
BEGIN
   IF EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE  rolname = 'wasaas_app') THEN
      GRANT SELECT, INSERT, UPDATE, DELETE ON leads TO wasaas_app;
      GRANT SELECT, INSERT, UPDATE, DELETE ON lead_qualification_rules TO wasaas_app;
   END IF;
END
$do$;
