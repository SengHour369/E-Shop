-- Durable triggers and jobs. No session credentials are stored in these tables.
CREATE TABLE IF NOT EXISTS ai_workflow_triggers (
 id uuid PRIMARY KEY, revision bigint, workflow_id uuid NOT NULL UNIQUE,
 version_id uuid NOT NULL, owner_id bigint NOT NULL, trigger_type varchar(16) NOT NULL,
 state varchar(32) NOT NULL, grant_id uuid NOT NULL, authorization_expires_at timestamptz NOT NULL,
 activated_at timestamptz NOT NULL, next_run_at timestamptz, interval_minutes integer,
 event_type varchar(40), event_name varchar(120), order_id bigint,
 last_run_at timestamptz, last_status varchar(32)
);
CREATE INDEX IF NOT EXISTS workflow_trigger_due ON ai_workflow_triggers(state,next_run_at);
CREATE INDEX IF NOT EXISTS workflow_trigger_events ON ai_workflow_triggers(state,trigger_type,event_type);
CREATE TABLE IF NOT EXISTS ai_workflow_trigger_jobs (
 id uuid PRIMARY KEY, revision bigint, trigger_id uuid NOT NULL, version_id uuid NOT NULL,
 owner_id bigint NOT NULL, status varchar(24) NOT NULL, source varchar(40) NOT NULL,
 event_key uuid NOT NULL, created_at timestamptz NOT NULL, started_at timestamptz,
 finished_at timestamptz, execution_id uuid, error_code varchar(80), request_id varchar(128)
);
CREATE INDEX IF NOT EXISTS workflow_job_queue ON ai_workflow_trigger_jobs(status,created_at);
CREATE TABLE IF NOT EXISTS ai_workflow_received_events (
 id uuid PRIMARY KEY, received_at timestamptz NOT NULL, event_type varchar(40) NOT NULL
);
