#!/usr/bin/env bash
# Writes the Play Console release notes ("What's new") of a release from the commits it contains.
#
#   scripts/generate-release-notes.sh <released commit>
#
# Prints the notes on stdout, one "• " line per note and at most 500 characters, or exits non-zero
# with a GitHub Actions ::error:: line. Run it inside the repository: the release covers the commits
# since the previous v<x.y.z> tag, and only the commits that change the app. Rules: docs/deployment.md#release-notes
set -euo pipefail
# Play counts characters, not bytes, and "•" and "…" are multi-byte.
export LC_ALL=C.UTF-8

max=500
more='• …and more improvements'
fallback='Bug fixes and improvements.'

fail() {
  echo "::error::$1 See docs/deployment.md#release-notes." >&2
  exit 1
}

commit="${1:-}"
[ -n "$commit" ] || fail "No commit given."
head=$(git rev-parse -q --verify "$commit^{commit}") || fail "'$commit' is not a commit."

# Describing from the first parent skips a tag on the released commit itself (a re-run).
if previous=$(git describe --tags --abbrev=0 --match 'v[0-9]*' "$head^" 2> /dev/null); then
  range=("$previous..$head")
else
  range=("$head")
fi

# The app's folders: app/, and src/ where it lived before the server joined the repository, so the first
# release after that move still gets notes for the work merged before it. Changes to server/ give no notes.
app_paths=(app/ src/)

# Whether a commit changes the app compared with its first parent (a merge: everything it brings in).
changes_app() { # <commit>
  if git rev-parse -q --verify "$1^1" > /dev/null; then
    ! git diff --quiet "$1^1" "$1" -- "${app_paths[@]}"
  else
    [ -n "$(git diff-tree --root -r --no-commit-id --name-only "$1" -- "${app_paths[@]}")" ]
  fi
}

feature_merge='^Merge pull request #[0-9]+ from [^/]+/feature/(.+)$'

# The Release-note: trailers of a commit, one per line, trimmed.
trailers() {
  git log -1 --format='%(trailers:key=Release-note,valueonly,separator=%x0A)' "$1" \
    | sed -E 's/^[[:space:]]+//; s/[[:space:]]+$//' | grep -v '^$' || true
}

# A PR title as a note: without the change-name prefix, issue references and +semver markers.
clean_title() { # <title> <change-name>
  local title="$1" change="$2"
  title="${title#"$change:"}"
  title=$(sed -E 's/[[:space:]]*\((issue[[:space:]]+)?#[0-9]+\)//g; s/\+semver:[[:space:]]*[A-Za-z]+//g;
                  s/[[:space:]]+/ /g; s/^ //; s/ $//' <<< "$title")
  printf '%s' "${title^}"
}

mapfile -t commits < <(git rev-list --reverse --topo-order "${range[@]}")

# Commits that arrived through a feature pull request count towards that PR, not on their own.
declare -A owned=()
for c in "${commits[@]}"; do
  [[ "$(git log -1 --format=%s "$c")" =~ $feature_merge ]] || continue
  while read -r o; do owned[$o]=1; done < <(git rev-list "$c^1..$c^2")
done

notes=()
add_notes() { # <commit>... : their trailers as notes, or return 1 if there are none; none drops them
  local c t found=()
  for c in "$@"; do mapfile -t -O "${#found[@]}" found < <(trailers "$c"); done
  (( ${#found[@]} > 0 )) || return 1
  for t in "${found[@]}"; do [ "${t,,}" = none ] && return 0; done
  notes+=("${found[@]}")
}

for c in "${commits[@]}"; do
  changes_app "$c" || continue
  subject=$(git log -1 --format=%s "$c")
  if [[ "$subject" =~ $feature_merge ]]; then
    change="${BASH_REMATCH[1]}"
    mapfile -t pr_commits < <(git rev-list --reverse --topo-order "$c^1..$c^2")
    add_notes "$c" "${pr_commits[@]}" && continue
    title=$(git log -1 --format=%b "$c" | sed -E '/^[[:space:]]*$/d' | head -n 1)
    [ -n "$title" ] && notes+=("$(clean_title "$title" "$change")")
  elif [ -z "${owned[$c]:-}" ] && [ "$(git rev-list --parents -n 1 "$c" | wc -w)" -le 2 ]; then
    add_notes "$c" || true
  fi
done

# Each note once, at its first position, as long as the notes fit; then make room for the "more" line.
declare -A seen=()
out=""
cut=false
for n in "${notes[@]}"; do
  [ -n "$n" ] && [ -z "${seen[$n]:-}" ] || continue
  seen[$n]=1
  candidate="${out:+$out$'\n'}• $n"
  if (( ${#candidate} > max )); then cut=true; break; fi
  out="$candidate"
done
if $cut; then
  while [ -n "$out" ] && (( ${#out} + 1 + ${#more} > max )); do
    if [[ "$out" == *$'\n'* ]]; then out="${out%$'\n'*}"; else out=""; fi
  done
  out="${out:+$out$'\n'}$more"
fi

printf '%s\n' "${out:-$fallback}"
