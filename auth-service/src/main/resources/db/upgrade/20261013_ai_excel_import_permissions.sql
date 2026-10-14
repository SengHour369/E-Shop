-- Add function definitions only. Assign grants through existing user/group APIs.
-- ADMIN and SUPER_ADMIN receive no implicit grants from this migration.
INSERT INTO function_permissions
    (func_id, func_code, func_name, module, is_active, is_delete, created_at, updated_at)
SELECT
    (SELECT COALESCE(MAX(func_id), 0) FROM function_permissions) + row_number() OVER (ORDER BY code),
    code,
    code,
    'AI_IMPORT',
    true,
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM (VALUES
    ('IMPORT_UPLOAD'),
    ('IMPORT_REVIEW'),
    ('IMPORT_APPROVE'),
    ('IMPORT_EXECUTE'),
    ('CATEGORY_CREATE'),
    ('SKU_CREATE'),
    ('PRODUCT_CREATE'),
    ('BRANCH_CREATE'),
    ('INVENTORY_ADJUST')
) AS required(code)
WHERE NOT EXISTS (
    SELECT 1 FROM function_permissions existing WHERE existing.func_code = required.code
);
