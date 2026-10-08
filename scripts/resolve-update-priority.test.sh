#!/usr/bin/env bash
# Tests scripts/resolve-update-priority.sh.
#   bash scripts/resolve-update-priority.test.sh
set -euo pipefail

script="$(cd "$(dirname "$0")" && pwd)/resolve-update-priority.sh"

failures=0
expect_ok() { # <name> <expected stdout> <args...>
  local name="$1" expected="$2" out
  shift 2
  if out=$(bash "$script" "$@" 2>&1) && [ "$out" = "$expected" ]; then echo "ok   $name"
  else echo "FAIL $name: expected '$expected', got '$out'"; failures=$((failures + 1)); fi
}
expect_fail() { # <name> <part of the error> <args...>
  local name="$1" part="$2" out
  shift 2
  if out=$(bash "$script" "$@" 2>&1); then echo "FAIL $name: expected an error, got '$out'"; failures=$((failures + 1))
  elif [[ "$out" == *"$part"* && "$out" == "::error::"* ]]; then echo "ok   $name refused"
  else echo "FAIL $name: expected '$part' in '$out'"; failures=$((failures + 1)); fi
}

required=$'UPDATE_PRIORITY=5\nUPDATE_KIND=required\nUPDATE_SOURCE=pull request label'
optional=$'UPDATE_PRIORITY=0\nUPDATE_KIND=optional\nUPDATE_SOURCE=pull request label'

expect_ok "required label" "$required" --labels "update-required"
expect_ok "optional label" "$optional" --labels "update-optional"
expect_ok "label among others" "$required" --labels $'enhancement\nupdate-required\napp'
expect_ok "label with surrounding whitespace and CRLF" "$optional" --labels $'  update-optional \r'

both="both update-optional and update-required"
none="exactly one of the labels update-optional or update-required"
expect_fail "no labels" "$none" --labels ""
expect_fail "unrelated labels only" "$none" --labels $'enhancement\napp'
expect_fail "a label that only looks alike" "$none" --labels "update-required-later"
expect_fail "both labels" "$both" --labels $'update-optional\nupdate-required'

expect_ok "manual true" $'UPDATE_PRIORITY=5\nUPDATE_KIND=required\nUPDATE_SOURCE=manual input' --manual true
expect_ok "manual false" $'UPDATE_PRIORITY=0\nUPDATE_KIND=optional\nUPDATE_SOURCE=manual input' --manual false
expect_fail "manual not a boolean" "must be true or false" --manual yes

expect_ok "check passes with one label" "Release update label: update-required" --check --labels $'app\nupdate-required'
expect_ok "check after the labels" "Release update label: update-optional" --labels "update-optional" --check
expect_fail "check without a label" "$none" --check --labels "app"
expect_fail "check with both labels" "$both" --check --labels $'update-required\nupdate-optional'

expect_fail "no arguments" "Pass --labels or --manual"
expect_fail "unknown argument" "Unknown argument" --priority 5
expect_fail "labels without a value" "--labels needs a value" --labels

if (( failures > 0 )); then echo "$failures test(s) failed"; exit 1; fi
echo "all tests passed"
