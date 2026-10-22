-- Define missing administrative functions without assigning them to users or groups.
INSERT INTO function_permissions (func_id, func_code, func_name, module, is_active, is_delete, created_at, updated_at)
SELECT (SELECT COALESCE(MAX(func_id), 0) FROM function_permissions) + row_number() OVER (ORDER BY r.code),
       r.code, r.name, r.module, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
  ('INVENTORY_VIEW', 'View inventory', 'INVENTORY'),
  ('INVENTORY_ADJUST', 'Adjust inventory', 'INVENTORY'),
  ('REPORT_VIEW', 'View reports', 'REPORT'),
  ('PROMOTION_VIEW', 'View promotions', 'PROMOTION'),
  ('PROMOTION_MANAGE', 'Manage promotions', 'PROMOTION')
) AS r(code, name, module)
WHERE NOT EXISTS (SELECT 1 FROM function_permissions f WHERE f.func_code = r.code);

-- Align current admin APIs with existing function codes. No user/group grants are changed.
INSERT INTO api_permissions (method, api, func_id, is_active, is_delete, created_at, updated_at)
SELECT r.method, r.api, f.func_id, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
('POST', '/api/v1/inventory/id/', 'INVENTORY_VIEW'),
('POST', '/api/v1/inventory/all/', 'INVENTORY_VIEW'),
('POST', '/api/v1/inventory/low-stock', 'INVENTORY_VIEW'),
('POST', '/api/v1/inventory/sku/', 'INVENTORY_VIEW'),
('POST', '/api/v1/inventory/product/', 'INVENTORY_VIEW'),
('POST', '/api/v1/inventory/search', 'INVENTORY_VIEW'),
('GET', '/api/v1/inventory/summary', 'INVENTORY_VIEW'),
('GET', '/api/v1/inventory/history/*', 'INVENTORY_VIEW'),
('POST', '/api/v1/inventory', 'INVENTORY_ADJUST'),
('POST', '/api/v1/inventory/exact', 'INVENTORY_ADJUST'),
('POST', '/api/v1/inventory/delete', 'INVENTORY_ADJUST'),
('PATCH', '/api/v1/inventory/restock', 'INVENTORY_ADJUST'),
('POST', '/api/v1/refunds/list', 'REFUND_VIEW'),
('GET', '/api/v1/refunds/summary', 'REFUND_VIEW'),
('GET', '/api/v1/refunds/*', 'REFUND_VIEW'),
('GET', '/api/v1/refunds/*/history', 'REFUND_VIEW'),
('GET', '/api/v1/refunds/*/similar-products', 'REFUND_VIEW'),
('POST', '/api/v1/refunds/*/process', 'REFUND_PROCESS'),
('POST', '/api/v1/refunds/*/cancel', 'REFUND_CANCEL'),
('POST', '/api/v1/cancelations/list', 'CANCELATION_VIEW'),
('GET', '/api/v1/cancelations/summary', 'CANCELATION_VIEW'),
('GET', '/api/v1/cancelations/*', 'CANCELATION_VIEW'),
('GET', '/api/v1/admin/promotions', 'PROMOTION_VIEW'),
('GET', '/api/v1/admin/promotions/*', 'PROMOTION_VIEW'),
('GET', '/api/v1/admin/promotions/*/skus', 'PROMOTION_VIEW'),
('POST', '/api/v1/admin/promotions', 'PROMOTION_MANAGE'),
('PUT', '/api/v1/admin/promotions/*', 'PROMOTION_MANAGE'),
('DELETE', '/api/v1/admin/promotions/*', 'PROMOTION_MANAGE'),
('POST', '/api/v1/admin/promotions/*/skus', 'PROMOTION_MANAGE'),
('DELETE', '/api/v1/admin/promotions/*/skus/*', 'PROMOTION_MANAGE'),
('POST', '/api/v1/admin/promotions/*/schedule', 'PROMOTION_MANAGE'),
('POST', '/api/v1/admin/promotions/*/activate', 'PROMOTION_MANAGE'),
('POST', '/api/v1/admin/promotions/*/disable', 'PROMOTION_MANAGE'),
('POST', '/api/v1/products/delete/', 'PRODUCT_DELETE'),
('POST', '/api/v1/products/update/status/', 'PRODUCT_UPDATE'),
('GET', '/api/v1/orders/reports/monthly', 'REPORT_VIEW'),
('GET', '/api/v1/payments/reports/monthly', 'REPORT_VIEW'),
('GET', '/api/v1/orders/reports/weekly', 'REPORT_VIEW')
) AS r(method, api, code)
JOIN function_permissions f ON f.func_code = r.code AND f.is_active AND NOT f.is_delete
ON CONFLICT (method, api) DO NOTHING;
