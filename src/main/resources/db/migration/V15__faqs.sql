-- F14: FAQ Matching via PostgreSQL Full-Text Search and pg_trgm
-- Pure PostgreSQL FAQ matching with typo tolerance and RLS.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE faqs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    question            TEXT NOT NULL,
    answer              TEXT NOT NULL,
    enabled             BOOLEAN NOT NULL DEFAULT true,
    search_vector       tsvector GENERATED ALWAYS AS (
        setweight(to_tsvector('english', coalesce(question, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(answer, '')), 'B')
    ) STORED,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- GIN Index on search_vector
CREATE INDEX idx_faqs_search_vector ON faqs USING GIN (search_vector);

-- Trigram index on question for typo tolerance
CREATE INDEX idx_faqs_question_trgm ON faqs USING GIN (question gin_trgm_ops);

-- Tenant index
CREATE INDEX idx_faqs_tenant_enabled ON faqs (tenant_id, enabled);

-- RLS
ALTER TABLE faqs ENABLE ROW LEVEL SECURITY;
ALTER TABLE faqs FORCE ROW LEVEL SECURITY;

CREATE POLICY faqs_tenant_isolation ON faqs
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

DO
$do$
BEGIN
   IF EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE  rolname = 'wasaas_app') THEN
      GRANT SELECT, INSERT, UPDATE, DELETE ON faqs TO wasaas_app;
   END IF;
END
$do$;
