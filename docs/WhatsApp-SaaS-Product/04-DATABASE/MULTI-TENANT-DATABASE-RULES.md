# Multi-Tenant Database Rules

**Classification: BUILD NOW.** These rules are absolute. Read
`03-ARCHITECTURE/MULTI-TENANCY.md` for the reasoning.

## The checklist for every new table

Copy this into every migration's header comment and tick it off:

```sql
-- Increment: F##
-- [ ] tenant_id UUID NOT NULL REFERENCES tenants(id)
-- [ ] index on (tenant_id, <primary lookup column>)
-- [ ] ENABLE ROW LEVEL SECURITY
-- [ ] FORCE ROW LEVEL SECURITY
-- [ ] POLICY with BOTH USING and WITH CHECK
-- [ ] GRANT to the app role
-- [ ] isolation test added
```

## The standard RLS pattern — copy this exactly

```sql
CREATE TABLE example (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID NOT NULL REFERENCES tenants(id),
    -- ... columns ...
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_example_tenant ON example (tenant_id, created_at DESC);

ALTER TABLE example ENABLE ROW LEVEL SECURITY;
ALTER TABLE example FORCE ROW LEVEL SECURITY;

CREATE POLICY example_tenant_isolation ON example
    USING      (tenant_id = current_setting('app.tenant_id', true)::uuid)
    WITH CHECK (tenant_id = current_setting('app.tenant_id', true)::uuid);

GRANT SELECT, INSERT, UPDATE, DELETE ON example TO wasaas_app;
```

### Why each line

| Line | Without it |
|---|---|
| `tenant_id NOT NULL` | Nullable means rows visible to nobody or everybody depending on your policy |
| `REFERENCES tenants(id)` | Orphan rows with invented tenant ids |
| Index with `tenant_id` first | RLS filters on `tenant_id` on every query — it must lead the composite |
| `ENABLE ROW LEVEL SECURITY` | The policy is inert |
| `FORCE ROW LEVEL SECURITY` | The table owner bypasses the policy |
| `USING` | Reads leak |
| **`WITH CHECK`** | **Tenant A can INSERT rows belonging to Tenant B** |
| `GRANT` | The app can't use the table |

## Absolute rules

1. **The application must NOT connect as a superuser or table owner.**
   Superusers bypass RLS entirely. Create `wasaas_app` with `NOSUPERUSER NOBYPASSRLS`.
   **This includes tests** — Testcontainers defaults to a superuser, which silently makes every
   isolation test pass for the wrong reason.

2. **`app.tenant_id` is set transaction-locally.**
   `SELECT set_config('app.tenant_id', ?, true)` — the `true` means transaction-scoped. With
   `false` the value survives on a pooled connection and the next request inherits it. That is a
   cross-tenant leak caused by one boolean.

3. **Never accept `tenant_id` from a client.** It comes from the authenticated session, always.

4. **Audit every join against a table without RLS** (`tenants`, `users`, `whatsapp_rates`,
   `webhook_events`). Joining an unprotected table can widen a result set unexpectedly.

5. **`webhook_events` must never be exposed through a tenant-facing endpoint.** It has no
   `tenant_id` by design.

## Verifying RLS is actually on

```sql
-- every tenant table should show rowsecurity = true and forcerowsecurity = true
SELECT tablename, rowsecurity, forcerowsecurity
FROM   pg_tables
WHERE  schemaname = 'public'
ORDER  BY tablename;

-- policies must exist, with both qual (USING) and with_check
SELECT tablename, policyname, qual, with_check
FROM   pg_policies
WHERE  schemaname = 'public';

-- confirm the app role cannot bypass
SELECT rolname, rolsuper, rolbypassrls FROM pg_roles WHERE rolname = 'wasaas_app';
-- expect: false, false
```

Add these as an automated check in the pre-production checklist. A table that quietly shipped
without a policy is invisible until it isn't.

## Common mistakes

| Mistake | Consequence |
|---|---|
| `WITH CHECK` omitted | Cross-tenant writes possible |
| App connects as superuser | RLS entirely inert; all isolation tests meaningless |
| `set_config(..., false)` | Tenant id leaks across pooled connections |
| `TenantContext` returns null instead of throwing | Queries run unfiltered |
| Missing `finally { TenantContext.clear(); }` | Thread-pool reuse leaks tenant to the next request |
| New table added without a policy | Silent leak; found only by audit |
| Testcontainers as superuser | Green tests, broken production |
