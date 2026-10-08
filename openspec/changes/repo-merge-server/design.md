# Design

## Context

See proposal.md for the motivation. Today:

- This repository: the Gradle root is `src/` (modules `core` and `app`); one workflow, `deploy-play-internal.yml`, runs on every push to `main`, versions with GitVersion (`GitVersion.yml` at the root, `GitHubFlow/v1`, pushes a `v<x.y.z>` tag per release) and builds Play release notes from the commits since the previous `v*` tag. Branch flow: `feature/*` → `development` → `main`.
- `leak-out-server`: a .NET solution in `src/` (`src/leak-out-server.slnx`, API project `src/HexMaster.LeakOut.Api`), Bicep in `infrastructure/`, root `global.json`, `Directory.Build.props`, `CodeCoverage.settings.xml`, `GitVersion.yml` (`TrunkBased/preview1`, no tags pushed), `.github/scripts/coverage-gate.sh`, and three workflows: `build-test.yml` (reusable), `pull-request.yml` (PRs into `development`, required checks `build-test / build-test` and `bicep`) and `deploy.yml` (push to `main`; `production` environment; Azure OIDC login). It has its own `openspec/` (specs `azure-hosting`, `ci-cd`, `database-migrations`, `level-leaderboard`, `observability`, `player-authentication`, `score-submission`), `CLAUDE.md`, and many .NET/Aspire skills in `.claude/`, `.agents/` and `.github/`. Only a `main` branch, no open issues, no tags. Its ten secrets are repository secrets.

## Goals / Non-Goals

**Goals:**
- One repository, two independently built and released products, without either side's pushes triggering the other's release.
- History of both sides preserved and followable.
- A cut-over without a window in which the server is deployed from both repositories, or from neither when a fix is needed.

**Non-Goals:**
- Sharing code between the Kotlin app and the .NET server (for example generated API clients).
- A single build tool or a single pull request workflow for both sides.
- Changing the server's Azure infrastructure.

## Decisions

### 1. `src/` becomes `app/src/` as a whole
`git mv src app/src` keeps the Gradle root intact, so modules become `app/src/core` and `app/src/app` and Gradle project paths (`:core`, `:app`) do not change. Both sides then share one shape: `<side>/src/` holds the sources, and `<side>/` holds what belongs to that side but is not source (versioning, docs, and for the server its infrastructure). *Alternative:* `src/` becomes `app/` directly — rejected by the maintainer in favour of the symmetric `<side>/src/` layout. *Alternative:* flatten so `app/` is the Android module — rejected: it spreads the Gradle root over several folders.
`GitVersion.yml` moves to `app/GitVersion.yml` and the deploy workflow passes it with the GitVersion action's `configFilePath`, so each side owns its versioning file.

### 2. Import the server with `git filter-repo --to-subdirectory-filter server`, then merge
In a fresh clone of `leak-out-server`, rewrite history so every path is under `server/`, then `git merge --allow-unrelated-histories` it into the feature branch. `git log -- server/...` then shows the original commits without `--follow`. *Alternative:* `git subtree add --prefix=server` — keeps history too, but the original commits keep root paths, so per-file history needs subtree-aware tooling. *Alternative:* copy the files — loses history.

### 3. What goes where after the import
After the merge everything sits under `server/`. Then:
- **Stays in `server/`:** `src/` (the solution `leak-out-server.slnx` and every project, so `server/src/`), `infrastructure/` (the Bicep templates, so `server/infrastructure/`), `global.json`, `Directory.Build.props`, `CodeCoverage.settings.xml`, `GitVersion.yml`, `README.md`, `docs/`, `LICENSE` (dropped if identical to the root one), `.gitignore` (kept; Git applies nested ignore files).
- **Moves to the root `.github/`:** the three workflows (renamed `server-build-test.yml`, `server-pull-request.yml`, `server-deploy.yml`) and `.github/scripts/coverage-gate.sh` → `.github/scripts/server-coverage-gate.sh`.
- **OpenSpec:** there is one OpenSpec root per repository (the CLI reads `openspec/` at the root). The server's specs move to `openspec/specs/server-<name>/` (for example `server-score-submission`), its archived changes to `openspec/changes/archive/` (their dates make the names unique; clashes get a `server-` prefix), and active changes, if any, to `openspec/changes/server-<name>/`. References between server specs are updated to the new names, and `server-ci-cd` is rewritten for the new workflow names and paths as part of this change (it does not exist here yet, so it cannot be a delta).
- **Agent configuration:** `server/CLAUDE.md` stays as a nested instructions file (Claude Code loads it when working under `server/`); the server's useful content for Copilot goes into `server/AGENTS.md`. Server skills move to `.claude/skills/` unless a skill with the same name already exists here (`openspec-*`, `triage-github-issues`): those keep this repository's version. `.agents/skills` duplicates are dropped. The sync script then mirrors them to `.github/skills/`. `.mcp.json` entries are merged into the root one, if present.

