-- 10000 payments and transactions. order_id and user_id match the order and auth seeds.
BEGIN;

INSERT INTO payments (
    id, created_at, updated_at, amount, code, code_order, currency, deleted, order_id,
    payment_date, payment_method, payment_provider, payment_provider_response, payment_url,
    qr_code, status, transaction_id, user_id
)
SELECT
    i, now(), now(),
    CASE WHEN i % 2 = 0 THEN round((20 + (i % 100)) * 0.80, 2) ELSE (20 + (i % 100))::numeric(12, 2) END,
    'PM-' || lpad(i::text, 8, '0'),
    'ORD-' || lpad(i::text, 8, '0'),
    'USD',
    false,
    i,
    now(),
    'BAKONG',
    'BAKONG',
    'seed accepted',
    'https://pay.example/seed/' || i,
    'seed-qr-' || i,
    'COMPLETED',
    'PAY-' || lpad(i::text, 8, '0'),
    i
FROM generate_series(1, 10000) AS i;

INSERT INTO payment_transactions (
    id, created_at, updated_at, amount, currency, customer_id, masked_account, order_id,
    payment_method, remarks, status, transaction_no
)
SELECT
    i, now(), now(),
    CASE WHEN i % 2 = 0 THEN round((20 + (i % 100)) * 0.80, 2) ELSE (20 + (i % 100))::numeric(12, 2) END,
    'USD',
    i,
    '****' || lpad((i % 10000)::text, 4, '0'),
    i,
    'BAKONG',
    'Seed payment',
    'SUCCESS',
    'TXN-' || lpad(i::text, 8, '0')
FROM generate_series(1, 10000) AS i;

INSERT INTO payment_transaction_status_history (
    id, created_at, updated_at, changed_at, changed_by, new_status, old_status, reason, transaction_id
)
SELECT i, now(), now(), now(), 'seed', 'SUCCESS', 'PENDING', 'Seed payment completed', i
FROM generate_series(1, 10000) AS i;

INSERT INTO audit_logs (
    id, action, actor_id, actor_type, description, error_code, ip_address,
    new_value, occurred_at, old_value, request_id, resource_id, resource_type,
    result, service_name, trace_id, user_agent
)
SELECT
    i, 'CREATE', i::text, 'USER', 'Seed payment ' || i, NULL, '127.0.0.1',
    jsonb_build_object('transaction', 'TXN-' || lpad(i::text, 8, '0')),
    now(), NULL, 'req_seed_payment_' || i, i::text, 'PAYMENT',
    'SUCCESS', 'payment-service', lpad(i::text, 32, '0'), 'seed-script'
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
