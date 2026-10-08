#!/usr/bin/env bash
# Fails when the total line coverage of a Cobertura report is below the threshold.
# Usage: coverage-gate.sh <cobertura-report> [threshold-basis-points, default 8000 = 80%]
# The rate is compared in basis points with integer arithmetic, so exactly 80% passes.
# When GITHUB_STEP_SUMMARY is set, the result is written to the job summary.
set -euo pipefail

report="$1"
threshold="${2:-8000}"

# The first line-rate attribute is the one on the root <coverage> element: the total.
rate=$(grep -o -m1 'line-rate="[0-9.]*"' "$report" | head -n1 | cut -d'"' -f2)
if [[ -z "$rate" ]]; then
  echo "::error::No line-rate found in $report"
  exit 1
fi

whole="${rate%%.*}"
fraction="${rate#*.}"
[[ "$fraction" == "$rate" ]] && fraction=0
fraction=$(printf '%-4s' "$fraction" | tr ' ' '0' | cut -c1-4)
basis_points=$((10#$whole * 10000 + 10#$fraction))

measured=$(printf '%d.%02d%%' $((basis_points / 100)) $((basis_points % 100)))
required=$(printf '%d.%02d%%' $((threshold / 100)) $((threshold % 100)))

if (( basis_points >= threshold )); then
  result="passed"
else
  result="failed"
fi

if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
  {
    echo "### Code coverage"
    echo
    echo "| Line coverage | Threshold | Result |"
    echo "| --- | --- | --- |"
    echo "| $measured | $required | $result |"
  } >> "$GITHUB_STEP_SUMMARY"
fi

if [[ "$result" == "failed" ]]; then
  echo "::error::Line coverage $measured is below the required $required"
  exit 1
fi

echo "Line coverage $measured meets the required $required"
