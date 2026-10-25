# Payment report

The admin Payment Report reads `GET /api/v1/payments/reports/breakdown?year=YYYY` with existing REPORT_VIEW access. The endpoint groups visible, non-deleted COMPLETED and REFUNDED payment records by payment month, currency, method and status. It uses an inclusive year start and exclusive next-year start.

The selected currency applies to all cards, charts, table rows and CSV output. Completed amounts exclude records currently marked REFUNDED. Average payment is completed amount divided by completed count. Refund totals use the full payment amount of REFUNDED records and the original payment date; they are not a partial-refund ledger or refund-date cash-flow report. This basis appears below the table.

All 12 months appear, including zero months. Payment method labels come from the actual records. Empty years show zero values and a neutral donut, with no fabricated method shares. Export reflects the applied year and currency, not unsaved filter edits.

Run the additive auth migration `20261025_payment_report_breakdown.sql` for existing databases. It maps the endpoint to REPORT_VIEW without granting new user or group permissions. Fresh instances seed the same API mapping.
