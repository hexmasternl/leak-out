#!/usr/bin/env bash
# Tests scripts/publish-release.sh against a local origin repository and a stand-in for gh.
#   bash scripts/publish-release.test.sh
set -euo pipefail

script="$(cd "$(dirname "$0")" && pwd)/publish-release.sh"
root=$(mktemp -d)
trap 'rm -rf "$root"' EXIT

# gh stand-in: a release is a file in $GH_STUB_DIR/releases holding its notes, next to <tag>.args.
# GH_STUB_RACE=1 makes "release create" fail after another run created the release;
# GH_STUB_FAIL=1 makes it fail outright.
export GH_STUB_DIR="$root/gh"
mkdir -p "$GH_STUB_DIR/bin" "$GH_STUB_DIR/releases"
cat > "$GH_STUB_DIR/bin/gh" << 'EOF'
#!/usr/bin/env bash
echo "$*" >> "$GH_STUB_DIR/calls"
[ "$1" = release ] || exit 2
tag="$3"
case "$2" in
  view) [ -f "$GH_STUB_DIR/releases/$tag" ] ;;
  create)
    [ -z "${GH_STUB_FAIL:-}" ] || exit 1
    notes=""
    shift 3
    while [ $# -gt 0 ]; do
      [ "$1" = --notes-file ] && notes="$2"
      shift
    done
    cp "$notes" "$GH_STUB_DIR/releases/$tag"
    tail -n 1 "$GH_STUB_DIR/calls" > "$GH_STUB_DIR/releases/$tag.args"
    [ -z "${GH_STUB_RACE:-}" ] || exit 1 ;;
  *) exit 2 ;;
esac
EOF
chmod +x "$GH_STUB_DIR/bin/gh"
export PATH="$GH_STUB_DIR/bin:$PATH"
unset GITHUB_REPOSITORY GH_STUB_RACE GH_STUB_FAIL

git init -q --bare "$root/origin.git"
git clone -q "$root/origin.git" "$root/work" 2> /dev/null
cd "$root/work"
git config user.name test
git config user.email test@example.com
git config core.autocrlf false
git config commit.gpgsign false
git config tag.gpgsign false
commit() { # <file> <subject>
  mkdir -p "$(dirname "$1")"
  echo "$2" >> "$1"
  git add "$1"
  git commit -q -m "$2"
}
commit app/a.txt "app: first app change"
commit server/s.txt "server: first server change"
commit website/w.txt "website: first website change"
git push -q origin HEAD 2> /dev/null

failures=0
pass() { echo "ok   $1"; }
flunk() { echo "FAIL $1: $2"; failures=$((failures + 1)); }
run() { bash "$script" "$@" 2>&1; }
on_origin() { git ls-remote origin "refs/tags/$1^{}" | cut -f1; }
release() { cat "$GH_STUB_DIR/releases/$1" 2> /dev/null || true; }
args() { cat "$GH_STUB_DIR/releases/$1.args" 2> /dev/null || true; }
creates() { grep -c "^release create $1 " "$GH_STUB_DIR/calls" || true; }

# First server release: tag on origin, release with only the server's commits, not marked latest.
head=$(git rev-parse HEAD)
if out=$(run --prefix server-v --version 0.3.3 --commit "$head" --title "Game server 0.3.3" --path server); then
  [ "$(on_origin server-v0.3.3)" = "$head" ] && pass "first release pushes the tag" \
    || flunk "first release pushes the tag" "origin has '$(on_origin server-v0.3.3)'"
  notes=$(release server-v0.3.3)
  [[ "$notes" == *"server: first server change"* && "$notes" != *"app:"* && "$notes" != *"website:"* ]] \
    && pass "notes only hold commits under --path" || flunk "notes only hold commits under --path" "$notes"
  [[ "$(args server-v0.3.3)" == *"--verify-tag"* && "$(args server-v0.3.3)" == *"--latest=false"* \
     && "$(args server-v0.3.3)" == *"--title Game server 0.3.3"* ]] \
    && pass "release uses the existing tag, the title and is not latest" || flunk "release arguments" "$(args server-v0.3.3)"
  [[ "$(git cat-file -p server-v0.3.3)" == *"Game server 0.3.3"*"server: first server change"* ]] \
    && pass "the tag is annotated with title and notes" || flunk "tag message" "$(git cat-file -p server-v0.3.3)"
else
  flunk "first release" "$out"
fi

# Re-running the same release changes nothing and succeeds.
if out=$(run --prefix server-v --version 0.3.3 --commit "$head" --title "Game server 0.3.3" --path server) \
   && [[ "$out" == *"Tag server-v0.3.3 already exists on $head"* && "$out" == *"Release server-v0.3.3 already exists"* ]] \
   && [ "$(creates server-v0.3.3)" = 1 ]; then
  pass "re-run is a no-op"
else
  flunk "re-run is a no-op" "$out"
fi

