#!/usr/bin/env bash
set -euo pipefail

health_url="${1:?health URL is required}"
timeout_seconds="${2:-60}"
deadline=$((SECONDS + timeout_seconds))

until curl --fail --silent --show-error --max-time 5 "$health_url" >/dev/null; do
  if (( SECONDS >= deadline )); then
    echo "health check timed out: $health_url" >&2
    exit 1
  fi
  sleep 2
done
