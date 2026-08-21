-- F08: Message Ledger
-- Append-only record of every message with billing categories and immutable audit events.

CREATE TABLE message_ledger (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    whatsapp_account_id     UUID REFERENCES whatsapp_accounts(id) ON DELETE SET NULL,
    direction               VARCHAR(20) NOT NULL,
    wamid                   VARCHAR(255),
    recipient_phone_hash    VARCHAR(64) NOT NULL,
    recipient_phone_last4   VARCHAR(4) NOT NULL,
    billing_category        VARCHAR(50) NOT NULL,
    template_name           VARCHAR(255),
    conversation_window     VARCHAR(50),
    status                  VARCHAR(50) NOT NULL,
    status_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    idempotency_key         VARCHAR(255),
    job_id                  UUID,
    error_code              INT,
    error_message           TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE message_ledger_status_events (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    ledger_id   UUID NOT NULL REFERENCES message_ledger(id) ON DELETE CASCADE,
    status      VARCHAR(50) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    raw_payload JSONB
);

-- Unique index per tenant on wamid
CREATE UNIQUE INDEX uq_message_ledger_tenant_wamid ON message_ledger (tenant_id, wamid) WHERE wamid IS NOT NULL;

-- Unique index per tenant on idempotency_key
CREATE UNIQUE INDEX uq_message_ledger_tenant_idempotency ON message_ledger (tenant_id, idempotency_key) WHERE idempotency_key IS NOT NULL;

-- Index for monthly aggregation counts
CREATE INDEX idx_message_ledger_monthly_counts ON message_ledger (tenant_id, billing_category, created_at);

-- Index for status events by ledger ID
CREATE INDEX idx_message_ledger_status_events_ledger ON message_ledger_status_events (ledger_id, occurred_at);

-- Trigger function enforcing immutability of critical billing and identification fields
CREATE OR REPLACE FUNCTION prevent_immutable_ledger_updates()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.tenant_id IS DISTINCT FROM NEW.tenant_id THEN
        RAISE EXCEPTION 'Cannot modify tenant_id of message_ledger row';
    END IF;
    IF OLD.direction IS DISTINCT FROM NEW.direction THEN
        RAISE EXCEPTION 'Cannot modify direction of message_ledger row';
    END IF;
    IF OLD.billing_category IS DISTINCT FROM NEW.billing_category THEN
        RAISE EXCEPTION 'Cannot modify billing_category of message_ledger row';
    END IF;
    IF OLD.recipient_phone_hash IS DISTINCT FROM NEW.recipient_phone_hash THEN
        RAISE EXCEPTION 'Cannot modify recipient_phone_hash of message_ledger row';
    END IF;
    IF OLD.recipient_phone_last4 IS DISTINCT FROM NEW.recipient_phone_last4 THEN
        RAISE EXCEPTION 'Cannot modify recipient_phone_last4 of message_ledger row';
    END IF;
    IF OLD.created_at IS DISTINCT FROM NEW.created_at THEN
        RAISE EXCEPTION 'Cannot modify created_at of message_ledger row';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_message_ledger_immutable_guard
BEFORE UPDATE ON message_ledger
FOR EACH ROW
EXECUTE FUNCTION prevent_immutable_ledger_updates();

-- RLS for message_ledger
ALTER TABLE message_ledger ENABLE ROW LEVEL SECURITY;
ALTER TABLE message_ledger FORCE ROW LEVEL SECURITY;

CREATE POLICY message_ledger_tenant_isolation ON message_ledger
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

-- RLS for message_ledger_status_events
ALTER TABLE message_ledger_status_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE message_ledger_status_events FORCE ROW LEVEL SECURITY;

CREATE POLICY message_ledger_status_events_tenant_isolation ON message_ledger_status_events
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

DO
$do$
BEGIN
   IF EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE  rolname = 'wasaas_app') THEN
      GRANT SELECT, INSERT, UPDATE, DELETE ON message_ledger TO wasaas_app;
      GRANT SELECT, INSERT, UPDATE, DELETE ON message_ledger_status_events TO wasaas_app;
   END IF;
END
$do$;
