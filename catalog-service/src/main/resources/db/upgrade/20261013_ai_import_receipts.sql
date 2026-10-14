CREATE TABLE IF NOT EXISTS ai_import_receipt (
 id varchar(100) PRIMARY KEY, actor_id bigint NOT NULL, checksum varchar(255) NOT NULL,
 resource_id varchar(255), created_at timestamptz NOT NULL
);
