-- Register existing profile endpoints under USER_MANAGE. No user or group grants are added.
INSERT INTO api_permissions (method, api, func_id, is_active, is_delete, created_at, updated_at)
VALUES
('GET', '/api/v1/user/*/details', 601, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('GET', '/api/v1/user/*/details/activity', 601, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('GET', '/api/v1/user/editor-options', 601, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('GET', '/api/v1/user/*/editor', 601, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PUT', '/api/v1/user/*/editor', 601, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('POST', '/api/v1/user/*/editor/photo', 601, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (method, api) DO NOTHING;
