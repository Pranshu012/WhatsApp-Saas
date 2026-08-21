# Analytics

Increment **F20** (minimal). Mostly a **deliberate non-feature** in the MVP.

## The rule

Build a metric when a customer asks for that specific metric. Not before.

Analytics is the classic solo-founder trap: it feels productive, it's fun to build, it demos
well, and almost nobody churns over its absence. Meanwhile the thing they actually churn over
is a confusing Meta bill.

## What ships in the MVP

All of it comes from `message_ledger` with one indexed query each.

### 1. Monthly message counts by category — **required**

Marketing / Utility / Authentication / Service / Inbound. On the dashboard.

Required because from **1 October 2026** service messages and in-window utility templates
become billable in India. Customers will get a larger Meta invoice and ask you to explain it.
Without this screen you cannot.

### 2. Delivery outcomes — **required**

Sent / Delivered / Read / Failed for the last 7 and 30 days, plus a failure list with plain
language reasons. Customers ask "did my message go?" constantly.

### 3. Automation effectiveness — **required**

- Auto-replied count
- Unmatched count, with a link to the unmatched list

This is the retention metric. A customer who sees "834 messages answered automatically" knows
what they're paying for. A customer who sees their unmatched list shrinking is engaged.

## What does not ship

| Not building | Why |
|---|---|
| Time-series charts | Needs a charting library; counts answer the same question |
| Response-time percentiles | Nobody has asked; automation is sub-second anyway |
| Contact growth curves | Vanity |
| Peak-hours heatmap | Interesting, not actionable for an SMB |
| Funnel/conversion analysis | You don't have their sales data |
| Benchmarks vs other businesses | Needs cross-tenant aggregation — a multi-tenancy risk for zero revenue |
| CSV/PDF export | Wait for the request; it will come from exactly one customer first |
| Custom date ranges | Current month + last 7/30 days covers it |

## Implementation notes

- Every query filtered by `tenant_id` and covered by the
  `(tenant_id, billing_category, created_at)` index
- **Never aggregate across tenants** for a customer-facing screen. Cross-tenant queries are
  where multi-tenancy leaks happen, and there's no MVP feature worth that risk.
- Compute on read. No summary tables, no materialised views, no nightly rollup jobs. At MVP
  volumes (a few thousand rows per tenant per month) these queries are milliseconds. Add
  rollups when `EXPLAIN ANALYZE` on real data says to — see `../12-SCALING/`.
- Read rates from the dated `whatsapp_rates` table if showing money. Never hardcode.

## Your own analytics (not the customer's)

Separate concern, and more important to you right now. Track manually in a spreadsheet for
the first 20 customers:

- Signup → WhatsApp connected (activation rate, and where they drop)
- Time to first automated reply
- Unmatched rate per tenant (is deterministic matching enough? — ADR-007)
- Monthly churn and the stated reason
- Support tickets per customer per month

A spreadsheet you actually read beats a dashboard you built and ignore.
