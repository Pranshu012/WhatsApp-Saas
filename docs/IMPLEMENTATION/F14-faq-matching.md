# F14 — FAQ Matching

**Status:** Complete  
**Completed:** 2026-08-21  
**Spec:** ../WhatsApp-SaaS-Product/14-CLAUDE-CODE/PROMPTS/PHASE-C-AUTOMATION.md#f14

## What this does
Answers free-text customer queries from a per-tenant FAQ knowledge base using native PostgreSQL Full-Text Search (`tsvector`) and trigram similarity (`pg_trgm`) without any external LLM or vector database (per ADR-007). Evaluates queries with a combined confidence ranking score (60% trigram typo tolerance + 40% full-text search rank), replies when above a configurable confidence threshold, and falls back to safe escalation (`unmatched_messages`) rather than guessing when confidence is low.

## Files
| File | Purpose |
|---|---|
| `src/main/resources/db/migration/V15__faqs.sql` | `faqs` table with `pg_trgm` extension, GIN indexes on `search_vector` and `question`, and RLS |
| `src/main/java/com/example/wasaas/automation/faq/Faq.java` | FAQ entity |
| `src/main/java/com/example/wasaas/automation/faq/FaqMatchProjection.java` | Projection interface for combined ranking SQL query results |
| `src/main/java/com/example/wasaas/automation/faq/FaqRepository.java` | Repository with native combined ranking SQL query |
| `src/main/java/com/example/wasaas/automation/faq/FaqMatchResult.java` | Match result DTO with question, answer, and confidence score |
| `src/main/java/com/example/wasaas/automation/faq/FaqMatchService.java` | Service executing FAQ search with configurable confidence threshold |
| `src/main/java/com/example/wasaas/automation/AutomationEngine.java` | Hooked FAQ fallback into automation pipeline (Keyword Rules -> FAQ -> Unmatched/Escalate) |
| `src/test/java/com/example/wasaas/automation/faq/FaqMatchingTest.java` | 6 integration tests for exact match, typo tolerance, low confidence fallback, precedence, configurable threshold, and RLS |

## Database changes
- Migration: `V15__faqs.sql`
- Extension: `CREATE EXTENSION IF NOT EXISTS pg_trgm`
- Table `faqs`:
  - `id`: UUID Primary Key
  - `tenant_id`: UUID NOT NULL REFERENCES `tenants(id)`
  - `question`: TEXT NOT NULL
  - `answer`: TEXT NOT NULL
  - `enabled`: BOOLEAN NOT NULL DEFAULT true
  - `search_vector`: tsvector GENERATED ALWAYS AS (`setweight(to_tsvector('english', coalesce(question, '')), 'A') || setweight(to_tsvector('english', coalesce(answer, '')), 'B')`) STORED
  - `created_at`, `updated_at`: TIMESTAMPTZ
- Indexes:
  - `idx_faqs_search_vector`: GIN on `search_vector`
  - `idx_faqs_question_trgm`: GIN on `question gin_trgm_ops`
  - `idx_faqs_tenant_enabled`: BTree on `(tenant_id, enabled)`
- RLS Policy: `faqs_tenant_isolation`

## Combined Ranking SQL Query
```sql
SELECT
    f.id AS id,
    f.tenant_id AS tenantId,
    f.question AS question,
    f.answer AS answer,
    f.enabled AS enabled,
    similarity(f.question, :query) AS trgmScore,
    ts_rank(f.search_vector, plainto_tsquery('english', :query)) AS tsScore,
    (
        0.6 * similarity(f.question, :query) +
        0.4 * LEAST(1.0, ts_rank(f.search_vector, plainto_tsquery('english', :query)) * 2.0)
    ) AS combinedScore
FROM faqs f
WHERE f.tenant_id = :tenantId
  AND f.enabled = true
  AND (
      similarity(f.question, :query) > 0.15
      OR f.search_vector @@ plainto_tsquery('english', :query)
  )
ORDER BY combinedScore DESC
LIMIT 1;
```

## Key decisions and why
- **No LLM / Pure PostgreSQL (ADR-007):** Rather than paying for third-party LLM inference or hosting vector databases, PostgreSQL native trigram similarity provides robust typo tolerance (`"prcing"`, `"watsapp"`) and full-text search provides linguistic stemming (`"pricing"`, `"prices"`).
- **Execution Precedence in AutomationEngine:**
  1. **Keyword Rules (F13):** Exact business workflows take precedence.
  2. **FAQ Fallback (F14):** Free-text questions are matched against tenant FAQs.
  3. **Escalation / Unmatched:** Low confidence queries are recorded in `unmatched_messages` without guessing.
- **Configurable Confidence Threshold:** `app.automation.faq.confidence-threshold` (default `0.35`) separates actionable matches from vague queries.

## Divergence from the architecture docs
None.

## Test coverage
- `testExactQuestionMatchScoresHighAndReplies`: Exact question scores `1.0` and dispatches auto-reply.
- `testTypoTolerantQuestionMatchSucceeds`: Query with typos (`"wats the prcing plan for whatsapp saas?"`) matches with score `> 0.8`.
- `testLowConfidenceUnrelatedQueryDoesNotGuessAndLogsUnmatched`: Unrelated query does not reply and logs to `unmatched_messages`.
- `testKeywordRuleTakesPrecedenceOverFaq`: Keyword rule wins over FAQ match.
- `testConfigurableConfidenceThreshold`: Strict threshold suppresses reply; relaxed threshold permits reply.
- `testMultiTenantFaqIsolation`: RLS ensures tenant FAQs are completely isolated.
