# Configured staff-group access — 2026-10-18

Follow-up to the Administrator group fix: frontend admission, administrator-only parent guards and menu filtering still depended on the login role. Backend entry was also restricted to group code ADM.

Frontend guards and menus now accept current server-resolved staff entry and check the route's effective permissions even if the session role is USER. Backend staff entry recognizes an active, nondeleted membership in an active, nondeleted non-USR group with at least one active function grant. The default customer group remains excluded. Group-scoped tokens retain the live API permission check introduced in the earlier fix. No stored roles, memberships, passwords or grants were changed.

Validation:

- 17 frontend tests passed, including USER-role group admission, permission-denied routes, administrator parent routes and permission-filtered navigation.
- 17 backend tests passed, including custom group codes, empty groups, default-customer exclusion, inactive/deleted memberships/groups, effective grants and login authorities.
- Frontend production build and frontend/Auth Docker builds passed. Both were deployed.
- Read-only HTTP through the running frontend at http://127.0.0.1:4200 confirms the existing account resolves as administrator with 64 effective permissions; order summary returns 200 and inventory summary returns 403 because INVENTORY_VIEW is absent.

The old temporary development port 41075 was no longer listening during final verification. The member's password login was not attempted. Sign out/in after refreshing the frontend to obtain a fresh group-scoped session.

[Sanitized HTTP results](staff-group-access-http-2026-10-18.json).
