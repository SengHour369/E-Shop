# Approval flow deployment correction — 20 October 2026

The user's running environment diverged from the implemented workflow for three confirmed reasons:

1. Running AI/Catalog JARs were older than the verified source: no `/manual-product` or `/requests` endpoints and no legacy Product approval guard.
2. `compose.frontend.yaml` was absent even though the running frontend originated from it. The startup script skipped rebuilding the frontend without this file.
3. The shared AI/Catalog approval-signing key was unconfigured. Signed approved operations could not execute.

Corrections applied to the local Docker environment:

- Restored `compose.frontend.yaml` with the existing local-only port mapping `127.0.0.1:4200:80` and the real frontend build context.
- Initialized the missing shared signing key in private `.env.local`; no key was printed or committed, no user grants were expanded.
- Applied `20261020_unified_product_approval.sql` to the existing AI database. This adds request metadata and before-image storage, preserving requests and live catalog data.
- Ran `./run-docker.sh --no-deps ai-service catalog-service frontend` to build and restart the coordinated services.

Verification after restart:

- Running AI JAR now contains `/manual-product` and `/requests`.
- Running Catalog JAR contains `APPROVAL_REQUIRED` protection.
- AI/Catalog signing keys are configured and match (values never included in the report).
- Actual served frontend JavaScript contains the new manual-product staging API.
- `/admin/my-requests` and `/admin/review-authorization` return HTTP 200 frontend HTML.
- Catalog OpenAPI and storefront product HTTP requests return 200 after normal service-discovery refresh. Temporary 500/503 responses during replacement were followed by verified recovery.
- Protected import endpoints return 401 without authentication. This is not a successful authenticated workflow test.

User test sequence after refreshing the browser:

1. Sign in as the maker with IMPORT_UPLOAD and the relevant Product/SKU grants.
2. Create or edit a Product and choose an independent authorized reviewer ID.
3. Submit for Approval; confirm the request in My Requests. Live product data remains unchanged while pending.
4. Sign in as that assigned reviewer with IMPORT_REVIEW, IMPORT_APPROVE, IMPORT_EXECUTE and the relevant domain grants.
5. Open Review Authorization and inspect the actual revision, changes, validation and images.
6. Approve & Execute, Reject with a reason, or Return to Maker with a reason.
7. For returned requests, the maker edits the existing request and resubmits; for approved requests, inspect actual execution results. Use Continue approved execution for remaining chunks.

A maker cannot approve their own request. Existing stock changes and unsupported deletion operations require their supported separate flows. No maker/reviewer credentials were provided, so authenticated submission/approval/execution has not been claimed as verified.

Machine-readable evidence: `reports/ai-automation/approval-deployment-verification.json`.
