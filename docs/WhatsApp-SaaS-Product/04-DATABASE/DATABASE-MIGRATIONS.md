# Database Migrations

**Classification: BUILD NOW (from increment F00).** Flyway, from commit #1. Golden Rule 9.

## Setup

```text
src/main/resources/db/migration/
├── V1__baseline.sql
├── V2__tenants_users.sql
├── V3__rls.sql
└── ...
```

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    validate-on-migrate: true
  jpa:
    hibernate:
      ddl-auto: validate      # NEVER update, NEVER create. Flyway owns the schema.
```

`ddl-auto: validate` fails startup if entities and schema disagree. That's the point — it turns
a silent drift into a loud, immediate failure.

## Rules

1. **Never edit an applied migration.** Flyway checksums them; editing breaks every environment
   that already ran it. Always add `V{n+1}__`.
2. **One logical change per migration.** Easier to read, easier to revert.
3. **Every table-creating migration also creates its RLS policy** — in the same file. Splitting
   them is how a table ships unprotected.
4. **Test against a fresh database**, not just an incremental apply. `docker compose down -v`
   then up, and confirm the whole chain applies cleanly. Integration tests with Testcontainers
   do this automatically, which is one more reason not to use H2.
5. **Header comment** naming the increment:
   ```sql
   -- Increment F08 — message ledger (append-only billing evidence)
   -- See docs/.../03-ARCHITECTURE/MESSAGE-LEDGER.md
   ```
6. **No `DROP` on a table with production data** without a written plan. Prefer additive
   changes; deprecate columns before removing them.
7. **Backfills go in their own migration**, separate from the schema change, and must be safe
   to re-run.

## Naming

```text
V{version}__{snake_case_description}.sql
V8__message_ledger.sql
```

Integer versions, no gaps, no timestamps (a solo developer has no merge-conflict problem to
solve, and integers sort readably).

## Adding a column safely

```sql
-- Safe: nullable, or NOT NULL with a default
ALTER TABLE contacts ADD COLUMN notes TEXT;
ALTER TABLE contacts ADD COLUMN opt_in_status TEXT NOT NULL DEFAULT 'UNKNOWN';
```

`NOT NULL` without a default on a populated table fails. On very large tables, adding a
`NOT NULL DEFAULT` used to rewrite the table — Postgres 11+ handles constant defaults without a
rewrite, so this is fine at our scale either way.

## Renaming — don't, yet

At 0–20 customers you can afford a brief maintenance window, so a direct rename is acceptable.
Past that, use expand/contract: add the new column, backfill, dual-write, switch reads, drop the
old one across several releases. **Don't build that machinery until you need it.**

## Indexes on a live table

```sql
-- CREATE INDEX locks writes. CONCURRENTLY does not, but cannot run inside a transaction.
CREATE INDEX CONCURRENTLY idx_example ON example (tenant_id, created_at);
```

Flyway wraps migrations in a transaction by default. For `CONCURRENTLY`, disable it for that
migration (`-- flyway:executeInTransaction=false`) or run it manually during a window. At
current table sizes a plain `CREATE INDEX` is instant — this matters later, not now.

## Verifying a migration

- [ ] Applies to a fresh DB
- [ ] Applies to a copy of production
- [ ] `ddl-auto: validate` passes (entities match)
- [ ] RLS policy created and verified via `pg_policies`
- [ ] Index present and used (check with `EXPLAIN`)
- [ ] `./mvnw clean verify` green

## Rollback

Flyway Community has no automatic undo. That is fine — the discipline is:

1. **Prefer additive changes.** Additions rarely need rollback.
2. If a migration is wrong, write a **new** compensating migration.
3. For a destructive mistake: restore from backup
   (`10-OPERATIONS/BACKUP-RESTORE-PROCEDURE.md`).
4. This is another reason the restore must be tested — it is your rollback plan.

## DO NOT BUILD YET

Blue-green schema migrations · online schema change tooling · a migration approval workflow ·
automated rollback scripts.
