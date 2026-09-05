-- V23: Conversation Messages for Multi-Turn AI Context and Chat History
-- Stores text messages between customers, AI assistant, and human agents with tenant isolation.

CREATE TABLE conversation_messages (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    conversation_id     UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    contact_id          UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    sender_type         VARCHAR(20) NOT NULL, -- 'CUSTOMER', 'AI_BOT', 'AGENT'
    text_content        TEXT NOT NULL,
    wamid               VARCHAR(255),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_conv_messages_conv_created ON conversation_messages (tenant_id, conversation_id, created_at DESC);
CREATE INDEX idx_conv_messages_contact ON conversation_messages (tenant_id, contact_id, created_at DESC);

-- RLS for conversation_messages
ALTER TABLE conversation_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE conversation_messages FORCE ROW LEVEL SECURITY;

CREATE POLICY conversation_messages_tenant_isolation ON conversation_messages
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

DO
$do$
BEGIN
   IF EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE  rolname = 'wasaas_app') THEN
      GRANT SELECT, INSERT, UPDATE, DELETE ON conversation_messages TO wasaas_app;
   END IF;
END
$do$;
