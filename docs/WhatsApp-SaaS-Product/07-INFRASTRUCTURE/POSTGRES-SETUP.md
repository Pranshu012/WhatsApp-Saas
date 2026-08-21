# PostgreSQL Setup

Self-hosted PostgreSQL 17 on the same VM. Rationale in `../13-DECISIONS/ADR-006`; the short
version is that every free managed tier either suspends on idle (fatal for webhooks) or caps
compute-hours below what an always-on receiver needs (Neon: 100 CU-hours/month vs ~182 needed).

## Install

```bash
sudo apt install -y postgresql-17 postgresql-contrib-17
sudo systemctl enable --now postgresql
psql --version
```

If 17 isn't in Ubuntu 24.04's default repos, add the PGDG repository. Extensions needed:
`pgcrypto`, `pg_trgm` (both in `postgresql-contrib`).

## ⚠️ The single most important step: a non-superuser application role

**Superusers bypass Row-Level Security.** If your application connects as `postgres`, every
RLS policy you write is silently inert and your multi-tenancy has exactly one layer of
defence — the one that a single forgotten `WHERE tenant_id` clause defeats.

```sql
CREATE DATABASE wasaas;
CREATE ROLE wasaas_app WITH LOGIN PASSWORD '<strong-random>' NOSUPERUSER NOCREATEDB NOCREATEROLE;

\c wasaas
REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO wasaas_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO wasaas_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO wasaas_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO wasaas_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT USAGE, SELECT ON SEQUENCES TO wasaas_app;

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
```

**Migrations** may run as a higher-privileged role (`wasaas_migrator`) since Flyway needs DDL.
The **application** must never connect as that role.

Verify:
```sql
SELECT rolsuper FROM pg_roles WHERE rolname = 'wasaas_app';  -- must be false
```

This same trap catches you in tests: **Testcontainers defaults to a superuser**, so your RLS
tests will pass with RLS effectively disabled. See `../09-TESTING/MULTI-TENANCY-TESTING.md`.

## Network binding

`/etc/postgresql/17/main/postgresql.conf`:
```conf
listen_addresses = 'localhost'
```

`pg_hba.conf` — local connections with `scram-sha-256`, nothing else:
```conf
local   all   all                    peer
host    all   all   127.0.0.1/32     scram-sha-256
host    all   all   ::1/128          scram-sha-256
```

Port 5432 stays closed in both the Oracle Security List and UFW. The app is on the same box;
there is no reason for Postgres to be reachable from the internet, ever.

## Tuning for 2 OCPU / 12 GB (shared with the JVM)

The JVM and Postgres share 12 GB. Don't let Postgres assume it owns the machine.

```conf
# Memory — assume ~4 GB for Postgres, leaving room for two JVMs and the OS
shared_buffers = 2GB                  # ~25% of a 8GB budget for PG+cache
effective_cache_size = 6GB            # planner hint about OS cache; not an allocation
work_mem = 16MB                       # PER SORT PER CONNECTION — small on purpose
maintenance_work_mem = 512MB

# Connections — small, because you use a pool
max_connections = 60                  # web pool 20 + worker pool 10 + headroom

# Checkpoints — spread writes out
wal_buffers = 16MB
checkpoint_completion_target = 0.9
max_wal_size = 2GB
min_wal_size = 512MB

# SSD
random_page_cost = 1.1
effective_io_concurrency = 200

# Parallelism — only 2 cores; parallel workers mostly hurt here
max_worker_processes = 2
max_parallel_workers = 2
max_parallel_workers_per_gather = 1

# Observability
log_min_duration_statement = 500      # log slow queries
log_checkpoints = on
log_lock_waits = on
shared_preload_libraries = 'pg_stat_statements'

# Point-in-time recovery — required for BACKUP-SETUP.md
wal_level = replica
archive_mode = on
archive_command = '/opt/wasaas/bin/archive-wal.sh %p %f'
```

`work_mem` is per sort operation per connection — 60 connections each doing two sorts at 16 MB
is already 1.9 GB. This is the setting that OOMs small boxes.

## HikariCP on the application side

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20        # web profile; 10 for worker
      minimum-idle: 5
      connection-timeout: 10000
      max-lifetime: 1800000
```

Web pool + worker pool must stay comfortably under `max_connections`.

## Row-Level Security pattern

Every tenant-scoped table, without exception:

```sql
ALTER TABLE contacts ENABLE ROW LEVEL SECURITY;
ALTER TABLE contacts FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON contacts
  USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
```

`FORCE` matters: without it, the table owner bypasses the policy.

The application sets `app.tenant_id` per transaction from `TenantContext`. Per transaction, not
per connection — pooled connections are reused across tenants, and a leaked setting is a
cross-tenant read.

Full rules and the three documented exceptions (`tenants`, `users`, `webhook_events`) are in
`../04-DATABASE/MULTI-TENANT-DATABASE-RULES.md`.

## Definition of Done

- [ ] PostgreSQL 17 running, `pgcrypto` and `pg_trgm` installed
- [ ] `wasaas_app` exists and `rolsuper = false`
- [ ] App connects as `wasaas_app`; migrations as a separate role
- [ ] `listen_addresses = localhost`; port 5432 closed at both firewalls
- [ ] Tuning applied and the service restarted
- [ ] `wal_level = replica` and archiving configured
- [ ] `pg_stat_statements` available
- [ ] The app password is in the root-owned `0600` env file and nowhere else
