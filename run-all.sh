#!/usr/bin/env bash
#
# run-all.sh — launch the whole Smart Banking stack locally, in order,
# each service backgrounded with its own log, gated on a health check.
#
#   ./run-all.sh            start everything
#   ./stop-all.sh           stop everything
#
# Logs:  .run/logs/<service>.log     PIDs: .run/<service>.pid
#
# Notes:
#  - config-server is started in the Spring Cloud Config "native" profile so it
#    serves ./config-repo directly off disk (no git / CONFIG_REPO_URI needed).
#  - Business services import config as optional:, so they still boot on their
#    local fallbacks even if config-server or eureka is down.
#  - Requires: Java 21+, Maven, MongoDB on :27017.

set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="$ROOT/.run"
LOG_DIR="$RUN_DIR/logs"
mkdir -p "$LOG_DIR"

WAIT=240   # max seconds to wait for a service to become healthy (first run downloads deps)

# ---- helpers ---------------------------------------------------------------

start_service() {
  # start_service <name> <dir> <port> [extra mvn args...]
  local name="$1" dir="$2" port="$3"; shift 3
  local log="$LOG_DIR/$name.log"
  echo "▶  starting $name  (:$port)   log → ${log#$ROOT/}"
  # `clean` first: the IDE's Lombok/JDK-25 processor can leave broken .class files in
  # target/, which spring-boot:run (incremental) would otherwise reuse -> runtime
  # "Unresolved compilation problem". Clean forces a fresh Maven compile.
  ( cd "$ROOT/$dir" && exec mvn -Dspring-boot.run.fork=false clean spring-boot:run "$@" ) > "$log" 2>&1 &
  echo $! > "$RUN_DIR/$name.pid"
  echo "$port" > "$RUN_DIR/$name.port"
}

wait_ready() {
  # wait_ready <name> <url> [match-string]   (no match => just expect HTTP 200)
  local name="$1" url="$2" match="${3:-}"
  local pidf="$RUN_DIR/$name.pid"
  local deadline=$(( $(date +%s) + WAIT ))
  while [ "$(date +%s)" -lt "$deadline" ]; do
    if [ -z "$match" ]; then
      curl -sf -o /dev/null "$url" 2>/dev/null && { echo "✔  $name is ready"; return 0; }
    else
      curl -sf "$url" 2>/dev/null | grep -q "$match" && { echo "✔  $name is ready"; return 0; }
    fi
    if [ -f "$pidf" ] && ! kill -0 "$(cat "$pidf")" 2>/dev/null; then
      echo "✗  $name exited early — check $LOG_DIR/$name.log"; return 1
    fi
    sleep 3
  done
  echo "✗  $name not ready after ${WAIT}s — check $LOG_DIR/$name.log"; return 1
}

# ---- preflight -------------------------------------------------------------

command -v mvn     >/dev/null || { echo "Maven not found on PATH"; exit 1; }
command -v curl    >/dev/null || { echo "curl not found on PATH"; exit 1; }
command -v openssl >/dev/null || { echo "openssl not found on PATH (needed to generate dev secrets)"; exit 1; }
if ! nc -z -G2 -w2 localhost 27017 2>/dev/null; then
  echo "⚠  MongoDB does not appear to be running on localhost:27017."
  echo "   Auth/User/Account/Wallet/Transaction need it. Start it, e.g.:"
  echo "     brew services start mongodb-community    # or: docker run -d -p 27017:27017 mongo"
  read -r -p "   Continue anyway? [y/N] " ans; [ "$ans" = "y" ] || exit 1
fi

# ---- secrets ---------------------------------------------------------------
# Load .env if present, else generate ephemeral secrets for this run.
if [ -f "$ROOT/.env" ]; then
  echo "🔑 loading secrets from .env"
  set -a; . "$ROOT/.env"; set +a
fi
: "${JWT_SECRET:=$(openssl rand -hex 32)}"
: "${USER_INTERNAL_API_KEY:=$(openssl rand -hex 24)}"
export JWT_SECRET USER_INTERNAL_API_KEY
echo "🔑 JWT_SECRET and USER_INTERNAL_API_KEY set for this run (create .env to pin stable values)"

echo "== Smart Banking — starting all services =="

# ---- 1) platform: config-server (native profile serves ./config-repo) ------
start_service config-server config-server 8888 \
  "-Dspring-boot.run.profiles=native" \
  "-Dspring-boot.run.arguments=--spring.cloud.config.server.native.search-locations=file:$ROOT/config-repo"
wait_ready config-server "http://localhost:8888/application/default" \
  || echo "   (continuing — services will use their local fallback config)"

# ---- 2) platform: eureka ---------------------------------------------------
start_service eureka-server eureka-server 8761
wait_ready eureka-server "http://localhost:8761/actuator/health" '"status":"UP"' \
  || echo "   (continuing — gateway routing by service name needs Eureka though)"

# ---- 3) business services + gateway (started together) ---------------------
start_service auth-service        auth-service        8081
start_service user-service        user-service        8082
start_service account-service     account-service     8083
start_service wallet-service      wallet-service      8084
start_service transaction-service transaction-service 8085
start_service api-gateway         api-gateway         8080

for pair in \
  "auth-service:8081" "user-service:8082" "account-service:8083" \
  "wallet-service:8084" "transaction-service:8085" "api-gateway:8080"; do
  name="${pair%%:*}"; port="${pair##*:}"
  wait_ready "$name" "http://localhost:$port/actuator/health" '"status":"UP"'
done

# ---- summary ---------------------------------------------------------------
cat <<EOF

== Up (or check logs for any ✗ above) ==
  Config Server   http://localhost:8888
  Eureka          http://localhost:8761
  API Gateway     http://localhost:8080
  Auth            http://localhost:8081
  User            http://localhost:8082
  Account         http://localhost:8083
  Wallet          http://localhost:8084
  Transaction     http://localhost:8085

Tail a log:     tail -f .run/logs/auth-service.log
Stop all:       ./stop-all.sh

Quick auth smoke test (direct to auth-service):
  curl -s -X POST http://localhost:8081/auth/register -H 'Content-Type: application/json' \\
    -d '{"username":"alice","email":"alice@example.com","password":"secret123"}'
  curl -s -X POST http://localhost:8081/auth/login -H 'Content-Type: application/json' \\
    -d '{"usernameOrEmail":"alice","password":"secret123"}'
EOF
