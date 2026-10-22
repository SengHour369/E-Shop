-- Draft authoring foundation. Does not enable workflow execution.
CREATE TABLE IF NOT EXISTS ai_workflow_drafts (
 id uuid PRIMARY KEY,
 revision bigint NOT NULL DEFAULT 0,
 owner_id bigint NOT NULL,
 name varchar(120) NOT NULL,
 definition text NOT NULL,
 updated_at timestamp with time zone NOT NULL
);
CREATE INDEX IF NOT EXISTS ai_workflow_draft_owner_time ON ai_workflow_drafts(owner_id, updated_at DESC);
