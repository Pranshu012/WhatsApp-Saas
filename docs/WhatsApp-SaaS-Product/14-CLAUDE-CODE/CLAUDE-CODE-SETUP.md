# Claude Code — Initial Setup (One Time)

Do this **once**, before increment F00. It takes about 45 minutes and it is the difference
between Claude Code being a genuine force multiplier and being an expensive way to generate
code you then have to rewrite.

> Official docs: https://code.claude.com/docs/en/overview
> Memory docs: https://code.claude.com/docs/en/memory
> Verified 18 August 2026 — check the docs for anything that looks different.

---

## Step 1 — Install Claude Code Desktop

Download the desktop app from Anthropic's site. Available for macOS and Windows (Linux beta
since June 2026).

**You do not need to install Node.js or the CLI.** The desktop app includes Claude Code.

What you *do* need:

- A paid plan — the Code tab is available on Pro, Max, Team, and Enterprise
- **Git installed.** On Windows this is required for local sessions. Most Macs ship with it.
  Check: `git --version`
- Java 21, Docker Desktop, and the other project tools from
  `../05-BACKEND/BACKEND-SETUP.md` — the app doesn't install your language runtimes

Sign in, then open the **Code** tab.

> Desktop and CLI run the same engine and **share configuration** — `CLAUDE.md`, MCP servers,
> hooks, skills, and settings all work identically. You can install the CLI later and run both
> on the same project without changing anything.

---

## Step 2 — Create the repository, then open it as a project

**Do this in a terminal first, before opening the folder in Claude Code:**

```bash
mkdir wasaas && cd wasaas
git init
mkdir docs
# copy the WhatsApp-SaaS-Product folder into docs/
git add . && git commit -m "docs: architecture and execution workspace"
```

**Why `git init` first:** Claude Code anchors project memory to the Git repository root. Open a
non-repo folder and project memory won't attach properly. Ten seconds now saves confusion later.

Then in the desktop app:

1. **Code** tab → **New session**
2. Environment: **Local** (Claude works directly with your files on your machine)
3. **Select folder** → choose `wasaas`

