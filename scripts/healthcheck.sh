#!/usr/bin/env bash
set -euo pipefail

REST_ENDPOINT=${REST_GATEWAY_URL:-http://127.0.0.1:8080/api/health}
GRPC_TARGET=${GRPC_HEALTH_TARGET:-127.0.0.1:5051}
GRPC_SERVICE=${GRPC_HEALTH_SERVICE:-}

log() {
  printf '[%s] %s\n' "$(date '+%F %T')" "$*"
}

fail() {
  log "FAIL: $*"
  exit 1
}

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    fail "Missing required command: $1"
  fi
}

require_cmd curl
require_cmd grpcurl
require_cmd python3

log "Requesting REST endpoint ${REST_ENDPOINT}"
if ! rest_response=$(curl -fsS "$REST_ENDPOINT"); then
  fail "REST request failed"
fi
log "REST response: $rest_response"

if ! rest_parsed=$(python3 - "$rest_response" <<'PY'
import json
import sys

try:
    data = json.loads(sys.argv[1])
except Exception as exc:  # pragma: no cover - defensive branch
    print(f"error|{exc}")
    raise SystemExit(1)

ok = str(data.get("ok", False)).lower()
status = str(data.get("status", ""))
status_code = str(data.get("statusCode", ""))
print(f"{ok}|{status}|{status_code}")
PY
); then
  fail "Unable to parse REST response"
fi

IFS='|' read -r rest_ok rest_status rest_status_code <<<"$rest_parsed"

grpc_payload=$(printf '{"service":"%s"}' "$GRPC_SERVICE")
log "Invoking gRPC health check on ${GRPC_TARGET}"
if ! grpc_response=$(grpcurl -plaintext "$GRPC_TARGET" grpc.health.v1.Health/Check -d "$grpc_payload"); then
  fail "gRPC health check failed"
fi
log "gRPC response: $grpc_response"

if ! grpc_status=$(python3 - "$grpc_response" <<'PY'
import json
import sys

try:
    data = json.loads(sys.argv[1])
except Exception as exc:  # pragma: no cover - defensive branch
    print(f"error|{exc}")
    raise SystemExit(1)

print(data.get("status", ""))
PY
); then
  fail "Unable to parse gRPC response"
fi

if [[ "$rest_ok" == "true" && "$rest_status" == "SERVING" && "$grpc_status" == "SERVING" ]]; then
  log "PASS"
  exit 0
fi

fail "Health checks did not report SERVING (REST ok=${rest_ok}, REST status=${rest_status}, gRPC status=${grpc_status})"
