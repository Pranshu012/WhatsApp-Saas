-- F10: Webhook Events
-- Fast ingestion and audit storage for incoming Meta WhatsApp webhooks.

CREATE TABLE webhook_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id        VARCHAR(255),
    waba_id         VARCHAR(100),
    phone_number_id VARCHAR(100),
    raw_payload     JSONB NOT NULL,
    signature_valid BOOLEAN NOT NULL DEFAULT true,
    received_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at    TIMESTAMPTZ,
    status          VARCHAR(50) NOT NULL DEFAULT 'PENDING'
);

-- Unique index for ingest deduplication on Meta event IDs
CREATE UNIQUE INDEX uq_webhook_events_event_id ON webhook_events (event_id) WHERE event_id IS NOT NULL;

-- Index for processing queue polling / cleanup
CREATE INDEX idx_webhook_events_status ON webhook_events (status, received_at);

DO
$do$
BEGIN
   IF EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE  rolname = 'wasaas_app') THEN
      GRANT SELECT, INSERT, UPDATE, DELETE ON webhook_events TO wasaas_app;
   END IF;
END
$do$;
