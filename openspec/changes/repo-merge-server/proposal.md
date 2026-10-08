# Proposal

## Why

The online game server lives in its own repository, [leak-out-server](https://github.com/hexmasternl/leak-out-server), while more and more app work (posting scores, the top scores, cold-start timeouts) changes both sides at once. Specifying, reviewing and releasing those changes across two repositories means two proposals, two pull requests and two sets of conventions for one feature. Bringing both into this repository, each in its own folder with its own build and release, lets one change cover both. Tracked in [#115](https://github.com/hexmasternl/leak-out/issues/115).

## What Changes

- **BREAKING (for contributors):** the app's Gradle root moves from `src/` to `app/` (so the modules become `app/core` and `app/app`), with its history kept. Every Gradle command now runs from `app/`.
- The server's repository is imported into `server/` with its full history (the .NET solution, `infrastructure/`, `global.json`, `Directory.Build.props`, `CodeCoverage.settings.xml`, `GitVersion.yml`, its README and docs).
- The app's deploy workflow (`deploy-play-internal.yml`) uses `app/` and only runs for pushes to `main` that change `app/` (or the workflow itself). Its release notes only take commits that change `app/`.
- The server's workflows move to `.github/workflows/` as `server-build-test.yml`, `server-deploy.yml` and `server-pull-request.yml`, use `server/`, and only build or deploy when `server/` (or their own files) changed. The pull request check always reports a result, so it can stay a required status check without blocking app-only pull requests.
- The app and the server keep separate versions: the app keeps its `v<x.y.z>` tags; the server's GitVersion only reads `server-v<x.y.z>` tags, so app tags never move the server's version.
- The server's `production` environment and its ten secrets are recreated in this repository, and the Azure federated credential is changed to trust this repository. Values are re-entered by hand, as GitHub never reveals them.
- The server's OpenSpec specs and changes join this repository's `openspec/` with a `server-` prefix; its agent skills and instructions join `.claude/` and `.github/` (synced as usual), and its `CLAUDE.md` becomes `server/CLAUDE.md`.
- Every reference to `src/` (instructions, docs, skills, agents, `openspec/config.yaml`, scripts) points to `app/`, and a new `docs/repository-layout.md` describes the layout.
- Once everything is verified from this repository, `leak-out-server` gets a pointer to this one and is archived.

## Capabilities

### New Capabilities
- `repository-layout`: where the app and the server live in the repository, which workflows build and deploy each of them, how their versions stay apart, and where their secrets live.

### Modified Capabilities
- `app-deployment`: the deploy workflow only runs for pushes to `main` that change the app, and builds and tests from `app/` instead of `src/`.
- `play-release-notes`: the notes only take commits that change the app, so server-only commits never show up in the Play Store.

## Impact

- `src/` → `app/` (all app code, the Gradle wrapper and `libs.versions.toml`); `GitVersion.yml` → `app/GitVersion.yml`.
- New `server/` tree imported from `hexmasternl/leak-out-server`.
- `.github/workflows/deploy-play-internal.yml` and the release-notes script and tests in `scripts/`; three new `server-*.yml` workflows and `.github/scripts/coverage-gate.sh`.
- GitHub settings of this repository: the `production` environment, ten secrets, branch protection (required checks); the Azure AD app registration's federated credential.
- `CLAUDE.md` (and its synced copies), `README.md`, `docs/`, `openspec/config.yaml`, `openspec/specs/`, `.claude/`, `.github/skills|prompts|agents`, `scripts/`.
- Active feature worktrees and changes that refer to `src/` paths (for example `app-required-updates`) must be rebased onto the new layout.
