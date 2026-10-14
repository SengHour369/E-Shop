BEGIN;
CREATE TABLE IF NOT EXISTS catalog_branch(id bigserial PRIMARY KEY,version bigint NOT NULL DEFAULT 0,name varchar(120) NOT NULL,name_key varchar(120) UNIQUE NOT NULL,address varchar(500),active boolean NOT NULL DEFAULT true,created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS catalog_branch_access(id uuid PRIMARY KEY,branch_id bigint NOT NULL REFERENCES catalog_branch(id),user_id bigint NOT NULL,active boolean NOT NULL,updated_by bigint NOT NULL,updated_at timestamptz NOT NULL,UNIQUE(branch_id,user_id));
CREATE INDEX IF NOT EXISTS catalog_branch_access_actor ON catalog_branch_access(user_id,branch_id) WHERE active;
ALTER TABLE inventories ADD COLUMN IF NOT EXISTS branch_id bigint REFERENCES catalog_branch(id);
COMMIT;
