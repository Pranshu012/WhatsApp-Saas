#!/usr/bin/env bash
# ==============================================================================
# WASaaS Production VM Provisioning Script
# Target: Ubuntu 24.04 LTS (aarch64 / ARM64 or x86_64)
# Idempotent — safe to run on fresh VM or re-run for updates
# ==============================================================================

set -euo pipefail

echo "==> [1/10] System Update & Essential Packages..."
export DEBIAN_FRONTEND=noninteractive
apt-get update -y
apt-get upgrade -y
apt-get install -y --no-install-recommends \
    curl \
    wget \
    git \
    unzip \
    htop \
    jq \
    ufw \
    fail2ban \
    unattended-upgrades \
    age \
    b2 \
    acl \
    gnupg \
    lsb-release \
    ca-certificates

echo "==> [2/10] Configuring Security, UFW & Fail2ban..."
# Configure UFW
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp comment 'SSH'
ufw allow 80/tcp comment 'HTTP ACME'
ufw allow 443/tcp comment 'HTTPS'
ufw --force enable

# Enable Fail2ban
systemctl enable --now fail2ban

# Configure unattended security upgrades
dpkg-reconfigure -f noninteractive -p low unattended-upgrades

echo "==> [3/10] Configuring Swapfile (2GB)..."
if [ ! -f /swapfile ]; then
    fallocate -l 2G /swapfile || dd if=/dev/zero of=/swapfile bs=1M count=2048
    chmod 600 /swapfile
    mkswap /swapfile
    swapon /swapfile
    echo '/swapfile none swap sw 0 0' >> /etc/fstab
    sysctl vm.swappiness=10
    echo 'vm.swappiness=10' >> /etc/sysctl.d/99-swappiness.conf
fi

echo "==> [4/10] Installing Eclipse Temurin JDK 21 (aarch64/amd64)..."
if ! command -v java > /dev/null || ! java -version 2>&1 | grep -q "21\."; then
    mkdir -p /etc/apt/keyrings
    wget -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg
    echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(lsb_release -cs) main" > /etc/apt/sources.list.d/adoptium.list
    apt-get update -y
    apt-get install -y temurin-21-jdk
fi

echo "==> [5/10] Installing & Configuring PostgreSQL 17..."
if ! command -v psql > /dev/null; then
    install -d /etc/apt/keyrings
    curl -fsSL https://www.postgresql.org/media/keys/ACCC4CF8.asc | gpg --dearmor -o /etc/apt/keyrings/postgresql.gpg
    echo "deb [signed-by=/etc/apt/keyrings/postgresql.gpg] http://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list
    apt-get update -y
    apt-get install -y postgresql-17 postgresql-contrib-17
fi

# Ensure PostgreSQL service is running
systemctl enable --now postgresql

# Configure extensions & non-superuser application role
sudo -u postgres psql -c "CREATE DATABASE wasaas;" || true
sudo -u postgres psql -d wasaas -c "CREATE EXTENSION IF NOT EXISTS pgcrypto;"
sudo -u postgres psql -d wasaas -c "CREATE EXTENSION IF NOT EXISTS pg_trgm;"
sudo -u postgres psql -d wasaas -c "CREATE EXTENSION IF NOT EXISTS pg_stat_statements;"

# Validate required passwords from environment
if [ -z "${DB_APP_PASSWORD:-}" ] || [ "${DB_APP_PASSWORD}" = "change_in_production" ]; then
    echo "ERROR: DB_APP_PASSWORD environment variable must be set to a secure production password." >&2
    exit 1
fi

if [ -z "${DB_MIGRATOR_PASSWORD:-}" ] || [ "${DB_MIGRATOR_PASSWORD}" = "change_in_production" ]; then
    echo "ERROR: DB_MIGRATOR_PASSWORD environment variable must be set to a secure production password." >&2
    exit 1
fi

# Create migrator & application roles if not exist
sudo -u postgres psql -c "DO \$\$ BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'wasaas_app') THEN
        CREATE ROLE wasaas_app WITH LOGIN PASSWORD '${DB_APP_PASSWORD}' NOSUPERUSER NOBYPASSRLS;
    ELSE
        ALTER ROLE wasaas_app WITH PASSWORD '${DB_APP_PASSWORD}';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'wasaas_migrator') THEN
        CREATE ROLE wasaas_migrator WITH LOGIN CREATEDB PASSWORD '${DB_MIGRATOR_PASSWORD}';
    ELSE
        ALTER ROLE wasaas_migrator WITH PASSWORD '${DB_MIGRATOR_PASSWORD}';
    END IF;
END \$\$;"

