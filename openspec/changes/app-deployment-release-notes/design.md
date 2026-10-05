# Design

## Context

`.github/workflows/deploy-play-internal.yml` runs on every push to `main`: unit tests, GitVersion, `scripts/compute-release-version.sh`, a signed bundle, `r0adkll/upload-google-play` and finally an annotated `v<x.y.z>` tag on the released commit. It checks out with `fetch-depth: 0`, so the full history and every tag are available. The upload step currently passes no release notes.

How commits reach `main` in this repo (see `CLAUDE.md`): each OpenSpec change is a `feature/<change-name>` branch merged into `development` with a **merge commit** whose subject is `Merge pull request #N from <owner>/feature/<change-name>` and whose body is the PR title, e.g. `app-game-more-achievements: 14 new achievements (issue #64)`. `development` is then merged into `main` by a PR titled `Development`. Direct commits on `development` are mostly bookkeeping (`<change>: archive the change and sync its specs`).

See proposal.md for motivation and specs/play-release-notes/spec.md for the exact rules.

## Goals / Non-Goals

**Goals:**
- Deterministic notes: the same commits always give the same text, testable without network or secrets.
- No new tools or actions: bash and git, like `compute-release-version.sh`.
- A broken generator degrades to the fallback text instead of stopping a release.

**Non-Goals:**
- Squash-merged or rebased PRs are not recognised as PRs (the repo uses merge commits). Such commits only produce notes through a `Release-note:` trailer.
- No GitHub Release objects, no translated notes, no changes to versioning, track or release status.

## Decisions

### 1. A bash script with a throwaway-repo test, mirroring the version script
`scripts/generate-release-notes.sh <released-commit>` prints the notes on stdout and exits non-zero on an error (not a git repo, unknown commit). `scripts/generate-release-notes.test.sh` builds throwaway git repositories with empty commits, merge commits and tags, runs the script and compares its output, in the same `expect_ok` style as `compute-release-version.test.sh`.

*Alternatives:* a Node script (`node` is already used for `sync-agent-config.mjs`) would be fine too, but the deployment job doesn't set up Node and the neighbouring release script is bash. `git-cliff` or `release-drafter` add a dependency and are built around conventional commits or PR labels, which this repo doesn't use.

### 2. Range: describe from the parent
The previous release is `git describe --tags --abbrev=0 --match 'v[0-9]*' <commit>^`. Describing from the first parent skips a tag on the released commit itself (a re-run) and follows `main`'s first-parent line, where the workflow puts every tag. No tag, or no parent, means the whole history (`git rev-list <commit>`). Commits are walked oldest first with `git rev-list --reverse --topo-order <tag>..<commit>`.

### 3. PRs are found from merge-commit subjects, their commits from the merge's parents
A merge commit whose subject matches `^Merge pull request #[0-9]+ from [^/]+/feature/(.+)$` is a feature PR; its title is the first non-empty body line, and its commits are `git rev-list M^1..M^2`. Those commits are marked "owned" so their trailers count towards the PR instead of adding notes of their own. The note for a PR is emitted at the position of its merge commit.

*Alternative:* the GitHub API (`gh pr list --search <sha>`) gives exact PR titles and labels, but needs a token and network in tests, and the merge commit already holds the title.

### 4. Trailers via git's own parser
`git log -1 --format='%(trailers:key=Release-note,valueonly,separator=%x0A)' <sha>` reads trailers exactly as git defines them (last paragraph, `Key: value`). `none` (case-insensitive) suppresses. Trailers on the merge commit itself count as part of its PR.

### 5. Title clean-up is a small set of `sed` rules
Strip a leading `<kebab-case>:` prefix, ` (issue #N)` / ` (#N)`, `+semver: <word>`; collapse whitespace; trim; upper-case the first character. Nothing else is rewritten: the PR title is the author's player-facing sentence, which is why the `openspec-work-on-change` skill is updated to write titles that way.

### 6. Limit counted in characters under UTF-8
The script sets `LC_ALL=C.UTF-8` so `${#text}` counts characters, not bytes (`•` and `…` are multi-byte; Play counts characters). Lines are appended while the total, including the reserved `• …and more improvements` line, stays within 500; once a line doesn't fit, the rest are dropped and the "more" line is added. If everything fits, no reservation is applied.

### 7. Workflow wiring
After *Compute version*:
1. **Generate release notes**: runs `generate-release-notes.test.sh`, then the script. If either fails, it emits `::warning::` and uses `Bug fixes and improvements.`. It writes `$RUNNER_TEMP/whatsnew/whatsnew-en-US` and appends the notes to `$GITHUB_STEP_SUMMARY`. Failing tests are treated like a failing generator: the output of an untested generator isn't trusted, but the release still goes out.
2. **Upload to Google Play** gets `whatsNewDirectory: ${{ runner.temp }}/whatsnew`; the action maps `whatsnew-<locale>` files to the release's notes.
3. **Tag the release** writes the message (`Leak Out x.y.z (version code n)`, blank line, notes) to a file and uses `git tag -a -F`.

## Risks / Trade-offs

- [PR titles written for developers leak into the notes] → The skill that opens PRs writes player-facing titles; `Release-note:` trailers override a title after the fact by adding a commit before merging into `main`; the docs explain both.
- [A PR merged into `development` but released later] → It is released with the next `main` deploy and its merge commit is in that range, so it is noted then. Correct by construction.
- [Locale codes] → Play requires the exact listing locale; the app's default listing language must be `en-US`. If it isn't, the action rejects the file. Documented in troubleshooting; the locale is one constant in the workflow.
- [`whatsNewDirectory` behaviour changes in a newer action version] → The action is pinned by SHA; the pin is reviewed when it's bumped.

## Migration Plan

Additive. The first deployment after merging lists every feature PR since the latest existing `v*` tag. Rollback: remove `whatsNewDirectory` from the upload step; releases go back to having no notes.

## Open Questions

- Once #72 adds languages, should notes be provided per language (hand-written or translated) or left `en-US` only? This only adds `whatsnew-<locale>` files later and doesn't change this design.
