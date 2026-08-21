#!/usr/bin/env bash
# ==============================================================================
# WASaaS Production Database Backup Script
# Logical Custom Dump (-Fc) + Age Public-Key Encryption + Backblaze B2 Upload
# ==============================================================================

set -euo pipefail

# Load environment configuration
ENV_FILE="/etc/wasaas/wasaas.env"
if [ -f "$ENV_FILE" ]; then
    export $(grep -v '^#' "$ENV_FILE" | xargs)
fi

STAMP=$(date -u +%Y%m%dT%H%M%SZ)
BACKUP_DIR="/var/backups/wasaas"
DUMP_FILE="${BACKUP_DIR}/wasaas-${STAMP}.dump"
ENCRYPTED_FILE="${DUMP_FILE}.age"

mkdir -p "$BACKUP_DIR"

echo "==> [1/4] Generating PostgreSQL logical backup..."
pg_dump --format=custom --compress=9 --no-owner --no-privileges \
    --dbname="${DATABASE_URL:-jdbc:postgresql://localhost:5432/wasaas}" \
    --file="$DUMP_FILE"

echo "==> [2/4] Encrypting backup with Age public key..."
if [ -n "${BACKUP_AGE_PUBLIC_KEY:-}" ] && [ "$BACKUP_AGE_PUBLIC_KEY" != "CHANGE_ME" ]; then
    age -r "$BACKUP_AGE_PUBLIC_KEY" -o "$ENCRYPTED_FILE" "$DUMP_FILE"
    shred -u "$DUMP_FILE"
else
    echo "WARNING: BACKUP_AGE_PUBLIC_KEY not configured, storing unencrypted dump."
    ENCRYPTED_FILE="$DUMP_FILE"
fi

echo "==> [3/4] Uploading to Backblaze B2 bucket..."
if [ -n "${B2_BUCKET:-}" ] && [ "$B2_BUCKET" != "CHANGE_ME" ]; then
    b2 file upload "$B2_BUCKET" "$ENCRYPTED_FILE" "daily/$(basename "$ENCRYPTED_FILE")"
    rm -f "$ENCRYPTED_FILE"
else
    echo "NOTICE: B2_BUCKET not configured. Retaining encrypted backup locally at $ENCRYPTED_FILE."
fi

echo "==> [4/4] Emitting health heartbeat..."
if [ -n "${BETTERSTACK_BACKUP_HEARTBEAT_URL:-}" ]; then
    curl -fsS --retry 3 "$BETTERSTACK_BACKUP_HEARTBEAT_URL" > /dev/null || true
fi

echo "Backup completed successfully at ${STAMP}."
