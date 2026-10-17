# Edit User

The Angular admin page `/admin/users/edit/:id` uses administrator-only endpoints:

- `GET /api/v1/user/editor-options`: roles, active permission groups, departments and managers.
- `GET /api/v1/user/{id}/editor`: profile and assignments, without password hashes.
- `PUT /api/v1/user/{id}/editor`: validate and save the complete form in one transaction.
- `POST /api/v1/user/{id}/editor/photo`: multipart `file`, JPEG/PNG up to 2 MiB and 4096 pixels; sanitize and resize to a 256-pixel PNG avatar.

Apply `auth-service/src/main/resources/db/upgrade/20261017_user_editor.sql` before starting the new auth-service. Existing rows retain their profile and access settings; two-factor defaults to false. Roles and groups come from the existing database; no sample identities or assignments are inserted. Existing administrator authority protects every editor endpoint. Changing access or sign-in identity revokes refresh tokens. Administrators cannot deactivate or remove administrator access from their own account.

Two-factor means a six-digit email code after successful password or linked external authentication. No access token is returned until `POST /api/v1/public/second-factor` verifies `{challenge, code}`. Codes expire after five minutes, are single use, and allow at most five wrong attempts. Only a digest is held in memory; restarting this single-instance local auth-service invalidates pending challenges. For multiple auth-service replicas, move challenges to a shared expiring store before scaling. SMTP must be configured (local Compose uses Mailpit). Refreshing an existing session does not request another code.

Profile fields save first, then an optional photo upload. If photo upload fails, the profile is already saved and the photo can be retried. User language is persisted as an account preference; the existing site language selector remains separate.

## User Details

`GET /api/v1/user/{id}/details` is administrator-only and supplies the same saved profile as the editor, resolved group and role labels, currently configured function permissions, recorded creation/login/password-change dates, consecutive failed login attempts, and unexpired refresh-session metadata. Session token hashes and passwords are never returned. Blocked or unverified users report no usable sessions.

`GET /api/v1/user/{id}/details/activity?page=0` supplies 20 events per page, limited to actions performed by the selected user or actions targeting that USER resource. It returns safe event metadata only, without audit snapshots. The UI shows five recent events and supports paging through the full list. A failed activity request leaves the profile visible. View Sessions lists only non-secret IDs and timestamps; it does not revoke sessions. Time zone displays Not configured because the current profile does not store a time-zone preference.
