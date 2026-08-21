-- F05: WhatsApp Accounts
-- Stores customer WABA details and AES-256-GCM encrypted access tokens with RLS isolation.

CREATE TABLE whatsapp_accounts (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    waba_id                 VARCHAR(100) NOT NULL,
    phone_number_id         VARCHAR(100) NOT NULL,
    display_phone_number    VARCHAR(50),
    verified_name           VARCHAR(255),
    quality_rating          VARCHAR(50) NOT NULL DEFAULT 'UNKNOWN',
    messaging_limit_tier    VARCHAR(50) NOT NULL DEFAULT 'TIER_250',
    access_token_encrypted  BYTEA NOT NULL,
    token_encrypted_at      TIMESTAMPTZ NOT NULL,
    status                  VARCHAR(50) NOT NULL DEFAULT 'CONNECTED',
    connected_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_whatsapp_accounts_tenant_phone UNIQUE (tenant_id, phone_number_id)
);

CREATE INDEX idx_whatsapp_accounts_tenant_lookup ON whatsapp_accounts (tenant_id, phone_number_id);
CREATE INDEX idx_whatsapp_accounts_tenant_waba ON whatsapp_accounts (tenant_id, waba_id);

-- Enable RLS and FORCE ROW LEVEL SECURITY
ALTER TABLE whatsapp_accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE whatsapp_accounts FORCE ROW LEVEL SECURITY;

CREATE POLICY whatsapp_accounts_tenant_isolation ON whatsapp_accounts
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

DO
$do$
BEGIN
   IF EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE  rolname = 'wasaas_app') THEN
      GRANT SELECT, INSERT, UPDATE, DELETE ON whatsapp_accounts TO wasaas_app;
   END IF;
END
$do$;
