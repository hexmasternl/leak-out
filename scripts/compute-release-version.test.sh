#!/usr/bin/env bash
# Tests scripts/compute-release-version.sh in a throwaway git repository.
#   bash scripts/compute-release-version.test.sh
set -euo pipefail

script="$(cd "$(dirname "$0")" && pwd)/compute-release-version.sh"
repo="$(mktemp -d)"
trap 'rm -rf "$repo"' EXIT
cd "$repo"
[ "$(pwd)" = "$(cd "$repo" && pwd)" ] || { echo "not in the throwaway repository" >&2; exit 1; }
git init -q
commit() { git -c user.name=test -c user.email=test@test commit -q --allow-empty -m "$1"; git rev-parse HEAD; }

failures=0
expect_ok() { # <version> <commit> <expected stdout>
  local out
  if out=$(bash "$script" "$1" "$2" 2>&1) && [ "$out" = "$3" ]; then echo "ok   $1"
  else echo "FAIL $1: expected '$3', got '$out'"; failures=$((failures + 1)); fi
}
expect_fail() { # <version> <commit> <part of the error>
  local out
  if out=$(bash "$script" "$1" "$2" 2>&1); then echo "FAIL $1: expected an error, got '$out'"; failures=$((failures + 1))
  elif [[ "$out" == *"$3"* ]]; then echo "ok   $1 refused"
  else echo "FAIL $1: expected '$3' in '$out'"; failures=$((failures + 1)); fi
}

first=$(commit first)
expect_ok 0.1.0 "$first" $'VERSION_NAME=0.1.0\nVERSION_CODE=1000'
expect_ok 1.2.3 "$first" $'VERSION_NAME=1.2.3\nVERSION_CODE=1002003'
expect_ok 01.02.03 "$first" $'VERSION_NAME=1.2.3\nVERSION_CODE=1002003'
expect_ok 2099.999.999 "$first" $'VERSION_NAME=2099.999.999\nVERSION_CODE=2099999999'
expect_fail 2100.0.0 "$first" "does not fit"
expect_fail 0.1000.0 "$first" "does not fit"
expect_fail 0.1.1000 "$first" "does not fit"
expect_fail 0.1.0-5 "$first" "is not a <major>.<minor>.<patch>"
expect_fail "" "$first" "is not a <major>.<minor>.<patch>"
expect_fail 0.1.0 "" "No commit given"

git tag v0.1.0 "$first"
expect_ok 0.1.0 "$first" $'VERSION_NAME=0.1.0\nVERSION_CODE=1000'   # re-run on the released commit
second=$(commit second)
expect_ok 0.1.1 "$second" $'VERSION_NAME=0.1.1\nVERSION_CODE=1001'
expect_fail 0.1.0 "$second" "already exists on another commit"
git tag v0.10.0 "$second"
expect_fail 0.9.5 "$second" "below the newest release v0.10.0"   # numeric, not alphabetical, order
expect_ok 0.10.1 "$second" $'VERSION_NAME=0.10.1\nVERSION_CODE=10001'

# Order preservation: every bump gives a strictly higher code.
git tag -d v0.1.0 v0.10.0 > /dev/null
previous=0
for v in 0.1.0 0.1.1 0.1.999 0.2.0 0.999.999 1.0.0 1.0.1 2099.999.999; do
  code=$(bash "$script" "$v" "$second" | sed -n 's/^VERSION_CODE=//p')
  if (( code <= previous )); then echo "FAIL order: $v gives $code, not above $previous"; failures=$((failures + 1)); fi
  previous=$code
done
echo "ok   codes increase with the version"

if (( failures > 0 )); then echo "$failures test(s) failed"; exit 1; fi
echo "all tests passed"
