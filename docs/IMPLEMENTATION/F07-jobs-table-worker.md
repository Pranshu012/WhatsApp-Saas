# F07 — Jobs Table and Worker (Durable Job Queue)

## Status
Complete — verified with concurrency tests, exponential backoff, locking recovery, and idempotency tests across 43 passing automated tests.

## Summary
Implemented a durable, PostgreSQL-backed background job queue without external message brokers.
- Designed a `jobs` table to persist job states (PENDING, RUNNING, SUCCEEDED, FAILED, DEAD).
- Implemented `JobService` which allows enqueueing jobs with idempotency keys (`INSERT ... ON CONFLICT DO NOTHING`).
- Used PostgreSQL's `SELECT ... FOR UPDATE SKIP LOCKED` inside `JobService.claimJobs` to claim batches of jobs lock-free across concurrent workers.
- Built a `JobWorker` daemon activated via the `worker` Spring Profile that polls for jobs, sets the correct `TenantContext`, executes `JobHandler` logic, and handles transient (exponential backoff) vs permanent (`PermanentJobException`) failures.
- Added `V8__jobs.sql` migration creating the table and the partial unique index required for idempotency.

## Key Decisions & Gotchas
- **Tenant Context Isolation**: The `jobs` table has a nullable `tenant_id` allowing both tenant-specific jobs (where the worker sets the `TenantContext` before dispatching) and global system jobs.
- **Skip Locked Query**: `SELECT id FROM jobs WHERE ... FOR UPDATE SKIP LOCKED` ensures horizontally scaled workers never wait on each other or process duplicate jobs simultaneously.
- **Timestamp conversion in JdbcTemplate**: Java `Instant` is not natively parsed by the standard PostgreSQL JDBC driver when used inside `JdbcTemplate.update` parameters; wrapping it in `java.sql.Timestamp.from(Instant)` prevents SQL type mapping errors.
- **Idempotency constraints**: When using `ON CONFLICT (idempotency_key) ...`, the `WHERE idempotency_key IS NOT NULL` clause from the partial index must perfectly match in the query, OR you must dynamically omit the `ON CONFLICT` clause when the key is null.

## Key Files
- `V8__jobs.sql`: The schema migration.
- `JobService.java`: Enqueueing and claiming queries.
- `JobWorker.java`: The scheduled daemon.
- `JobWorkerTest.java`: Testing concurrency with real thread pools and database instances.
