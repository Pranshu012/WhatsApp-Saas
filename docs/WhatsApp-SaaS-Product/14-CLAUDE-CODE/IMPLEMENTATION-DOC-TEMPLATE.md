# Implementation Doc Template

Every increment produces one file at `docs/IMPLEMENTATION/F##-<slug>.md` in your **code repo**
(not in this documentation workspace).

## Why this exists

The docs in `WhatsApp-SaaS-Product/` describe what you **intend** to build. `docs/IMPLEMENTATION/`
records what you **actually built** — including the places where reality diverged from the plan.

Three concrete payoffs:

1. **Claude Code reads it.** Future sessions get accurate context about existing code instead of
   re-deriving it (or guessing wrong).
2. **You forget.** In four months you will not remember why `webhook_events` has no `tenant_id`.
3. **Divergence gets caught.** Writing "this differs from the architecture doc because…" forces
   you to notice you've drifted, while it's still cheap to fix.

## The template

Copy this into every increment doc.

```markdown
# F## — <Feature Name>

**Status:** Complete | Partial | Superseded by F##
**Completed:** YYYY-MM-DD
**Commit:** <short sha>
**Spec:** ../WhatsApp-SaaS-Product/14-CLAUDE-CODE/PROMPTS/PHASE-X-....md#f

## What this does
Two or three sentences, in plain language. What can the system do now that it couldn't before?

## Files
| File | Purpose |
|---|---|
| `src/main/java/.../Foo.java` | ... |
| `src/main/resources/db/migration/V7__jobs.sql` | ... |

## Database changes
- Tables added/changed, with column purpose where non-obvious
- Indexes added and the query each one serves
- RLS policy (or the documented reason there isn't one)
- Migration version number

## API surface
| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/...` | session, OWNER | ... |

## Key decisions and why
The important section. For each non-obvious choice: what you chose, what you rejected, why.

Example:
- **Claim query uses `FOR UPDATE SKIP LOCKED` with `LIMIT 20`.** A plain `SELECT ... FOR UPDATE`
  serialises workers; `NOWAIT` throws instead of skipping. Batch of 20 balances round-trips
  against lock hold time.

## Divergence from the architecture docs
Anything you built differently from `WhatsApp-SaaS-Product/`, and why. If nothing: "None."
If you changed an architecture doc as a result, link it.

## Tests
| Test | Proves |
|---|---|
| `JobWorkerConcurrencyTest` | Two workers never claim the same job |

State explicitly what is **not** covered.

## Gotchas — read before touching this code
The section that saves you hours later. Things that are true but surprising:
- Testcontainers defaults to a superuser, which silently disables RLS — tests connect as
  `wasaas_app` via `test-init.sql`
- `app.tenant_id` is set per **transaction**, not per connection (pooled connections are reused)
- `message_ledger` is append-only; a trigger blocks UPDATE

## Configuration
| Env var / property | Default | Purpose |
|---|---|---|

## Known limitations / TODOs
Deliberate omissions and where they get addressed.
- In-process send throttle is per-instance. Breaks with 2+ workers →
  `12-SCALING/WHEN-TO-INTRODUCE-REDIS.md`
```

## `docs/IMPLEMENTATION/INDEX.md`

One table, updated every increment:

```markdown
# Implementation Index

| # | Feature | Status | Date | Doc |
|---|---|---|---|---|
| F00 | Project skeleton | Complete | 2026-09-01 | [F00](F00-project-skeleton.md) |
| F01 | Tenant and user model | Complete | 2026-09-03 | [F01](F01-tenant-user-model.md) |
```

## Rules

- **Written in the same session as the code**, not "later". Later doesn't happen, and by then
  you've forgotten the reasoning that made the doc worth writing.
- **Prose over bullet fragments** in "Key decisions" and "Gotchas". Those sections carry the
  reasoning; the rest is reference.
- **Under 200 lines.** Long enough to be useful, short enough to stay in context.
- **Update, don't append.** When F13 changes F11's behaviour, edit F11's doc and note the change.
- **Honest about what's missing.** A "Known limitations" section you actually fill in is worth
  more than a polished doc that implies completeness.
