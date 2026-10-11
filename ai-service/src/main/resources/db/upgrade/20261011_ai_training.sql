-- Apply to the existing ai_db as its migration owner. No extra database or extension.
BEGIN;
CREATE SCHEMA IF NOT EXISTS ai_training;

CREATE TABLE IF NOT EXISTS ai_training.datasets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(80) NOT NULL UNIQUE CHECK (code ~ '^[a-z0-9][a-z0-9_-]*$'),
    name VARCHAR(160) NOT NULL CHECK (length(btrim(name)) > 0),
    description TEXT NOT NULL DEFAULT '',
    created_by VARCHAR(120) NOT NULL CHECK (length(btrim(created_by)) > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS ai_training.examples (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dataset_id UUID NOT NULL REFERENCES ai_training.datasets(id) ON DELETE RESTRICT,
    language VARCHAR(2) NOT NULL CHECK (language IN ('en', 'km')),
    prompt TEXT NOT NULL CHECK (length(btrim(prompt)) BETWEEN 1 AND 20000),
    response TEXT NOT NULL CHECK (length(btrim(response)) BETWEEN 1 AND 40000),
    expected_tools JSONB NOT NULL DEFAULT '[]'::jsonb CHECK (jsonb_typeof(expected_tools) = 'array'),
    split VARCHAR(12) NOT NULL DEFAULT 'TRAIN' CHECK (split IN ('TRAIN', 'VALIDATION', 'TEST')),
    source VARCHAR(500) NOT NULL CHECK (length(btrim(source)) > 0),
    source_verified BOOLEAN NOT NULL DEFAULT false,
    consent_confirmed BOOLEAN NOT NULL DEFAULT false,
    review_status VARCHAR(12) NOT NULL DEFAULT 'DRAFT' CHECK (review_status IN ('DRAFT', 'APPROVED', 'REJECTED')),
    reviewed_by VARCHAR(120),
    reviewed_at TIMESTAMPTZ,
    review_notes TEXT NOT NULL DEFAULT '',
    created_by VARCHAR(120) NOT NULL CHECK (length(btrim(created_by)) > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    content_hash TEXT GENERATED ALWAYS AS (md5(length(prompt)::text || ':' || prompt || response)) STORED,
    UNIQUE (dataset_id, language, content_hash),
    CHECK (review_status = 'DRAFT' OR (reviewed_by IS NOT NULL AND length(btrim(reviewed_by)) > 0 AND reviewed_at IS NOT NULL)),
    CHECK (review_status <> 'APPROVED' OR (source_verified AND consent_confirmed))
);
CREATE INDEX IF NOT EXISTS examples_review ON ai_training.examples(dataset_id, review_status, split, language);

-- Editing reviewed content invalidates its approval, even through direct SQL.
CREATE OR REPLACE FUNCTION ai_training.invalidate_review() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF ROW(NEW.prompt, NEW.response, NEW.language, NEW.expected_tools, NEW.source,
           NEW.source_verified, NEW.consent_confirmed, NEW.dataset_id, NEW.split)
       IS DISTINCT FROM
       ROW(OLD.prompt, OLD.response, OLD.language, OLD.expected_tools, OLD.source,
           OLD.source_verified, OLD.consent_confirmed, OLD.dataset_id, OLD.split) THEN
        NEW.review_status := 'DRAFT';
        NEW.reviewed_by := NULL;
        NEW.reviewed_at := NULL;
        NEW.review_notes := '';
    END IF;
    NEW.updated_at := clock_timestamp();
    RETURN NEW;
END;
$$;
CREATE OR REPLACE TRIGGER examples_invalidate_review
    BEFORE UPDATE ON ai_training.examples
    FOR EACH ROW EXECUTE FUNCTION ai_training.invalidate_review();

CREATE TABLE IF NOT EXISTS ai_training.runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dataset_id UUID NOT NULL REFERENCES ai_training.datasets(id) ON DELETE RESTRICT,
    base_model VARCHAR(160) NOT NULL CHECK (length(btrim(base_model)) > 0),
    status VARCHAR(12) NOT NULL DEFAULT 'PLANNED' CHECK (status IN ('PLANNED','RUNNING','COMPLETED','FAILED','CANCELLED')),
    -- A run must identify the exact externally exported, reviewed JSONL snapshot.
    dataset_sha256 VARCHAR(64) NOT NULL CHECK (dataset_sha256 ~ '^[a-f0-9]{64}$'),
    artifact_uri TEXT,
    parameters JSONB NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(parameters) = 'object'),
    metrics JSONB NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(metrics) = 'object'),
    error_summary TEXT,
    created_by VARCHAR(120) NOT NULL CHECK (length(btrim(created_by)) > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    CHECK (status <> 'RUNNING' OR started_at IS NOT NULL),
    CHECK (status NOT IN ('COMPLETED','FAILED','CANCELLED') OR finished_at IS NOT NULL),
    CHECK (status <> 'COMPLETED' OR (started_at IS NOT NULL AND artifact_uri IS NOT NULL AND length(btrim(artifact_uri)) > 0)),
    CHECK (finished_at IS NULL OR started_at IS NULL OR finished_at >= started_at)
);
CREATE INDEX IF NOT EXISTS runs_dataset ON ai_training.runs(dataset_id, created_at DESC);

CREATE OR REPLACE VIEW ai_training.approved_examples AS
SELECT e.id, d.code AS dataset_code, e.language, e.split, e.content_hash,
       jsonb_build_object('messages', jsonb_build_array(
           jsonb_build_object('role', 'user', 'content', e.prompt),
           jsonb_build_object('role', 'assistant', 'content', e.response))) AS training_record
FROM ai_training.examples e JOIN ai_training.datasets d ON d.id = e.dataset_id
WHERE e.review_status = 'APPROVED' AND e.source_verified AND e.consent_confirmed;
COMMIT;
