-- Run once against the EXISTING order database before deploying this release.
BEGIN;
ALTER TABLE order_details ADD COLUMN checkout_key varchar(100);
ALTER TABLE order_details ADD COLUMN catalog_completed boolean NOT NULL DEFAULT true;
ALTER TABLE order_details ADD COLUMN checkout_retry_at timestamp;
ALTER TABLE order_details ADD CONSTRAINT uk_order_checkout UNIQUE(user_id,checkout_key);
CREATE INDEX ix_order_checkout_recovery ON order_details(status,catalog_completed,checkout_retry_at,id);
ALTER TABLE carts ADD COLUMN checkout_order_id bigint;
ALTER TABLE order_items ADD COLUMN base_unit_price numeric(19,2);
ALTER TABLE order_items ADD COLUMN discount_amount numeric(19,2);
ALTER TABLE order_items ADD COLUMN final_unit_price numeric(19,2);
ALTER TABLE order_items ADD COLUMN promotion_id bigint;
ALTER TABLE order_items ADD COLUMN promotion_name varchar(255);
-- Historical prices remain unchanged; old orders had no recorded promotion.
UPDATE order_items SET base_unit_price = unit_price, discount_amount = 0, final_unit_price = unit_price;
COMMIT;
