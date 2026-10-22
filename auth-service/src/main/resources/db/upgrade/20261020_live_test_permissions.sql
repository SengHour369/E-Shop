BEGIN;
LOCK TABLE function_permissions IN SHARE ROW EXCLUSIVE MODE;
INSERT INTO function_permissions(func_id,func_code,func_name,module,is_active,is_delete,created_at,updated_at)
SELECT (SELECT coalesce(max(func_id),0) FROM function_permissions)+row_number() OVER(ORDER BY r.code),r.code,r.name,'SYSTEM',true,false,now(),now()
FROM (VALUES('SYSTEM_TEST_VIEW','View own live test reports'),('SYSTEM_TEST_RUN','Run fixed live HTTP checks'))r(code,name)
WHERE NOT EXISTS(SELECT 1 FROM function_permissions f WHERE f.func_code=r.code);
INSERT INTO api_permissions(method,api,func_id,is_active,is_delete,created_at,updated_at)
SELECT r.method,r.api,f.func_id,true,false,now(),now() FROM (VALUES
 ('GET','/api/v1/admin/live-tests/runs','SYSTEM_TEST_VIEW'),
 ('GET','/api/v1/admin/live-tests/runs/*','SYSTEM_TEST_VIEW'),
 ('POST','/api/v1/admin/live-tests/runs','SYSTEM_TEST_RUN'))r(method,api,code)
JOIN function_permissions f ON f.func_code=r.code ON CONFLICT(method,api) DO NOTHING;
COMMIT;
