# Tasks

## 1. Release-notes generator

- [x] 1.1 Create `scripts/generate-release-notes.test.sh` in the style of `scripts/compute-release-version.test.sh` (throwaway repo, `expect_ok` / `expect_fail`), with cases for: range since the previous `v*` tag, first release without a tag, tag on the released commit ignored, feature-PR merge title clean-up (prefix, `(issue #N)`, `(#N)`, `+semver: minor`, capitalisation), `Release-note:` trailers replacing a PR title, `Release-note: none` on a PR, a trailer on a direct commit, `development` merges and `Merge remote-tracking branch` skipped, plain commits skipped, duplicates listed once, oldest-first order, truncation at 500 characters on whole lines with multi-byte characters, the fallback text, and an error for an unknown commit; verify it runs and fails because the script doesn't exist yet
- [x] 1.2 Implement `scripts/generate-release-notes.sh <released-commit>` per design decisions 2 to 6 (header comment with usage, `set -euo pipefail`, `LC_ALL=C.UTF-8`); verify `bash scripts/generate-release-notes.test.sh` passes
- [x] 1.3 Run the script against this repository's own history (`bash scripts/generate-release-notes.sh origin/main`) and verify the output lists the feature PRs since the newest `v*` tag with cleaned-up titles and no bookkeeping commits

## 2. Deployment workflow

- [ ] 2.1 Add a *Generate release notes* step after *Compute version* in `.github/workflows/deploy-play-internal.yml` that runs the tests and the script, falls back to `Bug fixes and improvements.` with a `::warning::` when either fails, writes `$RUNNER_TEMP/whatsnew/whatsnew-en-US` and appends the notes to the step summary; verify by running the step's shell body locally against a throwaway `RUNNER_TEMP`/`GITHUB_STEP_SUMMARY`, once normally and once with the script made to fail, and checking the file contents and the warning
- [ ] 2.2 Pass `whatsNewDirectory: ${{ runner.temp }}/whatsnew` to the *Upload to Google Play* step; verify against the pinned `r0adkll/upload-google-play` version's `action.yml` that the input exists and expects `whatsnew-<locale>` files
- [ ] 2.3 Change *Tag the release* to write the tag message (`Leak Out <x.y.z> (version code <n>)`, blank line, notes) to a file and tag with `git tag -a -F`; verify locally in a throwaway repo that `git show <tag>` prints the notes
- [ ] 2.4 Validate the workflow syntax (`actionlint` if available, otherwise a YAML parse) and verify it reports no errors

## 3. Documentation and conventions

- [ ] 3.1 Add a "Release notes" section to `docs/deployment.md` (where the notes come from, the range, PR-title conventions, `Release-note:` / `Release-note: none` with examples, the 500-character limit, the fallback, where to see them) plus a troubleshooting entry for a rejected `whatsnew-en-US` locale, and link it from "How it works"; verify every command shown runs as written
- [ ] 3.2 Update `.claude/skills/openspec-work-on-change/SKILL.md` so pull request titles are written as a short player-facing release note after the `<change-name>:` prefix, and mention `Release-note: none` for changes players don't see; verify `node scripts/sync-agent-config.mjs --check` reports in sync (run it without `--check` first if needed)

## 4. Integration

- [ ] 4.1 After merging into `main`, verify the deployment run's summary shows the notes, the Play Console release shows them under "What's new" (`en-US`), and `git show v<x.y.z>` contains them
