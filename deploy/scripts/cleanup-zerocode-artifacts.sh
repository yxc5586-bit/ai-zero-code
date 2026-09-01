#!/usr/bin/env bash
set -euo pipefail

base_dir="/opt/resume-demo/zero-code"
execute=false
if [[ "${1:-}" == "--execute" ]]; then
  execute=true
elif [[ -n "${1:-}" && "${1:-}" != "--dry-run" ]]; then
  echo "usage: $0 [--dry-run|--execute]" >&2
  exit 2
fi

resolved_base="$(readlink -f -- "$base_dir")"
if [[ "$resolved_base" != "$base_dir" ]]; then
  echo "unexpected base path: $resolved_base" >&2
  exit 1
fi

targets=(
  "$base_dir/tmp/code_output"
  "$base_dir/tmp/screenshots"
)

for target in "${targets[@]}"; do
  [[ -d "$target" ]] || continue
  [[ ! -L "$target" ]] || { echo "refusing symbolic-link target: $target" >&2; exit 1; }
  resolved_target="$(readlink -f -- "$target")"
  [[ "$resolved_target" == "$base_dir/"* ]] || { echo "target escaped base path: $target" >&2; exit 1; }

  while IFS= read -r -d '' candidate; do
    printf '%s\n' "$candidate"
    if [[ "$execute" == true ]]; then
      rm -rf -- "$candidate"
    fi
  done < <(find "$resolved_target" -mindepth 1 -maxdepth 1 -mmin +1440 -print0)
done
