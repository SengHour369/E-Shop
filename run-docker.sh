#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
# Compose only auto-loads .env. Support the owner's legacy env file and private overrides.
compose=(docker compose)
if [[ -n "${ESHOP_ENV_FILE:-}" ]]; then
  if [[ ! -f "$ESHOP_ENV_FILE" ]]; then
    echo "ESHOP_ENV_FILE does not exist." >&2
    exit 1
  fi
  compose+=(--env-file "$ESHOP_ENV_FILE")
else
  if [[ -f .env ]]; then
    compose+=(--env-file .env)
  elif [[ -f env ]]; then
    compose+=(--env-file env)
  fi
  if [[ -f .env.local ]]; then
    compose+=(--env-file .env.local)
  fi
fi
compose+=(-f compose.yaml -f compose.ai.yaml -f compose.local.yaml)
# Local Docker/frontend overrides are optional and may be absent in another checkout.
if [[ -f compose.frontend.yaml ]]; then
  compose+=(-f compose.frontend.yaml)
fi
if [[ -f compose.docker-local.yaml ]]; then
  compose+=(-f compose.docker-local.yaml)
fi
https_enabled="$("${compose[@]}" config --environment | awk -F= '$1=="ESHOP_HTTPS_ENABLED" {print tolower($2)}')"
if [[ "$https_enabled" == "true" ]]; then
  if [[ ! -f .local/https/server.crt || ! -f .local/https/server.key ]]; then
    echo "Generate local HTTPS certificates with scripts/configure-local-https.py --ip YOUR_LAN_IP" >&2
    exit 1
  fi
fi
# Read only the opt-in flag from Compose's resolved environment; never print credentials.
automation_enabled="$("${compose[@]}" config --environment | awk -F= '$1=="COMMERCE_AUTOMATION_ENABLED" {print tolower($2)}')"
if [[ "$automation_enabled" == "true" ]]; then
  compose+=(-f compose.automation.yaml)
fi
mail_mode="$("${compose[@]}" config --environment | awk -F= '$1=="ESHOP_MAIL_MODE" {print tolower($2)}')"
case "$mail_mode" in
  smtp) compose+=(-f compose.smtp.yaml) ;;
  ""|mailpit) ;;
  *) echo "ESHOP_MAIL_MODE must be smtp or mailpit" >&2; exit 1 ;;
esac
if [[ "$https_enabled" == "true" ]]; then compose+=(-f compose.https.yaml); fi
"${compose[@]}" up -d --build --wait --wait-timeout 300 "$@"
# Gateway discovery caches can lag behind healthy service containers.
for service in auth-service catalog-service order-service payment-service admin-service; do
  ready=false
  for ((attempt=0; attempt<90; attempt++)); do
    if curl -fsS --max-time 5 "http://localhost:8080/openapi/$service" >/dev/null 2>&1; then
      ready=true
      break
    fi
    sleep 2
  done
  if [[ "$ready" != true ]]; then
    echo "Gateway cannot reach $service. Inspect Docker Compose logs." >&2
    exit 1
  fi
done
if [[ "$https_enabled" == "true" ]]; then
  https_port="$("${compose[@]}" config --environment | awk -F= '$1=="ESHOP_HTTPS_PORT" {print $2}')"
  echo "Docker shop ready: https://localhost:${https_port:-8443} (trust the local CA first)"
else
  echo "Docker shop ready: http://localhost:4200 (gateway: http://localhost:8080)"
fi
