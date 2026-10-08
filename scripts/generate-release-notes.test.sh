#!/usr/bin/env bash
# Tests scripts/generate-release-notes.sh in throwaway git repositories.
#   bash scripts/generate-release-notes.test.sh
set -euo pipefail
export LC_ALL=C.UTF-8

script="$(cd "$(dirname "$0")" && pwd)/generate-release-notes.sh"
root="$(mktemp -d)"
trap 'rm -rf "$root"' EXIT

failures=0
expect_ok() { # <name> <commit> <expected stdout>
  local out
  if out=$(bash "$script" "$2" 2>&1) && [ "$out" = "$3" ]; then echo "ok   $1"
  else echo "FAIL $1: expected"; echo "$3" | sed 's/^/       /'; echo "     got"; echo "$out" | sed 's/^/       /'
    failures=$((failures + 1)); fi
}
expect_fail() { # <name> <commit> <part of the error>
  local out
  if out=$(bash "$script" "$2" 2>&1); then echo "FAIL $1: expected an error, got '$out'"; failures=$((failures + 1))
  elif [[ "$out" == *"$3"* ]]; then echo "ok   $1 refused"
  else echo "FAIL $1: expected '$3' in '$out'"; failures=$((failures + 1)); fi
}

# A fresh repository on main with one commit; every scenario gets its own.
new_repo() {
  local dir="$root/$1"
  mkdir -p "$dir" && cd "$dir"
  [ "$(pwd)" = "$(cd "$dir" && pwd)" ] || { echo "not in the throwaway repository" >&2; exit 1; }
  git init -q -b main
  git config user.name test && git config user.email test@test && git config commit.gpgsign false && git config tag.gpgsign false
  commit "initial commit"
}
# commit_in <folder> <paragraph>...: a commit that adds a file under <folder>, one -m per paragraph.
commit_in() {
  local folder="$1"; shift
  mkdir -p "$folder" && mktemp -p "$folder" change.XXXXXX > /dev/null && git add "$folder"
  git commit -q "${@/#/-m}"
}
commit() { commit_in app "$@"; } # an app change; each argument is one paragraph of the message
# feature_pr <number> <change-name> <PR title> [<commit message> ...]: a merged pull request from
# feature/<change-name>, with one commit per message (each message may hold several paragraphs, split on |).
feature_pr() {
  local number="$1" change="$2" title="$3"; shift 3
  git checkout -q -b "feature/$change"
  if [ $# -eq 0 ]; then commit "$change: do the work"; fi
  local message
  for message in "$@"; do IFS='|' read -r -a paragraphs <<< "$message"; commit "${paragraphs[@]}"; done
  git checkout -q main
  git merge -q --no-ff "feature/$change" -m "Merge pull request #$number from owner/feature/$change" -m "$title"
}

# --- arguments -------------------------------------------------------------------------------
new_repo errors
expect_fail "no commit" "" "No commit given"
expect_fail "unknown commit" "does-not-exist" "is not a commit"

# --- range -----------------------------------------------------------------------------------
new_repo first-release
feature_pr 1 app-welcome-screen "app-welcome-screen: a welcome screen"
feature_pr 2 app-splash-screen "app-splash-screen: a splash screen"
expect_ok "first release uses the whole history" HEAD $'• A welcome screen\n• A splash screen'

new_repo range
feature_pr 1 app-welcome-screen "app-welcome-screen: a welcome screen"
git tag -a v0.1.0 -m "Leak Out 0.1.0"
feature_pr 2 app-splash-screen "app-splash-screen: a splash screen"
feature_pr 3 app-game-pause "app-game-pause: pause the game"
expect_ok "only commits since the previous tag" HEAD $'• A splash screen\n• Pause the game'
git tag -a v0.1.1 -m "Leak Out 0.1.1"
expect_ok "a tag on the released commit is ignored" HEAD $'• A splash screen\n• Pause the game'

# --- pull request titles ---------------------------------------------------------------------
new_repo titles
feature_pr 64 app-game-more-achievements "app-game-more-achievements: 14 new achievements (issue #64)"
feature_pr 65 app-splash-screen "app-splash-screen: show a splash screen +semver: minor"
feature_pr 66 app-game-pause "app-game-pause:   pause   the game (#66)  "
feature_pr 67 app-sound "Louder sound effects"
feature_pr 68 app-colours "app-colours: élan for the colours +semver:patch (issue #12)"
expect_ok "PR titles are cleaned up" HEAD \
  $'• 14 new achievements\n• Show a splash screen\n• Pause the game\n• Louder sound effects\n• Élan for the colours'

# --- trailers --------------------------------------------------------------------------------
new_repo trailers
feature_pr 1 app-glow "app-glow: internal title" \
  "app-glow: add the glow|Release-note: Pipes glow when they fill" \
  "app-glow: add levels|Release-note: New level pack"
feature_pr 2 app-refactor "app-refactor: tidy the engine" "app-refactor: tidy|Release-note: none"
feature_pr 3 app-ci "app-ci: speed up CI" "app-ci: cache gradle|Release-note: NONE"
commit "app-fix: fix the crash" "Release-note: Fixed a crash on start"
commit "app-fix: also internal" "Release-note: none"
expect_ok "trailers replace, drop and add notes" HEAD \
  $'• Pipes glow when they fill\n• New level pack\n• Fixed a crash on start'

new_repo trailer-on-merge
git checkout -q -b feature/app-hud
commit "app-hud: work"
git checkout -q main
git merge -q --no-ff feature/app-hud -m "Merge pull request #9 from owner/feature/app-hud" -m "app-hud: hud title" \
  -m "Release-note: A clearer HUD"
expect_ok "a trailer on the merge commit counts for its PR" HEAD '• A clearer HUD'

# --- app and server --------------------------------------------------------------------------
new_repo server-only
feature_pr 1 app-welcome-screen "app-welcome-screen: a welcome screen"
git checkout -q -b feature/server-scores
commit_in server "server-scores: store scores" "Release-note: Faster scores"
commit_in server "server-scores: tidy"
git checkout -q main
git merge -q --no-ff feature/server-scores -m "Merge pull request #2 from owner/feature/server-scores"   -m "server-scores: store scores faster"
commit_in server "server-fix: fix the server" "Release-note: A server fix"
expect_ok "server-only merges and commits give no notes" HEAD '• A welcome screen'

new_repo app-and-server
git checkout -q -b feature/app-scores
commit_in server "app-scores: the endpoint"
commit "app-scores: the screen"
git checkout -q main
git merge -q --no-ff feature/app-scores -m "Merge pull request #3 from owner/feature/app-scores" -m "app-scores: top scores"
expect_ok "a merge that changes the app and the server counts" HEAD '• Top scores'

new_repo before-the-move
git checkout -q -b feature/app-glow
commit_in src "app-glow: glow"
git checkout -q main
git merge -q --no-ff feature/app-glow -m "Merge pull request #4 from owner/feature/app-glow" -m "app-glow: pipes glow"
feature_pr 5 app-pause "app-pause: pause the game"
expect_ok "app changes in src/ from before the move still count" HEAD $'• Pipes glow
• Pause the game'

# --- noise and order -------------------------------------------------------------------------
new_repo noise
git checkout -q -b development
feature_pr_on_development() {
  git checkout -q -b "feature/$2"; commit "$2: work"; git checkout -q development
  git merge -q --no-ff "feature/$2" -m "Merge pull request #$1 from owner/feature/$2" -m "$3"
}
feature_pr_on_development 68 app-welcome-exit-button "app-welcome-exit-button: confirm before Exit"
commit "app-welcome-exit-button: archive the change and sync its specs"
commit "app-welcome-exit-button: mark the tasks done"
feature_pr_on_development 69 app-game-more-achievements "app-game-more-achievements: 14 new achievements (issue #64)"
feature_pr_on_development 70 app-sound "app-sound: 14 new achievements"
git checkout -q main
git merge -q --no-ff development -m "Merge pull request #71 from owner/development" -m "Development"
git checkout -q -b other && commit "elsewhere" && git checkout -q main
git merge -q --no-ff other -m "Merge remote-tracking branch 'origin/development' into development"
expect_ok "noise is skipped, duplicates listed once, oldest first" HEAD \
  $'• Confirm before Exit\n• 14 new achievements'

new_repo nothing
commit "app-docs: update the docs"
git checkout -q -b development && commit "app-x: archive the change" && git checkout -q main
git merge -q --no-ff development -m "Merge pull request #2 from owner/development" -m "Development"
expect_ok "fallback when nothing qualifies" HEAD 'Bug fixes and improvements.'

new_repo all-none
feature_pr 1 app-refactor "app-refactor: tidy" "app-refactor: tidy|Release-note: none"
expect_ok "fallback when every PR is marked none" HEAD 'Bug fixes and improvements.'

# --- the 500-character limit -----------------------------------------------------------------
new_repo limit
# 30 PRs with a 41-character note: "• " + 41 = 43 characters a line, 44 with the newline.
for i in $(seq -w 1 30); do
  feature_pr "$i" "app-change-$i" "app-change-$i: note number $i is about forty chars long…"
done
out=$(bash "$script" HEAD)
lines=$(printf '%s\n' "$out" | wc -l)
last=$(printf '%s\n' "$out" | tail -n 1)
if (( ${#out} <= 500 )) && [ "$last" = '• …and more improvements' ] \
   && [ "$(printf '%s\n' "$out" | head -n 1)" = '• Note number 01 is about forty chars long…' ] \
   && ! printf '%s\n' "$out" | sed '$d' | grep -qv '^• Note number [0-9][0-9] is about forty chars long…$'; then
  echo "ok   long notes are cut on whole lines (${#out} characters, $lines lines)"
else echo "FAIL long notes: ${#out} characters"; echo "$out" | sed 's/^/       /'; failures=$((failures + 1)); fi
# Fill exactly: the greedy fit must leave room for the "more" line.
if (( ${#out} + 44 > 500 )); then echo "ok   no further note would have fit"
else echo "FAIL another note would have fit (${#out} characters)"; failures=$((failures + 1)); fi

new_repo exact-fit
# 11 lines of 44 characters plus 10 newlines = 494 characters: all fit, so no room is kept for a "more" line.
for i in $(seq -w 1 11); do feature_pr "$i" "app-fit-$i" "app-fit-$i: exactly forty-two characters long, okay $i"; done
out=$(bash "$script" HEAD)
if (( ${#out} <= 500 )) && [ "$(printf '%s\n' "$out" | wc -l)" -eq 11 ] && [[ "$out" != *"more improvements"* ]]; then
  echo "ok   notes that fit are all listed (${#out} characters)"
else echo "FAIL notes that fit: ${#out} characters"; echo "$out" | sed 's/^/       /'; failures=$((failures + 1)); fi

new_repo one-huge-note
long=$(printf 'x%.0s' $(seq 1 600))
feature_pr 1 app-huge "app-huge: $long"
feature_pr 2 app-small "app-small: small"
expect_ok "a note over the limit is dropped, not cut" HEAD '• …and more improvements'

if (( failures > 0 )); then echo "$failures test(s) failed"; exit 1; fi
echo "all tests passed"
