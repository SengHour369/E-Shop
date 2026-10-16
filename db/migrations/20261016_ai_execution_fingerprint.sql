-- Apply to the AI database before deploying the router changes.
-- Existing rows remain readable as history. Unfingerprinted legacy execution keys
-- cannot be replayed by the router; clients must use a fresh UUID.
ALTER TABLE ai_executions ADD COLUMN IF NOT EXISTS request_fingerprint VARCHAR(64);
