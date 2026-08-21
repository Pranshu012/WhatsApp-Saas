CREATE TABLE jobs (
    id UUID PRIMARY KEY,
    tenant_id UUID,
    job_type VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(255),
    attempts INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 3,
    run_after TIMESTAMPTZ NOT NULL,
    locked_at TIMESTAMPTZ,
    locked_by VARCHAR(255),
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_jobs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id) ON DELETE CASCADE
);

-- Index for the SKIP LOCKED claim query
CREATE INDEX idx_jobs_claim ON jobs (status, run_after);

-- Prevent duplicate jobs for the same idempotency key
CREATE UNIQUE INDEX idx_jobs_idempotency ON jobs (idempotency_key) WHERE idempotency_key IS NOT NULL;
