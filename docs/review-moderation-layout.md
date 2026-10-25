# Review moderation

The admin Review Moderation page supports server-side search, rating, status, customer type, and inclusive UTC date-range filters. Search covers author, title, body, and product name. Dates are passed as ISO local dates and interpreted as UTC midnight through the exclusive midnight after the end date.

The existing GET `/api/v1/admin/guest-reviews` endpoint retains PENDING as its default status for older clients. The new page explicitly requests ALL initially. The response includes a 20-row page, totalElements, totalPages, last, and counts for each status. Counts honor non-status filters across all matching pages. Product names are loaded in one batch for the current page.

The endpoint retains PRODUCT_REVIEW_MODERATE authorization. Only PENDING records can be approved or rejected, and existing server conflict checks and audit logging remain in place. The page refreshes authoritative rows and counts after a decision. Public storefront queries continue returning approved reviews only. No bulk or automatic moderation is added.
