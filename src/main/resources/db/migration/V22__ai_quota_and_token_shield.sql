-- V22: AI Token Protection Shield & Monthly Quota Management
-- Adds per-tenant monthly reply limits and usage tracking to prevent token exhaustion.

ALTER TABLE tenant_ai_configs 
ADD COLUMN IF NOT EXISTS monthly_limit INT NOT NULL DEFAULT 500,
ADD COLUMN IF NOT EXISTS used_this_month INT NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS current_period_month VARCHAR(7) NOT NULL DEFAULT to_char(now(), 'YYYY-MM');
