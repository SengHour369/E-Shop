BEGIN;
CREATE TABLE IF NOT EXISTS ai_daily_quota(user_id bigint PRIMARY KEY,business_day date NOT NULL,used integer NOT NULL CHECK(used>=0));
CREATE TABLE IF NOT EXISTS ai_quota_request(id varchar(80) PRIMARY KEY,fingerprint varchar(64) NOT NULL,status varchar(20) NOT NULL,accepted_at timestamptz NOT NULL,http_status integer,response_json text);
ALTER TABLE ai_quota_request ADD COLUMN IF NOT EXISTS request_id varchar(128);
ALTER TABLE ai_quota_request ADD COLUMN IF NOT EXISTS trace_id varchar(128);
ALTER TABLE ai_quota_request ADD COLUMN IF NOT EXISTS feedback varchar(4);
CREATE INDEX IF NOT EXISTS ai_quota_request_accepted ON ai_quota_request(accepted_at);
COMMIT;


