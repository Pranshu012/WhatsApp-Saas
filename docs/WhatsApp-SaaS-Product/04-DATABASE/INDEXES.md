# Indexes

**Classification: BUILD NOW (the listed ones only).**

Every index costs write throughput and disk. This is a write-heavy application — messages,
ledger entries, jobs, webhook events all insert constantly. Index for the queries you actually
run, and no more.

## The rule for tenant tables

`tenant_id` goes **first** in any composite index on a tenant-scoped table, because RLS adds
`tenant_id = ...` to every single query.

```sql
CREATE INDEX idx_contacts_tenant_phone ON contacts (tenant_id, phone_e164);   -- ✅
CREATE INDEX idx_contacts_phone ON contacts (phone_e164);                     -- ❌ can't serve the RLS predicate efficiently
```

## Required indexes

### `jobs` — the hottest table
```sql
CREATE INDEX idx_jobs_claimable ON jobs (status, run_after)
    WHERE status IN ('PENDING', 'RUNNING');
CREATE UNIQUE INDEX idx_jobs_idempotency ON jobs (idempotency_key)
    WHERE idempotency_key IS NOT NULL;
```
Both **partial** — they exclude terminal-state rows, which will be the vast majority over time.
This keeps the claim index small and fast even as the table grows.

### `message_ledger`
```sql
CREATE UNIQUE INDEX idx_ledger_wamid ON message_ledger (tenant_id, wamid)
    WHERE wamid IS NOT NULL;                        -- dedupe + status matching
CREATE INDEX idx_ledger_monthly ON message_ledger (tenant_id, billing_category, created_at);
CREATE INDEX idx_ledger_recipient ON message_ledger (tenant_id, recipient_phone_hash, created_at);
CREATE INDEX idx_ledger_status ON message_ledger (tenant_id, status, created_at);
```
`idx_ledger_monthly` serves the dashboard count query — the one customers will look at most.

### `webhook_events`
```sql
CREATE UNIQUE INDEX idx_webhook_event_id ON webhook_events (event_id) WHERE event_id IS NOT NULL;
CREATE INDEX idx_webhook_unprocessed ON webhook_events (status, received_at)
    WHERE status IN ('RECEIVED', 'FAILED');
```

### `contacts` / `conversations`
```sql
CREATE UNIQUE INDEX idx_contacts_tenant_phone ON contacts (tenant_id, phone_e164);
CREATE INDEX idx_contacts_hash ON contacts (tenant_id, phone_hash);

CREATE UNIQUE INDEX idx_conv_unique ON conversations (tenant_id, contact_id, whatsapp_account_id);
CREATE INDEX idx_conv_recent ON conversations (tenant_id, last_inbound_at DESC);   -- inbox list
CREATE INDEX idx_conv_window ON conversations (tenant_id, service_window_expires_at)
    WHERE status = 'OPEN';
```

### `automation_rules`
```sql
CREATE INDEX idx_rules_eval ON automation_rules (tenant_id, priority)
    WHERE enabled = true;
```
Partial on `enabled` — the evaluation path only ever reads enabled rules.

### `faqs` — full-text + trigram
```sql
CREATE INDEX idx_faqs_fts ON faqs USING GIN (search_vector);
CREATE INDEX idx_faqs_trgm ON faqs USING GIN (question gin_trgm_ops);
CREATE INDEX idx_faqs_tenant ON faqs (tenant_id) WHERE enabled = true;
```
Two different indexes because they answer different questions: GIN/`tsvector` handles word
matching and stemming; trigram handles typos. The FAQ matcher combines both scores.

### `scheduled_messages`
```sql
CREATE INDEX idx_sched_due ON scheduled_messages (status, scheduled_for)
    WHERE status = 'SCHEDULED';
```

### `spring_session`
Use the official Spring Session JDBC schema — it ships with the right indexes. Don't invent them.

---

## Verifying an index is used

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT billing_category, count(*) FROM message_ledger
WHERE tenant_id = '...' AND created_at >= date_trunc('month', now())
GROUP BY billing_category;
```

Look for `Index Scan` or `Bitmap Index Scan`, not `Seq Scan`. At MVP data volumes Postgres will
often choose a sequential scan because the table is tiny and that is *correct* — don't fight the
planner on 500 rows. Re-check with realistic volumes before worrying.

Find unused indexes later with:
```sql
SELECT relname, indexrelname, idx_scan
FROM   pg_stat_user_indexes
WHERE  idx_scan = 0
ORDER  BY relname;
```

## DO NOT BUILD YET

Covering indexes (`INCLUDE`) · partitioning `message_ledger` by month · BRIN indexes ·
materialised views for analytics · a full-text index across message bodies.

**Partitioning trigger:** `message_ledger` past ~50M rows. At 1,000 customers that's roughly a
year and a half away.
