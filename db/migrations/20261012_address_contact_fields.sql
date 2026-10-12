-- Run against the auth database when schema auto-update is disabled. Nullable additions preserve legacy addresses.
ALTER TABLE addresses
    ADD COLUMN IF NOT EXISTS label varchar(40),
    ADD COLUMN IF NOT EXISTS recipient_name varchar(100),
    ADD COLUMN IF NOT EXISTS phone_number varchar(30),
    ADD COLUMN IF NOT EXISTS district varchar(100),
    ADD COLUMN IF NOT EXISTS commune varchar(100),
    ADD COLUMN IF NOT EXISTS additional_information varchar(500);
