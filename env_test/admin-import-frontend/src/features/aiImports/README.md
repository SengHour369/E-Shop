# AI assistant and Excel imports

Routes in the existing React admin layout:

- `/dashboard/ai-assistant`: chat, Excel attachment, mapping confirmation and editable preview.
- `/dashboard/import-approvals`: pending reviews and independent approval decisions.
- `/dashboard/import-executions`: batches, confirmed row results, retry controls and audit history.

The `batch` query parameter opens an import directly. Staff with upload and review grants can switch between their uploads and their assigned imports. Assigned history includes approved/completed imports; pending approvals remain a separate queue.

The pages use the existing Axios client and bearer-token interceptor. `GET /api/v1/user/access` supplies live permissions; role names do not create client-side grants. Admin access comes from the backend's active-function policy. API keys never belong in browser code or Vite environment variables.

Configure the existing `VITE_API_URL` to the E-Shop gateway, following the application's current environment setup. Serve the app through the usual frontend deployment; the React development server does not itself provide business APIs.

Backend prerequisites include the import migrations, updated AI/catalog/notification services, and matching private `AI_IMPORT_SIGNING_KEY` values for AI and catalog. Branch imports and spreadsheet-only Product creation remain unavailable until their domain contracts support them. Reviewer IDs must identify an active, eligible independent account; the backend verifies this at submission.

Execution is an explicit action of up to ten rows at a time. Only the approving reviewer with current grants can execute. UI fields show a resource ID only when the domain service has confirmed it. Unknown/time-out results prompt a reload; writes are never automatically retried. The server remains authoritative for validation, authorization, revision binding and idempotency.

Run `npm test` for import control tests and existing scanner tests. Run `npm run build` for the production build. Lint the feature with `npx eslint src/features/aiImports`.
