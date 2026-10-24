-- Add lexical retrieval for reviewed training examples; does not approve any content.
BEGIN;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX IF NOT EXISTS examples_approved_train_trgm
 ON ai_training.examples USING gin(lower(prompt) gin_trgm_ops)
 WHERE review_status='APPROVED' AND source_verified=true AND consent_confirmed=true AND split='TRAIN';
COMMIT;
