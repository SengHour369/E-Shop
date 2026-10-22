BEGIN;
CREATE TABLE IF NOT EXISTS live_test_runs (
 id uuid PRIMARY KEY, actor_id bigint NOT NULL, status varchar(255) NOT NULL,
 started_at timestamptz NOT NULL, finished_at timestamptz, steps_json text NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_live_test_actor_time ON live_test_runs(actor_id,started_at DESC);
COMMIT;
