# F02 — Tenant Isolation and Row-Level Security

## Status
Complete — verified with native PostgreSQL RLS and 4 mandatory isolation tests.

## Summary
Implemented multi-tenant isolation across two defense layers: application-level `TenantContext` / `TenantFilterAspect` and database-level PostgreSQL Row-Level Security (`wasaas_app` role with `USING` and `WITH CHECK` policies).

## Key Files
- `TenantContext.java`: Request-scoped `ThreadLocal` holder that fails loudly when accessed while unset.
- `TenantContextFilter.java`: Request filter establishing tenant context.
- `TenantFilterAspect.java`: AOP aspect enabling Hibernate tenant filter on repository operations.
- `TenantDataSourcePostProcessor.java`: Connection proxy configuring `set_config('app.tenant_id', ?, true)` per transaction.
- `V3__rls.sql`: Migration creating `wasaas_app` non-superuser role and RLS policies on tenant-scoped tables.
- `TenantIsolationTest.java`: 4 mandatory isolation tests proving cross-tenant isolation and RLS enforcement.
