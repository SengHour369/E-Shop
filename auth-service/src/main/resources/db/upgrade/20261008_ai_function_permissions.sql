-- Add function definitions only. Assign grants through existing user/group APIs.
-- ADMIN and SUPER_ADMIN receive no implicit grants from this migration.
INSERT INTO function_permissions
    (func_id, func_code, func_name, module, is_active, is_delete, created_at, updated_at)
SELECT
    (SELECT COALESCE(MAX(func_id), 0) FROM function_permissions) + row_number() OVER (ORDER BY code),
    code,
    code,
    'AI_ASSISTANT',
    true,
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM (VALUES
    ('ORDER_VIEW_ALL'),
    ('PAYMENT_VIEW_ALL'),
    ('RETURN_VIEW_ALL'),
    ('INVENTORY_VIEW'),
    ('PROMOTION_VIEW'),
    ('PROMOTION_MANAGE'),
    ('REPORT_VIEW'),
    ('USER_VIEW'),
    ('AUDIT_VIEW'),
    ('KNOWLEDGE_MANAGE')
) AS required(code)
WHERE NOT EXISTS (
    SELECT 1 FROM function_permissions existing WHERE existing.func_code = required.code
);
