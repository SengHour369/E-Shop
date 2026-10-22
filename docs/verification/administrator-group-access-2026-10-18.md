# Administrator group access — 2026-10-18

The affected account belonged to the active `ADM` group but retained the `USER` database role. Login previously derived admin entry solely from that role.

Active `ADM` memberships now produce administrator entry during login and refresh without changing stored roles. Effective module permissions continue to come from current active user/group grants, not the unrestricted administrator function set. Stored ADMIN/SUPER_ADMIN roles retain their existing behavior.

Group-derived administrator authorities carry `GROUP_PERMISSION_SCOPED`. The shared downstream JWT filter validates each `/api/v1/**` request against the current API/function mapping and effective account grants via Auth Service. Unknown mappings, revoked groups and unavailable authorization checks fail closed. Auth Service's own permission interceptor uses the same active-grant query; a group-derived admin entry role does not bypass it.

Auth, Catalog, Order, Payment, Admin, AI and Notification services were rebuilt and redeployed. No account password, stored role, membership or grant was changed.

## Validation

- 17 focused auth tests passed: active/deleted memberships and groups, group-only admin entry, effective permissions, role revocation, disabled accounts, login authorities and interceptor enforcement.
- 2 shared-filter HTTP tests passed: granted calls proceed and missing grants return 403 before domain handling.
- Real HTTP checks using a short-lived signed test token for the existing member: access snapshot HTTP 200, administrator true, 64 effective permissions; order summary HTTP 200; inventory summary HTTP 403 because INVENTORY_VIEW is absent.
- The member's password login was not attempted; the user must sign out/in to obtain the updated login role and scoped token. No credentials or test tokens are recorded here.

See [sanitized HTTP results](administrator-group-http-2026-10-18.json). Newly added API routes must have a function mapping before scoped group administrators can call them.
