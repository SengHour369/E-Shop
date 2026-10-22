-- Map current Returns routes to the existing view/action functions. No grants are added.
INSERT INTO api_permissions (method, api, func_id, is_active, is_delete, created_at, updated_at)
VALUES
('GET', '/api/v1/returns/summary', 1101, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('POST', '/api/v1/returns/returns', 1101, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('GET', '/api/v1/returns/*', 1101, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('GET', '/api/v1/returns/*/history', 1101, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('POST', '/api/v1/returns/*/approve', 1102, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('POST', '/api/v1/returns/*/reject', 1103, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('POST', '/api/v1/returns/*/receive', 1106, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('POST', '/api/v1/returns/*/inspect/start', 1107, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('POST', '/api/v1/returns/*/inspect/complete', 1107, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (method, api) DO NOTHING;
