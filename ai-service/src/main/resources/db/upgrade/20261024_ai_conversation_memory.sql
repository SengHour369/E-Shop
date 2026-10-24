-- Durable account-scoped text memory. No automatic expiry and no raw audio.
BEGIN;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE TABLE IF NOT EXISTS ai_memory_conversation (
 id uuid NOT NULL, actor_id bigint NOT NULL, scope varchar(8) NOT NULL CHECK(scope IN ('ADMIN','CUSTOMER')),
 created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY(id,actor_id,scope)
);
CREATE TABLE IF NOT EXISTS ai_memory_turn (
 id bigserial PRIMARY KEY, conversation_id uuid NOT NULL, actor_id bigint NOT NULL, scope varchar(8) NOT NULL,
 event_id varchar(160) NOT NULL, role varchar(9) NOT NULL CHECK(role IN ('user','assistant')),
 channel varchar(4) NOT NULL CHECK(channel IN ('CHAT','CALL')), content varchar(4000) NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now(),
 FOREIGN KEY(conversation_id,actor_id,scope) REFERENCES ai_memory_conversation(id,actor_id,scope) ON DELETE CASCADE,
 UNIQUE(conversation_id,actor_id,scope,event_id,role)
);
CREATE INDEX IF NOT EXISTS ai_memory_owner_recent ON ai_memory_turn(actor_id,scope,id DESC);
CREATE INDEX IF NOT EXISTS ai_memory_conversation_recent ON ai_memory_turn(conversation_id,actor_id,scope,id DESC);
CREATE INDEX IF NOT EXISTS ai_memory_text_search ON ai_memory_turn USING gin(lower(content) gin_trgm_ops);
COMMIT;
