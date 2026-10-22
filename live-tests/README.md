# Live Tests module

Runs **real HTTP requests** through the existing admin-service and gateway. The terminal runner and Admin → Live Tests use the same server-side fixed test catalog. No WireMock, fake bank responses, generated products, direct database business writes, or simulated PASS results are used.

## Run from the terminal

Requires Python 3 (standard library only), the running shop, and an account with SYSTEM_TEST_VIEW and SYSTEM_TEST_RUN. Start the app normally, then:

```bash
python3 live-tests/run.py --base-url http://localhost:4200 --username admin --report live-tests/report.json
```

The password is requested privately. CI may provide `ESHOP_TEST_USERNAME` and `ESHOP_TEST_PASSWORD` through its secret manager. An existing bearer credential can instead be supplied as `ESHOP_TEST_TOKEN`; no token argument, credential file or report field is created. Do not paste credentials into shell commands or commit them.

For direct gateway access use `--base-url http://localhost:8080 --origin http://localhost:4200`; Origin must be trusted by the existing gateway/session settings. TLS certificate verification stays enabled, redirects are not followed. The local server runner defaults to `http://api-gateway:8080`; deployments can set `LIVE_TESTS_GATEWAY_URL` to a trusted HTTPS gateway. Guest cookies configured Secure require an HTTPS test gateway.

Optional isolated guest cookie check:

```bash
python3 live-tests/run.py --guest-session --report live-tests/guest-report.json
```

Resume an existing report without starting another run using `--run-id UUID`. Exit codes: 0 all passed, 1 any failed, 2 blocked prerequisite(s), 3 transport/client error, 130 interrupted waiting. A blocked result is never converted to success.

## Admin UI

Open `/admin/live-tests` under Settings. Start a run, optionally include guest session bootstrap/revocation, watch each fixed step, reopen your own last 20 runs and download the actual JSON report. Every step shows status, HTTP status, latency and correlation ID where the downstream service provides one. A missing configuration or unimplemented workflow cannot appear as an invented successful execution.

`GET /api/v1/admin/live-tests/runs` lists your recent runs; `POST` starts a job and returns 202; `GET /api/v1/admin/live-tests/runs/{id}` polls/exports your own report. Both permissions are required to start. The original actor's bearer identity is kept in memory while the job calls existing service APIs; downstream authorization is still enforced. Reports never contain tokens, cookies or raw business response bodies. Session credentials are not sent to anonymous probes.

## Deployment

Apply `admin-service/src/main/resources/db/upgrade/20261020_live_tests.sql` to admin-db and `auth-service/src/main/resources/db/upgrade/20261020_live_test_permissions.sql` to auth-db using the existing upgrade process. Permission migration adds definitions, not group grants. Rebuild admin-service, api-gateway and the Angular frontend; future auth-service builds preserve definitions via DataInitializer. admin-service must have AUTHORIZATION_AUTH_URL configured as usual.

One active run per admin-service process is allowed; extra starts return 409. The local shop uses one admin-service instance. Do not use this in multiple replicas without a distributed run lease. Interrupted RUNNING reports are marked failed on service startup; unexecuted steps become blocked, rather than silently resumed with a different user's session.

## What a real pass means

Most checks validate availability and selected response contracts against existing data. An empty valid list proves the API responds, not that order processing has been tested. The catalog probe validates actual product/SKU arrays. Payment availability proves only configuration, never settlement. The workflow probe reports the actual unsupported/unavailable route; it does not implement a workflow engine. No orders, stock reservations, payments, refunds, notifications, workflow activation or AI proposal execution are performed. The optional guest test creates and revokes one isolated server session; an interrupted call may leave it to expire normally.

Use this module for progressive integration diagnosis. Additional role accounts, configured bank credentials, approved recipients and controlled business fixtures are prerequisites for full acceptance scenarios from the master specification. Missing capabilities are recorded in `docs/ai-automation-gap-analysis.md`.
