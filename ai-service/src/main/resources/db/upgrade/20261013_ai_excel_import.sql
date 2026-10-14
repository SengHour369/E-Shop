-- Run against ai_db before deploying with DDL_AUTO=validate.
BEGIN;
CREATE TABLE IF NOT EXISTS ai_import_batch (
 id uuid PRIMARY KEY,
 lock_version bigint NOT NULL,
 owner_id bigint NOT NULL,
 revision integer NOT NULL,
 status text,
 checksum text NOT NULL UNIQUE,
 validated_revision integer,
 approved_revision integer,
 reviewer_id bigint,
 assigned_reviewer_id bigint,
 last_editor_id bigint,
 request_id text,
 trace_id text,
 created_at timestamptz
);
CREATE TABLE IF NOT EXISTS ai_import_sheet (
 id uuid PRIMARY KEY,
 batch_id uuid NOT NULL REFERENCES ai_import_batch(id),
 name text,
 tool text,
 headers text,
 confirmed boolean
);
CREATE INDEX IF NOT EXISTS ai_import_sheet_batch ON ai_import_sheet(batch_id);
CREATE TABLE IF NOT EXISTS ai_import_approval (
 id uuid PRIMARY KEY,
 batch_id uuid NOT NULL REFERENCES ai_import_batch(id),
 revision integer NOT NULL,
 actor_id bigint,
 decision text,
 reason text,
 created_at timestamptz
);
CREATE INDEX IF NOT EXISTS ai_import_approval_batch ON ai_import_approval(batch_id);
CREATE TABLE IF NOT EXISTS ai_import_audit_history (
 id uuid PRIMARY KEY,
 batch_id uuid NOT NULL REFERENCES ai_import_batch(id),
 revision integer NOT NULL,
 actor_id bigint,
 action text,
 request_id text,
 trace_id text,
 details text,
 created_at timestamptz
);
CREATE INDEX IF NOT EXISTS ai_import_audit_history_batch ON ai_import_audit_history(batch_id);
CREATE TABLE IF NOT EXISTS ai_import_column_mapping (
 id uuid PRIMARY KEY,
 batch_id uuid NOT NULL REFERENCES ai_import_batch(id),
 sheet_id uuid NOT NULL REFERENCES ai_import_sheet(id),
 suggestions text,
 confirmed_values text
);
CREATE INDEX IF NOT EXISTS ai_import_column_mapping_batch ON ai_import_column_mapping(batch_id);
CREATE TABLE IF NOT EXISTS ai_import_execution (
 id uuid PRIMARY KEY,
 batch_id uuid NOT NULL REFERENCES ai_import_batch(id),
 revision integer NOT NULL,
 status text,
 started_at timestamptz,
 completed_at timestamptz
);
CREATE INDEX IF NOT EXISTS ai_import_execution_batch ON ai_import_execution(batch_id);
CREATE TABLE IF NOT EXISTS ai_import_file (
 id uuid PRIMARY KEY,
 batch_id uuid NOT NULL REFERENCES ai_import_batch(id),
 filename text,
 content bytea
);
CREATE INDEX IF NOT EXISTS ai_import_file_batch ON ai_import_file(batch_id);
CREATE TABLE IF NOT EXISTS ai_import_revision (
 id uuid PRIMARY KEY,
 batch_id uuid NOT NULL REFERENCES ai_import_batch(id),
 revision integer NOT NULL,
 snapshot text,
 created_at timestamptz
);
CREATE INDEX IF NOT EXISTS ai_import_revision_batch ON ai_import_revision(batch_id);
CREATE TABLE IF NOT EXISTS ai_import_row (
 id uuid PRIMARY KEY,
 batch_id uuid NOT NULL REFERENCES ai_import_batch(id),
 sheet_id uuid NOT NULL REFERENCES ai_import_sheet(id),
 row_number integer,
 original_values text,
 values_json text,
 status text,
 edited boolean,
 skipped boolean NOT NULL DEFAULT false,
 resource_id text,
 affected_resource_id text
);
CREATE INDEX IF NOT EXISTS ai_import_row_batch ON ai_import_row(batch_id);
CREATE TABLE IF NOT EXISTS ai_import_row_execution (
 id uuid PRIMARY KEY,
 batch_id uuid NOT NULL REFERENCES ai_import_batch(id),
 execution_id uuid NOT NULL REFERENCES ai_import_execution(id),
 row_id uuid NOT NULL REFERENCES ai_import_row(id),
 idempotency_key text,
 status text,
 resource_id text,
 error_code text,
 created_at timestamptz
);
CREATE INDEX IF NOT EXISTS ai_import_row_execution_batch ON ai_import_row_execution(batch_id);
CREATE TABLE IF NOT EXISTS ai_import_validation_error (
 id uuid PRIMARY KEY,
 batch_id uuid NOT NULL REFERENCES ai_import_batch(id),
 row_id uuid NOT NULL REFERENCES ai_import_row(id),
 message text,
 explanation text
);
CREATE INDEX IF NOT EXISTS ai_import_validation_error_batch ON ai_import_validation_error(batch_id);
CREATE INDEX IF NOT EXISTS ai_import_batch_owner ON ai_import_batch(owner_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ai_import_row_filter ON ai_import_row(batch_id,status,row_number);
COMMIT;
