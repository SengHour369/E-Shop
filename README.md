# E-Shop local backend

Requires Java 17, Maven, and Docker Compose. The Angular frontend is in the adjacent `Order_Shop_Frontend` directory.

Start the backend in a terminal and leave it open:

```sh
bash run-local.sh
```

Use `bash run-local.sh --no-build` when the jars are already built. The launcher uses IPv4 for Maven downloads, starts local PostgreSQL databases, Redis, Mailpit and Kafka, waits for dependency health, and runs the Java services with the correct local database ports. Logs and PID files are in `/tmp/eshop-local`. Local HTTP cookies and catalog seeding are enabled for development.

For optional AI inference, also run:

```sh
docker compose -f compose.yaml -f compose.ai.yaml -f compose.local.yaml up -d ai-inference
```

Configure a supported AI provider and its credentials in `.env` for AI responses. The inference container can start without credentials, but cannot generate provider responses.

- Gateway: http://localhost:8080/actuator/health
- Service discovery: http://localhost:8761
- Local verification mail: http://localhost:8025
- Frontend: http://localhost:4200

Start the frontend with `bash run-local.sh` in the adjacent frontend directory.

## Real verification email

The launcher reads private sender settings from ignored `.env`. Set `ESHOP_MAIL_MODE=smtp`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, and `MAIL_PASSWORD`. Gmail requires a sender app password. The launcher fails early when SMTP credentials are missing and never prints them. Restart auth-service after changing these values.

`ESHOP_MAIL_MODE=mailpit` sends only to the development inbox at http://localhost:8025; those messages are not delivered to Gmail. Verification email failures return an error instead of a success response. Password-reset links open the browser reset page. Public registration requires email verification; administrator-created accounts are enabled immediately and do not receive a verification code.

## Cloudinary images

Set `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, and `CLOUDINARY_API_SECRET` in ignored `.env`. All three must belong to the same Cloudinary product environment. Restart catalog-service after changing credentials. The example file intentionally contains no real image credentials.

Uploads preserve the provider's confirmed HTTPS URL. Missing configuration and provider failures return actionable errors. Product updates keep existing images if a replacement upload fails. Multipart catalog image requests use a dedicated gateway timeout.

## Docker: backend and frontend

Keep the frontend checkout at `../Order_Shop_Frontend`. Stop any host Java services and Angular development server using the same ports, then run:

```sh
bash run-docker.sh
```

This builds the backend reactor in a shared Docker stage and serves the production Angular build through nginx on http://localhost:4200. PostgreSQL data stays in the existing Compose volumes. The gateway remains on http://localhost:8080 and local mail on http://localhost:8025. The launcher waits for container health and checks that the gateway can reach the backend services. AI responses require provider configuration. The Docker launcher reads `.env` (or the legacy `env` file when `.env` is absent), then overlays ignored `.env.local` if present. `ESHOP_ENV_FILE` selects one explicit environment file instead. Set `AI_PROVIDER=GEMINI`, `GEMINI_MODEL`, and a private `GEMINI_API_KEY`; changing a file requires recreating `ai-inference`, not just restarting it. Configuration readiness is checked before dependent AI services start. The optional `compose.docker-local.yaml` supplies local frontend/Docker overrides when present.

To stop the stack while keeping database data:

```sh
docker compose -f compose.yaml -f compose.ai.yaml -f compose.local.yaml -f compose.docker-local.yaml stop
```

## Bakong / individual KHQR

The Docker launcher includes `compose.automation.yaml` when the resolved private configuration has `COMMERCE_AUTOMATION_ENABLED=true`. It enables both Order and Payment event processing; a payment-only restart is insufficient for a complete checkout flow.

Use `BAKONG_KHQR_TYPE=INDIVIDUAL` for an individual Bakong account. This mode requires `BAKONG_ACCOUNT_ID` and a Bakong-registered `BAKONG_EMAIL` for provider verification; merchant ID and acquiring bank are not required. The QR display label comes from the configured account identifier. Optional `BAKONG_CITY` defaults to the generic `KH` label. `MERCHANT` mode retains the registered merchant name, merchant ID and acquiring-bank checks.

Before first activation, apply each service's `src/main/resources/db/upgrade/20261011_commerce_automation.sql` to its own database. The current local Order and Payment Docker databases have these migrations applied. Do not use another service's database or enable workers before their required tables exist. See [Bakong verification report](docs/BAKONG_VERIFICATION.md).

## Run or rebuild the Docker frontend

With the backend already running, from the E-Shop directory:

```bash
bash run-docker.sh frontend
```

`compose.frontend.yaml` builds the adjacent `../Order_Shop_Frontend` checkout and serves it at http://localhost:4200 on the existing backend network. A plain `bash run-docker.sh` starts the complete configured stack, including the frontend.

The updated checkout is a single page: saved address selection and inline address management, configured payment choices, actual cart summary and an explicit Place Order action. Phone navigation is replaced by the checkout's fixed action bar on this route. See `docs/SINGLE_PAGE_CHECKOUT.md` for validation and limits.
