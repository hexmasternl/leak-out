# Proposal

Tracking issue: [#97 Require an update for major fixes](https://github.com/hexmasternl/leak-out/issues/97)

## Why

The first deploy after `app-required-updates` was merged ([run 37782307981](https://github.com/hexmasternl/leak-out/actions/runs/37782307981), release 1.0.0) failed in the step "Resolve in-app update priority", so nothing was uploaded. `scripts/resolve-update-priority.sh` prints `UPDATE_SOURCE=pull request label`. That line is a valid `$GITHUB_ENV` entry, but the step also runs `source` on the file to fill in the job summary, and bash then runs `request` as a command (`update.env: line 3: request: command not found`, exit 127). A manual run fails the same way on `manual input`. The script tests passed because they only compare the printed text and never read it back the way the workflow does. As long as this is broken, no release can reach Play.

## What Changes

- The deploy workflow reads the priority file line by line as `KEY=value` pairs, without running it as shell code. The values stay unquoted, because they also go into `$GITHUB_ENV`, which would keep any quotes as part of the value.
- `scripts/resolve-update-priority.test.sh` gets a test that reads the script's output back the same way the workflow does, for a label and for a manual run, so values with spaces are covered.
- The `app-deployment` spec gets a scenario: the decided priority and its source are carried into the job summary and the upload whether they came from a label or from the manual input.
- The `app-updates` spec, created when `app-required-updates` was archived, gets a real Purpose instead of "TBD".

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `app-deployment`: the requirement "Release update priority follows the update label" gets a scenario that the step's output is read back without running it as shell code.

## Impact

- `.github/workflows/deploy-play-internal.yml`: step "Resolve in-app update priority".
- `scripts/resolve-update-priority.test.sh`: new tests.
- `openspec/specs/app-updates/spec.md`: Purpose.
- No app code changes. Release 1.0.0 is uploaded by the first deploy after this change reaches `main`.
