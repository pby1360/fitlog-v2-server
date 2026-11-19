#!/usr/bin/env bash
set -euo pipefail

fail() {
  printf '::error::%s\n' "$1" >&2
  exit 1
}

has_commit() {
  [[ $1 =~ ^[0-9a-f]{40}$ ]] && git cat-file -e "$1^{commit}" 2>/dev/null
}

case "$GITHUB_EVENT_NAME" in
  pull_request)
    has_commit "$PR_BASE" && has_commit "$PR_HEAD" || fail 'PR commit history is unavailable.'
    from=$(git merge-base "$PR_BASE" "$PR_HEAD") || fail 'The PR has no available merge base.'
    to=$PR_HEAD
    ;;
  push)
    [[ $GITHUB_REF == refs/heads/main ]] || fail 'Only main pushes can be evaluated for deployment.'
    from=$PUSH_BEFORE
    to=$GITHUB_SHA
    has_commit "$from" && has_commit "$to" || fail 'The complete push range is unavailable.'
    git merge-base --is-ancestor "$from" "$to" || fail 'The push is not a fast-forward of the previous main.'
    ;;
  workflow_dispatch)
    [[ $GITHUB_REF == refs/heads/main && $CONFIRM_DEPLOY == true ]] ||
      fail 'Manual production deployment requires main and explicit confirmation.'
    printf 'kind=runtime\n' >> "$GITHUB_OUTPUT"
    exit 0
    ;;
  *)
    fail 'Unsupported event for deploy classification.'
    ;;
esac

# Treat renames as an added and a removed path so both sides are classified.
paths=$(mktemp)
trap 'rm -f "$paths"' EXIT
git diff --no-ext-diff --no-renames --name-only -z "$from" "$to" -- > "$paths" ||
  fail 'Could not enumerate changed files.'

tests=false
runtime=false
review=false
while IFS= read -r -d '' path; do
  case "$path" in
    *.md|.agents/*|.github/workflows/docs-sync.yml)
      ;;
    src/test/*)
      tests=true
      ;;
    .env*|gcp-setup-wif.sh|artifact-cleanup-policy.json|docker-compose.yml|src/main/resources/application*|src/main/java/*/domain/auth/*|src/main/java/*/global/security/*|src/main/java/*/global/config/SecurityConfig.java)
      review=true
      printf 'Deployment review required for %q\n' "$path"
      ;;
    src/main/*|Dockerfile|.dockerignore|build.gradle|settings.gradle|gradle/*|gradlew|gradlew.bat)
      runtime=true
      ;;
    *)
      review=true
      printf 'Deployment review required for %q\n' "$path"
      ;;
  esac
done < "$paths"

kind=non-runtime
if "$review"; then
  kind=review
  printf '::warning::Review the listed paths before production deployment. Automatic deployment is blocked.\n'
elif "$runtime"; then
  kind=runtime
elif "$tests"; then
  kind=tests
fi

printf 'kind=%s\n' "$kind" >> "$GITHUB_OUTPUT"
printf '### Deployment classification: `%s`\n' "$kind" >> "$GITHUB_STEP_SUMMARY"
