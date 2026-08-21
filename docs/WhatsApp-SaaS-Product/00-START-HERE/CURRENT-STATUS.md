# Current Status

> Update this at the end of **every** working session. It is the first thing you read
> after a break. Keep it honest — a stale status file is worse than none.

**Last updated:** 21 August 2026

---

```text
Current Phase:        PHASE 2 — Local development setup

Current Goal:         Complete Docker-backed F00 runtime verification using Java 21.

Last Completed:       F00 source skeleton and unit build (`./mvnw clean verify`) pass.

Currently Working On: F00 — local Docker/PostgreSQL runtime verification

Next Task:            Finish Java 21 and Docker Desktop installation, start PostgreSQL,
                      then verify Flyway V1 and `/actuator/health`.

Blocked By:           Java 21 installation requires an interactive macOS administrator-password
                      prompt. Docker Desktop has not yet been installed. Java 26 can compile F00
                      but does not satisfy the project requirement.

Important Decisions:  - Tech Provider model, customer pays Meta directly (ADR-003, ADR-005)
                      - Modular monolith, single VM (ADR-001)
                      - Postgres job queue, no Redis/Kafka (ADR-002)
                      - Row-level multi-tenancy with tenant_id + RLS (ADR-004)
                      - Self-hosted Postgres on app VM initially (ADR-006)
                      - No AI in core automation path (ADR-007)

Open Questions:       See 13-DECISIONS/DECISIONS.md — 9 items currently open,
                      of which D-01 (core feature scope) blocks Phase 7.
```

---

## Session log

Append a line each session. Newest at top.

| Date | Phase | What I did | Next |
|---|---|---|---|
| 2026-08-21 | F00 | Retried Temurin 21 installation; macOS requires interactive sudo authentication | Install Java 21 and Docker Desktop, then verify F00 runtime |
| 2026-08-21 | F00 | Added Spring Boot skeleton, local Postgres compose config, Flyway baseline, error/logging contracts; unit build passes on Java 26 | Install Java 21 + Docker, verify full runtime |
| 2026-08-21 | Setup | Created local repository, copied docs, added cross-agent guidance | Install Java 21 and Docker, then F00 |
| 2026-08-18 | — | Created documentation workspace | Start Phase 0.1 (domain) |

---

## Quick health check

Run through this if you've been away more than a week:

- [ ] Is the Oracle instance still running? (Oracle reclaims idle Always Free instances)
- [ ] Did last night's backup complete? Check the B2 bucket.
- [ ] Any Sentry errors accumulating?
- [ ] Any Meta developer-account emails about policy or pricing changes?
- [ ] Has anything in `00-START-HERE/ASSUMPTIONS-AND-EXPIRY-DATES.md` passed its re-verify date?
