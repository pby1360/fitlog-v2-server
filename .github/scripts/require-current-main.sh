#!/usr/bin/env bash
set -euo pipefail

# The deploy job is serialized. Check after entering it and again before publishing.
main_ref=$(git ls-remote --exit-code origin refs/heads/main) || {
  printf '::error::Could not verify the current main SHA.\n' >&2
  exit 1
}
if [[ $main_ref == "$GITHUB_SHA"$'\t'refs/heads/main ]]; then
  printf 'current=true\n' >> "$GITHUB_OUTPUT"
else
  printf '::notice::The deployment commit is no longer current on main.\n'
  printf 'current=false\n' >> "$GITHUB_OUTPUT"
fi
