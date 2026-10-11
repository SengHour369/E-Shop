# AI training data storage

The existing AI PostgreSQL database `ai_db` now owns an `ai_training` schema. It is separate from live order, payment, inventory and knowledge tables. No additional database server or pgvector extension is needed.

## Tables

- `datasets`: named collections of training examples.
- `examples`: English/Khmer prompt and answer pairs, expected tools, TRAIN/VALIDATION/TEST split, source, consent and review metadata. Duplicate prompt/answer pairs within a dataset and language are rejected.
- `runs`: training job records, base model, parameters, metrics, output artifact, timestamps, and SHA-256 of the exact dataset export.
- `approved_examples` view: only reviewed, approved examples with verified sources and confirmed consent; exposes a `training_record` JSON object suitable for chat JSONL export.

This is dataset storage, not a trainer, inference integration, REST API or admin screen. Ollama is not retrained by creating tables or inserting rows. An external training pipeline must consume a reviewed export and produce a compatible model artifact. No automatic collection of conversations or customer/payment data is enabled. Review all synthetic examples before approval, and verify allowed tool names against the existing registry before training or execution.

## Apply to local PostgreSQL

Follow the project's manual migration convention. From the repository root in PowerShell:

```powershell
Get-Content -Raw -Encoding utf8 ai-service/src/main/resources/db/upgrade/20261011_ai_training.sql | docker exec -i e-shop-ai-db-1 psql -U eshop -d ai_db -v ON_ERROR_STOP=1
Get-Content -Raw -Encoding utf8 db/seed/ai_training_seed.sql | docker exec -i e-shop-ai-db-1 psql -U eshop -d ai_db -v ON_ERROR_STOP=1
Get-Content -Raw -Encoding utf8 ai-service/src/test/resources/db/ai_training_tests.sql | docker exec -i e-shop-ai-db-1 psql -U eshop -d ai_db -v ON_ERROR_STOP=1
```

Migration and seed can be applied repeatedly. Tests run in a transaction that rolls back all test records. Use a migration owner for DDL and an authorized admin workflow for review. Do not expose PostgreSQL directly to customer clients. Use existing JWT and live permissions if REST administration is added later; database reviewer text alone is not authentication.

## Example: add an example

```sql
INSERT INTO ai_training.examples
(dataset_id, language, prompt, response, source, created_by, split)
SELECT id, 'en', 'How can I track an order?',
       'Sign in and select your order to check its current status.',
       'internal:approved-order-help', 'admin-user-id', 'TRAIN'
FROM ai_training.datasets WHERE code = 'eshop-assistant-v1'
RETURNING id, review_status;
```

The result is a UUID and `DRAFT`. Only after source, consent, privacy and answer-quality review:

```sql
-- First record source verification and consent. This resets prior approval.
UPDATE ai_training.examples
SET source_verified = true, consent_confirmed = true
WHERE id = '<example-uuid>';
-- Then record the authorized human reviewer.
UPDATE ai_training.examples
SET review_status = 'APPROVED', reviewed_by = '<reviewer-user-id>',
    reviewed_at = now(), review_notes = 'Checked source, privacy, and answer.'
WHERE id = '<example-uuid>';
```

Changing the content, source, consent, language, expected tools, dataset or split resets the example to DRAFT and removes the prior approval. Training data must never authorize actions, confirm payment, or override deterministic business services.

## Export

```sql
SELECT training_record::text
FROM ai_training.approved_examples
WHERE dataset_code = 'eshop-assistant-v1' AND split = 'TRAIN'
ORDER BY id;
```

Use `psql -At` to write one JSON object per line. Export VALIDATION and TEST separately. Keep the exported files immutable, compute their SHA-256, and record the training input hash in `runs` before launching any separate training job. The approved view is live, not a versioned snapshot; the external export is the reproducible artifact.

Example JSONL record:

```json
{"messages":[{"role":"user","content":"Hello"},{"role":"assistant","content":"Hello! I can help you find products and review your E-Shop orders."}]}
```

The seed contains 12 synthetic examples (6 English, 6 Khmer), all DRAFT. Consequently the approved export is initially empty. Synthetic examples include payment verification safeguards and do not contain real customer records or credentials.
