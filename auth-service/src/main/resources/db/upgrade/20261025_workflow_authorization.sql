-- Revocable, version-specific authority; contains no access or refresh tokens.
CREATE TABLE IF NOT EXISTS workflow_authorization_grants (
 id uuid PRIMARY KEY, owner_id bigint NOT NULL, version_id uuid NOT NULL,
 expires_at timestamptz NOT NULL, revoked boolean NOT NULL DEFAULT false,
 created_at timestamptz NOT NULL
);
CREATE INDEX IF NOT EXISTS workflow_grant_owner ON workflow_authorization_grants(owner_id);
