# Repository layout

This repository holds two products: the **Leak Out Android app** and its **online game server**. They live in their own folders, have their own build tool, their own workflows, their own versions and their own secrets, so a change to one never builds, versions or releases the other. One OpenSpec change can still cover both sides, in one pull request. Next to them sits the public **website**, which follows the same rules: its own folder, workflows and deployment.

```
leak-out/
├── app/                      the Android app
│   ├── src/                  Gradle root: settings, wrapper, gradle/libs.versions.toml
│   │   ├── core/             pure Kotlin: game rules, levels (no Android)
│   │   └── app/              Android: Compose UI, rendering, screens
│   └── GitVersion.yml        the app's versioning (v<x.y.z> tags)
├── server/                   the online game server (.NET 10, Aspire, PostgreSQL on Azure)
│   ├── src/                  leak-out-server.slnx and every project
│   ├── infrastructure/       Bicep templates (main.bicep, main.bicepparam, modules/)
│   ├── docs/                 server docs (auth-setup.md)
│   ├── global.json, Directory.Build.props, CodeCoverage.settings.xml
│   ├── GitVersion.yml        the server's versioning (server-v<x.y.z> tags)
│   ├── README.md             running, CI, deployment, secrets of the server
│   └── CLAUDE.md, AGENTS.md  agent instructions for work under server/
├── website/                  the public website (Hugo, Azure Static Web App)
│   ├── src/                  Hugo site: hugo.toml, content per language, layouts, assets, static
│   ├── infrastructure/       Bicep templates (main.bicep, main.bicepparam, modules/)
│   ├── tests/                unit tests for the leaderboard script (npm test)
│   └── README.md             building, deployment, secrets, SEO of the website
├── assets/                   design sources of the app's artwork and audio
├── docs/                     project plan, design system, level format, app deployment, this file
├── openspec/                 one OpenSpec root for both: server specs are named server-<name>
├── scripts/                  repository tooling (release version and notes, agent config sync)
└── .github/
    ├── workflows/            deploy-play-internal.yml (app); server-*.yml (server); website-*.yml (website)
    └── scripts/              server-coverage-gate.sh
```

The app and the server do not share code. Each side's own sources sit in `<side>/src/`; what belongs to that side but is not source (versioning, docs, infrastructure) sits next to it in `<side>/`.

## Building

| | App | Server |
| --- | --- | --- |
| Run from | `app/src/` | `server/` (or `server/src/`) |
| Build | `./gradlew :app:assembleDebug` | `dotnet build src/leak-out-server.slnx` |
| Tests | `./gradlew :core:test :app:testDebugUnitTest` | `dotnet test --solution src/leak-out-server.slnx` (Docker needed for the Scores tests) |
| Toolchain pinned in | `app/src/gradle/libs.versions.toml` | `server/global.json` |

## Workflows

| Workflow | Side | Runs on | Does |
| --- | --- | --- | --- |
| [`deploy-play-internal.yml`](../.github/workflows/deploy-play-internal.yml) | app | push to `main` that changes `app/`, the workflow or its release scripts; by hand | unit tests, signed bundle, upload to Play internal testing, tag `v<x.y.z>` |
| [`server-pull-request.yml`](../.github/workflows/server-pull-request.yml) | server | every pull request into `development` | checks `server / build-test` and `server / bicep`; skipped (= passed) when the pull request does not change the server |
| [`server-build-test.yml`](../.github/workflows/server-build-test.yml) | server | called by the two other server workflows | build, tests, 80% coverage gate |
| [`server-deploy.yml`](../.github/workflows/server-deploy.yml) | server | push to `main` that changes `server/` or a server workflow; by hand | build and test, publish the API image, deploy `server/infrastructure` to Azure, smoke test |

| [`website-pull-request.yml`](../.github/workflows/website-pull-request.yml) | website | every pull request into `development` | check `website / build` (Hugo build, leaderboard tests, Bicep); skipped (= passed) when the pull request does not change the website |
| [`website-deploy.yml`](../.github/workflows/website-deploy.yml) | website | push to `main` that changes `website/` or the workflow; by hand | leaderboard tests, deploy `website/infrastructure` to Azure, build the site for its public address, upload it to the Static Web App, smoke test |

What counts as "changes the server": a file under `server/`, a `.github/workflows/server-*.yml` workflow or `.github/scripts/server-coverage-gate.sh`. What counts as "changes the website": a file under `website/` or a `.github/workflows/website-*.yml` workflow.

Deploys use `on.push.paths` filters, which is safe because they are not required checks. The pull request check cannot: a filtered-out workflow never reports, and a required check would stay pending forever on app-only pull requests. So `server-pull-request.yml` always runs, and its `server / changes` job decides whether the other two jobs do any work.

A pull request into `development` that changes both sides gets both treatments; the app has no pull request workflow of its own.

## Versions and tags

| | App | Server |
| --- | --- | --- |
| Config | [`app/GitVersion.yml`](../app/GitVersion.yml), GitHub Flow | [`server/GitVersion.yml`](../server/GitVersion.yml), mainline (TrunkBased) |
| Tags | `v<x.y.z>`, pushed by every app release | `server-v<x.y.z>`, set by hand to set a new base |
| `tag-prefix` | `'[vV]?'` | `'server-v'` |

A GitVersion tag prefix must match at the start of a tag, so `server-v1.0.0` is never an app version and `v0.9.0` never a server version. The app's prefix keeps the `v` optional because GitVersion also parses `next-version` with it. The app's release notes and version code only look at `v*` tags too (`scripts/generate-release-notes.sh`, `scripts/compute-release-version.sh`), and the notes only take commits that change `app/`.

The commit that merged the server's history into this repository has no merge base with the app's history. Mainline versioning cannot walk past it, so it carries the tag `server-v0.3.2` (the last version deployed from the old repository); without a `server-v*` tag on or after it the server's GitVersion fails.

## Secrets

| Side | Where | Documented in |
| --- | --- | --- |
| App | environment `play-store` (limited to `main`) | [docs/deployment.md](deployment.md) |
| Server | repository secrets; the deploy job runs in environment `production` (limited to `main`), which Azure's federated credential trusts | [server/README.md](../server/README.md#secrets-and-settings) |
| Website | the server's `AZURE_PROD_*` secrets and federated credential (its deploy job also runs in `production`); no deployment token is stored | [website/README.md](../website/README.md#secrets-and-variables) |

## History

The app moved from `src/` to `app/src/` with `git mv`; `git log --follow` shows its history from before the move. The server came from [hexmasternl/leak-out-server](https://github.com/hexmasternl/leak-out-server), rewritten with `git filter-repo --to-subdirectory-filter server` and merged, so `git log -- server/...` shows its original commits. That repository is archived once the server deploys from here.
