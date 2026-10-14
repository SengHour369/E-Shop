BEGIN;
CREATE TABLE IF NOT EXISTS ai_import_image(id uuid PRIMARY KEY,batch_id uuid NOT NULL REFERENCES ai_import_batch(id),row_id uuid NOT NULL REFERENCES ai_import_row(id),actor_id bigint NOT NULL,checksum varchar(64) NOT NULL,filename text NOT NULL,size_bytes integer NOT NULL CHECK(size_bytes>0 AND size_bytes<=5242880),content bytea NOT NULL,created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE INDEX IF NOT EXISTS ai_import_image_batch ON ai_import_image(batch_id,row_id);
COMMIT;