The other environments — Cloud (runs on Anthropic's servers against a GitHub repo) and SSH — are
not what you want here. This project needs local Postgres, local Docker, and a local `.env`.

**Why the docs go inside the repo:** so Claude Code can read them. Once they're in `docs/`, you
can type `@docs/WhatsApp-SaaS-Product/03-ARCHITECTURE/MULTI-TENANCY.md` in the prompt box and it
reads the actual file. This is the single highest-value setup step — your architecture decisions
stop being something you re-explain every session.

---

## Step 3 — Create the project `CLAUDE.md`

Copy [PROJECT-CLAUDE-MD-TEMPLATE.md](PROJECT-CLAUDE-MD-TEMPLATE.md) to your repo root as
`CLAUDE.md`. Commit it.

**How memory works** (four scopes, loaded at session start, higher takes precedence):

| Scope | Location | Use for |
|---|---|---|
| Enterprise policy | OS-level path | Not relevant to you |
| **Project memory** | `./CLAUDE.md` | **This project's non-negotiables** ← your main file |
| User memory | `~/.claude/CLAUDE.md` | Your personal style across all projects |
| Project local | `./CLAUDE.local.md` | **Deprecated** — use `@imports` instead |

Notes that matter:

- `CLAUDE.md` is **context, not enforcement.** Claude treats it as high-priority
  instruction but it is not a hard boundary. Anything that must be *guaranteed* goes into
  tests, lint rules, or a PreToolUse hook — not into prose.
- Keep it **short**. Files over ~200 lines consume more context and reduce adherence.
  Don't paste architecture into it — **link** to `docs/` instead.
- Imports use `@path/to/file` syntax and load at launch (so importing doesn't save context,
  it just organises).
- Project-root `CLAUDE.md` survives `/compact` — Claude re-reads it from disk.
- `/memory` shows you which memory files are active. Use it when Claude ignores a rule.
- Don't run `/init` blindly on an empty repo — it generates a codebase summary, which is
  useless before there's code. Use the template instead, then run `/init` later if you want
  it refreshed.

---

## Step 4 — Permissions and settings

Create `.claude/settings.json` in the repo:

```json
{
  "permissions": {
    "allow": [
      "Bash(./mvnw *)",
      "Bash(git status)",
      "Bash(git diff *)",
      "Bash(git log *)",
      "Bash(docker compose *)",
      "Bash(psql *)"
    ],
    "deny": [
      "Bash(git push *)",
      "Bash(rm -rf *)",
      "Read(./.env)",
      "Read(./**/*.pem)",
      "Read(./**/secrets*)"
    ]
  }
}
```

**Why deny `git push`:** you want to review before anything leaves your machine.
**Why deny reading `.env`:** so real secrets never enter a transcript. Keep
`.env.example` readable with placeholder values — that's what Claude actually needs.

Commit `.claude/settings.json`. Add `.claude/settings.local.json` to `.gitignore` for
anything machine-specific.

---

## Step 5 — Optional: `.claude/rules/` for path-scoped rules

Rules in `.claude/rules/` can be scoped so they load only when Claude touches matching
files. Useful here for two things:

`.claude/rules/migrations.md`
```markdown
---
paths: ["src/main/resources/db/migration/**"]
---
- Never edit an existing migration file. Always create a new V{n}__ file.
- Every new table needs a NOT NULL tenant_id column and an RLS policy.
- Every new table needs an index on (tenant_id, <primary lookup column>).
```

`.claude/rules/webhooks.md`
```markdown
---
paths: ["src/main/java/**/webhook/**"]
---
- No outbound HTTP calls in this package. Persist and enqueue only.
- Signature verification must happen before any parsing of the body.
- Target p99 response time under 2 seconds.
```

---

## Step 6 — Optional: Context7 MCP for library docs

Claude Code's training data has a cutoff. For Spring Boot 3.x / Spring Security specifics,
an MCP server that pulls live official docs reduces hallucinated APIs noticeably:

In the desktop app, add it through **Settings → Connectors** (one-click Desktop Extensions),
or with the CLI if you install it:

```bash
claude mcp add --transport http context7 https://mcp.context7.com/mcp
```

Then append `use context7` to prompts where exact library API matters. Verify with `/mcp`.

This is genuinely optional. Skip it if you'd rather keep the setup minimal.

---

## Step 7 — Desktop-specific working habits

The desktop app changes *how* you do several things in
[WORKFLOW.md](WORKFLOW.md). The discipline is the same; the mechanics differ.

### `@` file mentions — use these constantly

Type `@` in the prompt box and it autocompletes paths from your project. This is how you
navigate 124 docs without opening a single one:

```text
Read @docs/WhatsApp-SaaS-Product/03-ARCHITECTURE/MULTI-TENANCY.md and
@docs/WhatsApp-SaaS-Product/09-TESTING/MULTI-TENANCY-TESTING.md, then plan increment F02.
```

Open **BUILD-LOG.html** in a browser — every doc has a click-to-copy `@` mention ready to paste.

(`@` works for local sessions only. Cloud sessions can't see your files.)

### Plan mode has a viewer

Ask for a plan, or use the plan mode toggle. The desktop app gives you a **dedicated plan
viewer** you can revisit while work proceeds — better than scrolling back through a terminal.
Read it properly. Correcting a plan costs one message; correcting 600 lines costs an hour.

### Review diffs in the diff viewer, not `git diff`

A change indicator (`+12 -1`) appears when files change. Click it for file-by-file review with
line-level comments. This is genuinely better than the CLI for step 6 of the workflow loop —
use it, and don't approve a file you can't explain.

### The built-in terminal replaces your second window

Commands run in-app and you watch them execute. You can still run `./mvnw clean verify`
yourself there. **Do run it yourself** — don't take "all tests pass" on trust.

### New session per increment

Instead of `/clear`, start a **new session** from the sidebar. Sessions are listed and
filterable (Active/Archived), so you get a per-increment history you can go back to — something
the CLI doesn't give you.

Slash commands (`/memory`, `/doctor`, `/compact`) work the same in the prompt box.

### ⚠️ Parallel sessions — resist them for this project

The app can run multiple sessions at once, each in its own Git worktree. It's a real feature and
it's the wrong tool here.

Your 23 increments are a **dependency chain**: F02 needs F01, F09 needs F07 and F08. Two
sessions building two increments against one schema will produce conflicting Flyway migrations
(both creating `V7__`) and contradictory decisions about the same modules.

One increment, one session, review, commit. Revisit parallel sessions once the foundation is
built and you have genuinely independent work — a bug fix alongside a feature, say.

### Side chats for questions

Ask a question without derailing the main thread — useful for "why did you choose X?" mid-build.

---

## Step 8 — Habits that matter more than any of the above

**Commit after every increment, yourself.** Review the diff first. If you can't explain what a
file does, don't commit it — ask for an explanation or a simpler version.

**Make it write the test in the same increment.** Not "later". Later never comes, and a
multi-tenant app without isolation tests is a data leak waiting to happen.

**Push back when it over-builds.** It will sometimes reach for an abstraction or a dependency you
don't need. Say: *"Remove that. Rule 9 in CLAUDE.md. Solve it with Postgres."* That's normal —
it's a collaborator, not an oracle.

**Never paste real secrets into a session.** Not the Meta app secret, not the encryption key, not
the DB password. `.env` is deny-listed in Step 4 for exactly this reason; keep `.env.example`
readable as the contract.

---

## Setup Definition of Done

- [ ] Claude Code Desktop installed and signed in; **Code** tab visible
- [ ] Git installed (`git --version`) — required on Windows
- [ ] Java 21, Docker Desktop, Node installed per `../05-BACKEND/BACKEND-SETUP.md`
- [ ] `git init` done **before** opening the folder in Claude Code
- [ ] Project opened as a **Local** session pointing at your `wasaas` folder
- [ ] Docs workspace copied into `docs/` and committed
- [ ] `CLAUDE.md` at repo root, from the template, committed
- [ ] `.claude/settings.json` with allow/deny lists, committed
- [ ] `.env.example` exists; real `.env` is gitignored and deny-listed
- [ ] `/memory` shows your project `CLAUDE.md` as loaded
- [ ] `@docs/...` autocomplete works in the prompt box — test it once
- [ ] **BUILD-LOG.html** opens in your browser and shows your next step
- [ ] You have read [WORKFLOW.md](WORKFLOW.md) and know the per-increment loop
