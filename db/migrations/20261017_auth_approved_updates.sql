-- Function definitions only. Grant explicitly using existing authorization management.
INSERT INTO function_permissions (func_id,func_code,func_name,module,is_active,is_delete,created_at,updated_at)
SELECT (SELECT COALESCE(MAX(func_id),0) FROM function_permissions)+row_number() OVER (ORDER BY code),
       code,code,'PRODUCT',true,false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM (VALUES ('CATEGORY_UPDATE'),('BRANCH_UPDATE')) AS required(code)
WHERE NOT EXISTS (SELECT 1 FROM function_permissions existing WHERE existing.func_code=required.code);
