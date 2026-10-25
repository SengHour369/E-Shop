-- Denormalized list metadata. Existing definitions and revision tokens stay unchanged.
ALTER TABLE ai_workflow_drafts ADD COLUMN IF NOT EXISTS module_code varchar(120);
ALTER TABLE ai_workflow_drafts ADD COLUMN IF NOT EXISTS trigger_type varchar(32);
UPDATE ai_workflow_drafts
SET module_code=COALESCE(definition::jsonb->>'module',''),
    trigger_type=COALESCE(definition::jsonb->>'triggerType','')
WHERE module_code IS NULL OR trigger_type IS NULL;
CREATE INDEX IF NOT EXISTS workflow_draft_owner_filters
ON ai_workflow_drafts(owner_id,module_code,trigger_type,updated_at DESC);
