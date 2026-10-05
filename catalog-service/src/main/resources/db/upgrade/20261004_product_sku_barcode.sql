-- Apply to catalog_db before deploying with ddl-auto=validate.
-- Retail barcodes are optional and unique. Multiple SKUs may still have no barcode.
-- Internal sku values are unchanged.
BEGIN;
ALTER TABLE product_skus ADD COLUMN IF NOT EXISTS barcode varchar(64);
CREATE INDEX IF NOT EXISTS idx_product_sku_barcode ON product_skus(barcode);
CREATE UNIQUE INDEX IF NOT EXISTS uk_product_sku_barcode ON product_skus(barcode) WHERE barcode IS NOT NULL;
COMMIT;
