-- Apply to order-service's PostgreSQL database before deploying shopping adapters.
CREATE TABLE IF NOT EXISTS ai_mutation_receipts (
    id VARCHAR(120) PRIMARY KEY,
    fingerprint VARCHAR(64) NOT NULL,
    response_json TEXT NOT NULL
);
ALTER TABLE order_details ADD COLUMN IF NOT EXISTS expected_total NUMERIC(19,2);
