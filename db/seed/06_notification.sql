-- 10000 notifications already marked emailed, so the mailer does not send them.
BEGIN;

INSERT INTO notification_preferences (user_id, ai_email)
SELECT i, i % 2 = 1 FROM generate_series(1, 10000) AS i;

INSERT INTO notifications (
    id, ai_execution_id, created_at, email_attempts, email_status, emailed_at, event_id,
    message, next_email_attempt_at, read_at, request_id, resource_id, resource_type,
    sent_at, status, title, trace_id, type, user_id
)
SELECT
    i,
    ('20000000-0000-4000-8000-' || lpad(to_hex(i), 12, '0'))::uuid,
    now(),
    1,
    'SENT',
    now(),
    ('30000000-0000-4000-8000-' || lpad(to_hex(i), 12, '0'))::uuid,
    'Seed order ORD-' || lpad(i::text, 8, '0') || ' is recorded.',
    NULL,
    CASE WHEN i % 2 = 0 THEN now() ELSE NULL END,
    'req_seed_notice_' || i,
    i::text,
    'ORDER',
    now(),
    CASE WHEN i % 2 = 0 THEN 'READ' ELSE 'SENT' END,
    'Order update ' || i,
    lpad(i::text, 32, '0'),
    CASE WHEN i % 5 = 0 THEN 'AI_ACTION_FAILED' ELSE 'AI_ACTION_COMPLETED' END,
    i
FROM generate_series(1, 10000) AS i;

INSERT INTO audit_logs (
    id, action, actor_id, actor_type, description, error_code, ip_address,
    new_value, occurred_at, old_value, request_id, resource_id, resource_type,
    result, service_name, trace_id, user_agent
)
SELECT
    i, 'CREATE', i::text, 'SYSTEM', 'Seed notification ' || i, NULL, '127.0.0.1',
    jsonb_build_object('eventId', i),
    now(), NULL, 'req_seed_notice_' || i, i::text, 'NOTIFICATION',
    'SUCCESS', 'notification-service', lpad(i::text, 32, '0'), 'seed-script'
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
