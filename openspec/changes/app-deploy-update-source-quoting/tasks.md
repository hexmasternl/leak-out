# Tasks

## 1. Fix

- [ ] 1.1 Add tests to `scripts/resolve-update-priority.test.sh` that read the script's output back the way the deploy workflow does (label and manual run, value with spaces intact); verify that a `source`-based read fails them and that `bash scripts/resolve-update-priority.test.sh` passes once the read loop is used
- [ ] 1.2 Replace `source "$RUNNER_TEMP/update.env"` in `deploy-play-internal.yml` with the same read loop; verify with `actionlint` if available, and by running the step's reading and summary lines locally against the script's output for a label and a manual run
- [ ] 1.3 Replace the "TBD" Purpose in `openspec/specs/app-updates/spec.md`
