# F02 - Tenant isolation and row-level security

## Goal
Implement two layers of defence to ensure tenant isolation: application layer filtering and database row-level security (RLS).

## Open Questions
None.

## Proposed Changes
1. **Tenant Context**:
   - `TenantContext`: `ThreadLocal` backed holder for current tenant ID. Throws `IllegalStateException` if accessed when unset.
   - `TenantContextFilter`: A servlet filter that reads `X-Tenant-Id` header and sets it in `TenantContext`. (Temporary for F02).
   
2. **Layer 1 (Application)**:
   - Use Hibernate `@Filter` mechanism via a `@MappedSuperclass` `BaseTenantEntity`. This ensures all tenant-scoped entities will have the filter applied.
   - To enable the filter automatically, I will implement a Hibernate `SessionEventListener` or interceptor. However, to keep it "simplest approach that can't be forgotten", an AOP Aspect on `@Repository` methods (or wrapping the `EntityManager`) can enable the filter. But actually, the most foolproof Layer 1 is just appending `tenantId` in the query methods. But to automate it, a custom `JpaRepository` implementation that overrides `findAll` etc. or a Hibernate `@Filter` enabled via an AOP aspect on `Repository` execution is best.
   - I will use the Hibernate `@Filter` approach and enable it via an AOP aspect intercepting `Repository` methods. This guarantees it's applied on all Spring Data JPA queries.

3. **Layer 2 (Database RLS)**:
   - Create `V3__rls.sql`.
   - Create a `wasaas_app` role (`NOSUPERUSER NOBYPASSRLS`).
   - Enable RLS on `tenant_users`. Create policy `USING` and `WITH CHECK` on `app.tenant_id`.
   - Grant permissions to `wasaas_app`.
   - Implement `TenantAwareDataSource` (wrapping `DataSource` using a `BeanPostProcessor`) to execute `SELECT set_config('app.tenant_id', ?, true)` on `getConnection()`, conditioned on `TenantContext` being set.
   - Update `application.yml` to connect as `wasaas_app`. (Wait, if Flyway connects as `wasaas_app`, it can't create roles! Flyway needs to connect as `wasaas_user` (superuser), but the app needs to connect as `wasaas_app`. So we will separate `spring.datasource.username` and `spring.flyway.user`).

4. **Tests**:
   - Create `TenantIsolationTest` with Testcontainers.
   - The test will insert data for two tenants.
   - Test 1: Repository query for Tenant A doesn't see Tenant B.
   - Test 2: Raw JDBC query (e.g., `SELECT * FROM tenant_users`) only sees Tenant A's rows.
   - Test 3: `TenantContext.require()` throws if unset.
   - Test 4: Disable RLS temporarily (by running as superuser or altering table) to prove Test 2 fails.

## Verification Plan
- Run `./mvnw clean verify`. All 4 tests must pass.
