-- Saved default reviewer; actual request submission still checks current permission and independence.
ALTER TABLE tbl_user ADD COLUMN IF NOT EXISTS approval_reviewer_id bigint;
