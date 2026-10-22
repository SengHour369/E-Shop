-- Public references only; database IDs, legacy references and provider transaction IDs remain unchanged.
CREATE TABLE IF NOT EXISTS business_code_counters (
    code_type VARCHAR(30) PRIMARY KEY,
    last_value BIGINT NOT NULL CHECK (last_value >= 0)
);
INSERT INTO business_code_counters(code_type,last_value)
SELECT 'ORDER', COALESCE(MAX(SUBSTRING(order_number FROM '^anOrder_([0-9]{1,18})$')::BIGINT),0)
FROM order_details
ON CONFLICT (code_type) DO UPDATE SET last_value = GREATEST(business_code_counters.last_value, EXCLUDED.last_value);
