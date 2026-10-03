CREATE TABLE IF NOT EXISTS notification_mail_deliveries (
 event_id UUID PRIMARY KEY, version BIGINT, user_id BIGINT NOT NULL, sent_at TIMESTAMPTZ NOT NULL
);
