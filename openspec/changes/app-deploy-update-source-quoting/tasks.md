# Tasks

## 1. Fix

- [x] 1.1 Add tests to `scripts/resolve-update-priority.test.sh` that read the script's output back the way the deploy workflow does (label and manual run, value with spaces intact); verify that a `source`-based read fails them and that `bash scripts/resolve-update-priority.test.sh` passes once the read loop is used — with `source` the new tests fail with `input: command not found`, as in the failed deploy; with the read loop all 20 tests pass
- [x] 1.2 Replace `source "$RUNNER_TEMP/update.env"` in `deploy-play-internal.yml` with the same read loop; verify with `actionlint` if available, and by running the step's reading and summary lines locally against the script's output for a label and a manual run — actionlint passes; the step's read and log lines print "Update required, priority 5, from the pull request label" and "Update optional, priority 0, from the manual input"
- [x] 1.3 Replace the "TBD" Purpose in `openspec/specs/app-updates/spec.md`
