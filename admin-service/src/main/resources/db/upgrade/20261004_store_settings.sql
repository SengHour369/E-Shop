CREATE TABLE IF NOT EXISTS store_settings (
    id BIGINT PRIMARY KEY,
    store_name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone VARCHAR(40),
    address VARCHAR(500),
    timezone VARCHAR(80) NOT NULL
);
