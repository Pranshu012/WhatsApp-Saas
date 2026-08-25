-- V20: WhatsApp Rates Table (Remediation Task T-007 / Finding F-09)
-- Stores official Meta WhatsApp Cloud API per-message rates in paise (integers)
-- with effective dates to handle quarterly price revisions and the Oct 1, 2026 service conversation billing change.

CREATE TABLE IF NOT EXISTS whatsapp_rates (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_code        VARCHAR(10) NOT NULL DEFAULT 'IN',
    category            VARCHAR(50) NOT NULL,
    rate_paise          INTEGER NOT NULL,
    effective_from      TIMESTAMPTZ NOT NULL,
    effective_to        TIMESTAMPTZ,
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_whatsapp_rates_country_cat_effective UNIQUE (country_code, category, effective_from)
);

CREATE INDEX IF NOT EXISTS idx_whatsapp_rates_lookup 
    ON whatsapp_rates (country_code, category, effective_from);

-- Grant privileges to wasaas_app if role exists
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'wasaas_app') THEN
        GRANT SELECT ON whatsapp_rates TO wasaas_app;
    END IF;
END $$;

-- Seed initial rates for India (IN)
-- Current baseline rates (pre-Oct 2026)
INSERT INTO whatsapp_rates (country_code, category, rate_paise, effective_from, effective_to, notes) VALUES
('IN', 'MARKETING', 86, '2026-01-01 00:00:00+00', NULL, 'Meta marketing conversation baseline ~₹0.86'),
('IN', 'UTILITY', 12, '2026-01-01 00:00:00+00', NULL, 'Meta utility conversation baseline ~₹0.115-0.12'),
('IN', 'AUTHENTICATION', 12, '2026-01-01 00:00:00+00', NULL, 'Meta auth OTP conversation baseline ~₹0.115-0.12'),
('IN', 'SERVICE', 0, '2026-01-01 00:00:00+00', '2026-10-01 00:00:00+00', '100% Free 24h customer service window prior to Oct 1 2026'),
('IN', 'SERVICE', 12, '2026-10-01 00:00:00+00', NULL, 'Meta service message billing effective Oct 1 2026 (~₹0.115)')
ON CONFLICT DO NOTHING;
