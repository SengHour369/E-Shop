CREATE TABLE IF NOT EXISTS gateway_routes (
    id BIGSERIAL PRIMARY KEY,
    route_key VARCHAR(100) NOT NULL UNIQUE,
    uri VARCHAR(500) NOT NULL,
    path_pattern VARCHAR(500) NOT NULL,
    http_method VARCHAR(16),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    rate_limit INTEGER,
    rate_limit_window_seconds INTEGER,
    sort INTEGER NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS gateway_request_logs (
    id BIGSERIAL PRIMARY KEY,
    correlation_id VARCHAR(128) NOT NULL,
    method VARCHAR(16) NOT NULL,
    path VARCHAR(1000) NOT NULL,
    response_status INTEGER,
    duration_ms BIGINT NOT NULL,
    client_ip VARCHAR(64),
    sort INTEGER NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE gateway_routes
    ADD COLUMN IF NOT EXISTS sort INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE gateway_request_logs
    ADD COLUMN IF NOT EXISTS sort INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_gateway_request_logs_correlation
    ON gateway_request_logs (correlation_id);
