# ADR-007 — No AI/LLM in the Core Automation Path

**Status:** Accepted · 18 August 2026

## Context
The obvious instinct for "WhatsApp automation" in 2026 is to route every inbound message to
an LLM. Free tiers exist (Gemini Flash ~15 RPM / ~1,500 RPD; Groq 30 RPM / ~14,400 req/day;
Cerebras ~1M tokens/day, all verified 18 Aug 2026).

## Decision
Core automation is **deterministic**: keyword/pattern rules, PostgreSQL full-text search plus
`pg_trgm` trigram matching for FAQ, WhatsApp interactive button and list messages, and
scheduled templates. Below a confidence threshold, **escalate to a human — never guess**.

AI may later be added as an **assistive, human-in-the-loop** feature (conversation
summarisation, suggested replies, intent classification for unmatched messages). Never
autonomous, never in the reply path without approval.

## Why
Note that cost is **not** the reason. A Flash-class call is a fraction of a rupee; a single
marketing message is ₹0.8631. AI would be the smallest line item in this product. The real
reasons:

1. **Privacy / legal.** Google's *free* Gemini tier permits use of prompts for model
   training. We would be feeding our customers' customers' conversations — names, phone
   numbers, order details — into a training corpus. For a B2B product under India's DPDP Act
   that is a contractual problem, not an optics one.
2. **Reliability.** Free tiers have no SLA, are deprioritised under load, and change quotas
   without notice (Gemini cut its free quota sharply in Dec 2025). Our automation is
   customer-facing and must not depend on that.
3. **Correctness.** A hallucinated price, stock level, or appointment time sent over WhatsApp
   to a real end-customer damages our customer's business. Deterministic rules cannot invent
   a price.
4. **Debuggability.** "Why did it reply that?" has a precise answer with a rules table. With
   an LLM it's a shrug.
5. **Latency variance.** Rules are sub-millisecond. LLM calls are seconds, sometimes tens.
6. Self-hosting a model on the app VM is not viable: 2 OCPU ARM yields ~5–8 tokens/sec on a
   quantised 7B model, and it would contend with the app and database for CPU on our one box.

## Alternatives considered
| Option | Rejected because |
|---|---|
| LLM for every inbound message | Privacy, reliability, hallucination risk, latency |
| Free-tier LLM with PII stripped | Stripping PII reliably from free-text is itself an unsolved problem |
| Paid LLM (Vertex/Gemini paid) in the reply path | Solves privacy and SLA, but not hallucination or debuggability. Reconsider for *assistive* use only. |
| Self-hosted small model | Too slow on our hardware; competes with app and DB for CPU |
| Embeddings + vector DB for FAQ | Postgres full-text + trigram is sufficient for a per-tenant FAQ of tens of entries, with zero new infrastructure |

## Consequences
**Positive:** predictable, private, debuggable, free, no vendor dependency.
**Negative:** cannot handle genuinely open-ended queries. Some inbound messages will escalate
that an LLM might have answered.

**Mitigation and the path forward:** from increment F14 onward, **log every unmatched
message** with the best candidate and its score. That dataset is the only honest answer to
"do we actually need AI?" — and if we ever do, it's also our evaluation set.

## When we would revisit
- The unmatched-message log shows a large, genuinely open-ended volume that customers complain about
- We can use a paid tier with contractual exclusion of training on our data
- The use case is assistive (summaries, suggested replies with human approval), not autonomous

Even then: AI is a feature, not the architecture.
