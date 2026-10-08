#!/usr/bin/env bash
# Tags a deployed commit <prefix><version> and creates the GitHub release for that tag. Every deploy
# workflow (app, server, website) calls it after a successful deployment.
#
#   scripts/publish-release.sh --prefix <tag prefix> --version <x.y.z> --commit <sha> --title <title>
#                              [--path <dir>]... [--notes-file <file>] [--latest]
#
# Each product has its own tag prefix (v, server-v, website-v) and GitVersion only counts its own
# prefix, so workflows of different products running at the same time never touch the same tag.
# The version lives in the tags only: nothing is committed, so no run races another for main.
#
# Idempotent: a tag that already sits on the same commit, and a release that already exists, are
# left as they are, so a re-run (or a run that lost a race to push the same tag) succeeds without
# changing anything. A tag that sits on another commit fails the run.
#
# The release notes are --notes-file when given, otherwise the commits since the previous
# <prefix> tag that change one of the --path directories (all commits without --path).
# --latest marks the release as the repository's latest; only the app's releases do.
# Needs gh with GH_TOKEN, and the full history with tags. Rules: docs/repository-layout.md#versions-and-tags
set -euo pipefail

fail() {
  echo "::error::$1 See docs/repository-layout.md#versions-and-tags." >&2
  exit 1
}

prefix="" version="" commit="" title="" notes_file="" latest=false
paths=()
while [ $# -gt 0 ]; do
  case "$1" in
    --prefix|--version|--commit|--title|--notes-file|--path)
      [ $# -ge 2 ] || fail "$1 needs a value."
      case "$1" in
        --prefix) prefix="$2" ;;
        --version) version="$2" ;;
        --commit) commit="$2" ;;
        --title) title="$2" ;;
        --notes-file) notes_file="$2" ;;
        --path) paths+=("$2") ;;
      esac
      shift 2 ;;
    --latest) latest=true; shift ;;
    *) fail "Unknown argument '$1'." ;;
  esac
done

[ -n "$prefix" ] || fail "No --prefix given."
[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || fail "'$version' is not a <major>.<minor>.<patch> version."
[ -n "$title" ] || fail "No --title given."
[ -z "$notes_file" ] || [ -f "$notes_file" ] || fail "Notes file '$notes_file' does not exist."
sha=$(git rev-parse -q --verify "${commit:-}^{commit}") || fail "'$commit' is not a commit."
tag="$prefix$version"

# The commit a tag on origin points at (annotated tags are peeled), or nothing when it is not there.
remote_commit() {
  git ls-remote origin "refs/tags/$tag" "refs/tags/$tag^{}" \
    | awk -v plain="refs/tags/$tag" -v peeled="refs/tags/$tag^{}" \
        '$2 == peeled { p = $1 } $2 == plain { c = $1 } END { print (p != "" ? p : c) }'
}

# Tags of this product that other runs pushed after the checkout, so the previous release is right.
git fetch --quiet --force origin "+refs/tags/$prefix*:refs/tags/$prefix*"

# The previous release of this product: the newest tag with the same prefix in the commit's history.
previous=$(git tag --list "$prefix[0-9]*" --merged "$sha" --sort=-v:refname | grep -vxF "$tag" | head -n 1 || true)

notes=$(mktemp)
trap 'rm -f "$notes" "$notes.tag"' EXIT
if [ -n "$notes_file" ]; then
  cat "$notes_file" > "$notes"
else
  range="$sha"
  [ -z "$previous" ] || range="$previous..$sha"
  git log --no-merges --format='- %s (%h)' "$range" -- "${paths[@]}" > "$notes"
  [ -s "$notes" ] || echo "- No changes since ${previous:-the first commit}." > "$notes"
fi
if [ -n "$previous" ] && [ -n "${GITHUB_REPOSITORY:-}" ]; then
  printf '\n**Full changelog**: %s/%s/compare/%s...%s\n' \
    "${GITHUB_SERVER_URL:-https://github.com}" "$GITHUB_REPOSITORY" "$previous" "$tag" >> "$notes"
fi

# 1. The tag. Pushing a tag that does not exist yet either creates it or fails, never overwrites it.
existing=$(remote_commit)
if [ "$existing" = "$sha" ]; then
  echo "Tag $tag already exists on $sha."
elif [ -n "$existing" ]; then
  fail "Tag $tag already exists on another commit ($existing), not on $sha."
else
  {
    echo "$title"
    echo ""
    cat "$notes"
  } > "$notes.tag"
  git -c user.name="github-actions[bot]" \
      -c user.email="41898282+github-actions[bot]@users.noreply.github.com" \
      tag -a -f "$tag" --cleanup=whitespace -F "$notes.tag" "$sha" > /dev/null
  if git push origin "refs/tags/$tag"; then
    echo "Pushed tag $tag on $sha."
  else
    # Another run may have pushed the same tag in the meantime: fine when it is on this commit.
    existing=$(remote_commit)
    [ "$existing" = "$sha" ] || fail "Could not push tag $tag (on origin: '${existing:-nothing}')."
    echo "Tag $tag was pushed on $sha by another run."
  fi
fi

# 2. The release.
if gh release view "$tag" > /dev/null 2>&1; then
  echo "Release $tag already exists."
elif gh release create "$tag" --verify-tag --title "$title" --notes-file "$notes" --latest="$latest"; then
  echo "Created release $tag."
elif gh release view "$tag" > /dev/null 2>&1; then
  echo "Release $tag was created by another run."
else
  fail "Could not create release $tag."
fi
