BEGIN;
-- An order reference is mandatory. A guest transaction has no Identity customer reference.
ALTER TABLE payment_transactions ALTER COLUMN customer_id DROP NOT NULL;
COMMIT;
