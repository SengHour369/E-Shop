# User reporting and approval assignments

**Reports to** (`managerId`) controls who receives the user's requests for review and approval. The old separate assigned-reviewer field is hidden and its saved value is ignored for routing.

Product and import forms automatically select the manager. My Requests resolves the current manager again when submitting or resubmitting a draft or returned request. The auth service also enforces this relationship when the AI service checks the proposed reviewer, so changing a browser request cannot choose another reviewer.

The manager must be enabled, active, not deleted, independent of the maker and last editor, and hold IMPORT_REVIEW, IMPORT_APPROVE and IMPORT_EXECUTE. No manager or an ineligible manager blocks submission; assigning a manager does not grant permissions. Existing submitted requests keep their recorded reviewer and immutable revision history.

`GET /api/v1/user/approval-options` returns only the eligible current manager and uses that manager as `defaultReviewerId`. It requires current IMPORT_UPLOAD permission and returns no-store responses. Administrator editor options may still list eligible reviewers for compatibility, but no separate reviewer assignment is shown in the user forms.
