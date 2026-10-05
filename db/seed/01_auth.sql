-- Adds 10000 rows on top of the roles, admin user, groups, and permissions
-- already created by DataInitializer.
-- Seed login: user00001 / Password123!
-- Existing admin login: admin / admin123
BEGIN;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM tbl_user WHERE username = 'user00001') THEN
        RAISE EXCEPTION 'seed users are already loaded';
    END IF;
END $$;

INSERT INTO tbl_user (
    id, created_at, updated_at, attempt, birthdate, deleted, email, enabled,
    full_name, password, phone, status, username, verification_code, verification_expiration
)
SELECT
    i + 1,
    now(),
    now(),
    0,
    '1995-01-15',
    false,
    'user' || lpad(i::text, 5, '0') || '@seed.eshop',
    true,
    'Seed User ' || i,
    '$2a$10$jNmMzCjGN6Rpn2uPukD6Xefr96sUgDHdgtIWDG2iDAqMy7u2bZo86',
    '01' || lpad(i::text, 8, '0'),
    'ACT',
    'user' || lpad(i::text, 5, '0'),
    lpad((i % 1000000)::text, 6, '0'),
    now() - interval '1 day'
FROM generate_series(1, 10000) AS i;

INSERT INTO users_roles (user_id, role_id)
SELECT i + 1, 5 FROM generate_series(1, 10000) AS i
UNION ALL
SELECT * FROM (VALUES (2, 1), (3, 2), (4, 3), (5, 4)) AS extra(user_id, role_id);

INSERT INTO addresses (
    id, created_at, updated_at, address_line1, city, country, deleted, is_default, zip_code
)
SELECT
    i, now(), now(),
    i || ' Seed Street',
    'Phnom Penh',
    'Cambodia',
    false,
    true,
    lpad((10000 + i)::text, 5, '0')
FROM generate_series(1, 10000) AS i;

INSERT INTO user_address (user_id, address_id)
SELECT i + 1, i FROM generate_series(1, 10000) AS i;

INSERT INTO tt_group (
    id, created_at, updated_at, description, group_code, is_active, is_delete, name, status, type
)
SELECT
    i + 5, now(), now(),
    'Seed group ' || i,
    'G' || lpad(i::text, 9, '0'),
    true,
    false,
    'Seed Group ' || i,
    'ACTIVE',
    'CUSTOM'
FROM generate_series(1, 10000) AS i;

INSERT INTO function_permissions (
    func_id, created_at, updated_at, description, func_code, func_name, is_active, is_delete, module
)
SELECT
    200000 + i, now(), now(),
    'Seed function ' || i,
    'SEED_FN_' || lpad(i::text, 5, '0'),
    'Seed function ' || i,
    true,
    false,
    'SEED'
FROM generate_series(1, 10000) AS i;

INSERT INTO api_permissions (
    id, created_at, updated_at, api, func_id, is_active, is_delete, method
)
SELECT
    1000 + i, now(), now(),
    '/api/v1/seed/functions/' || i,
    200000 + i,
    true,
    false,
    'GET'
FROM generate_series(1, 10000) AS i;

INSERT INTO group_permissions (
    group_permission_id, created_at, updated_at, func_id, group_id, is_active, is_delete
)
SELECT 1000 + i, now(), now(), 200000 + i, i + 5, true, false
FROM generate_series(1, 10000) AS i;

INSERT INTO user_groups (id, created_at, updated_at, group_id, is_active, is_delete, user_id)
SELECT i + 1, now(), now(), i + 5, true, false, i + 1
FROM generate_series(1, 10000) AS i;

INSERT INTO user_permissions (
    user_permission_id, created_at, updated_at, func_id, is_active, is_delete, user_id
)
SELECT i, now(), now(), 200000 + i, true, false, i + 1
FROM generate_series(1, 10000) AS i;

INSERT INTO tt_permission (id, created_at, updated_at, description, name, status)
SELECT i, now(), now(), 'Seed permission ' || i, 'SEED_PERMISSION_' || i, 'ACTIVE'
FROM generate_series(1, 10000) AS i;

INSERT INTO refresh_tokens (id, created_at, expires_at, token, user_id)
SELECT
    ('50000000-0000-4000-8000-' || lpad(to_hex(i), 12, '0'))::uuid,
    now(),
    now() + interval '30 days',
    'seed-refresh-' || i,
    i + 1
FROM generate_series(1, 10000) AS i;

INSERT INTO password_reset_tokens (id, created_at, expires_at, token, used, user_id)
SELECT
    ('51000000-0000-4000-8000-' || lpad(to_hex(i), 12, '0'))::uuid,
    now(),
    now() + interval '1 day',
    'seed-reset-' || i,
    false,
    i + 1
FROM generate_series(1, 10000) AS i;

INSERT INTO verification_tokens (id, created_at, expires_at, token, user_id)
SELECT
    ('52000000-0000-4000-8000-' || lpad(to_hex(i), 12, '0'))::uuid,
    now(),
    now() + interval '1 day',
    'seed-verify-' || i,
    i + 1
FROM generate_series(1, 10000) AS i;

INSERT INTO notification_mail_deliveries (event_id, sent_at, user_id, version)
SELECT
    ('10000000-0000-4000-8000-' || lpad(to_hex(i), 12, '0'))::uuid,
    now(),
    i + 1,
    0
FROM generate_series(1, 10000) AS i;

INSERT INTO audit_logs (
    id, action, actor_id, actor_type, description, error_code, ip_address,
    new_value, occurred_at, old_value, request_id, resource_id, resource_type,
    result, service_name, trace_id, user_agent
)
SELECT
    i, 'CREATE', (i + 1)::text, 'ADMIN', 'Seed user ' || i, NULL, '127.0.0.1',
    jsonb_build_object('username', 'user' || lpad(i::text, 5, '0')),
    now(), NULL, 'req_seed_auth_' || i, (i + 1)::text, 'USER',
    'SUCCESS', 'auth-service', lpad(i::text, 32, '0'), 'seed-script'
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
