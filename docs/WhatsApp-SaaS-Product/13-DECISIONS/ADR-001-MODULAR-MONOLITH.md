# ADR-001 — Modular Monolith

**Status:** Accepted · 18 August 2026

## Context
Solo developer, Java/Spring Boot background, targeting 10–20 paying customers before
spending meaningful money. Load projection: 20 customers × 3,000 messages/month ≈ 1.4
messages per minute average. Even at 1,000 customers it is ~70/minute.

## Decision
One Spring Boot application, one deployable JAR, organised into feature modules
(`tenant`, `whatsapp`, `job`, `ledger`, `automation`, …) with enforced boundaries.
Cross-module communication only via public service interfaces or Spring events.

The same JAR runs in two modes via Spring profile: `web` (HTTP) and `worker` (job polling).

## Why
- One developer cannot operate a distributed system and also sell to customers.
- Local debugging is a single process and a single stack trace.
- Deployment is `scp` a JAR and restart.
- Transactional consistency comes free — no distributed transactions, no sagas.
- The web/worker profile split gives independent scaling of the message pipeline without
  splitting the codebase, the database, or the deployment story.

## Alternatives considered
| Option | Rejected because |
|---|---|
| Microservices | Network boundaries, distributed tracing, and 5 deployment pipelines for a workload one JVM handles idle |
| Serverless (Lambda/Cloud Run) | Cold starts on webhooks (Meta retries on slow ACK), JVM startup cost, harder local dev |
| Single-file monolith, no modules | Boundaries never get added later; "monolith → services" becomes impossible rather than merely unnecessary |

## Consequences
**Positive:** fast iteration, trivial ops, cheap hosting, one place to look.
**Negative:** the whole app restarts on deploy (~30s downtime — acceptable at 20 customers);
module discipline is a convention Claude Code and you must actively maintain.

**Mitigation:** consider Spring Modulith to *enforce* boundaries in tests rather than
relying on discipline.

## When we would revisit
- A single module needs genuinely independent scaling that the worker profile can't provide
- Team grows past ~4 engineers wanting independent deploy cadence
- One module has a wildly different availability requirement

None of these is likely before 1,000 customers.
