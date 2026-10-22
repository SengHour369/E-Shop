BEGIN;
INSERT INTO function_permissions (func_id,func_code,func_name,module,is_active,is_delete,created_at,updated_at)
SELECT (SELECT COALESCE(MAX(func_id),0) FROM function_permissions)+row_number() OVER(ORDER BY r.code),
 r.code,r.name,'STOREFRONT',true,false,now(),now()
FROM (VALUES ('STOREFRONT_ACCESS_VIEW','View storefront access'),('STOREFRONT_ACCESS_UPDATE','Update storefront access'),
 ('PRODUCT_REVIEW_MODERATE','Moderate product reviews')) AS r(code,name)
WHERE NOT EXISTS(SELECT 1 FROM function_permissions f WHERE f.func_code=r.code);
INSERT INTO api_permissions(method,api,func_id,is_active,is_delete,created_at,updated_at)
SELECT r.method,r.api,f.func_id,true,false,now(),now() FROM (VALUES
 ('GET','/api/v1/admin/storefront/access-settings','STOREFRONT_ACCESS_VIEW'),
 ('PUT','/api/v1/admin/storefront/access-settings','STOREFRONT_ACCESS_UPDATE'),
 ('GET','/api/v1/admin/guest-reviews','PRODUCT_REVIEW_MODERATE'),
 ('PUT','/api/v1/admin/guest-reviews/*','PRODUCT_REVIEW_MODERATE')) AS r(method,api,code)
JOIN function_permissions f ON f.func_code=r.code ON CONFLICT(method,api) DO NOTHING;
-- Definitions only: no existing user or group receives new grants implicitly.
COMMIT;
