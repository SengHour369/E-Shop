-- Additive, read-only manual workflow execution. No domain database/state changes.
CREATE TABLE IF NOT EXISTS ai_workflow_published_versions (
 id uuid PRIMARY KEY, workflow_id uuid NOT NULL REFERENCES ai_workflow_drafts(id),
 draft_revision bigint NOT NULL, owner_id bigint NOT NULL, name varchar(120) NOT NULL,
 definition text NOT NULL, published_at timestamptz NOT NULL,
 UNIQUE(workflow_id,draft_revision)
);
CREATE TABLE IF NOT EXISTS ai_workflow_runs (
 id uuid PRIMARY KEY, revision bigint NOT NULL DEFAULT 0,
 published_version_id uuid NOT NULL REFERENCES ai_workflow_published_versions(id),
 owner_id bigint NOT NULL, status varchar(24) NOT NULL,
 request_id varchar(128) NOT NULL, trace_id varchar(128),
 started_at timestamptz NOT NULL, finished_at timestamptz
);
CREATE INDEX IF NOT EXISTS workflow_run_owner_time ON ai_workflow_runs(owner_id,started_at DESC);
CREATE TABLE IF NOT EXISTS ai_workflow_run_steps (
 id uuid PRIMARY KEY, run_id uuid NOT NULL REFERENCES ai_workflow_runs(id), position integer NOT NULL,
 step_key varchar(120) NOT NULL, name varchar(120) NOT NULL, tool varchar(80) NOT NULL,
 status varchar(24) NOT NULL, started_at timestamptz, finished_at timestamptz,
 error_code varchar(80), output_sha256 varchar(64), output_bytes integer,
 UNIQUE(run_id,position)
);
-- Never replay RUNNING steps after a crash. Explicit stale reconciliation marks them UNKNOWN.

-- Enforce immutable published snapshots even if a future repository method attempts a mutation.
CREATE OR REPLACE FUNCTION reject_ai_workflow_version_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 RAISE EXCEPTION 'Published workflow versions are immutable' USING ERRCODE = '23514';
END;
$$;
DROP TRIGGER IF EXISTS ai_workflow_version_immutable ON ai_workflow_published_versions;
CREATE TRIGGER ai_workflow_version_immutable BEFORE UPDATE OR DELETE ON ai_workflow_published_versions
 FOR EACH ROW EXECUTE FUNCTION reject_ai_workflow_version_mutation();
