-- 10000 carts, orders, items, cancellations, returns, and refunds.
-- user_id, product_sku_id, and payment_id match the auth, catalog, and payment seed ids.
BEGIN;

INSERT INTO carts (
    id, created_at, updated_at, checkout_order_id, deleted, total_items, total_price, user_id
)
SELECT
    i, now(), now(), i, false, 1,
    CASE WHEN i % 2 = 0 THEN round((20 + (i % 100)) * 0.80, 2) ELSE (20 + (i % 100))::numeric(12, 2) END,
    i
FROM generate_series(1, 10000) AS i;

INSERT INTO cart_items (
    id, created_at, updated_at, product_sku_id, quantity, total_price, cart_id
)
SELECT
    i, now(), now(), i, 1,
    CASE WHEN i % 2 = 0 THEN round((20 + (i % 100)) * 0.80, 2) ELSE (20 + (i % 100))::numeric(12, 2) END,
    i
FROM generate_series(1, 10000) AS i;

INSERT INTO order_details (
    id, created_at, updated_at, catalog_completed, checkout_key, deleted, order_date,
    order_number, payment_id, shipping_address_id, status, total_amount, user_id
)
SELECT
    i, now(), now(), true,
    'chk-' || i,
    false,
    now() - (i || ' minutes')::interval,
    'ORD-' || lpad(i::text, 8, '0'),
    i,
    i,
    CASE WHEN i % 20 = 0 THEN 'REFUNDED' WHEN i % 25 = 0 THEN 'PENDING' ELSE 'DELIVERED' END,
    CASE WHEN i % 2 = 0 THEN round((20 + (i % 100)) * 0.80, 2) ELSE (20 + (i % 100))::numeric(12, 2) END,
    i
FROM generate_series(1, 10000) AS i;

INSERT INTO order_items (
    id, created_at, updated_at, base_unit_price, discount_amount, final_unit_price,
    product_sku_id, promotion_id, promotion_name, quantity, total_price, unit_price, order_detail_id
)
SELECT
    i, now(), now(),
    (20 + (i % 100))::numeric(12, 2),
    CASE WHEN i % 2 = 0 THEN round((20 + (i % 100)) * 0.20, 2) ELSE 0 END,
    CASE WHEN i % 2 = 0 THEN round((20 + (i % 100)) * 0.80, 2) ELSE (20 + (i % 100))::numeric(12, 2) END,
    i,
    CASE WHEN i % 2 = 0 THEN i ELSE NULL END,
    CASE WHEN i % 2 = 0 THEN 'Seed 20 percent ' || i ELSE NULL END,
    1,
    CASE WHEN i % 2 = 0 THEN round((20 + (i % 100)) * 0.80, 2) ELSE (20 + (i % 100))::numeric(12, 2) END,
    CASE WHEN i % 2 = 0 THEN round((20 + (i % 100)) * 0.80, 2) ELSE (20 + (i % 100))::numeric(12, 2) END,
    i
FROM generate_series(1, 10000) AS i;

INSERT INTO tbl_order_cancelation (
    id, created_at, updated_at, amount, cancel_date, cancel_reason, cancel_source, cancel_status,
    cancelation_id, created_by, currency, customer_id, customer_name, order_id, order_no,
    remark, reviewed_at, reviewed_by, updated_by
)
SELECT
    i, now(), now(),
    CASE WHEN i % 2 = 0 THEN round((20 + (i % 100)) * 0.80, 2) ELSE (20 + (i % 100))::numeric(12, 2) END,
    now(),
    'Seed historical cancellation',
    'CUSTOMER',
    'REJECTED',
    'CAN-' || lpad(i::text, 8, '0'),
    'seed',
    'USD',
    i,
    'Seed User ' || i,
    i,
    'ORD-' || lpad(i::text, 8, '0'),
    'Closed seed cancellation. The order itself stays delivered unless marked refunded.',
    now(),
    'seed',
    'seed'
FROM generate_series(1, 10000) AS i;

INSERT INTO tbl_return_request (
    id, created_at, updated_at, amount, approved_at, approved_by, completed_at, completed_by,
    created_by, customer_id, inspected_at, inspected_by, order_id, product_id, reason,
    received_at, received_by, requested_at, requested_by, return_id, return_type, status, updated_by
)
SELECT
    i, now(), now(),
    CASE WHEN i % 2 = 0 THEN round((20 + (i % 100)) * 0.80, 2) ELSE (20 + (i % 100))::numeric(12, 2) END,
    now(), 'seed', now(), 'seed', 'seed', i, now(), 'seed', i, i,
    'Seed return for order ' || i,
    now(), 'seed', now(), 'user' || lpad(i::text, 5, '0'),
    'RET-' || lpad(i::text, 8, '0'),
    'REFUND',
    'COMPLETED',
    'seed'
FROM generate_series(1, 10000) AS i;

INSERT INTO return_status_history (
    id, changed_at, changed_by, new_status, old_status, remark, return_id
)
SELECT i, now(), 'seed', 'COMPLETED', 'REQUESTED', 'Seed return completed', i
FROM generate_series(1, 10000) AS i;

INSERT INTO refunds (
    id, created_at, updated_at, amount, created_by, customer_id, order_id, payment_transaction_id,
    processed_at, processed_by, reason, refund_id, remark, requested_at, requested_by,
    return_id, source, status, updated_by
)
SELECT
    i, now(), now(),
    CASE WHEN i % 2 = 0 THEN round((20 + (i % 100)) * 0.80, 2) ELSE (20 + (i % 100))::numeric(12, 2) END,
    'seed', i, i, i, now(), 'seed',
    'Seed refund',
    'RFD-' || lpad(i::text, 8, '0'),
    'Historical seed refund',
    now(), 'seed',
    'RET-' || lpad(i::text, 8, '0'),
    'RETURN',
    'PROCESSED',
    'seed'
FROM generate_series(1, 10000) AS i;

INSERT INTO refund_status_history (
    id, changed_at, changed_by, new_status, old_status, refund_id, remark
)
SELECT i, now(), 'seed', 'PROCESSED', 'PENDING', i, 'Seed refund processed'
FROM generate_series(1, 10000) AS i;

INSERT INTO audit_logs (
    id, action, actor_id, actor_type, description, error_code, ip_address,
    new_value, occurred_at, old_value, request_id, resource_id, resource_type,
    result, service_name, trace_id, user_agent
)
SELECT
    i, 'CREATE', i::text, 'USER', 'Seed order ' || i, NULL, '127.0.0.1',
    jsonb_build_object('orderNumber', 'ORD-' || lpad(i::text, 8, '0')),
    now(), NULL, 'req_seed_order_' || i, i::text, 'ORDER',
    'SUCCESS', 'order-service', lpad(i::text, 32, '0'), 'seed-script'
FROM generate_series(1, 10000) AS i;

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