sudo -u postgres psql -d wasaas -c "GRANT CONNECT ON DATABASE wasaas TO wasaas_app;"
sudo -u postgres psql -d wasaas -c "GRANT USAGE ON SCHEMA public TO wasaas_app;"
sudo -u postgres psql -d wasaas -c "GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO wasaas_app;"
sudo -u postgres psql -d wasaas -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO wasaas_app;"
sudo -u postgres psql -d wasaas -c "GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO wasaas_app;"
sudo -u postgres psql -d wasaas -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO wasaas_app;"

echo "==> [6/10] Installing Caddy Web Server..."
if ! command -v caddy > /dev/null; then
    apt-get install -y debian-keyring debian-archive-keyring apt-transport-https
    curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
    curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | tee /etc/apt/sources.list.d/caddy-stable.list
    apt-get update -y
    apt-get install -y caddy
fi

echo "==> [7/10] Setting up Application Directories and User..."
# Create deploy user if not existing
if ! id "wasaas" &>/dev/null; then
    useradd -r -s /bin/false -d /opt/wasaas wasaas
fi

mkdir -p /opt/wasaas/bin
mkdir -p /opt/wasaas/releases
mkdir -p /opt/wasaas/logs
mkdir -p /var/backups/wasaas
mkdir -p /var/log/caddy
mkdir -p /etc/wasaas

chown -R wasaas:wasaas /opt/wasaas
chown -R wasaas:wasaas /var/backups/wasaas
chown -R caddy:caddy /var/log/caddy

# Default environment file (root-only readable)
if [ ! -f /etc/wasaas/wasaas.env ]; then
    cat << 'EOF' > /etc/wasaas/wasaas.env
# WASaaS Production Environment Configuration
SPRING_PROFILES_ACTIVE=prod,web
DATABASE_URL=jdbc:postgresql://localhost:5432/wasaas
DATABASE_USER=wasaas_app
DATABASE_PASSWORD=CHANGE_ME
MIGRATOR_USER=wasaas_migrator
MIGRATOR_PASSWORD=CHANGE_ME
TOKEN_ENCRYPTION_KEY=CHANGE_ME_BASE64_32_BYTES
META_APP_ID=CHANGE_ME
META_APP_SECRET=CHANGE_ME
META_WEBHOOK_VERIFY_TOKEN=CHANGE_ME
META_GRAPH_VERSION=v21.0
RAZORPAY_KEY_ID=CHANGE_ME
RAZORPAY_KEY_SECRET=CHANGE_ME
RAZORPAY_WEBHOOK_SECRET=CHANGE_ME
R2_ACCESS_KEY_ID=CHANGE_ME
R2_SECRET_ACCESS_KEY=CHANGE_ME
R2_BUCKET=CHANGE_ME
R2_ENDPOINT=CHANGE_ME
B2_APPLICATION_KEY_ID=CHANGE_ME
B2_APPLICATION_KEY=CHANGE_ME
B2_BUCKET=CHANGE_ME
BACKUP_AGE_PUBLIC_KEY=CHANGE_ME
BREVO_API_KEY=CHANGE_ME
SENTRY_DSN=
BETTERSTACK_BACKUP_HEARTBEAT_URL=
BETTERSTACK_WORKER_HEARTBEAT_URL=
EOF
    chmod 600 /etc/wasaas/wasaas.env
    chown wasaas:wasaas /etc/wasaas/wasaas.env
fi

echo "==> [8/10] Installing Systemd Services..."
cp infra/systemd/wasaas-web.service /etc/systemd/system/wasaas-web.service
cp infra/systemd/wasaas-worker.service /etc/systemd/system/wasaas-worker.service
systemctl daemon-reload
systemctl enable wasaas-web
systemctl enable wasaas-worker

echo "==> [9/10] Installing Backup & Maintenance Scripts..."
cp infra/backup.sh /opt/wasaas/bin/backup.sh
cp infra/restore.sh /opt/wasaas/bin/restore.sh
cp infra/archive-wal.sh /opt/wasaas/bin/archive-wal.sh
chmod 750 /opt/wasaas/bin/*.sh
chown -R wasaas:wasaas /opt/wasaas/bin

# Install nightly backup cron at 02:30 IST (21:00 UTC)
crontab -u wasaas -l 2>/dev/null | grep -v 'backup.sh' | { cat; echo "0 21 * * * /opt/wasaas/bin/backup.sh >> /opt/wasaas/logs/backup.log 2>&1"; } | crontab -u wasaas -

echo "==> [10/10] Provisioning Complete!"
echo "Next steps:"
echo " 1. Populate /etc/wasaas/wasaas.env with real production credentials"
echo " 2. Configure /etc/caddy/Caddyfile and run: systemctl restart caddy"
echo " 3. Deploy your JAR using: ./infra/deploy.sh <path-to-jar>"
