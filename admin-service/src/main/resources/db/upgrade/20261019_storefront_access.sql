BEGIN;
CREATE TABLE IF NOT EXISTS storefront_access_settings (
 id bigint PRIMARY KEY CHECK(id=1), version bigint NOT NULL DEFAULT 0,
 guest_browsing boolean NOT NULL DEFAULT true, guest_reviews boolean NOT NULL DEFAULT false,
 guest_cart boolean NOT NULL DEFAULT false, guest_checkout boolean NOT NULL DEFAULT false,
 guest_payment boolean NOT NULL DEFAULT false, require_delivery_address boolean NOT NULL DEFAULT true,
 require_guest_contact_verification boolean NOT NULL DEFAULT false, guest_order_tracking boolean NOT NULL DEFAULT false,
 updated_at timestamptz NOT NULL DEFAULT now(), updated_by bigint,
 CONSTRAINT ck_guest_payment_checkout CHECK (NOT guest_payment OR guest_checkout),
 CONSTRAINT ck_guest_checkout_cart CHECK (NOT guest_checkout OR guest_cart),
 CONSTRAINT ck_physical_delivery_address CHECK (require_delivery_address)
);
INSERT INTO storefront_access_settings(id) VALUES(1) ON CONFLICT(id) DO NOTHING;
COMMIT;