### 4. Path filters on deploys, change detection on pull requests
Deploy workflows use `on.push.paths` (`app/**`, which covers `app/src/`, plus the app workflow; `server/**` plus the `server-*` workflows and the coverage gate script). This is safe because deploys are not required checks.
For the required pull request check, a `paths` filter would leave the check *pending* forever on app-only pull requests. Instead `server-pull-request.yml` always runs; a first `changes` job (using `git diff --name-only` against the base, no third-party action) outputs whether `server/` or a server workflow changed, and `build-test` and `bicep` run with `if: needs.changes.outputs.server == 'true'`. A skipped job counts as passing for branch protection. The required check names become `server / build-test` and `server / bicep` (job names stay stable from then on).

### 5. Separate tags and versions
App tags stay `v<x.y.z>` so existing tags, version codes and release-note ranges keep working. `server/GitVersion.yml` gets `tag-prefix: 'server-v'`; without it the server's `TrunkBased` config would read the app's `v*` tags on `main` as its base version. The app's GitVersion config gets an explicit `tag-prefix: 'v'` so a `server-v*` tag can never match. The server deploy runs GitVersion with `configFilePath: server/GitVersion.yml`. To keep the server version continuous, the merge commit on `main` gets a `server-v<last deployed version>` tag after the first deploy from this repository.

### 6. Release notes filter on `app/`
The release-notes script restricts its commit list to `-- app/` with `--first-parent`-aware handling: a merge counts when `git diff --quiet <merge>^1 <merge> -- app/` reports a change. Its tests gain a server-only merge case.

### 7. Secrets in the `production` environment
The server's secrets are recreated as **environment** secrets of a new `production` environment (limited to `main`), not repository secrets, matching how `play-store` holds the app's secrets. Since the deploy job uses `environment: production`, the Azure federated credential subject becomes `repo:hexmasternl/leak-out:environment:production`. Values come from their sources (Azure portal, Google Cloud console, the ACR admin page, the password manager for `POSTGRES_ADMIN_PASSWORD`); GitHub cannot export them.

### 8. Cut-over order
1. Land the move on `development` with the server deploy workflow present but only `workflow_dispatch` enabled.
2. Create the environment and secrets, add the federated credential for this repository (keep the old one).
3. Merge `development` into `main`; run the server deploy by hand from this repository; check the smoke test.
4. Enable the `push` trigger; disable `deploy.yml` in `leak-out-server` (Actions → Disable workflow).
5. Remove the old federated credential; add a README pointer in `leak-out-server` and archive it.

## Risks / Trade-offs

- [Active feature branches and worktrees still use `src/`] → Land this change while few changes are in flight; Git's rename detection handles most rebases; list the open branches in the pull request.
- [Server deploys twice or not at all during cut-over] → The ordered cut-over above: only one repository's deploy is enabled at any time.
- [A secret is re-entered wrong] → The deploy's "Check required secrets" step catches empty values; a wrong value fails the manual deploy or the smoke test before the push trigger is enabled.
- [GitVersion reads the other side's tags] → Explicit `tag-prefix` in both configs; verified by running GitVersion locally on a branch with both tag kinds before merging.
- [A required check stays pending] → Only the always-running `server-pull-request.yml` jobs are required; deploys are never required checks.
- [Larger repository, slower clones] → Accepted; the server history is small compared with the app's assets.

## Migration Plan

Follow *Cut-over order* above. Rollback before step 4: revert the merge on `development`/`main`; the server keeps deploying from `leak-out-server`. After step 4: re-enable `deploy.yml` in `leak-out-server` (it is archived only in step 5, and archiving can be undone).

## Open Questions

- Should the server's OpenSpec specs keep their own names under a `server-` prefix (this design), or be merged into the app's capabilities where they overlap (for example `server-score-submission` with the app's score posting)? This can be revisited after the move without changing the layout.
- Should the server's .NET/Aspire skills move to the root `.claude/skills/` (this design) or stay out of the app's agent configuration altogether?
- Should the app's workflow be renamed `app-deploy-play-internal.yml` for symmetry? This design keeps the current name to avoid touching the `play-store` environment and docs links.