# A tag pushed by another run on the same commit, without a release yet: the release is still made.
git tag website-v1.0.0 "$head"
git push -q origin refs/tags/website-v1.0.0 2> /dev/null
git tag -d website-v1.0.0 > /dev/null
if out=$(run --prefix website-v --version 1.0.0 --commit "$head" --title "Website 1.0.0" --path website) \
   && [[ "$out" == *"already exists on $head"* ]] && [[ "$(release website-v1.0.0)" == *"website: first website change"* ]]; then
  pass "tag from another run on the same commit is reused"
else
  flunk "tag from another run on the same commit is reused" "$out"
fi

# The next release only lists the commits since the previous tag of its own prefix, with a compare link.
commit server/s.txt "server: second server change"
commit app/a.txt "app: second app change"
head2=$(git rev-parse HEAD)
if out=$(GITHUB_REPOSITORY=hexmasternl/leak-out run --prefix server-v --version 0.3.4 --commit "$head2" \
           --title "Game server 0.3.4" --path server); then
  notes=$(release server-v0.3.4)
  [[ "$notes" == *"server: second server change"* && "$notes" != *"first server change"* && "$notes" != *"app:"* ]] \
    && pass "next notes start at the previous tag" || flunk "next notes start at the previous tag" "$notes"
  [[ "$notes" == *"https://github.com/hexmasternl/leak-out/compare/server-v0.3.3...server-v0.3.4"* ]] \
    && pass "notes link the changes since the previous tag" || flunk "compare link" "$notes"
else
  flunk "next release" "$out"
fi

# A product without changes since its previous tag still gets a release with a note saying so.
if out=$(run --prefix website-v --version 1.0.1 --commit "$head2" --title "Website 1.0.1" --path website) \
   && [[ "$(release website-v1.0.1)" == *"No changes since website-v1.0.0"* ]]; then
  pass "release without changes under --path"
else
  flunk "release without changes under --path" "$out / $(release website-v1.0.1)"
fi

# The app: notes from a file, marked latest; other products' tags never count as its previous release.
printf 'Bug fixes and improvements.' > "$root/whatsnew"
if out=$(GITHUB_REPOSITORY=hexmasternl/leak-out run --prefix v --version 1.0.1 --commit "$head2" \
           --title "Leak Out 1.0.1" --notes-file "$root/whatsnew" --latest); then
  notes=$(release v1.0.1)
  [[ "$notes" == "Bug fixes and improvements." ]] && pass "notes file is used as is, without a foreign previous tag" \
    || flunk "notes file is used as is" "$notes"
  [[ "$(args v1.0.1)" == *"--latest=true"* ]] && pass "--latest marks the release latest" || flunk "--latest" "$(args v1.0.1)"
else
  flunk "app release" "$out"
fi

# A tag already on another commit fails, and leaves the tag and the release alone.
if out=$(run --prefix server-v --version 0.3.4 --commit "$head" --title "Game server 0.3.4" --path server); then
  flunk "tag on another commit" "expected a failure, got '$out'"
elif [[ "$out" == *"::error::Tag server-v0.3.4 already exists on another commit"* ]] && [ "$(on_origin server-v0.3.4)" = "$head2" ]; then
  pass "tag on another commit is refused"
else
  flunk "tag on another commit" "$out"
fi

# Another run creating the same release at the same moment counts as success; a plain failure does not.
if out=$(GH_STUB_RACE=1 run --prefix server-v --version 0.3.5 --commit "$head2" --title "Game server 0.3.5" --path server) \
   && [[ "$out" == *"created by another run"* ]]; then
  pass "release created by another run"
else
  flunk "release created by another run" "$out"
fi
if out=$(GH_STUB_FAIL=1 run --prefix server-v --version 0.3.6 --commit "$head2" --title "Game server 0.3.6" --path server); then
  flunk "release creation fails" "expected a failure, got '$out'"
elif [[ "$out" == *"::error::Could not create release server-v0.3.6"* ]]; then
  pass "release creation failure is reported"
else
  flunk "release creation fails" "$out"
fi

expect_fail() { # <name> <part of the error> <args...>
  local name="$1" part="$2" out
  shift 2
  if out=$(run "$@"); then flunk "$name" "expected an error, got '$out'"
  elif [[ "$out" == *"::error::"*"$part"* ]]; then pass "$name refused"
  else flunk "$name" "expected '$part' in '$out'"; fi
}
expect_fail "version without a patch" "is not a <major>.<minor>.<patch> version" --prefix v --version 1.0 --commit "$head" --title t
expect_fail "no prefix" "No --prefix" --version 1.0.0 --commit "$head" --title t
expect_fail "no title" "No --title" --prefix v --version 1.0.0 --commit "$head"
expect_fail "unknown commit" "is not a commit" --prefix v --version 1.0.0 --commit nope --title t
expect_fail "missing notes file" "does not exist" --prefix v --version 1.0.0 --commit "$head" --title t --notes-file "$root/none"
expect_fail "unknown argument" "Unknown argument" --prefix v --tag v1.0.0
expect_fail "option without a value" "--title needs a value" --prefix v --title

if [ "$failures" -gt 0 ]; then
  echo "$failures test(s) failed."
  exit 1
fi
echo "All publish-release tests passed."
