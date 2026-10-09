-- Optional: install pgvector on the existing ai_db server before applying.
-- Apply with the migration owner; the application does not install extensions.
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS ai_knowledge_chunks (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    title VARCHAR(160) NOT NULL,
    language VARCHAR(2) NOT NULL CHECK (language IN ('en', 'km')),
    content TEXT NOT NULL,
    embedding_model VARCHAR(160) NOT NULL,
    embedding vector NOT NULL,
    approved BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS ai_knowledge_document ON ai_knowledge_chunks(document_id);
CREATE INDEX IF NOT EXISTS ai_knowledge_model ON ai_knowledge_chunks(embedding_model, approved);
