# ADR-004 — Row-Level Multi-Tenancy with `tenant_id` + RLS

**Status:** Accepted · 18 August 2026 · **Highest-risk decision in the system**

## Context
Multiple unrelated businesses share one application and one database. Each tenant's data
includes conversations between them and their own customers. A cross-tenant leak is not a
bug — it is an existential event for a B2B product.

## Decision
Shared database, shared schema, `tenant_id` on **every** table, with **two** independent
enforcement layers:

1. **Application layer** — request-scoped `TenantContext`, populated from the authenticated
   session, with tenant-scoped repository access that cannot be forgotten.
2. **Database layer** — PostgreSQL Row-Level Security. The application connects as a
   **non-superuser** role (superusers bypass RLS). `app.tenant_id` is set per transaction and
   RLS policies filter every query, including hand-written SQL that forgets `tenant_id`.

## Why
- Two layers means a single coding mistake is not a breach. Layer 1 will eventually be
  forgotten somewhere; layer 2 catches it.
- One schema means one Flyway migration run, not N.
- Cheap: one database, one connection pool.
- Straightforward to reason about and to test.

## Alternatives considered
| Option | Rejected because |
|---|---|
| Schema per tenant | Every migration runs N times; connection pooling gets complex; ~20× the operational pain at 100 customers |
| Database per tenant | Same problems, worse. Only justified by hard data-residency or contractual isolation requirements we don't have. |
| `tenant_id` with application enforcement only | One forgotten `WHERE` clause is a data breach. Not acceptable for conversation data. |
| Discriminator via Hibernate multi-tenancy | Still application-only; RLS is stronger and simpler |

## Consequences
**Positive:** strong isolation, cheap, single migration path, testable.
**Negative:**
- Every new table needs both a `tenant_id` column and an RLS policy. Easy to forget →
  mitigated with a `.claude/rules/migrations.md` path-scoped rule and a review checklist.
- RLS adds a small query-planning overhead. Irrelevant at our scale.
- The app must never connect as a superuser — including in tests, or the isolation tests
  pass for the wrong reason.

## Mandatory tests (increment F02)
1. Tenant A's repository query cannot return Tenant B's rows
2. A raw query **omitting** `tenant_id` still cannot cross tenants — proves RLS works
3. Reading `TenantContext` when unset **throws** rather than defaulting to anything
4. Disabling RLS makes test 2 **fail** — proves test 2 exercises RLS rather than passing
   because layer 1 happened to filter

Test 4 is not optional. An isolation test that passes when protection is off is worthless.

## When we would revisit
An enterprise customer with a contractual single-tenant requirement, or an Indian data
residency mandate we can't meet otherwise. Not before then.
