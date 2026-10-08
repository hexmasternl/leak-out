# Design

## Context

`scripts/resolve-update-priority.sh` writes three `KEY=value` lines. The step "Resolve in-app update priority" in `deploy-play-internal.yml` uses them twice: it appends them to `$GITHUB_ENV` for later steps, and it runs `source` on them so it can write the job summary in the same step. `UPDATE_SOURCE` has a space in it (`pull request label`, `manual input`), so `source` breaks.

## Decisions

### Read the file, don't run it

The step reads each line, splits on the first `=` and exports the pair (`export "$key=$value"`). Unlike `source`, this never runs a value as code.

Alternatives considered:
- **Quote the values in the script** (`UPDATE_SOURCE="pull request label"`): `$GITHUB_ENV` keeps the quotes as part of the value, so later steps would see `"pull request label"` with the quotes included.
- **Use slugs without spaces** (`pull-request-label`): this hides the problem without fixing it, and the job summary becomes harder to read.

### Test the way the output is consumed

The test script gets a small `read_env` helper that does the same as the workflow's read loop, and checks that `UPDATE_PRIORITY`, `UPDATE_KIND` and `UPDATE_SOURCE` come back intact for a label and for a manual run. A comment in the workflow and one in the test point to each other, so the two loops stay the same.

## Risks / Trade-offs

- The read loop exists twice, in the workflow and in the test. It is three lines, and the comments in both files point to each other.
