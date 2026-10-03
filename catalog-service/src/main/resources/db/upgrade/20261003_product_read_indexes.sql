-- Apply to catalog_db before deploying with ddl-auto=validate.
-- Supports the page-scoped product/SKU/attribute/image lookups.
-- attribute_values(attribute_id, value) and inventories(product_sku_id) already have unique indexes.
BEGIN;
CREATE INDEX IF NOT EXISTS idx_sku_product_id ON product_skus(product_id, id);
CREATE INDEX IF NOT EXISTS idx_attribute_sku_id ON attributes(product_sku_id, id);
CREATE INDEX IF NOT EXISTS idx_image_product_id ON images(product_id, id);
COMMIT;
