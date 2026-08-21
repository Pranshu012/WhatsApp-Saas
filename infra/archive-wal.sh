#!/usr/bin/env bash
# ==============================================================================
# WASaaS PostgreSQL WAL Segment Archiver
# Executed by PostgreSQL archive_command: /opt/wasaas/bin/archive-wal.sh %p %f
# ==============================================================================

set -euo pipefail

WAL_PATH="${1:-}"
WAL_FILE="${2:-}"

if [ -z "$WAL_PATH" ] || [ -z "$WAL_FILE" ]; then
    echo "ERROR: Missing WAL file arguments"
    exit 1
fi

ENV_FILE="/etc/wasaas/wasaas.env"
if [ -f "$ENV_FILE" ]; then
    export $(grep -v '^#' "$ENV_FILE" | xargs)
fi

TMP_ENCRYPTED="/tmp/${WAL_FILE}.age"

if [ -n "${BACKUP_AGE_PUBLIC_KEY:-}" ] && [ "$BACKUP_AGE_PUBLIC_KEY" != "CHANGE_ME" ]; then
    age -r "$BACKUP_AGE_PUBLIC_KEY" -o "$TMP_ENCRYPTED" "$WAL_PATH"
    if [ -n "${B2_BUCKET:-}" ] && [ "$B2_BUCKET" != "CHANGE_ME" ]; then
        b2 file upload "$B2_BUCKET" "$TMP_ENCRYPTED" "wal/${WAL_FILE}.age" > /dev/null
        rm -f "$TMP_ENCRYPTED"
    fi
fi

exit 0
