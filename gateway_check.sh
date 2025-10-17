#!/usr/bin/env bash
set -euo pipefail

if [[ ${1:-} == "" ]]; then
  echo "Usage: $0 <public-ip-address>" >&2
  exit 1
fi

PUBLIC_IP="$1"
LOCAL_URL="http://127.0.0.1:8080/api/health"
REMOTE_URL="http://${PUBLIC_IP}:8080/api/health"

log() {
  printf '%s\n' "$1"
}

check_url() {
  local label="$1"
  local url="$2"
  local body
  if ! body=$(curl -sS -m 5 "$url"); then
    log "[$label] FAIL: unable to reach $url"
    return 1
  fi
  if [[ "$body" != *'"ok":true'* ]]; then
    log "[$label] FAIL: unexpected response: $body"
    return 1
  fi
  log "[$label] OK: $body"
  return 0
}

status=0
check_url "local" "$LOCAL_URL" || status=1
check_url "remote" "$REMOTE_URL" || status=1

if [[ $status -eq 0 ]]; then
  log "PASS"
else
  log "FAIL"
fi

exit $status
