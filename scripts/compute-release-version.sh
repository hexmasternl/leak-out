#!/usr/bin/env bash
# Turns the GitVersion version of a release into the Android versionName and versionCode.
#
#   scripts/compute-release-version.sh <major.minor.patch> <commit sha>
#
# Prints VERSION_NAME=<major.minor.patch> and VERSION_CODE=<n> on stdout, or exits non-zero with a
# GitHub Actions ::error:: line. Run it inside the repository: it checks the version against the
# v<x.y.z> release tags. Strategy and limits: docs/deployment.md#versioning
set -euo pipefail

version="${1:-}"
commit="${2:-}"

fail() {
  echo "::error::$1 See docs/deployment.md#versioning." >&2
  exit 1
}

[[ "$version" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]] || fail "'$version' is not a <major>.<minor>.<patch> version."
[ -n "$commit" ] || fail "No commit given."

# 10# keeps a leading zero from being read as octal.
major=$((10#${BASH_REMATCH[1]}))
minor=$((10#${BASH_REMATCH[2]}))
patch=$((10#${BASH_REMATCH[3]}))
if (( major > 2099 || minor > 999 || patch > 999 )); then
  fail "Version $major.$minor.$patch does not fit the version code (major at most 2099, minor and patch at most 999)."
fi
name="$major.$minor.$patch"

# Every release is tagged, so a new release must be above the newest tag, or be that tag's own commit.
latest_tag=$(git tag --list 'v[0-9]*.[0-9]*.[0-9]*' --sort=-v:refname | head -n 1)
if [ -n "$latest_tag" ] && [ "v$name" != "$latest_tag" ]; then
  newest=$(printf '%s\n%s\n' "${latest_tag#v}" "$name" | sort -V | tail -n 1)
  [ "$newest" = "$name" ] || fail "Version $name is below the newest release $latest_tag."
fi
if git rev-parse -q --verify "refs/tags/v$name" > /dev/null \
   && [ "$(git rev-list -n 1 "v$name")" != "$(git rev-parse "$commit")" ]; then
  fail "Tag v$name already exists on another commit."
fi

# MMMMmmmppp keeps the order of the versions, so a higher version always gets a higher code.
# The largest, 2099.999.999, is 2099999999: below Google Play's limit of 2100000000.
echo "VERSION_NAME=$name"
echo "VERSION_CODE=$(( major * 1000000 + minor * 1000 + patch ))"
