# User reporting and approval assignments

Add Customer and Edit User now expose two searchable user selectors:

- **Reports to** saves the existing `managerId` relationship.
- **Assigned reviewer** saves `approvalReviewerId` as the default reviewer for requests
  created by that account. This does not put account creation itself into approval.

Names include usernames to distinguish people with the same name. Options retain actual
user IDs; typing text alone does not select an account. Both fields can be cleared.
The reviewer selector includes only enabled, active, non-deleted accounts that currently
have IMPORT_REVIEW, IMPORT_APPROVE and IMPORT_EXECUTE. A user cannot be their own reviewer.
Assigning a reviewer never grants permissions.

Product forms and import submission forms use the account's eligible saved default and
allow selecting another authorized reviewer by name. Existing submission and approval
checks still enforce current permissions, maker/reviewer independence, last-editor
independence, version checks and explicit submit/approve actions.

`GET /api/v1/user/approval-options` returns eligible reviewers and the current account's
default; it requires current IMPORT_UPLOAD permission and returns no-store responses.
The administrator-only editor options also include eligible reviewers for assignment.
If a default reviewer loses eligibility, it is no longer offered as the active default.

Schema: apply `auth-service/src/main/resources/db/upgrade/20261024_user_approval_reviewer.sql`
when deploying to a database without automatic schema updates. The additive column
migration has been applied to the local auth database. Existing assignments start empty.
