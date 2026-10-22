BEGIN;
CREATE TABLE IF NOT EXISTS guest_mail_deliveries(event_id uuid PRIMARY KEY,order_id bigint NOT NULL,sent_at timestamptz NOT NULL,version bigint);
CREATE INDEX IF NOT EXISTS ix_guest_mail_order ON guest_mail_deliveries(order_id);
COMMIT;
