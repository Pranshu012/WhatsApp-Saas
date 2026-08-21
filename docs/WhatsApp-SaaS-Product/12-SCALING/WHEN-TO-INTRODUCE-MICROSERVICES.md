# When to Introduce Microservices

**Short answer: when separate teams own separate services. Which means: not while you're
one to eight people.**

Full rationale in `../13-DECISIONS/ADR-001-MODULAR-MONOLITH.md`.

## What you have instead

A **modular monolith**: one deployable artifact, strict internal module boundaries.

```text
src/main/java/com/example/wasaas/
├── tenant/       ├── whatsapp/     ├── job/        ├── ledger/
├── user/         ├── messaging/    ├── automation/ ├── inbox/
├── auth/         ├── template/     ├── faq/        ├── billing/
```

Rules that keep it modular:
- Cross-module calls go through a **public service interface** or a Spring event
- Never reach into another module's repository or entity directly
- Enforced by **ArchUnit tests**, not by discipline alone

Those ArchUnit tests are what make this a real architecture rather than a folder convention. They
also mean that *if* you ever split a module out, the seams already exist.

## Microservices' real trade

Microservices solve an **organisational** problem: independent teams shipping independently
without coordinating deploys.

They cost you, technically:

| Cost | What it means for you |
|---|---|
| Distributed transactions | Your ledger-before-send guarantee becomes a saga |
| Network failure between services | Every internal call can now fail, timeout, or partially succeed |
| Distributed tracing | Needed to debug anything |
| Service discovery, mesh, gateways | New infrastructure to run |
| N deployment pipelines | N× the CI/CD to maintain |
| Eventual consistency | Bugs that only appear under load, in production |
| Local development | Run 6 services to test one change |

You have **zero** of the organisational problem and would take **all** of the technical cost.

## The genuine trigger

All three, simultaneously:

1. **Separate teams.** Not "we might hire" — actual teams with their own roadmaps who are being
   slowed down by coordinating on one deploy pipeline.
2. **A module with genuinely different scaling needs.** Not "the webhook receiver feels busy" —
   measured evidence that one module needs 10× the instances of the rest.
3. **A module with a genuinely different technology need.** E.g. real-time media processing that
   needs a different runtime.

Point 1 is the one that matters. Points 2 and 3 alone are solved by deploying the monolith more
times, or by a Spring profile.

**Realistic timeline: 15+ engineers.** Which is a long way from here.

## Scaling without splitting — what you already do

You are *already* running "two services" in the way that matters:

```text
wasaas-web.service     SPRING_PROFILES_ACTIVE=prod,web     (HTTP, no polling)
wasaas-worker.service  SPRING_PROFILES_ACTIVE=prod,worker  (polling, no HTTP)
```

Same JAR, different profile, separate processes, independent failure. A stuck job can't block a
webhook ACK. That's most of the operational benefit of separate services with none of the
distributed-systems cost.

At Stage 3 you deploy this same JAR to 2 app instances and 2 workers behind a load balancer. Still
one artifact. Still one pipeline. Still one place to debug.

## If you do split, split here first

The natural seams, in order of least-bad:

**1. Webhook receiver.** Genuinely independent: verify signature → persist → enqueue → 200. No
business logic, no shared transaction with anything. If any module can be extracted safely, this
is it.

**2. Worker pool.** Already a separate process. Extracting it as a service is mostly renaming.

**3. Frontend BFF.** Only if you add mobile apps with different data shapes.

**Never split:** tenant, ledger, and messaging. They share transactional boundaries you depend
on — the ledger-before-send ordering that stops duplicate charges to your customer. Splitting
those turns a database guarantee into a distributed-systems problem.

## Warning signs you're being talked into it

| Argument | Response |
|---|---|
| "It's more scalable" | Deploy the monolith N times. Same result, no distributed tax. |
| "Monoliths don't scale" | Yours handles ~1,000 customers on one 2 OCPU box. Check the load math. |
| "It's the modern way" | Modern is matching architecture to constraints. Yours are one developer and ₹100/month. |
| "Independent deploys" | You are one person. You have never needed to deploy two things at once. |
| "Different languages per service" | You are one Java developer. |
| "That's how [big company] does it" | They have 500 engineers and an organisational coordination problem. |

## Checklist before splitting anything

- [ ] Separate teams exist, with separate roadmaps
- [ ] Coordination on one pipeline is measurably slowing delivery
- [ ] Measured evidence of divergent scaling needs
- [ ] Module boundaries already clean (ArchUnit tests passing)
- [ ] Distributed tracing in place **before** the split
- [ ] The split preserves every transactional guarantee — written down, specifically
- [ ] An ADR explaining the decision and what you measured

If you can't tick box 1, stop.

## The honest summary

Your modular monolith with clean boundaries and ArchUnit enforcement is not a compromise or a
stepping stone. For a solo founder serving Indian SMBs at ₹1,999/month, it is the **correct**
architecture — and it would remain correct at 100× your target scale.
