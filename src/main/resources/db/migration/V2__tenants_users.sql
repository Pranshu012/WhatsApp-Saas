-- F01: Tenant, user, and membership foundation.
-- `tenants` is the tenant and `users` can belong to multiple tenants, so these are the
-- documented exceptions to the tenant_id rule. tenant_users is the membership boundary.

CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_name TEXT NOT NULL,
    slug TEXT NOT NULL UNIQUE,
    status TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT tenants_status_check CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    full_name TEXT NOT NULL,
    status TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT users_email_lowercase_check CHECK (email = lower(email)),
    CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE TABLE tenant_users (
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    user_id UUID NOT NULL REFERENCES users(id),
    role TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (tenant_id, user_id),
    CONSTRAINT tenant_users_role_check CHECK (role IN ('OWNER', 'MEMBER'))
);
