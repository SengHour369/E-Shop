BEGIN;
CREATE TABLE IF NOT EXISTS guest_sessions (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, token_hash varchar(64) NOT NULL UNIQUE, csrf_hash varchar(64) NOT NULL,
 created_at timestamptz NOT NULL, expires_at timestamptz NOT NULL, rotated_at timestamptz NOT NULL,
 contact_verified_at timestamptz, verified_email varchar(254)
);
CREATE INDEX IF NOT EXISTS ix_guest_expiry ON guest_sessions(expires_at);
ALTER TABLE carts ADD COLUMN IF NOT EXISTS guest_session_id uuid REFERENCES guest_sessions(id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_guest_cart ON carts(guest_session_id) WHERE guest_session_id IS NOT NULL;
ALTER TABLE carts DROP CONSTRAINT IF EXISTS ck_cart_owner;
ALTER TABLE carts ADD CONSTRAINT ck_cart_owner CHECK ((user_id IS NOT NULL) <> (guest_session_id IS NOT NULL));
ALTER TABLE order_details ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE order_details ADD COLUMN IF NOT EXISTS guest_session_id uuid REFERENCES guest_sessions(id);
ALTER TABLE order_details ADD COLUMN IF NOT EXISTS shipping_address_snapshot text;
ALTER TABLE order_details ADD COLUMN IF NOT EXISTS verified_contact_email varchar(254);
CREATE UNIQUE INDEX IF NOT EXISTS uk_guest_checkout ON order_details(guest_session_id,checkout_key) WHERE guest_session_id IS NOT NULL;
ALTER TABLE order_details DROP CONSTRAINT IF EXISTS ck_order_owner;
ALTER TABLE order_details ADD CONSTRAINT ck_order_owner CHECK ((user_id IS NOT NULL) <> (guest_session_id IS NOT NULL));
ALTER TABLE order_details DROP CONSTRAINT IF EXISTS ck_guest_order_address;
ALTER TABLE order_details ADD CONSTRAINT ck_guest_order_address CHECK (guest_session_id IS NULL OR shipping_address_snapshot IS NOT NULL);
CREATE INDEX IF NOT EXISTS ix_guest_order_history ON order_details(guest_session_id,id DESC);
CREATE TABLE IF NOT EXISTS guest_order_access (
 id uuid PRIMARY KEY, order_id bigint NOT NULL REFERENCES order_details(id),
 guest_session_id uuid NOT NULL REFERENCES guest_sessions(id), token_hash varchar(64) NOT NULL UNIQUE,
 expires_at timestamptz NOT NULL, UNIQUE(order_id,guest_session_id)
);
CREATE INDEX IF NOT EXISTS ix_guest_order_access_expiry ON guest_order_access(expires_at);
COMMIT;
