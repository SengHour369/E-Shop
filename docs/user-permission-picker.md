# Direct permissions in the customer editor

Add Customer and Edit User replace the Role and Language controls with a User Permissions
card matching the groups picker: searchable permission names/codes/modules, checkboxes,
selected chips and remove buttons. Permission selection is optional; customer group
selection retains its existing requirement.

New customers receive the standard CUSTOMER/USER role from editor options. There is no
fallback to an administrator role. Edits preserve existing role IDs and language.

The authenticated administrator-only editor API returns active function permission options
and the user's active direct `permissionIds`. Updates validate the chosen IDs, activate
or reactivate selected direct assignments, and deactivate removed direct assignments in
the same transaction as profile changes. Group permission assignments are separate and
remain effective. Access changes invalidate the user's refresh sessions and are included
in the existing user-update audit event. An omitted `permissionIds` field preserves direct
assignments for older clients; an empty array explicitly clears them.

This uses the existing `function_permissions` and `user_permissions` tables, so no new
schema migration is required.
