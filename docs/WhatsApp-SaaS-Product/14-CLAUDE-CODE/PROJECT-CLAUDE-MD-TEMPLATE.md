# Project CLAUDE.md Template

Copy everything between the markers into `CLAUDE.md` at your repository root.

Keep it under ~150 lines. Link to `docs/`, don't inline architecture. Update it when a
convention changes — a stale `CLAUDE.md` is worse than none.

---8<--- COPY FROM HERE ---8<---

# WhatsApp Automation SaaS — Project Instructions

## What this is
Multi-tenant WhatsApp automation SaaS for Indian SMBs. We are a Meta **Tech Provider**:
each customer owns their own WhatsApp Business Account (WABA) and pays Meta directly for
messages. We charge only a flat software subscription. Message cost never touches our P&L.

## Stack (do not add to this without asking me)
Java 21 · Spring Boot 3.x · PostgreSQL 17 · Flyway · Maven · React + Vite frontend.
Single modular monolith, single VM, self-hosted Postgres.

## Architecture docs — read before designing anything
- Overall: `docs/WhatsApp-SaaS-Product/03-ARCHITECTURE/SYSTEM-ARCHITECTURE.md`
- Module boundaries: `docs/WhatsApp-SaaS-Product/05-BACKEND/MODULES.md`
- Multi-tenancy: `docs/WhatsApp-SaaS-Product/03-ARCHITECTURE/MULTI-TENANCY.md`
- Jobs/outbox: `docs/WhatsApp-SaaS-Product/03-ARCHITECTURE/BACKGROUND-JOBS.md`
- Webhooks: `docs/WhatsApp-SaaS-Product/05-BACKEND/WEBHOOK-IMPLEMENTATION.md`
- Message ledger: `docs/WhatsApp-SaaS-Product/03-ARCHITECTURE/MESSAGE-LEDGER.md`
- Security: `docs/WhatsApp-SaaS-Product/03-ARCHITECTURE/SECURITY.md`
- Decisions (ADRs): `docs/WhatsApp-SaaS-Product/13-DECISIONS/`

If a doc contradicts what I ask for in chat, **say so** rather than silently picking one.

## Non-negotiable rules

1. **Every table has a `NOT NULL tenant_id`.** No exceptions. Add a Row-Level Security
   policy for every new table. A missing `tenant_id` is a cross-customer data leak.
2. **Never call the WhatsApp API inside a webhook request handler.** Webhooks: verify
   signature → persist raw event → enqueue job → return 200 in under 2 seconds.
3. **Never call the WhatsApp API synchronously from an HTTP request at all.** All outbound
   sends go through the `jobs` table.
4. **WhatsApp access tokens are encrypted at rest.** Never log them, never return them in
   an API response, never include them in an exception message.
5. **Every outbound send needs an idempotency key** and must write a `message_ledger` row
   **before** the API call. Duplicate sends spend the customer's real money.
6. **`message_ledger` is append-only.** Never `UPDATE` a ledger row. Status changes are
   new rows.
7. **Schema changes only via new Flyway migrations.** Never edit an applied migration.
   Never use `spring.jpa.hibernate.ddl-auto` beyond `validate`.
8. **Stateless application.** No in-memory sessions, no local file writes, no static
   mutable state. Sessions live in Postgres via Spring Session JDBC.
9. **No new infrastructure dependencies.** Specifically: no Redis, no Kafka, no RabbitMQ,
   no Kubernetes, no microservices, no service mesh, no Elasticsearch, no second database.
   Postgres + `FOR UPDATE SKIP LOCKED` is our queue. If you think we need one of these,
   stop and explain why — do not add it.
10. **No AI/LLM calls in the core automation path.** Automation is deterministic:
    keyword rules, Postgres full-text + `pg_trgm` FAQ matching, interactive buttons.
11. **Money and rates live in config/DB, never as constants.** Meta changes WhatsApp rates
    quarterly.
12. **Store timestamps in UTC** (`timestamptz`). Render IST in the frontend only.

## Documentation is part of "done"
Every increment MUST update `docs/IMPLEMENTATION/`. A feature without its doc is not finished.

- One file per increment: `docs/IMPLEMENTATION/F##-<slug>.md`
- Update `docs/IMPLEMENTATION/INDEX.md` with a one-line entry
- Follow `docs/WhatsApp-SaaS-Product/14-CLAUDE-CODE/IMPLEMENTATION-DOC-TEMPLATE.md`
- If a change contradicts an architecture doc in `docs/WhatsApp-SaaS-Product/`, update that doc
  too, or tell me why it should stay as it is
- If you had to assume a business rule, add it to `13-DECISIONS/DECISIONS.md`

Write it for me in six months, having forgotten everything: what it does, why it's built this
way, what will break it.

## Commands
```bash
./mvnw clean verify          # build + all tests — must pass before you say "done"
./mvnw spring-boot:run       # run locally (profile: local)
./mvnw test -Dtest=ClassName # single test
docker compose up -d db      # local Postgres only
```

## Code conventions
- Package by feature, not by layer: `tenant/`, `whatsapp/`, `job/`, `ledger/`, `automation/`.
- Cross-module calls go through a public service interface or a Spring event. Never reach
  into another module's repository or entity directly.
- Constructor injection only. No `@Autowired` on fields.
- DTOs (records) at the controller boundary. Never expose JPA entities in API responses.
- `@Transactional` on service methods, not controllers or repositories.
- Custom exceptions extend `DomainException`; `GlobalExceptionHandler` maps them to
  `ApiError`.
- Integration tests use **Testcontainers Postgres**, never H2 (we depend on real Postgres
  behaviour: `SKIP LOCKED`, RLS, full-text search).

## How I want you to work
- **Plan before coding** for anything touching more than 2 files. Show me the plan, wait.
- **One increment at a time.** Don't scaffold future features "while you're in there".
- **Write the tests in the same change** as the code, especially tenant-isolation tests.
- **Prefer the boring solution.** If a plain Postgres query works, don't add a library.
- **Ask instead of assuming** on business rules. Product scope questions belong in
  `docs/WhatsApp-SaaS-Product/13-DECISIONS/DECISIONS.md`, not in a guess.
- **Don't commit.** I review and commit myself.
- Tell me when you think something in my request is a mistake. I'd rather argue than
  rewrite.

## Never log
Access tokens · passwords or hashes · session IDs · OTPs · the token encryption key ·
full message bodies of end-customer conversations (log IDs and metadata instead) ·
end-customer phone numbers in full (mask to last 4 digits).

---8<--- COPY TO HERE ---8<---
