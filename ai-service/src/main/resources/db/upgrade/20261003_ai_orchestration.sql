-- Apply as migration owner before production ddl-auto=validate.
CREATE TABLE IF NOT EXISTS ai_executions (
 id UUID PRIMARY KEY, version BIGINT, user_id BIGINT NOT NULL,
 request_id VARCHAR(128) NOT NULL, trace_id VARCHAR(32), intent VARCHAR(40),
 status VARCHAR(24) NOT NULL, error_code VARCHAR(80), resource_id VARCHAR(100),
 started_at TIMESTAMPTZ NOT NULL, completed_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS ai_execution_owner_time ON ai_executions(user_id,started_at);
CREATE INDEX IF NOT EXISTS ai_execution_request ON ai_executions(request_id);
CREATE INDEX IF NOT EXISTS ai_execution_status ON ai_executions(status,started_at);
CREATE TABLE IF NOT EXISTS notification_outbox (
 id UUID PRIMARY KEY, payload TEXT NOT NULL, created_at TIMESTAMPTZ NOT NULL,
 next_attempt_at TIMESTAMPTZ NOT NULL, published_at TIMESTAMPTZ, attempts INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS outbox_pending ON notification_outbox(published_at,next_attempt_at);
