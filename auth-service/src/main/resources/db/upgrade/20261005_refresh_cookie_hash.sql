-- PostgreSQL: stop auth-service before applying, then deploy the hash-aware version.
-- Re-runnable. Existing browser cookies continue working after conversion.
BEGIN;
UPDATE refresh_tokens
SET token = 'sha256:' || encode(sha256(convert_to(token, 'UTF8')), 'hex')
WHERE token NOT LIKE 'sha256:%';
COMMIT;
