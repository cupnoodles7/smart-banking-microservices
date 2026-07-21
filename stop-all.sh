#!/usr/bin/env bash
#
# stop-all.sh — stop everything run-all.sh started.
# Kills recorded PIDs, then frees any process still bound to our ports.

set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="$ROOT/.run"

echo "== stopping services =="

# 1) kill by recorded PID
if [ -d "$RUN_DIR" ]; then
  for pidf in "$RUN_DIR"/*.pid; do
    [ -e "$pidf" ] || continue
    name="$(basename "$pidf" .pid)"
    pid="$(cat "$pidf")"
    if kill -0 "$pid" 2>/dev/null; then
      echo "  stopping $name (pid $pid)"
      kill "$pid" 2>/dev/null
    fi
    rm -f "$pidf"
  done
fi

# 2) fallback: free any leftover *java* process still holding a port.
#    We only ever kill java (our Spring Boot apps) — never Docker proxies or
#    anything else that happens to share these ports.
kill_java_on_port() {
  local port="$1" sig="${2:-}"
  local pids="$(lsof -ti tcp:"$port" 2>/dev/null || true)"
  for pid in $pids; do
    local comm; comm="$(ps -p "$pid" -o comm= 2>/dev/null || true)"
    case "$comm" in
      *java*) echo "  freeing port $port (java pid $pid)"; kill $sig "$pid" 2>/dev/null ;;
      *)      echo "  leaving port $port alone (held by ${comm:-unknown}, not ours)" ;;
    esac
  done
}

for port in 8888 8761 8080 8081 8082 8083 8084 8085; do kill_java_on_port "$port"; done
sleep 2
for port in 8888 8761 8080 8081 8082 8083 8084 8085; do kill_java_on_port "$port" -9; done

echo "== done =="
