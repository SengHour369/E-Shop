#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
export MAVEN_OPTS="${MAVEN_OPTS:-} -Djava.net.preferIPv4Stack=true"
runtime_dir="${ESHOP_RUNTIME_DIR:-/tmp/eshop-local}"
mkdir -p "$runtime_dir"
# Local SMTP settings are private and may contain quoted app passwords.
if [[ -f .env ]]; then
  set -a
  source .env
  set +a
fi
case "${ESHOP_MAIL_MODE:-mailpit}" in
  smtp)
    if [[ -z "${MAIL_USERNAME:-}" || -z "${MAIL_PASSWORD:-}" ]]; then
      echo "Real email requires MAIL_USERNAME and MAIL_PASSWORD in .env. Credentials are never printed." >&2
      exit 1
    fi
    export SPRING_MAIL_HOST="${MAIL_HOST:-smtp.gmail.com}" SPRING_MAIL_PORT="${MAIL_PORT:-587}"
    export SPRING_MAIL_USERNAME="$MAIL_USERNAME" SPRING_MAIL_PASSWORD="$MAIL_PASSWORD"
    export SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
    export SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true
    export SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_REQUIRED=true
    echo "Mail mode: real SMTP (credentials hidden)"
    ;;
  mailpit)
    export SPRING_MAIL_HOST=localhost SPRING_MAIL_PORT=1025
    export SPRING_MAIL_USERNAME=no-reply@eshop.local SPRING_MAIL_PASSWORD=""
    export SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=false
    export SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=false
    export SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_REQUIRED=false
    echo "Mail mode: local inbox at http://localhost:8025 (no external delivery)"
    ;;
  *) echo "ESHOP_MAIL_MODE must be smtp or mailpit" >&2; exit 1 ;;
esac
export SPRING_MAIL_PROPERTIES_MAIL_SMTP_CONNECTIONTIMEOUT=5000
export SPRING_MAIL_PROPERTIES_MAIL_SMTP_TIMEOUT=10000
export SPRING_MAIL_PROPERTIES_MAIL_SMTP_WRITETIMEOUT=10000
if [[ "${1:-}" != "--no-build" ]]; then
  mvn -B -ntp -DskipTests package
fi
docker compose -f compose.yaml -f compose.ai.yaml -f compose.local.yaml up -d --wait --wait-timeout 180 kafka auth-db catalog-db order-db payment-db admin-db gateway-db gateway-redis mailpit ai-db notification-db
export JWT_SECRET="${JWT_SECRET:-66546A5744444446E5A7234743777217A25432A462D4A614E645267556B587032733576}"
export AUTH_COOKIE_SECURE=false AUTH_COOKIE_SAME_SITE=Lax CATALOG_SEED_ENABLED=true
export APP_BASE_URL=http://localhost:8080
export GATEWAY_CORS_ORIGINS="${GATEWAY_CORS_ORIGINS:-http://localhost:4200,http://127.0.0.1:4200,http://localhost:8080}"
wait_for_service() {
  local service="$1" port="$2"
  for ((attempt=0; attempt<120; attempt++)); do
    local status
    status="$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 "http://localhost:$port/actuator/health" || true)"
    if [[ "$status" == 200 ]] || { [[ "$status" == 401 ]] && grep -q 'Started .*Application' "$runtime_dir/$service.log"; }; then
      echo "$service ready on port $port (HTTP $status)"
      return 0
    fi
    if ! kill -0 "$(cat "$runtime_dir/$service.pid")" 2>/dev/null; then
      tail -n 40 "$runtime_dir/$service.log" >&2
      return 1
    fi
    sleep 2
  done
  echo "$service did not become healthy; see $runtime_dir/$service.log" >&2
  return 1
}
for service in discovery-server api-gateway auth-service catalog-service order-service payment-service admin-service ai-service notification-service; do
  pid_file="$runtime_dir/$service.pid"
  if [[ -f "$pid_file" ]] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
    echo "$service already running"
    continue
  fi
  args=()
  case "$service" in
    catalog-service) args+=(--spring.datasource.url=jdbc:postgresql://localhost:5433/catalog_db) ;;
    order-service) args+=(--spring.datasource.url=jdbc:postgresql://localhost:5434/order_db) ;;
    payment-service) args+=(--spring.datasource.url=jdbc:postgresql://localhost:5435/payment_db) ;;
  esac
  nohup "$JAVA_HOME/bin/java" -Xms32m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Xss512k -jar "$service/target/$service-0.0.1-SNAPSHOT.jar" "${args[@]}" > "$runtime_dir/$service.log" 2>&1 < /dev/null &
  echo "$!" > "$pid_file"
  echo "Started $service (log: $runtime_dir/$service.log)"
  if [[ "$service" == discovery-server ]]; then wait_for_service "$service" 8761; fi
done
for entry in api-gateway:8080 auth-service:8081 catalog-service:8082 order-service:8083 payment-service:8084 admin-service:8087 ai-service:8085 notification-service:8086; do
  wait_for_service "${entry%:*}" "${entry#*:}"
done
# Eureka registration and gateway discovery caches can lag behind service startup.
for service in auth-service catalog-service order-service payment-service admin-service; do
  routed=false
  for ((attempt=0; attempt<90; attempt++)); do
    if curl -fsS --max-time 5 "http://localhost:8080/openapi/$service" >/dev/null 2>&1; then
      routed=true
      break
    fi
    sleep 2
  done
  if [[ "$routed" != true ]]; then
    echo "Gateway cannot reach $service; see $runtime_dir/api-gateway.log" >&2
    exit 1
  fi
done
echo "Backend ready: http://localhost:8080"
wait
