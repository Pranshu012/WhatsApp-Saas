# Building This Product With Claude Code

This section turns the architecture docs into an **incremental build plan** with
copy-paste prompts.

## Read in this order

| # | File | Purpose |
|---|---|---|
| 1 | [CLAUDE-CODE-SETUP.md](CLAUDE-CODE-SETUP.md) | One-time setup: install, repo, project memory, permissions, guardrails |
| 2 | [PROJECT-CLAUDE-MD-TEMPLATE.md](PROJECT-CLAUDE-MD-TEMPLATE.md) | Copy this to your repo root as `CLAUDE.md` |
| 3 | [FEATURE-BREAKDOWN.md](FEATURE-BREAKDOWN.md) | The 23 increments, in dependency order |
| 4 | [WORKFLOW.md](WORKFLOW.md) | The loop you repeat for every increment |
| 5 | [IMPLEMENTATION-DOC-TEMPLATE.md](IMPLEMENTATION-DOC-TEMPLATE.md) | Every increment writes its own doc — required |
| 6 | [PROMPTS/](PROMPTS/) | Paste-ready prompts, grouped by phase |

## Navigation

Open **[BUILD-LOG.html](../BUILD-LOG.html)** in a browser (double-click the file). It shows your
next step, the docs to read for it, and a searchable index of all 124 files — every one
click-to-copy as an `@` mention you paste straight into Claude Code Desktop.

## Core principle

**Build vertical slices, not horizontal layers.**

Wrong: "build all entities" → "build all repositories" → "build all services" → "build all
controllers". You get 4 weeks in with nothing that works and no feedback.

Right: each increment goes DB migration → entity → repository → service → controller →
test, for **one narrow capability**, and ends with something you can run and verify.

Every increment below is a slice you can demo, commit, and stop at.

## Prompt files

| Phase | File | Increments |
|---|---|---|
| A — Foundation | [PROMPTS/PHASE-A-FOUNDATION.md](PROMPTS/PHASE-A-FOUNDATION.md) | F00–F04 |
| B — WhatsApp core | [PROMPTS/PHASE-B-WHATSAPP.md](PROMPTS/PHASE-B-WHATSAPP.md) | F05–F11 |
| C — Automation | [PROMPTS/PHASE-C-AUTOMATION.md](PROMPTS/PHASE-C-AUTOMATION.md) | F12–F16 |
| D — Frontend | [PROMPTS/PHASE-D-FRONTEND.md](PROMPTS/PHASE-D-FRONTEND.md) | F17–F20 |
| E — Production | [PROMPTS/PHASE-E-PRODUCTION.md](PROMPTS/PHASE-E-PRODUCTION.md) | F21–F23 |
| Reusable | [PROMPTS/REUSABLE-PROMPTS.md](PROMPTS/REUSABLE-PROMPTS.md) | Review, debug, test, refactor |
