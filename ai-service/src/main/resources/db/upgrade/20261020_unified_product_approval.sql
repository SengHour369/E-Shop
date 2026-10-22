BEGIN;
ALTER TABLE ai_import_batch ADD COLUMN IF NOT EXISTS source_type varchar(40) NOT NULL DEFAULT 'EXCEL_IMPORT';
ALTER TABLE ai_import_batch ADD COLUMN IF NOT EXISTS filename varchar(255);
ALTER TABLE ai_import_batch ADD COLUMN IF NOT EXISTS entity_type varchar(40);
ALTER TABLE ai_import_batch ADD COLUMN IF NOT EXISTS entity_id varchar(100);
ALTER TABLE ai_import_batch ADD COLUMN IF NOT EXISTS submitted_at timestamptz;
ALTER TABLE ai_import_batch ADD COLUMN IF NOT EXISTS reviewed_at timestamptz;
ALTER TABLE ai_import_row ADD COLUMN IF NOT EXISTS current_values text;
CREATE INDEX IF NOT EXISTS import_owner_status ON ai_import_batch(owner_id,status,created_at DESC);
CREATE INDEX IF NOT EXISTS import_reviewer_status ON ai_import_batch(assigned_reviewer_id,status,created_at DESC);

-- Preserve metadata from real stored files/history; never invent sample people/dates.
UPDATE ai_import_batch b SET filename=left(f.filename,255)
FROM ai_import_file f WHERE f.batch_id=b.id AND b.filename IS NULL;
UPDATE ai_import_batch b SET source_type='AI_ASSISTANT'
FROM ai_import_file f WHERE f.batch_id=b.id AND f.filename='assistant-draft.xlsx' AND b.source_type='EXCEL_IMPORT';
WITH types AS (
 SELECT batch_id,CASE WHEN count(DISTINCT CASE WHEN tool LIKE 'PRODUCT_SKU_%' THEN 'PRODUCT_SKU' ELSE split_part(tool,'_BULK',1) END)=1
 THEN min(CASE WHEN tool LIKE 'PRODUCT_SKU_%' THEN 'PRODUCT_SKU' ELSE split_part(tool,'_BULK',1) END) ELSE 'MULTIPLE' END AS entity_type
 FROM ai_import_sheet WHERE tool IS NOT NULL GROUP BY batch_id
) UPDATE ai_import_batch b SET entity_type=t.entity_type FROM types t WHERE b.id=t.batch_id AND b.entity_type IS NULL;
UPDATE ai_import_batch b SET submitted_at=h.submitted_at
FROM (SELECT batch_id,max(created_at) AS submitted_at FROM ai_import_audit_history WHERE action='SUBMITTED' GROUP BY batch_id) h
WHERE b.id=h.batch_id AND b.submitted_at IS NULL;
UPDATE ai_import_batch b SET reviewed_at=h.reviewed_at
FROM (SELECT batch_id,max(created_at) AS reviewed_at FROM ai_import_approval GROUP BY batch_id) h
WHERE b.id=h.batch_id AND b.reviewed_at IS NULL;
COMMIT;
