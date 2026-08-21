#!/usr/bin/env bash
# ==============================================================================
# WASaaS Zero-Downtime Production Deployment Script
# Usage: ./infra/deploy.sh <path-to-new-wasaas-jar>
# ==============================================================================

set -euo pipefail

JAR_PATH="${1:-}"

if [ -z "$JAR_PATH" ] || [ ! -f "$JAR_PATH" ]; then
    echo "ERROR: Please specify a valid JAR file."
    echo "Usage: $0 <path-to-wasaas.jar>"
    exit 1
fi

TIMESTAMP=$(date -u +%Y%m%d_%H%M%SZ)
RELEASE_DIR="/opt/wasaas/releases/${TIMESTAMP}"
CURRENT_LINK="/opt/wasaas/releases/current"
PREVIOUS_RELEASE=""

if [ -L "$CURRENT_LINK" ]; then
    PREVIOUS_RELEASE=$(readlink -f "$CURRENT_LINK")
fi

echo "==> [1/6] Staging new release: ${TIMESTAMP}..."
mkdir -p "$RELEASE_DIR"
cp "$JAR_PATH" "${RELEASE_DIR}/wasaas.jar"
chown -R wasaas:wasaas "$RELEASE_DIR"

echo "==> [2/6] Running Flyway Database Migrations..."
# Run Flyway migrations before switching service symlink
export $(grep -v '^#' /etc/wasaas/wasaas.env | xargs)
java -cp "${RELEASE_DIR}/wasaas.jar" \
    -Dloader.main=org.flywaydb.commandline.Main \
    org.springframework.boot.loader.launch.PropertiesLauncher migrate || true

echo "==> [3/6] Switching active release symlink..."
ln -sfn "$RELEASE_DIR" "$CURRENT_LINK"

echo "==> [4/6] Restarting WASaaS Services..."
systemctl restart wasaas-worker
systemctl restart wasaas-web

echo "==> [5/6] Verifying Service Health..."
MAX_ATTEMPTS=30
ATTEMPT=1
HEALTHY=false

while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8080/actuator/health/readiness || true)
    if [ "$HTTP_CODE" = "200" ]; then
        HEALTHY=true
        break
    fi
    echo "Waiting for health probe (attempt $ATTEMPT/$MAX_ATTEMPTS, status: $HTTP_CODE)..."
    sleep 2
    ATTEMPT=$((ATTEMPT + 1))
done

if [ "$HEALTHY" = true ]; then
    echo "==> [6/6] Deployment SUCCESSFUL! Application is UP and healthy."
    # Prune old releases, keeping the 5 most recent
    cd /opt/wasaas/releases
    ls -dt 20* | tail -n +6 | xargs rm -rf || true
else
    echo "ERROR: Health check failed after $MAX_ATTEMPTS attempts!"
    if [ -n "$PREVIOUS_RELEASE" ] && [ -d "$PREVIOUS_RELEASE" ]; then
        echo "==> Rolling back to previous release: $PREVIOUS_RELEASE..."
        ln -sfn "$PREVIOUS_RELEASE" "$CURRENT_LINK"
        systemctl restart wasaas-web
        systemctl restart wasaas-worker
        echo "Rollback completed. Check logs: journalctl -u wasaas-web -n 50"
    fi
    exit 1
fi
