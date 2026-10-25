-- Reuse existing report access; no account or group grants are changed.
INSERT INTO api_permissions (method, api, func_id, is_active, is_delete, created_at, updated_at)
SELECT 'GET', '/api/v1/payments/reports/breakdown', f.func_id, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM function_permissions f WHERE f.func_code = 'REPORT_VIEW' AND f.is_delete = false
AND NOT EXISTS (SELECT 1 FROM api_permissions WHERE method = 'GET' AND api = '/api/v1/payments/reports/breakdown');
