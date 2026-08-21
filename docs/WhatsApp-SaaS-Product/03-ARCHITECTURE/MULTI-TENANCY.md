# Multi-Tenancy

**Classification: BUILD NOW (increment F02)** — and the single most important thing to get right.

See `13-DECISIONS/ADR-004-MULTI-TENANCY.md` for why this design.

---

## The problem

```text
Business A (a dental clinic)      ─┐
Business B (a boutique)            ├──► ONE application, ONE database
Business C (a coaching centre)    ─┘
```

Each tenant's data includes conversations between them and *their* customers. If Business A
can see Business B's conversations, the product is over. Not damaged — over.

So we use **two independent layers**. Layer 1 will eventually be forgotten somewhere; layer 2
catches it.

---

## Layer 1 — Application

### `tenant_id` on every table

Only `tenants` and `users` lack it (a user may belong to multiple tenants via `tenant_users`).
**Everything else** has `tenant_id UUID NOT NULL`.

### `TenantContext`

Request-scoped holder of the current tenant. Critically: **it throws when read unset.**

```java
public final class TenantContext {
    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    public static void set(UUID tenantId) { CURRENT.set(tenantId); }
    public static void clear() { CURRENT.remove(); }

    public static UUID require() {
        UUID id = CURRENT.get();
        if (id == null) {
            throw new IllegalStateException("No tenant in context");
        }
        return id;
    }
}
```

Never `getOrNull()`. Never a default. If the tenant is unknown, the request must fail — a
query that silently runs without a tenant filter is the bug we are preventing.

### `TenantContextFilter`

Populates the context from the **authenticated session principal** — never from a client-
supplied header in production. (F02 temporarily uses a header because auth lands in F03; F03
must remove that path entirely.)

```java
// after authentication
TenantContext.set(principal.tenantId());
try {
    chain.doFilter(req, res);
} finally {
    TenantContext.clear();   // MUST be in finally — thread pools are reused
}
```

Forgetting `clear()` in a `finally` block leaks a tenant id to the next request on that
thread. That is a cross-tenant leak caused by a missing `finally`.

---

## Layer 2 — PostgreSQL Row-Level Security

RLS filters at the database, so even hand-written SQL that forgets `tenant_id` is safe.

```sql
-- The application role MUST NOT be a superuser. Superusers bypass RLS entirely.
CREATE ROLE wasaas_app WITH LOGIN PASSWORD '...' NOSUPERUSER NOBYPASSRLS;

ALTER TABLE contacts ENABLE ROW LEVEL SECURITY;
ALTER TABLE contacts FORCE ROW LEVEL SECURITY;   -- applies even to the table owner

CREATE POLICY contacts_tenant_isolation ON contacts
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid)
    WITH CHECK (tenant_id = current_setting('app.tenant_id', true)::uuid);
```

- `USING` filters reads (SELECT/UPDATE/DELETE visibility)
- `WITH CHECK` blocks writes with the wrong `tenant_id` — without it, tenant A could *insert*
  a row belonging to tenant B
- `FORCE ROW LEVEL SECURITY` makes the policy apply to the table owner too

### Setting `app.tenant_id` per transaction

```java
@Component
public class TenantAwareDataSourceHook {
    // Executed at the start of each transaction, before any query
    void applyTenant(Connection conn) throws SQLException {
        try (PreparedStatement ps =
                 conn.prepareStatement("SELECT set_config('app.tenant_id', ?, true)")) {
            ps.setString(1, TenantContext.require().toString());
            ps.execute();
        }
    }
}
```

`set_config(..., true)` makes it **transaction-local**, so a pooled connection returned to the
pool carries no leftover tenant. Using `false` (session-local) is a leak waiting to happen.

---

## Good and bad queries

**Bad — relies on the developer remembering:**
```java
@Query("SELECT c FROM Contact c WHERE c.phoneE164 = :phone")
Optional<Contact> findByPhone(String phone);      // ← no tenant filter
```
Layer 2 saves you. But don't rely on it.

**Good — tenant is structural:**
```java
@Query("SELECT c FROM Contact c WHERE c.tenantId = :#{T(...TenantContext).require()} " +
       "AND c.phoneE164 = :phone")
Optional<Contact> findByPhone(String phone);
```

**Better — a base repository or Hibernate `@Filter` so it cannot be forgotten.** Pick one
approach and apply it everywhere; the worst outcome is three different patterns in one codebase.

**Bad — trusting client input:**
```java
@GetMapping("/api/contacts")
List<Contact> list(@RequestParam UUID tenantId) { ... }   // ← never
```
Tenant comes from the session. Always.

**Bad — a cross-tenant join that looks innocent:**
```sql
SELECT c.*, t.business_name
FROM contacts c
JOIN tenants t ON t.id = c.tenant_id;   -- tenants has no RLS; fine here,
                                        -- but audit every join against an un-RLS'd table
```

---

## Adding a new table — the checklist

Every migration, every time:

- [ ] `tenant_id UUID NOT NULL` column
- [ ] Foreign key to `tenants(id)`
- [ ] Index on `(tenant_id, <primary lookup column>)`
- [ ] `ENABLE ROW LEVEL SECURITY` + `FORCE ROW LEVEL SECURITY`
- [ ] Policy with **both** `USING` and `WITH CHECK`
- [ ] `GRANT` to the app role
- [ ] An isolation test for the new table

This checklist lives in `.claude/rules/migrations.md` so Claude Code sees it automatically
when touching migration files.

---

## Testing isolation — four mandatory tests

See `09-TESTING/MULTI-TENANCY-TESTING.md` for the code.

1. Tenant A's repository query cannot return Tenant B's rows
2. A raw query **omitting** `tenant_id` still cannot cross tenants — **proves RLS works**
3. `TenantContext.require()` throws when unset
4. **Disabling RLS makes test 2 fail** — proves test 2 actually exercises RLS

**Test 4 is not optional.** If test 2 passes with RLS off, it was passing because layer 1
filtered, and you have no evidence layer 2 works at all.

Also: tests must connect as the **non-superuser** app role. Testcontainers defaults to a
superuser, which silently bypasses RLS and makes every isolation test meaningless.

---

## What we are NOT doing

**DO NOT BUILD YET:** schema-per-tenant · database-per-tenant · tenant-specific deployments ·
cross-tenant analytics · tenant data export to separate stores.

Revisit only for a contractual single-tenant requirement or a data-residency mandate.
