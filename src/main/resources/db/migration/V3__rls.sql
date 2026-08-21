-- F02: Row Level Security Foundation

-- Create the application role for connecting to the database.
-- It MUST NOT be a superuser or bypass RLS.
-- IF NOT EXISTS is not standard for CREATE ROLE in older postgres, but we use a DO block to be safe.
DO
$do$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE  rolname = 'wasaas_app') THEN
      BEGIN
         CREATE ROLE wasaas_app WITH LOGIN PASSWORD 'wasaas_app_password' NOSUPERUSER NOBYPASSRLS;
      EXCEPTION WHEN insufficient_privilege THEN
         NULL;
      END;
   END IF;
END
$do$;

-- Grant necessary privileges on existing tables to wasaas_app if it exists
DO
$do$
BEGIN
   IF EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE  rolname = 'wasaas_app') THEN
      GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO wasaas_app;
      GRANT SELECT, INSERT, UPDATE, DELETE ON tenants TO wasaas_app;
      GRANT SELECT, INSERT, UPDATE, DELETE ON users TO wasaas_app;
      GRANT SELECT, INSERT, UPDATE, DELETE ON tenant_users TO wasaas_app;
      GRANT SELECT ON flyway_schema_history TO wasaas_app;
   END IF;
END
$do$;

-- Enable RLS on tenant_users
ALTER TABLE tenant_users ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_users FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_users_tenant_isolation ON tenant_users
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);
