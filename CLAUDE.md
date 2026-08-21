# WhatsApp Automation SaaS — Project Instructions

## Product and stack

Multi-tenant WhatsApp automation SaaS for Indian SMBs. We operate as a Meta Tech Provider: each customer owns its WhatsApp Business Account and pays Meta directly. The stack is Java 21, Spring Boot 3.x, PostgreSQL 17, Flyway, Maven, and React + Vite. Build a single modular monolith on one VM with self-hosted Postgres.

## Read before designing

- Overall architecture: `docs/WhatsApp-SaaS-Product/03-ARCHITECTURE/SYSTEM-ARCHITECTURE.md`
- Module boundaries: `docs/WhatsApp-SaaS-Product/05-BACKEND/MODULES.md`
- Tenancy: `docs/WhatsApp-SaaS-Product/03-ARCHITECTURE/MULTI-TENANCY.md`
- Jobs: `docs/WhatsApp-SaaS-Product/03-ARCHITECTURE/BACKGROUND-JOBS.md`
- Webhooks: `docs/WhatsApp-SaaS-Product/05-BACKEND/WEBHOOK-IMPLEMENTATION.md`
- Ledger: `docs/WhatsApp-SaaS-Product/03-ARCHITECTURE/MESSAGE-LEDGER.md`
- Security: `docs/WhatsApp-SaaS-Product/03-ARCHITECTURE/SECURITY.md`
- ADRs: `docs/WhatsApp-SaaS-Product/13-DECISIONS/`

If documentation conflicts with a request, identify the conflict rather than silently choosing.

## Non-negotiable rules

1. Every tenant-scoped table has `NOT NULL tenant_id` and its own Row-Level Security policy.
2. Webhook handlers only verify signature, persist raw event, enqueue a job, and return 200 in under two seconds.
3. Never call the WhatsApp API synchronously from HTTP; all outbound sends use the `jobs` table.
4. Encrypt WhatsApp access tokens at rest. Never log or return them.
5. Every outbound send needs an idempotency key and writes `message_ledger` before the API call.
6. `message_ledger` is append-only; statuses are separate rows.
7. Make schema changes exclusively in new Flyway migrations. Hibernate DDL is always `validate`.
8. Keep the application stateless; sessions live in Postgres via Spring Session JDBC.
9. Do not add Redis, Kafka, RabbitMQ, Kubernetes, microservices, service mesh, Elasticsearch, or a second database without explicit approval. Use Postgres `FOR UPDATE SKIP LOCKED` for the queue.
10. No AI/LLM calls on the core automation path. Use deterministic rules and Postgres full-text/`pg_trgm` matching.
11. Money and Meta rates belong in configuration or the database, never code constants.
12. Store timestamps as UTC `timestamptz`; render IST only in the frontend.

## Documentation is required

Every increment updates `docs/IMPLEMENTATION/`: one `F##-<slug>.md` file and `INDEX.md`, using `docs/WhatsApp-SaaS-Product/14-CLAUDE-CODE/IMPLEMENTATION-DOC-TEMPLATE.md`. Record needed business-rule assumptions in `docs/WhatsApp-SaaS-Product/13-DECISIONS/DECISIONS.md`.

## Code conventions

- Package by feature, not layer.
- Cross-module calls use a public service interface or Spring event; never another module's repository/entity directly.
- Constructor injection only; no field `@Autowired`.
- Controllers accept/return DTO records; never expose JPA entities.
- Put `@Transactional` on services, not controllers or repositories.
- Custom exceptions extend `DomainException`; `GlobalExceptionHandler` returns `ApiError`.
- Integration tests use Testcontainers Postgres, never H2.

## Working agreement

- Plan before a change spanning more than two files, then wait for approval.
- Implement exactly one documented increment at a time; do not pre-build future work.
- Include tests with implementation work, particularly tenancy tests.
- Prefer the smallest PostgreSQL-based solution.
- Do not commit or push unless explicitly asked.

## Never log

Access tokens, passwords or hashes, session IDs, OTPs, encryption keys, full customer message bodies, or end-customer phone numbers beyond their last four digits.
