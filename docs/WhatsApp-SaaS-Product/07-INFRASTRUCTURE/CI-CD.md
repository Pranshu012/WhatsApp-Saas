# CI/CD

Increment **F22**. GitHub Actions free tier: 2,000 minutes/month on private repos.

## Philosophy

Automate the build and the test run. Keep a **human approval gate** before production. You are
one person; a fully automatic pipeline to production means a bad merge at midnight reaches
customers before you notice.

## Pipeline

```text
push to any branch
  └─ build + test (Testcontainers Postgres)
       └─ merge to main
            └─ build release JAR
                 └─ ⏸ MANUAL APPROVAL
                      └─ deploy to production
                           └─ health check
                                └─ rollback if unhealthy
```

## `.github/workflows/ci.yml`

```yaml
name: CI
on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven
      - run: ./mvnw -B clean verify
      - uses: actions/upload-artifact@v4
        if: failure()
        with:
          name: test-reports
          path: target/surefire-reports/
```

Testcontainers works on GitHub runners without extra setup — Docker is present. Don't switch to
H2 to "speed up CI"; you depend on real Postgres behaviour (`SKIP LOCKED`, RLS, full-text
search) and H2 tests would pass while production breaks.

## `.github/workflows/deploy.yml`

```yaml
name: Deploy
on:
  workflow_dispatch:          # manual trigger — deliberate
  push:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin', cache: maven }
      - run: ./mvnw -B clean package -DskipTests=false
      - uses: actions/upload-artifact@v4
        with: { name: app-jar, path: target/*.jar }

  deploy:
    needs: build
    runs-on: ubuntu-latest
    environment: production        # ← configure a required reviewer on this environment
    steps:
      - uses: actions/download-artifact@v4
        with: { name: app-jar, path: ./ }
      - name: Deploy over SSH
        env:
          SSH_KEY: ${{ secrets.DEPLOY_SSH_KEY }}
          HOST: ${{ secrets.DEPLOY_HOST }}
        run: |
          mkdir -p ~/.ssh && echo "$SSH_KEY" > ~/.ssh/id && chmod 600 ~/.ssh/id
          ssh-keyscan -H "$HOST" >> ~/.ssh/known_hosts
          scp -i ~/.ssh/id *.jar deploy@"$HOST":/opt/wasaas/releases/
          ssh -i ~/.ssh/id deploy@"$HOST" '/opt/wasaas/bin/deploy.sh'
```

**The approval gate:** GitHub → Settings → Environments → `production` → Required reviewers →
yourself. Now every production deploy needs a deliberate click.

## `deploy.sh` on the server

Must be safe to run repeatedly and must roll back on failure:

```text
1. Symlink the new JAR into releases/, keep the previous 3
2. Run Flyway migrations (fail fast — abort the deploy on migration failure)
3. Restart wasaas-worker, wait for its health
4. Restart wasaas-web, wait for readiness
5. Poll /actuator/health/readiness for up to 60s
6. If unhealthy → repoint the symlink to the previous release, restart, alert
```

Worker before web. If a migration changed a job payload shape, you want the worker on new code
before new-shaped jobs are enqueued.

## Migrations and zero-downtime

You have one instance, so there's a brief restart gap — acceptable at MVP (Meta retries
webhooks). But write migrations to be **backward compatible with the currently running code**
anyway:

- Adding a column: safe
- Dropping a column: two deploys (stop using it, then drop it)
- Renaming: never rename. Add new, migrate data, stop using old, drop later.
- `NOT NULL` on an existing table: add nullable → backfill → add the constraint

This discipline costs little now and saves you when you eventually run two instances.

## Secrets

GitHub Secrets: `DEPLOY_SSH_KEY`, `DEPLOY_HOST`. Nothing else — the application's own secrets
live in the root-owned `0600` env file on the server and never pass through CI.

Use a **deploy-specific SSH key** with a restricted `authorized_keys` entry (`command=` where
practical), not your personal key.

## Definition of Done

- [ ] CI runs on every push, with Testcontainers, and fails on test failure
- [ ] Deploy requires manual approval
- [ ] `deploy.sh` is idempotent and rolls back on a failed health check
- [ ] Worker restarts before web
- [ ] Migration failure aborts the deploy without restarting the app
- [ ] Previous 3 releases retained on the box for fast rollback
- [ ] A rollback has been tested for real, at least once
- [ ] Under 2,000 CI minutes/month
