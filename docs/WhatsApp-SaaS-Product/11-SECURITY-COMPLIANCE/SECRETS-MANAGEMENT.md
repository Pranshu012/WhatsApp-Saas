# Secrets Management

Deliberately simple. One person, one server, no Vault.

## Where secrets live

| Location | Contents | Notes |
|---|---|---|
| **Password manager** (Bitwarden/1Password) | Every secret, canonically | The source of truth |
| `/opt/wasaas/env` on the server | Runtime values | root-owned, `0600` |
| GitHub Secrets | `DEPLOY_SSH_KEY`, `DEPLOY_HOST` only | Nothing else passes through CI |
| Cloudflare Pages env | `VITE_*` only (all public by design) | |
| **Off-server, two places** | The `age` backup **private** key | See below — critical |
| Git | `.env.example` with placeholders | Never real values |

## The complete secret inventory

```bash
DATABASE_PASSWORD              # wasaas_app role
TOKEN_ENCRYPTION_KEY           # base64, exactly 32 bytes — AES-256-GCM
META_APP_SECRET                # server only; HMAC verification
META_WEBHOOK_VERIFY_TOKEN      # your choice; constant-time compared
RAZORPAY_KEY_SECRET
RAZORPAY_WEBHOOK_SECRET
R2_SECRET_ACCESS_KEY
B2_APPLICATION_KEY
BACKUP_AGE_PUBLIC_KEY          # public — safe on the server
BREVO_API_KEY
SENTRY_DSN                     # semi-public but treat as secret
DEPLOY_SSH_KEY                 # private key, GitHub Secrets
BACKUP_AGE_PRIVATE_KEY         # ⚠️ NOT on the server
```

Public by design (in the SPA bundle, and that's fine): `META_APP_ID`, `META_CONFIG_ID`,
`RAZORPAY_KEY_ID`, `VITE_API_BASE_URL`.

**Everything in a Vite bundle is readable by anyone.** If you're unsure whether something belongs
there, it doesn't.

## ⚠️ The backup encryption key

The `age` **private** key decrypts your backups. If it lives only on the VM being backed up,
then losing the VM loses both the data and the recovery path — your backups become
cryptographically useless files.

Store it in:
1. Your password manager (primary)
2. Printed, in a physically secure place (secondary)

Test it during the monthly restore drill. A key you can't find at 2am is a key you don't have.

## Generating secrets

```bash
# 32-byte key for AES-256-GCM (base64)
openssl rand -base64 32

# Database password
openssl rand -base64 24 | tr -d '/+='

# Webhook verify token
openssl rand -hex 32

# age keypair for backups
age-keygen -o backup-key.txt        # contains the private key — move it OFF this box
```

## The server env file

```bash
sudo install -o root -g root -m 0600 /dev/null /opt/wasaas/env
sudo nano /opt/wasaas/env
```

Referenced by both systemd units via `EnvironmentFile=/opt/wasaas/env`. The `deploy` user must
not be able to read it — only root and the service.

Verify: `sudo -u deploy cat /opt/wasaas/env` must fail.

## Never

- Commit a real secret, ever. Not "temporarily", not in a branch you'll rebase.
- Paste a secret into a Claude Code session, an issue, a chat, or a screenshot.
- Log a secret, or let one reach an exception message.
- Put a secret in a `VITE_` variable.
- Email a secret to yourself.
- Reuse a secret across environments.

## If a secret leaks

Assume compromise. Rotate immediately — removing it from Git history does not un-leak it.

| Secret | Rotation | Impact |
|---|---|---|
| `META_APP_SECRET` | Meta App Dashboard → regenerate | Webhook verification breaks until updated |
| `TOKEN_ENCRYPTION_KEY` | **Hard.** Decrypt all tokens with the old key, re-encrypt with the new, in one migration. Plan it. | Every customer's automation dies if botched |
| `DATABASE_PASSWORD` | `ALTER ROLE ... PASSWORD` + env update + restart | Brief downtime |
| `RAZORPAY_KEY_SECRET` | Razorpay dashboard | Payments fail until updated |
| `DEPLOY_SSH_KEY` | New keypair, update `authorized_keys` and GitHub Secrets | Deploys blocked until updated |
| `BACKUP_AGE_PRIVATE_KEY` | Generate a new pair; old backups need the old key **forever** | Keep the old key archived |

The token encryption key is the one to think about *before* you need to rotate it. Write the
re-encryption migration path down now, while nothing is on fire.

## Secret scanning in CI

```bash
grep -rIn --exclude-dir=.git -E \
  '(EAA[A-Za-z0-9]{20,}|rzp_live_|-----BEGIN .* PRIVATE KEY)' . && exit 1 || true
```

Plus GitHub secret scanning and Dependabot enabled on the repo.

Scan history once, now: `git log -p | grep -iE 'app_secret|access_token|rzp_live|PRIVATE KEY'`.

## Local development

`.env` in the project root, gitignored, **and** deny-listed in `.claude/settings.json` so it can
never enter a Claude Code transcript:

```json
{ "permissions": { "deny": ["Read(./.env)", "Read(./**/*.pem)"] } }
```

Keep `.env.example` readable with placeholders — that's what Claude actually needs to understand
your configuration.

Use throwaway values locally. Never point local development at production credentials.

## When to upgrade this approach

A `0600` file plus a password manager is genuinely appropriate for one person and one server.
Move to a secrets manager (AWS Secrets Manager, Infisical, Doppler) when you have **more than
one server** or **more than one person** — not before. Complexity you don't need is its own
security risk.
