-- 10000 completed AI executions and outbox rows. published_at is set so the publisher does not send them again.
BEGIN;

INSERT INTO ai_executions (
    id, completed_at, error_code, intent, request_id, resource_id, started_at, status, trace_id, user_id, version
)
SELECT
    ('20000000-0000-4000-8000-' || lpad(to_hex(i), 12, '0'))::uuid,
    now(),
    NULL,
    (ARRAY['PRODUCT_SEARCH', 'SKU_GET', 'INVENTORY_GET', 'ORDER_GET', 'PROMOTION_GET'])[1 + (i % 5)],
    'req_seed_ai_' || i,
    i::text,
    now() - interval '1 second',
    'SUCCESS',
    lpad(i::text, 32, '0'),
    i,
    0
FROM generate_series(1, 10000) AS i;

INSERT INTO notification_outbox (
    id, attempts, created_at, next_attempt_at, payload, published_at
)
SELECT
    ('40000000-0000-4000-8000-' || lpad(to_hex(i), 12, '0'))::uuid,
    1,
    now(),
    now(),
    '{"eventId":"30000000-0000-4000-8000-' || lpad(to_hex(i), 12, '0') || '","userId":' || i || '}',
    now()
FROM generate_series(1, 10000) AS i;

INSERT INTO audit_logs (
    id, action, actor_id, actor_type, description, error_code, ip_address,
    new_value, occurred_at, old_value, request_id, resource_id, resource_type,
    result, service_name, trace_id, user_agent
)
SELECT
    i, 'CREATE', i::text, 'USER', 'Seed AI execution ' || i, NULL, '127.0.0.1',
    jsonb_build_object('intent', 'PRODUCT_SEARCH'),
    now(), NULL, 'req_seed_ai_' || i, i::text, 'AI_EXECUTION',
    'SUCCESS', 'ai-service', lpad(i::text, 32, '0'), 'seed-script'
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
