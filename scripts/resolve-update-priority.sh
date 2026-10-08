#!/usr/bin/env bash
# Decides the in-app update priority of a release (openspec/specs/app-deployment, app-updates).
#
#   scripts/resolve-update-priority.sh --labels "<labels, one per line>"   the labels of the release pull request
#   scripts/resolve-update-priority.sh --manual true|false                 the required-update input of a manual run
#   scripts/resolve-update-priority.sh --check --labels "<labels>"         only validate, for the pull request check
#
# A pull request into main carries exactly one of the labels update-optional (priority 0) or
# update-required (priority 5). Prints UPDATE_PRIORITY=<0|5>, UPDATE_KIND=<optional|required> and
# UPDATE_SOURCE=<pull request label|manual input> on stdout, or exits non-zero with a GitHub Actions
# ::error:: line. Labels and setup: docs/deployment.md#required-updates
set -euo pipefail

OPTIONAL_LABEL=update-optional
REQUIRED_LABEL=update-required

fail() {
  echo "::error::$1 See docs/deployment.md#required-updates." >&2
  exit 1
}

mode=""
labels=""
manual=""
check=false
while [ $# -gt 0 ]; do
  case "$1" in
    --labels) [ $# -ge 2 ] || fail "--labels needs a value."; mode=labels; labels="$2"; shift 2 ;;
    --manual) [ $# -ge 2 ] || fail "--manual needs a value."; mode=manual; manual="$2"; shift 2 ;;
    --check) check=true; shift ;;
    *) fail "Unknown argument '$1'." ;;
  esac
done

emit() { # <kind> <source>
  local priority=0
  [ "$1" = required ] && priority=5
  echo "UPDATE_PRIORITY=$priority"
  echo "UPDATE_KIND=$1"
  echo "UPDATE_SOURCE=$2"
}

case "$mode" in
  manual)
    $check && fail "--check takes --labels, not --manual."
    case "$manual" in
      true) emit required "manual input" ;;
      false) emit optional "manual input" ;;
      *) fail "The required-update input must be true or false, not '$manual'." ;;
    esac
    ;;
  labels)
    has_optional=false
    has_required=false
    while IFS= read -r label; do
      # Trims surrounding whitespace and a carriage return from each line.
      label="$(printf '%s' "$label" | tr -d '\r' | sed 's/^[[:space:]]*//; s/[[:space:]]*$//')"
      [ "$label" = "$OPTIONAL_LABEL" ] && has_optional=true
      [ "$label" = "$REQUIRED_LABEL" ] && has_required=true
    done <<< "$labels"
    if $has_optional && $has_required; then
      fail "The pull request has both $OPTIONAL_LABEL and $REQUIRED_LABEL; keep exactly one."
    elif ! $has_optional && ! $has_required; then
      fail "The pull request needs exactly one of the labels $OPTIONAL_LABEL or $REQUIRED_LABEL."
    fi
    kind=optional
    $has_required && kind=required
    if $check; then
      echo "Release update label: update-$kind"
    else
      emit "$kind" "pull request label"
    fi
    ;;
  *) fail "Pass --labels or --manual." ;;
esac
