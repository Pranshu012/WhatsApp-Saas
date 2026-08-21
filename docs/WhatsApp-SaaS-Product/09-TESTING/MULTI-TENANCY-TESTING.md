# Multi-Tenancy Testing

**The most important document in this folder.** A cross-tenant leak in a 20-customer B2B SaaS
where customers know each other is not a bug you recover from.

## Two layers, tested independently

| Layer | Mechanism | Fails when |
|---|---|---|
| 1 — Application | `TenantContext` + repository enforcement | A developer writes a raw query |
| 2 — Database | PostgreSQL Row-Level Security | Only if the app connects as a superuser |

Layer 2 exists precisely because layer 1 depends on human discipline. Test both, separately.

## ⚠️ The trap that makes all of this worthless

**Superusers bypass RLS. Testcontainers' default user is a superuser.**

If your tests connect as the container's default user, every RLS test passes — and would pass
with the policies deleted. You'd ship believing you have defence in depth and have one layer.

`test-init.sql` must create a `NOSUPERUSER` role and the datasource must use it:

```sql
CREATE ROLE wasaas_app WITH LOGIN PASSWORD 'test' NOSUPERUSER NOCREATEDB NOCREATEROLE;
GRANT USAGE ON SCHEMA public TO wasaas_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO wasaas_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO wasaas_app;
```

Guard it with a test:

```java
@Test
void applicationRoleIsNotSuperuser() {
    Boolean isSuper = jdbc.queryForObject(
        "SELECT rolsuper FROM pg_roles WHERE rolname = current_user", Boolean.class);
    assertThat(isSuper)
        .as("app role must not be superuser — superusers bypass RLS, making every "
          + "isolation test in this suite meaningless")
        .isFalse();
}
```

## The four mandatory tests (increment F02)

### Test 1 — Repository queries are tenant-scoped

```java
@Test
void repositoryCannotReadAnotherTenantsRows() {
    var a = createTenant("A");  var b = createTenant("B");
    withTenant(a, () -> contactRepository.save(contact("+919000000001")));
    withTenant(b, () -> contactRepository.save(contact("+919000000002")));

    withTenant(a, () -> assertThat(contactRepository.findAll())
        .extracting(Contact::getPhoneE164)
        .containsExactly("+919000000001"));
}
```

### Test 2 — RLS blocks a raw query that omits `tenant_id`

This is the one that matters. It simulates a developer forgetting the `WHERE` clause.

```java
@Test
void rlsBlocksRawQueryWithoutTenantFilter() {
    var a = createTenant("A");  var b = createTenant("B");
    withTenant(a, () -> contactRepository.save(contact("+919000000001")));
    withTenant(b, () -> contactRepository.save(contact("+919000000002")));

    withTenant(a, () -> {
        // deliberately no tenant_id predicate — RLS must still filter
        var rows = jdbc.queryForList("SELECT phone_e164 FROM contacts");
        assertThat(rows)
            .as("RLS must filter even when the query author forgot to")
            .hasSize(1);
    });
}
```

### Test 3 — `TenantContext` fails loudly when unset

```java
@Test
void tenantContextThrowsWhenUnset() {
    TenantContext.clear();
    assertThatThrownBy(() -> TenantContext.require())
        .isInstanceOf(IllegalStateException.class);
}
```

**Never let it return `null` or a default.** A null tenant id that reaches a query is either an
exception or a full-table read — and the second one is a breach.

### Test 4 — The meta-test: disabling RLS must make Test 2 fail

```java
@Test
void isolationTestActuallyDependsOnRls() {
    admin.execute("ALTER TABLE contacts DISABLE ROW LEVEL SECURITY");
    try {
        var a = createTenant("A");  var b = createTenant("B");
        withTenant(a, () -> contactRepository.save(contact("+919000000001")));
        withTenant(b, () -> contactRepository.save(contact("+919000000002")));

        withTenant(a, () -> {
            var rows = jdbc.queryForList("SELECT phone_e164 FROM contacts");
            assertThat(rows)
                .as("with RLS off both rows must be visible — otherwise Test 2 "
                  + "passes for the wrong reason and proves nothing")
                .hasSize(2);
        });
    } finally {
        admin.execute("ALTER TABLE contacts ENABLE ROW LEVEL SECURITY");
        admin.execute("ALTER TABLE contacts FORCE ROW LEVEL SECURITY");
    }
}
```

**Why bother:** without this, a mistake in `withTenant` (say, it filters in Java) would make
Test 2 pass while RLS is entirely absent. Test 4 is what proves your safety net is load-bearing.

## `FORCE ROW LEVEL SECURITY`

`ENABLE` alone does not apply policies to the **table owner**. Always add:

```sql
ALTER TABLE contacts ENABLE ROW LEVEL SECURITY;
ALTER TABLE contacts FORCE ROW LEVEL SECURITY;
```

Test that the owner is also filtered.

## Per-transaction, not per-connection

`app.tenant_id` must be set **per transaction**. Connections are pooled and reused across
tenants; a setting that survives into the next borrower is a cross-tenant read.

```java
@Test
void tenantSettingDoesNotLeakBetweenPooledConnections() {
    withTenant(tenantA, () -> contactRepository.count());
    // force reuse of the same physical connection
    var leaked = jdbc.queryForObject("SELECT current_setting('app.tenant_id', true)", String.class);
    assertThat(leaked).isNullOrEmpty();
}
```

## Every new table — the checklist

Run this for **every** migration that adds a table. Add it to your increment DoD.

- [ ] `tenant_id uuid NOT NULL` present
- [ ] Foreign key to `tenants`
- [ ] Index leading with `tenant_id`
- [ ] `ENABLE ROW LEVEL SECURITY`
- [ ] `FORCE ROW LEVEL SECURITY`
- [ ] Policy using `current_setting('app.tenant_id', true)::uuid`
- [ ] Test 1 written for the new table
- [ ] Test 2 written for the new table
- [ ] Test 4 verified once by hand

## The three documented exceptions

Only these tables legitimately lack `tenant_id`:

| Table | Why | Guard |
|---|---|---|
| `tenants` | It *is* the tenant | Row access via membership only |
| `users` | A user could belong to multiple tenants | Access via `tenant_users` |
| `webhook_events` | Arrives before the tenant is resolved; the worker resolves it | **Never exposed via any tenant-facing API** |

`jobs.tenant_id` is nullable for system-wide jobs (template sync, scheduler sweeps). Every
tenant-scoped job must set it.

Any *other* table without `tenant_id` is a bug. Enforce it with a test that reads
`information_schema` and asserts the allowlist:

```java
@Test
void everyTableHasTenantIdExceptDocumentedExceptions() {
    var allowed = Set.of("tenants", "users", "webhook_events",
                         "flyway_schema_history", "spring_session", "spring_session_attributes",
                         "whatsapp_rates");
    var missing = jdbc.queryForList("""
        SELECT t.table_name FROM information_schema.tables t
        WHERE t.table_schema='public' AND t.table_type='BASE TABLE'
          AND NOT EXISTS (SELECT 1 FROM information_schema.columns c
                          WHERE c.table_name=t.table_name AND c.column_name='tenant_id')
        """, String.class);
    assertThat(missing).isSubsetOf(allowed);
}
```

This test catches the mistake automatically, forever, including when you're tired at 1am. It is
worth more than any amount of documentation telling you to remember.
