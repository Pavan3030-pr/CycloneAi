#!/usr/bin/env bash
#
# One-command local demo: starts the API on 8080 and the console on 5173, waits for the API to
# answer, and shuts both down on Ctrl-C.
#
# Both servers run in the foreground of their own subshell so that a single interrupt stops the pair.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
API_PORT="${SERVER_PORT:-8080}"
UI_PORT=5173

cleanup() {
  echo
  echo "Stopping the API and the console..."
  # Kill the whole process group of each child, so maven and node take their JVMs and esbuild
  # children with them.
  kill -- "-${API_PID}" 2>/dev/null || true
  kill -- "-${UI_PID}" 2>/dev/null || true
  wait 2>/dev/null || true
  echo "Stopped."
}
trap cleanup EXIT INT TERM

echo "Starting the API on port ${API_PORT}..."
(
  cd "${ROOT_DIR}/backend"
  exec mvn -B spring-boot:run
) > "${ROOT_DIR}/backend.log" 2>&1 &
API_PID=$!

echo "Waiting for the API to answer on /actuator/health..."
for _ in $(seq 1 120); do
  if curl -fsS -m 2 "http://localhost:${API_PORT}/actuator/health" > /dev/null 2>&1; then
    echo "API is up."
    break
  fi
  sleep 1
done

if ! curl -fsS -m 2 "http://localhost:${API_PORT}/actuator/health" > /dev/null 2>&1; then
  echo "The API did not start; see backend.log" >&2
  exit 1
fi

if [ ! -d "${ROOT_DIR}/frontend/node_modules" ]; then
  echo "Installing frontend dependencies (first run only)..."
  (cd "${ROOT_DIR}/frontend" && npm install --no-fund --no-audit)
fi

echo "Starting the console on port ${UI_PORT}..."
(
  cd "${ROOT_DIR}/frontend"
  exec npm run dev
) > "${ROOT_DIR}/frontend.log" 2>&1 &
UI_PID=$!

echo
echo "  Console   http://localhost:${UI_PORT}"
echo "  API docs  http://localhost:${API_PORT}/swagger-ui.html"
echo "  Logs      backend.log, frontend.log"
echo "  Demo      sign in with analyst / cyclone-demo-analyst, or click Demo mode"
echo
echo "Press Ctrl-C to stop both."

wait
