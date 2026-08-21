-- F11: Contacts and Conversations
-- Manages contact profiles with full E.164 phone numbers and tracks 24-hour service windows per conversation.

CREATE TABLE contacts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    phone_e164      VARCHAR(50) NOT NULL,
    phone_hash      VARCHAR(64) NOT NULL,
    display_name    VARCHAR(255),
    last_seen_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    opt_in_status   VARCHAR(50) NOT NULL DEFAULT 'OPTED_IN',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_contacts_tenant_phone UNIQUE (tenant_id, phone_e164)
);

CREATE TABLE conversations (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                   UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    contact_id                  UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    whatsapp_account_id         UUID REFERENCES whatsapp_accounts(id) ON DELETE SET NULL,
    last_inbound_at             TIMESTAMPTZ,
    last_outbound_at            TIMESTAMPTZ,
    service_window_expires_at   TIMESTAMPTZ,
    status                      VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_conversations_tenant_contact_account UNIQUE (tenant_id, contact_id, whatsapp_account_id)
);

CREATE INDEX idx_contacts_tenant_lookup ON contacts (tenant_id, phone_e164);
CREATE INDEX idx_contacts_tenant_hash ON contacts (tenant_id, phone_hash);
CREATE INDEX idx_conversations_tenant_lookup ON conversations (tenant_id, contact_id, whatsapp_account_id);
CREATE INDEX idx_conversations_window_expiry ON conversations (tenant_id, service_window_expires_at);

-- RLS for contacts
ALTER TABLE contacts ENABLE ROW LEVEL SECURITY;
ALTER TABLE contacts FORCE ROW LEVEL SECURITY;

CREATE POLICY contacts_tenant_isolation ON contacts
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

-- RLS for conversations
ALTER TABLE conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE conversations FORCE ROW LEVEL SECURITY;

CREATE POLICY conversations_tenant_isolation ON conversations
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

DO
$do$
BEGIN
   IF EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE  rolname = 'wasaas_app') THEN
      GRANT SELECT, INSERT, UPDATE, DELETE ON contacts TO wasaas_app;
      GRANT SELECT, INSERT, UPDATE, DELETE ON conversations TO wasaas_app;
   END IF;
END
$do$;
