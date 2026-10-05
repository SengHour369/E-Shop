-- 10000 products, each with one SKU, barcode, image, inventory row, variant, and promotion.
-- Even ids have an active 20 percent promotion. Ids divisible by 50 are out of stock.
-- Ids divisible by 100 are inactive.
BEGIN;

ALTER TABLE product_skus ADD COLUMN IF NOT EXISTS barcode varchar(64);
CREATE INDEX IF NOT EXISTS idx_product_sku_barcode ON product_skus (barcode);
CREATE UNIQUE INDEX IF NOT EXISTS uk_product_sku_barcode ON product_skus (barcode) WHERE barcode IS NOT NULL;

CREATE OR REPLACE FUNCTION seed_ean13(n integer) RETURNS varchar AS $$
DECLARE
    body text := '885' || lpad(n::text, 9, '0');
    total int := 0;
    pos int;
BEGIN
    FOR pos IN 1..12 LOOP
        total := total + substr(body, pos, 1)::int * (CASE WHEN (12 - pos) % 2 = 0 THEN 3 ELSE 1 END);
    END LOOP;
    RETURN body || ((10 - (total % 10)) % 10)::text;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

DO $$
BEGIN
    IF seed_ean13(12345678) <> '8850123456787' THEN
        RAISE EXCEPTION 'EAN-13 check digit helper failed: %', seed_ean13(12345678);
    END IF;
END $$;

INSERT INTO categories (id, created_at, updated_at, deleted, description, icon, name, status)
SELECT
    i, now(), now(), false,
    'Seed category ' || i,
    'icon-' || i,
    'Category ' || i,
    true
FROM generate_series(1, 10000) AS i;

INSERT INTO sub_categories (
    id, created_at, updated_at, deleted, description, name, status, category_id, image_id
)
SELECT
    i, now(), now(), false,
    'Seed subcategory ' || i,
    'Subcategory ' || i,
    true,
    i,
    NULL
FROM generate_series(1, 10000) AS i;

INSERT INTO products (
    id, created_at, updated_at, deleted, description, is_active, name, sub_category_id
)
SELECT
    i, now(), now(), false,
    'Seed product ' || i || ' for scanner and catalog tests.',
    i % 100 <> 0,
    (ARRAY['Nike Air Max', 'Adidas Runner', 'Puma Court', 'Sony Headphones', 'Cotton Shirt'])[1 + (i % 5)] || ' ' || i,
    i
FROM generate_series(1, 10000) AS i;

INSERT INTO images (
    id, created_at, updated_at, url_image, user_id, product_id, sub_category_id
)
SELECT
    i, now(), now(),
    'https://cdn.example/seed/' || i || '.jpg',
    ((i - 1) % 10000) + 1,
    i,
    i
FROM generate_series(1, 10000) AS i;

UPDATE sub_categories SET image_id = id;

INSERT INTO product_skus (
    id, created_at, updated_at, operator_product_attribute, description, is_default,
    price, sku, barcode, image_id, product_id
)
SELECT
    i, now(), now(), true,
    'Color Black, size ' || (40 + (i % 6)),
    true,
    (20 + (i % 100))::numeric(12, 2),
    'SEED-' || lpad(i::text, 6, '0'),
    seed_ean13(i),
    i,
    i
FROM generate_series(1, 10000) AS i;

INSERT INTO inventories (
    id, created_at, updated_at, available_quantity, is_default, last_restocked_at,
    low_stock_threshold, quantity, reserved_quantity, version, warehouse_location, product_sku_id
)
SELECT
    i, now(), now(),
    CASE WHEN i % 50 = 0 THEN 0 WHEN i % 7 = 0 THEN 5 ELSE 20 + (i % 10) END,
    true,
    now(),
    5,
    CASE WHEN i % 50 = 0 THEN 2 WHEN i % 7 = 0 THEN 6 ELSE 20 + (i % 10) END,
    CASE WHEN i % 50 = 0 THEN 2 WHEN i % 7 = 0 THEN 1 ELSE 0 END,
    0,
    'A-' || lpad((i % 20)::text, 2, '0'),
    i
FROM generate_series(1, 10000) AS i;

INSERT INTO attributes (id, name, product_sku_id)
SELECT i, 'Color', i FROM generate_series(1, 10000) AS i;

INSERT INTO attribute_values (id, attribute_id, value)
SELECT i, i, 'Black' FROM generate_series(1, 10000) AS i;

