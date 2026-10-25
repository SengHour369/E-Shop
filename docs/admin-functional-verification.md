# Admin functionality verification — 25 October 2026

The verification distinguishes real service requests from unit tests. It does not certify every admin action.

Implemented fixes:

- Scheduled and event workflow definitions now have real configuration controls, persistent trigger/job storage, activation/pause/renewal, and background execution history. Schedules support one-time and minute intervals, coalescing missed intervals after downtime. Events subscribe to real commerce topics or owner-private custom signals; event IDs prevent duplicate execution.
- Automatic execution uses revocable version-specific grants lasting 1–30 days. No browser access or refresh token is saved. The worker obtains a short-lived read-scoped credential and rechecks live account/tool permissions. Interrupted jobs become UNKNOWN after 15 minutes and are not replayed.

- Activity and Guest Activity use the backend's ADMIN/AUDIT_READ rule rather than the undefined AUDIT_VIEW code. Existing function grants and normal landing-page priorities remain enforced.
- Inventory history returns flat movement DTOs instead of recursively serializing inventory/SKU/product entities. The UI reads movement types and signed quantity changes.
- Orders with an explicitly absent payment do not request a nonexistent payment. Combined order filters read all pages before filtering and paginating.
- Workflow inputs can be entered through registry-typed fields; switching tools clears incompatible parameters. Validation errors appear inside the builder and report the missing input.
- Workflow execution tabs are scoped to the selected workflow. Active manual runs can be cancelled while their execution request is pending.
- Duplicating a workflow remaps source and branch references and respects cancellation of the discard prompt.
- Workflow module/trigger/publication filters run on the server before pagination. Published means the current draft revision has an immutable published version; editing creates a new draft revision without changing the old version.

Apply `ai-service/src/main/resources/db/upgrade/20261025_workflow_list_filters.sql` before deploying the workflow-list filter changes. It adds metadata columns/index and backfills existing JSON definitions without changing revision tokens. The local database has been upgraded. `scripts/verify-ai-workflow-migrations.py` also checks repeat application and backfill in disposable PostgreSQL.

Apply `20261025_workflow_triggers.sql` in the AI database and `20261025_workflow_authorization.sql` in the auth database before deploying automatic triggers. Rebuild every service consuming common-lib so delegated credentials are restricted to permitted read endpoints. Both migrations have been applied locally.

## Repeat real HTTPS checks

```bash
python3 live-tests/admin-pages.py --base-url https://localhost:8443 --ca-file .local/https/local-ca.crt --report .local/test-reports/admin-pages-https.json
python3 live-tests/admin-ui.py --base-url https://localhost:8443 --ca-file .local/https/local-ca.crt
python3 live-tests/workflow-triggers.py --base-url https://localhost:8443 --ca-file .local/https/local-ca.crt
```

Passwords are prompted privately; CI can provide ESHOP_TEST_USERNAME and ESHOP_TEST_PASSWORD through its secret manager. Credentials are passed only in memory/environment, never written into reports. Certificate validation stays enabled and requests do not follow redirects.

`admin-ui.py` renders the actual Angular routes/components under jsdom with real guards, real identity, and real HTTPS APIs. It discovers existing detail records rather than inventing IDs. Workflow tests create retained draft/version/run records and execute read-only catalog handlers. They do not add sample products, alter stock, make payments, or execute approvals. Missing promotion/refund/return detail fixtures are reported explicitly. These tests do not verify browser certificate trust or the cookie-session login flow.

The normal frontend suite skips the opt-in live tests. Running the two live test files with ESHOP_TEST_SESSION/ESHOP_TEST_TOKEN is separate from the unit suite; there is no HttpTestingController or fake server in those files.

## Verified results for automatic triggers

- 336 common/auth/AI backend tests passed; 32 environment-dependent tests skipped. 659 frontend unit tests passed.
- 67 rendered-component HTTPS checks passed, covering real admin routes, existing detail records, manual execution/cancellation, and scheduled/custom-event creation and actual background reads. This uses jsdom and real HTTPS, not a browser session.
- Eight real HTTPS automatic-trigger checks passed: capabilities, scheduled execution, one-time deduplication, custom-event execution, event deduplication, pause, renewal, and successful execution after restarting the AI worker. No browser needed to remain open. Test workflows and output digests remain in the server ledger; subscriptions are paused after testing.
- 49 admin read API checks passed after deployment. Empty promotion/refund/return detail fixture sets remain explicitly untested rather than populated with sample business records.
- Reports: `.local/test-reports/workflow-triggers-https.json`, `admin-ui-https.summary.json`, and `admin-pages-https.json`. Earlier startup and grant-response contract failures remain separately recorded; they are not counted as successful acceptance.

The real cross-service activation test exposed a strict-decoder mismatch: auth returned `revoked` and `createdAt` fields absent from the AI grant DTO. The DTO now matches the complete response and a regression test uses strict decoding. The full acceptance suite passed after deployment.

## Outstanding acceptance work

- Browser-based sign-in and interaction require the user to handle the local certificate warning.
- Product approval acceptance requires the assigned independent reviewer's authenticated session and a concrete review request.
- Workflow-specific approval steps, approved workflow write execution, and durable long waits remain unimplemented. Automatic triggers execute the same permitted read-only tools as manual runs.
- Financial mutations and settlement, and destructive admin actions, have not been certified by page-loading checks.
