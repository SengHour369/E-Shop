# Order summary permission fix — 2026-10-18

Request `e94580b9-5c9b-46ed-bc95-146890884b2e` was a GET to `/api/v1/orders/summary`. The authenticated admin could list orders but the summary required `REPORT_VIEW`, unlike the order list and detail endpoints, which require `ORDER_VIEW_ALL`.

The order-status summary now requires `ORDER_VIEW_ALL`, matching the order management page that consumes these counts. Live account/permission verification remains required; no account grants were changed.

Two focused regression tests passed: authorization precedes the repository query, and a denied caller cannot read summary data. Order Service was rebuilt and recreated with the existing deployment configuration.

Real HTTP checks through `http://192.168.1.10:41075`:

- Admin summary: HTTP 200; total 2 orders, 1 pending, 1 failed.
- Admin order list: HTTP 200.
- Summary without authentication: HTTP 401.

See [sanitized results](order-summary-permission-http-2026-10-18.json).
