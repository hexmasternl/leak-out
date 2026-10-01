# Leak Out

A 2D pipe-building puzzle game for Android, written in Kotlin with Jetpack Compose. The player connects a start pipe to a finish pipe on a grid, using pipe sections drawn from a stack, before the flowing liquid leaks out. The full design and roadmap are in [docs/project-plan.md](docs/project-plan.md).

> This file is used by both Claude Code and GitHub Copilot. `CLAUDE.md`, `AGENTS.md` and `.github/copilot-instructions.md` are identical copies kept in sync automatically. Edit any one of them, never two at once. See [Keeping Claude Code and Copilot in sync](#keeping-claude-code-and-copilot-in-sync).

## Repository layout

**All application source code lives in `src/`**, which is the Gradle root (settings, wrapper, `gradle/libs.versions.toml`). Nothing outside `src/` is part of the app build.

```
leak-out/
├── src/
│   ├── core/     pure Kotlin: game rules, level model, parsing, validation (no Android dependencies)
│   └── app/      Android app: Compose UI, rendering, screens
├── docs/         project plan, design system, level format
├── openspec/     specs (openspec/specs) and change proposals (openspec/changes)
└── scripts/      repository tooling
```

Paths such as `core/...` or `app/...` in specs and tasks are relative to `src/`.

## Build and test

Run Gradle from `src/` (on Windows use `.\gradlew.bat`):

```sh
cd src
./gradlew :core:test                        # JVM unit tests for the game logic
./gradlew :app:testDebugUnitTest            # app JVM unit tests
./gradlew :app:connectedDebugAndroidTest    # instrumented tests (needs a running emulator/device)
```

Versions are pinned exactly in `src/gradle/libs.versions.toml`. Change them there, never inline in a build file.

## Conventions

- Keep game rules in `core` as pure, immutable Kotlin so they can be unit tested without Android. `app` renders state and forwards input.
- Every change comes with tests, and a change is only done once those tests have actually been run and pass.
- The design system is in [docs/design-system.md](docs/design-system.md); the level file format is in [docs/level-format.md](docs/level-format.md).

## Workflow: OpenSpec, worktrees and branches

Work is specified and tracked with [OpenSpec](https://github.com/Fission-AI/OpenSpec): propose, explore, apply and archive changes with the `openspec-*` skills (`/opsx:*` commands in Claude Code, `opsx-*` prompts in Copilot).

- `main` is production: never commit, push, rebase or reset it.
- `development` is the integration branch, branched from `main`.
- Each OpenSpec change is implemented on `feature/<change-name>`, branched from `development`, in its own worktree under `../leak-out-worktrees/<change-name>`. The `openspec-work-on-change` skill sets this up and owns committing and pushing.

## Releases

Every push to `main` runs `.github/workflows/deploy-play-internal.yml`: it runs the unit tests, builds a signed release bundle and uploads it to the Play Console's internal testing track. Signing material and Play credentials live only in GitHub secrets: never commit a keystore or key, and none is needed locally (without the `LEAKOUT_UPLOAD_*` variables the release build is simply unsigned). The version code comes from `-PversionCode` in CI; bump `versionName` by hand. Setup of every secret, key and variable: [docs/deployment.md](docs/deployment.md).

## Keeping Claude Code and Copilot in sync

Instructions, skills, commands/prompts and custom agents exist once per tool. `scripts/sync-agent-config.mjs` copies a change made on one side to the other:

| Claude Code                        | GitHub Copilot                                  |
| ---------------------------------- | ----------------------------------------------- |
| `CLAUDE.md`                        | `AGENTS.md`, `.github/copilot-instructions.md`  |
| `.claude/skills/<skill>/**`        | `.github/skills/<skill>/**`                     |
| `.claude/commands/<dir>/<name>.md` | `.github/prompts/<dir>-<name>.prompt.md`        |
| `.claude/agents/<name>.md`         | `.github/agents/<name>.agent.md`                |

Hooks run the script automatically in both tools (`.claude/settings.json`, `.github/hooks/sync-agent-config.json`). When you add, change or delete any of these files:

1. Edit **one** side only, and never hand-copy to the counterpart.
2. Before finishing, run `node scripts/sync-agent-config.mjs --check` and make sure it reports *in sync*. If it doesn't, run it without `--check`.
3. If it reports a `CONFLICT`, follow the `sync-agent-config` skill to resolve it.
