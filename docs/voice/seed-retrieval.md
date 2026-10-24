# Using reviewed seed examples in runtime chat

The running AI database previously had no `ai_training` tables. Training seeding was disabled, and neither conversational replies nor knowledge search read that dataset. Saving examples alone does not train Gemini.

## Changes

- Applied training schema, resumable seed-progress and lexical-retrieval migrations to the existing `ai_db`.
- Loaded the original 12 bilingual seed examples and the bounded 100,000-example generated corpus. The seeder remains resumable/idempotent; no 10-million-row run was enabled.
- Added `AiTrainingExampleService`, using PostgreSQL `pg_trgm` to retrieve at most three related **approved TRAIN** examples with verified source and consent metadata. Validation/test splits, drafts, rejected examples and other-language records are excluded. Khmer text is detected even if the UI language defaults to English. Approval revocation takes effect on the next lookup.
- Conversational replies include these bounded examples as separate reference data alongside the current request. Examples are kept out of system instructions and cannot supply current stock, prices, permissions or payment outcomes.
- `KNOWLEDGE_SEARCH` can return reviewed seed guidance without the optional vector/embedding subsystem. Voice exposes it as `get_shop_guidance`; admin voice also retains all its existing permitted tools. Live business facts still require the authoritative service APIs.
- Enabled `AI_TRAINING_SEED_ENABLED=true`, `AI_TRAINING_SEED_TARGET=100000` and `AI_TRAINING_RETRIEVAL_ENABLED=true` in the ignored local environment and mapped them through Compose. Defaults remain opt-in. `AI_KNOWLEDGE_ENABLED=false` is intentional for the separate vector-document subsystem; seed retrieval does not require it or paid embeddings.

## Current data and review scope

100,012 examples are stored: **8 approved, 100,004 draft**. Only eight small repository-owned synthetic TRAIN examples were agent-reviewed for runtime use: bilingual greetings, current-stock lookup guidance, provider payment-verification guidance and owned-order lookup guidance. Their review metadata explicitly identifies an agent review, not a human review. Their provenance/consent flags record that they are internally generated and contain no human/customer data.

The large generated corpus and all validation/test examples were not bulk-approved. Further material needs a genuine content/provenance review before becoming approved runtime context. No new model weights, fine-tuning job, billing change or automated business mutation was created.

## Verification

- Inference suite: **127 passed**. Tests cover reference placement outside system instructions and rejection of excessive or role-forged context.
- AI-service suite: **214 tests, 191 passed and 23 skipped**, zero failures/errors. Shared library: **29 tests, 24 passed and 5 skipped**.
- Retrieval integration tests ran against the actual PostgreSQL database in rolled-back transactions with unique synthetic datasets. They verified review flags, TRAIN split, language selection and immediate exclusion after revocation.
- Java HTTP integration verified that approved examples reach the inference conversation request without replacing its current message/admin mode.
- Runtime database checks verified all 100,000 generator rows were committed and the eight reviewed examples were searchable.
- A real HTTPS guest chat request, `Is this product in stock?`, returned HTTP 200/SUCCESS and asked for the product and variant before checking current stock. It did not invent stock. This is one live acceptance example, not a broad answer-quality benchmark.

Migrations: `20261011_ai_training.sql`, `20261012_ai_training_seed_progress.sql`, and `20261024_ai_training_retrieval.sql` under `ai-service/src/main/resources/db/upgrade/`. Apply them before enabling seeding on a fresh database. Local verification reports are in ignored `reports/seed-retrieval-*`.