INSERT INTO variant_attributes (id, attribute_id, attribute_value_id, product_sku_id)
SELECT i, i, i, i FROM generate_series(1, 10000) AS i;

INSERT INTO promotions (
    id, created_at, updated_at, code, created_by, description, discount_type, discount_value,
    end_at, is_active, max_discount_amount, minimum_order_amount, name, priority,
    promotion_type, stackable, start_at, status, updated_by, usage_limit, usage_per_customer
)
SELECT
    i, now(), now(),
    'SEED' || lpad(i::text, 6, '0'),
    'seed',
    'Seed promotion ' || i,
    'PERCENTAGE',
    20.0000,
    now() + interval '60 days',
    i % 2 = 0,
    50.00,
    0.00,
    'Seed 20 percent ' || i,
    1,
    'PRODUCT_DISCOUNT',
    false,
    now() - interval '1 day',
    CASE WHEN i % 2 = 0 THEN 'ACTIVE' ELSE 'DRAFT' END,
    'seed',
    100000,
    5
FROM generate_series(1, 10000) AS i;

INSERT INTO promotion_skus (id, created_at, updated_at, product_sku_id, promotion_id)
SELECT i, now(), now(), i, i FROM generate_series(1, 10000) AS i;

INSERT INTO promotion_usages (
    id, discount_amount, order_id, released, used_at, user_id, promotion_id
)
SELECT
    i,
    round(((20 + (i % 100)) * 0.20)::numeric, 2),
    i,
    false,
    now(),
    i,
    i
FROM generate_series(1, 10000) AS i;

INSERT INTO checkout_reservations (
    order_id, created_at, updated_at, snapshot, status, user_id
)
SELECT
    i, now(), now(),
    '{"skuId":' || i || ',"quantity":1}',
    'CONFIRMED',
    i
FROM generate_series(1, 10000) AS i;

INSERT INTO stock_movements (
    id, created_at, updated_at, movement_type, new_quantity, performed_by,
    previous_quantity, quantity_change, remark, warehouse_location, inventory_id
)
SELECT
    i, now(), now(), 'RESTOCK',
    CASE WHEN i % 50 = 0 THEN 2 WHEN i % 7 = 0 THEN 6 ELSE 20 + (i % 10) END,
    'seed',
    0,
    CASE WHEN i % 50 = 0 THEN 2 WHEN i % 7 = 0 THEN 6 ELSE 20 + (i % 10) END,
    'Initial seed stock',
    'A-' || lpad((i % 20)::text, 2, '0'),
    i
FROM generate_series(1, 10000) AS i;

INSERT INTO category_icons (id, created_at, updated_at, name, url)
SELECT i, now(), now(), 'Category logo ' || i, 'https://cdn.example/category-logos/' || i || '.svg'
FROM generate_series(1, 200) AS i;

CREATE TABLE IF NOT EXISTS brand_logos (
    id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    created_at timestamp,
    updated_at timestamp,
    deleted_at timestamp,
    name varchar(255) NOT NULL,
    url varchar(255) NOT NULL
);

INSERT INTO brand_logos (id, created_at, updated_at, name, url)
SELECT i, now(), now(), 'Brand logo ' || i, 'https://cdn.example/brand-logos/' || i || '.svg'
FROM generate_series(1, 50) AS i;

INSERT INTO audit_logs (
    id, action, actor_id, actor_type, description, error_code, ip_address,
    new_value, occurred_at, old_value, request_id, resource_id, resource_type,
    result, service_name, trace_id, user_agent
)
SELECT
    i, 'PRODUCT_CREATE', '1', 'ADMIN', 'Seed product ' || i, NULL, '127.0.0.1',
    jsonb_build_object('sku', 'SEED-' || lpad(i::text, 6, '0'), 'barcode', seed_ean13(i)),
    now(), NULL, 'req_seed_catalog_' || i, i::text, 'PRODUCT',
    'SUCCESS', 'catalog-service', lpad(i::text, 32, '0'), 'seed-script'
FROM generate_series(1, 10000) AS i;

DROP FUNCTION seed_ean13(integer);

COMMIT;

DO $$
DECLARE r record;
BEGIN
  FOR r IN
    SELECT table_name, column_name
    FROM information_schema.columns
    WHERE table_schema = 'public' AND is_identity = 'YES'
  LOOP
    EXECUTE format(
      'SELECT setval(pg_get_serial_sequence(%L, %L), COALESCE((SELECT MAX(%I) FROM %I), 1))',
      r.table_name, r.column_name, r.column_name, r.table_name);
  END LOOP;
END $$;
