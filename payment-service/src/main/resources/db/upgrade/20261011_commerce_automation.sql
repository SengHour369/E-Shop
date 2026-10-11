CREATE TABLE IF NOT EXISTS automation_events (
    event_id UUID NOT NULL, direction VARCHAR(10) NOT NULL, topic VARCHAR(120) NOT NULL,
    order_id BIGINT NOT NULL, event_type VARCHAR(40) NOT NULL, payload TEXT NOT NULL,
    state VARCHAR(20) NOT NULL DEFAULT 'PENDING', attempts INTEGER NOT NULL DEFAULT 0,
    request_id VARCHAR(128) NOT NULL, trace_id VARCHAR(32), last_error VARCHAR(120),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE, dead_published BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY(event_id,direction), CHECK(direction IN ('INBOX','OUTBOX'))
);
CREATE INDEX IF NOT EXISTS automation_due ON automation_events(state,next_attempt_at);
CREATE INDEX IF NOT EXISTS automation_order ON automation_events(order_id,created_at);
CREATE TABLE IF NOT EXISTS automation_attempts (
    id BIGSERIAL PRIMARY KEY, event_id UUID NOT NULL, direction VARCHAR(10) NOT NULL,
    outcome VARCHAR(20) NOT NULL, reason VARCHAR(120), request_id VARCHAR(128) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS automation_reviews (
    id BIGSERIAL PRIMARY KEY, order_id BIGINT NOT NULL, code VARCHAR(120) NOT NULL,
    state VARCHAR(20) NOT NULL DEFAULT 'OPEN', summary VARCHAR(1000) NOT NULL,
    request_id VARCHAR(128) NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE payment_transactions ADD COLUMN IF NOT EXISTS qr_payload VARCHAR(4000);
ALTER TABLE payment_transactions ADD COLUMN IF NOT EXISTS qr_md5 VARCHAR(32);
ALTER TABLE payment_transactions ADD COLUMN IF NOT EXISTS recipient_account VARCHAR(120);
ALTER TABLE payment_transactions ADD COLUMN IF NOT EXISTS provider_hash VARCHAR(64);
ALTER TABLE payment_transactions ADD COLUMN IF NOT EXISTS verified_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE payment_transactions ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE payment_transactions ADD COLUMN IF NOT EXISTS next_verification_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE payment_transactions ADD COLUMN IF NOT EXISTS verification_attempts INTEGER NOT NULL DEFAULT 0;
CREATE UNIQUE INDEX IF NOT EXISTS payment_qr_unique ON payment_transactions(qr_md5) WHERE qr_md5 IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS payment_provider_hash_unique ON payment_transactions(provider_hash) WHERE provider_hash IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS payment_order_qr_unique ON payment_transactions(order_id) WHERE qr_md5 IS NOT NULL;
CREATE INDEX IF NOT EXISTS payment_verification_due ON payment_transactions(status,next_verification_at) WHERE qr_md5 IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS automation_open_review ON automation_reviews(order_id,code) WHERE state='OPEN';
