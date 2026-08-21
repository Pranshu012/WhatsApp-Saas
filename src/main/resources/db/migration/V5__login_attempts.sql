-- F03: Login rate limiting
-- Tracks failed login attempts per email+IP for rate limiting.
-- No Redis needed — a simple Postgres counter table.

CREATE TABLE login_attempts (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL,
    ip_address    VARCHAR(45)  NOT NULL,
    attempted_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_login_attempts_lookup
    ON login_attempts (email, ip_address, attempted_at);

-- Cleanup: periodically delete rows older than 1 hour.
-- A scheduled job or manual CRON can run:
--   DELETE FROM login_attempts WHERE attempted_at < now() - interval '1 hour';
