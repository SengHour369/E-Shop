# Sales report reference layout

The Sales Report route now uses a dedicated layout with four metrics, a monthly
sales/order combination SVG chart, category donut, twelve-month breakdown, CSV export
and an accessible native month-detail dialog. Payment and tax reports keep their
existing components. The selected year is captured with the results; failed optional
category or prior-year reads never produce invented figures.

Existing order monthly totals are unchanged. New read-only endpoints supply the extra data:

- `GET /api/v1/orders/reports/sales-details?year=2026` aggregates recorded item quantities
  by month and recorded item amounts by SKU, excluding deleted orders and CANCELLED,
  FAILED, REFUNDED and CHECKOUT_PENDING statuses. It retains the existing report role checks.
- `POST /api/v1/products/reports/sku-categories` resolves up to 500 real SKU IDs per batch,
  requires current REPORT_VIEW permission and returns current catalog category names.

Orders and catalog services retain ownership of their databases. The order service forwards
the caller's bearer token for catalog category reads. Missing SKU categories become
Uncategorized. If catalog access fails, the category chart is marked unavailable while
monthly item counts remain usable. Category amounts use item-level recorded prices and
current category labels; they are not historical category snapshots and exclude order fees.
Order count, total sales and averages use the existing order totals API in USD.

Previous-year percentage badges appear only for a known nonzero baseline and nonzero
change. A missing baseline is not shown as a fabricated improvement. Month details show
that month's actual summary without claiming an unfiltered order list is month-filtered.
No shipping charges, category shares or comparison percentages from the reference image
are seeded into the production report.
