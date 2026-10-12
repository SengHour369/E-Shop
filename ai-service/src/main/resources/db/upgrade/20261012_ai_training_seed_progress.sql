-- Apply after 20261011_ai_training.sql. No existing examples or reviews are modified.
CREATE TABLE IF NOT EXISTS ai_training.seed_progress (
    dataset_id UUID PRIMARY KEY REFERENCES ai_training.datasets(id) ON DELETE RESTRICT,
    generator_version VARCHAR(80) NOT NULL,
    target_count INTEGER NOT NULL CHECK (target_count BETWEEN 2 AND 10000000 AND target_count % 2 = 0),
    next_index INTEGER NOT NULL DEFAULT 0 CHECK (next_index BETWEEN 0 AND target_count AND next_index % 2 = 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
