# WhatsApp SaaS — Complete System Gap Analysis & Admin Dashboard Plan

> **Generated**: 22 August 2026  
> **Scope**: Full-stack audit — Frontend UI promises vs Backend reality, missing features, admin controls, and customer onboarding flow  
> **Status**: Analysis Complete — Implementation Pending Approval

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Current Architecture Overview](#2-current-architecture-overview)
3. [UI Promises vs Backend Reality — Gap Matrix](#3-ui-promises-vs-backend-reality--gap-matrix)
4. [Critical Missing Systems](#4-critical-missing-systems)
5. [Customer Self-Onboarding Flow Analysis](#5-customer-self-onboarding-flow-analysis)
6. [Admin Dashboard — Feature Plan](#6-admin-dashboard--feature-plan)
7. [Implementation Roadmap](#7-implementation-roadmap)

---

## 1. Executive Summary

### What's Working Well ✅
The core WhatsApp automation engine is **production-grade and solid**:
- ✅ Multi-tenant PostgreSQL RLS isolation (enterprise-level security)
- ✅ Official Meta Cloud API integration with AES-256-GCM token encryption
- ✅ Embedded signup OAuth flow for connecting WhatsApp numbers
- ✅ Typo-tolerant FAQ matching (pg_trgm + full-text search hybrid)
- ✅ Keyword automation rule engine with priority, regex, and ReDoS protection
- ✅ Reliable async job queue (PostgreSQL `FOR UPDATE SKIP LOCKED`)
- ✅ Message ledger with privacy-compliant phone hashing
- ✅ 24-hour service window tracking & enforcement
- ✅ Scheduled message broadcasting with template validation
- ✅ Webhook processing with HMAC-SHA256 signature verification & deduplication
- ✅ Brute-force login protection (5 attempts / 15 mins)
- ✅ Argon2 password hashing + secure password reset with session invalidation
- ✅ 103/103 backend tests passing

### What's Critically Missing ❌

| Gap | Impact | Severity |
|-----|--------|----------|
| **No subscription/billing/plan system** | UI promises ₹499/mo and 14-day trial but backend has ZERO billing code | 🔴 Critical |
| **No admin/super-admin dashboard** | Owner (Pranshu) has NO way to view all tenants, activate/deactivate accounts, or control anything | 🔴 Critical |
| **No payment gateway** | No Razorpay/Stripe integration — cannot collect ₹499/mo from customers | 🔴 Critical |
| **No trial tracking** | UI says "14-Day Free Trial" but backend has no trialStartDate, trialEndDate, or trial enforcement | 🔴 Critical |
| **No plan activation/deactivation** | Cannot turn off a customer's service after trial expires or payment fails | 🔴 Critical |
| **No ADMIN role** | Backend only has OWNER and MEMBER roles — no platform superadmin | 🟡 High |
| **Pricing inconsistency** | Landing page says ₹499/mo but WhatsApp screen says ₹1,999/mo | 🟡 High |
| **No customer list/management** | Cannot see how many businesses have signed up or their usage | 🟡 High |
| **No usage limits/quotas** | "Unlimited" everything but no way to enforce fair-use or per-plan limits | 🟡 High |
| **No revenue/MRR tracking** | No way to track monthly recurring revenue or payment history | 🟡 High |

---

## 2. Current Architecture Overview

### Backend Stack
- **Java 21** + **Spring Boot 3.5.5** + **PostgreSQL 17**
- **19 JPA Entities** across 15 packages
- **12 REST Controllers** with **26+ endpoints**
- **23+ Services** including job handlers and security components
- **Flyway migrations** for schema management
- **Spring Session JDBC** for authentication

### Existing Database Entities (19 total)

| Entity | Table | Purpose |
|--------|-------|---------|
| Tenant | tenants | Business account (id, name, slug, status, GSTIN) |
| User | users | Login credentials (email, Argon2 hash, fullName) |
| TenantUser | tenant_users | Maps users to tenants with role (OWNER/MEMBER) |
| WhatsAppAccount | whatsapp_accounts | Connected WABA phone numbers with encrypted tokens |
| WhatsAppTemplate | whatsapp_templates | Meta-approved message templates |
| Contact | contacts | End-customer phone records |
| Conversation | conversations | 24h service window tracking per contact |
| AutomationRule | automation_rules | Keyword trigger → auto-reply rules |
| Faq | faqs | Q&A knowledge base entries |
| UnmatchedMessage | unmatched_messages | Customer queries with no bot match |
| MessageLedger | message_ledger | Privacy-compliant message billing audit log |
| MessageLedgerStatusEvent | message_ledger_status_events | Delivery status progression events |
| ScheduledMessage | scheduled_messages | Future broadcast scheduling |
| Job | jobs | Persistent background job queue |
| WebhookEvent | webhook_events | Raw Meta webhook payloads |
| LoginAttempt | login_attempts | Brute-force tracking |
| PasswordResetToken | password_reset_tokens | Secure reset tokens |

### Existing Roles & Auth
- **TenantRole enum**: OWNER, MEMBER (NO ADMIN)
- **TenantStatus enum**: ACTIVE, SUSPENDED
- Auth: Session-based (Spring Session JDBC), CSRF protected

---

## 3. UI Promises vs Backend Reality — Gap Matrix

### 🔴 CRITICAL GAPS (No backend support at all)

| # | UI Promise / Claim | Where in UI | Backend Reality | Gap |
|---|-------------------|-------------|-----------------|-----|
| 1 | **14-Day Free Trial (₹0)** | Landing Page, Pricing Section, Guide Screen | ❌ No trialStartDate, trialEndDate, planType, or trial logic exists in Tenant entity or any service | **MISSING: Trial tracking system** |
| 2 | **₹499/month All-in-One Plan** | Landing Page Pricing, Hero Section, ROI Calculator | ❌ Zero billing/subscription/payment code in entire backend. No Razorpay/Stripe/UPI integration | **MISSING: Entire billing system** |
| 3 | **"No Credit Card Required to Start"** | Landing Page Pricing | ❌ No payment system exists at all, so technically true but misleading — there's no way to ever charge | **MISSING: Payment collection** |
| 4 | **Plan activation / deactivation** | Implied by pricing tiers | ❌ TenantStatus has ACTIVE/SUSPENDED but nothing sets it to SUSPENDED ever. No enforcement middleware | **MISSING: Plan enforcement** |
| 5 | **Indian GST Tax Invoices & Input Credit** | Landing Page, Pricing | ⚠️ GSTIN field exists in Settings but no actual invoice generation, PDF creation, or invoice ledger | **PARTIAL: Only GSTIN capture, no invoice generation** |
| 6 | **Admin/Super-Admin Dashboard** | Not in UI (owner's need) | ❌ No admin role, no admin controllers, no admin endpoints | **MISSING: Entire admin system** |

### 🟡 HIGH PRIORITY GAPS (Partial backend support)

| # | UI Promise / Claim | Backend Reality | Gap |
|---|-------------------|-----------------|-----|
| 7 | **"Unlimited Automated Bot Replies"** | ✅ AutoReplyRateLimiter exists (5/contact/hour) but no per-tenant plan-based limits | No plan-based quota enforcement |
| 8 | **"Unlimited Typo-Tolerant FAQs"** | ✅ FAQ CRUD works, no limit on FAQ count | No plan-based FAQ count limits |
| 9 | **"1-Click Bulk Broadcast Campaigns"** | ✅ Scheduled messages work, but only to single contactId per schedule | No bulk multi-recipient broadcast API |
| 10 | **"Send festive offers to 10,000+ opted-in customers"** | ❌ No bulk send endpoint, no opt-in list management, no segment/audience builder | Major feature gap |
| 11 | **"98% open rate"** | ⚠️ Dashboard tracks READ status from Meta webhooks, but no explicit open rate metric | Could be computed, not surfaced |

### 🟢 WORKING AS PROMISED

| # | UI Promise | Backend Status |
|---|-----------|---------------|
| 12 | Official Meta Cloud API (0% ban risk) | ✅ Full Meta Graph API integration with OAuth |
| 13 | Typo-tolerant FAQ matching | ✅ pg_trgm + FTS hybrid with confidence scoring |
| 14 | Keyword auto-replies (MENU, PRICE, etc.) | ✅ Full automation rule engine with priority ordering |
| 15 | 24h service window tracking | ✅ serviceWindowExpiresAt field with enforcement |
| 16 | Live bot simulator testing | ✅ /api/faqs/test and /api/automation-rules/test |
| 17 | Instant <1s replies | ✅ Async job queue processes within 1s poll interval |
| 18 | Webhook signature verification | ✅ HMAC-SHA256 constant-time comparison |
| 19 | Message delivery tracking | ✅ Full SENT→DELIVERED→READ→FAILED status chain |
| 20 | AES-256-GCM token encryption | ✅ TokenCipher with fresh IV per token |

### 🟡 PRICING INCONSISTENCY

| Location | Price Shown |
|----------|-------------|
| Landing Page (LandingPage.tsx) | ₹499/month |
| WhatsApp Connection Screen (WhatsAppConnectionScreen.tsx line 338-351) | ₹1,999/month |

> **WARNING**: The WhatsApp Connection Screen still shows the old ₹1,999/month pricing text in the "Two-Bill Model Explained" section. This needs to be updated to match the ₹499/month pricing.

---

## 4. Critical Missing Systems

### 4.1 Subscription & Billing System (Does NOT exist)

**Current State**: ZERO code for subscriptions, plans, billing, or payments.

**What's needed**:
1. **Subscription entity** — tracks each tenant's plan, trial dates, payment status
2. **PlanType enum** — FREE_TRIAL, BUSINESS_499, SUSPENDED
3. **Trial enforcement middleware** — check trial expiry on every API request
4. **Payment gateway integration** — Razorpay (best for Indian SMBs: UPI, cards, wallets)
5. **Webhook handler for payment callbacks** — auto-activate on successful payment
6. **Invoice generation** — PDF invoices with GSTIN for tax input credit

### 4.2 Admin/Super-Admin System (Does NOT exist)

**Current State**: Owner (Pranshu) has NO visibility or control over any tenant.

**What's needed**:
1. **SUPER_ADMIN role** — platform-level administrator (separate from tenant OWNER)
2. **Admin API endpoints** (/api/admin/**) — list all tenants, view usage, activate/suspend
3. **Admin frontend dashboard** — separate view with tenant management, revenue tracking
4. **Manual plan activation/deactivation** — toggle tenant status from admin panel

### 4.3 Bulk Broadcast System (Partially exists)

**Current State**: Can schedule 1 message to 1 contact. Cannot do bulk campaigns.

**What's needed**:
1. **Audience/segment builder** — select contacts by tags, opt-in status, or upload CSV
2. **Bulk broadcast API** — send approved template to multiple contacts in one action
3. **Campaign tracking** — sent/delivered/read/failed counts per campaign

---

## 5. Customer Self-Onboarding Flow Analysis

### Current Flow (What actually happens today)

```
Step 1: Customer visits http://localhost:5173/
        → Sees landing page with roadmap, simulator, pricing
        
Step 2: Clicks "Start Free Trial (₹0)"
        → Redirected to /register
        
Step 3: Fills registration form
        → Business Name, Full Name, Email, Password (min 12 chars)
        → Auto-generates slug, auto-logs in after signup
        
Step 4: Lands on Dashboard (/)
        → Sees onboarding banner "Set up in 4 steps"
        
Step 5: Connects WhatsApp (/whatsapp)
        → Meta Embedded Signup popup
        → OAuth code exchange → account saved
        
Step 6: Adds FAQs (/faq) & Keywords (/automation)
        → Bot brain loaded
        
Step 7: Tests in simulator
        → Bot is live, customers start getting auto-replies
```

### Problems with Current Flow

| Problem | Impact |
|---------|--------|
| No trial timer starts on registration | Customer gets **unlimited free access forever** |
| No payment wall after trial expires | **Zero revenue** — customer never needs to pay |
| No plan enforcement | Even if someone manually sets SUSPENDED, no middleware blocks API access |
| No admin notification on new signups | Owner doesn't know when new tenants register |
| No usage dashboard for owner | Can't see which tenants are active, which are freeloading |
| No email verification | Anyone can register with fake email |

### Recommended Flow (After fixes)

```
Step 1: Customer visits landing page
Step 2: Registers → Backend creates tenant with:
        - planType = FREE_TRIAL
        - trialStartDate = now()
        - trialExpiresAt = now() + 14 days
Step 3: Customer gets full access for 14 days
Step 4: At day 10, show in-app banner "Trial expires in 4 days"
Step 5: At day 14, redirect to payment page
        - Razorpay checkout for ₹499/month subscription
Step 6: On successful payment:
        - planType = BUSINESS_499
        - subscriptionActive = true
        - nextBillingDate = now() + 30 days
Step 7: On payment failure / no payment:
        - tenantStatus = SUSPENDED
        - All API endpoints return 402 Payment Required
        - Bot stops replying to customers
Step 8: Admin (Pranshu) can manually override any tenant's status
```

---

## 6. Admin Dashboard — Feature Plan

### 6.1 New Backend Components Needed

#### New Entity: Subscription

```
┌─────────────────────────────────────────────┐
│ Subscription (NEW)                          │
├─────────────────────────────────────────────┤
│ id              UUID PK                     │
│ tenantId        UUID FK → tenants           │
│ planType        PlanType enum               │
│ status          SubscriptionStatus enum     │
│ trialStartDate  Instant                     │
│ trialExpiresAt  Instant                     │
│ currentPeriodStart  Instant                 │
│ currentPeriodEnd    Instant                 │
│ razorpaySubscriptionId  String (nullable)   │
│ razorpayCustomerId      String (nullable)   │
│ amount          Integer (paise, 49900)      │
│ currency        String (INR)                │
│ lastPaymentAt   Instant                     │
│ cancelledAt     Instant (nullable)          │
│ createdAt / updatedAt                       │
└─────────────────────────────────────────────┘

PlanType enum:         FREE_TRIAL | BUSINESS_499
SubscriptionStatus:    TRIALING | ACTIVE | PAST_DUE | CANCELLED | SUSPENDED
```

#### New Controllers

| Controller | Endpoints | Purpose |
|-----------|-----------|---------|
| AdminController | GET /api/admin/tenants | List all tenants with usage stats |
| | GET /api/admin/tenants/{id} | Tenant detail with subscription info |
| | POST /api/admin/tenants/{id}/activate | Manually activate a tenant |
| | POST /api/admin/tenants/{id}/suspend | Manually suspend a tenant |
| | GET /api/admin/stats | Platform-wide MRR, total tenants, active users |
| | GET /api/admin/subscriptions | All subscriptions with payment status |
| SubscriptionController | GET /api/subscription | Current tenant's subscription info |
| | POST /api/subscription/checkout | Initiate Razorpay checkout session |
| | POST /api/subscription/webhook | Razorpay payment webhook handler |

#### New Services
- SubscriptionService — manage plan lifecycle, trial tracking, payment status
- SubscriptionEnforcementFilter — HTTP filter that checks subscription status on every API call
- AdminService — cross-tenant queries for platform admin
- RazorpayService — payment gateway integration (optional, can start manual)

### 6.2 Admin Frontend Dashboard Plan

#### New Route: /admin (only for SUPER_ADMIN users)

**Page 1: Admin Home / Platform Overview**
- Total registered businesses count
- Active vs Trial vs Suspended breakdown
- This month's new signups
- Monthly Recurring Revenue (MRR) if payments are integrated
- Messages processed platform-wide this month

**Page 2: All Tenants List**
- Table: Business Name | Email | Plan | Status | WhatsApp Connected | FAQs | Messages This Month | Registered Date
- Search and filter by status (Active/Trial/Suspended)
- Quick actions: Activate / Suspend / View Details

**Page 3: Tenant Detail**
- Full business profile (name, GSTIN, slug)
- Subscription & plan info
- WhatsApp connection status
- Usage metrics (messages sent, FAQs count, automation rules)
- Manual plan override buttons

### 6.3 Subscription Enforcement Logic

```
On every authenticated API request:
  1. Load tenant's subscription from cache/DB
  2. If planType == FREE_TRIAL:
     - Check if now() > trialExpiresAt
     - If expired → return 402 "Trial expired. Please upgrade."
  3. If subscriptionStatus == SUSPENDED:
     - Return 402 "Account suspended. Contact support."
  4. If subscriptionStatus == PAST_DUE:
     - Allow read-only access, block writes
  5. Otherwise → allow full access
```

---

## 7. Implementation Roadmap

### Phase 1: Foundation (Backend — Must Do First)
> Without these, you cannot onboard paying customers

- [ ] Add Subscription entity + Flyway migration
- [ ] Add PlanType and SubscriptionStatus enums
- [ ] Create SubscriptionService with trial lifecycle logic
- [ ] Auto-create subscription on tenant registration (FREE_TRIAL, 14 days)
- [ ] Add SubscriptionEnforcementFilter to block expired trials
- [ ] Fix pricing inconsistency in WhatsAppConnectionScreen.tsx (₹1,999 → ₹499)
- [ ] Add superAdmin flag to User entity
- [ ] Create AdminController with tenant list & activate/suspend endpoints
- [ ] Add admin auth guard (check superAdmin flag)

### Phase 2: Admin Dashboard (Frontend)
> For Pranshu to manage all tenants

- [ ] Create /admin route with admin layout
- [ ] Build Admin Home screen with platform stats
- [ ] Build Tenant List screen with search/filter/actions
- [ ] Build Tenant Detail screen with usage & subscription info
- [ ] Add manual Activate/Suspend tenant toggle buttons

### Phase 3: Payment Integration (Revenue)
> For collecting ₹499/month from customers

- [ ] Integrate Razorpay subscription API
- [ ] Add checkout flow in frontend (in-app upgrade banner at trial expiry)
- [ ] Handle Razorpay webhooks for payment success/failure
- [ ] Auto-activate on payment success, auto-suspend on failure
- [ ] Generate PDF invoices with GSTIN

### Phase 4: Enhanced Features
> Deliver on remaining UI promises

- [ ] Build bulk broadcast API (send template to multiple contacts)
- [ ] Add audience/segment builder for campaigns
- [ ] Add email verification on registration
- [ ] Add trial expiry reminder emails (day 10, day 13)
- [ ] Build campaign analytics (sent/delivered/read per broadcast)

---

> **IMPORTANT**: Phase 1 is mandatory before onboarding even 1 paying customer. The 14-day trial promise in the UI is currently meaningless — every signup gets permanent free access forever with no expiry.

> **CAUTION**: Without Razorpay/payment integration, there is literally no mechanism to collect money from customers. The ₹499/month pricing on the landing page is purely decorative right now.
