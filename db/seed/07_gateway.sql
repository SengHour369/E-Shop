-- 10000 request logs and 10000 disabled routes.
-- Routes stay disabled so they are not added to the live gateway.
-- gateway_admin_keys allows only id = 1. The gateway writes that secret when it starts.
BEGIN;

INSERT INTO gateway_request_logs (
    id, correlation_id, method, path, response_status, duration_ms, client_ip, sort, deleted, created_at
)
SELECT
    i,
    'req_seed_gw_' || i,
    'GET',
    '/api/v1/products/get/all',
    200,
    15 + (i % 40),
    '127.0.0.1',
    0,
    false,
    now()
FROM generate_series(14, 10013) AS i;

INSERT INTO gateway_routes (
    id, route_key, uri, path_pattern, http_method, enabled, rate_limit,
    rate_limit_window_seconds, sort, deleted, created_at, updated_at
)
SELECT
    i,
    'seed-' || lpad(i::text, 5, '0'),
    'http://127.0.0.1:9',
    '/internal-seed/' || i,
    'GET',
    false,
    NULL,
    NULL,
    i,
    false,
    now(),
    now()
FROM generate_series(2, 10001) AS i;

COMMIT;

SELECT setval('gateway_request_logs_id_seq', (SELECT MAX(id) FROM gateway_request_logs));
SELECT setval('gateway_routes_id_seq', (SELECT MAX(id) FROM gateway_routes));
