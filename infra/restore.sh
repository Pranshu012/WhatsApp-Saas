#!/usr/bin/env bash
# ==============================================================================
# WASaaS Production Database Restore Procedure
# Usage: ./infra/restore.sh <path-to-encrypted-dump.age> <path-to-age-identity-file>
# ==============================================================================

set -euo pipefail

BACKUP_FILE="${1:-}"
KEY_FILE="${2:-}"

if [ -z "$BACKUP_FILE" ] || [ ! -f "$BACKUP_FILE" ]; then
    echo "ERROR: Please specify a valid backup file."
    echo "Usage: $0 <path-to-backup.dump.age> [path-to-age-key]"
    exit 1
fi

DECRYPTED_DUMP="/tmp/wasaas_restore_$(date +%s).dump"

if [[ "$BACKUP_FILE" == *.age ]]; then
    if [ -z "$KEY_FILE" ] || [ ! -f "$KEY_FILE" ]; then
        echo "ERROR: Encrypted backup requires an age private key file."
        echo "Usage: $0 <path-to-backup.dump.age> <path-to-age-key>"
        exit 1
    fi
    echo "==> [1/3] Decrypting backup file using Age key..."
    age -d -i "$KEY_FILE" -o "$DECRYPTED_DUMP" "$BACKUP_FILE"
else
    echo "==> [1/3] Using unencrypted backup dump..."
    cp "$BACKUP_FILE" "$DECRYPTED_DUMP"
fi

echo "==> [2/3] Restoring database to PostgreSQL..."
ENV_FILE="/etc/wasaas/wasaas.env"
if [ -f "$ENV_FILE" ]; then
    export $(grep -v '^#' "$ENV_FILE" | xargs)
fi

DB_NAME="wasaas"
pg_restore --clean --if-exists --no-owner --no-privileges \
    --dbname="${DATABASE_URL:-jdbc:postgresql://localhost:5432/wasaas}" \
    "$DECRYPTED_DUMP"

echo "==> [3/3] Cleaning up temporary decrypted files..."
shred -u "$DECRYPTED_DUMP"

echo "Database restore completed successfully!"
