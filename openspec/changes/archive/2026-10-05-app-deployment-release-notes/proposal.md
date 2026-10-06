# Proposal

## Why

Every push to `main` uploads a release to the Play Console (see `app-deployment-workflow-in-github`), but that release has no "What's new" text, so testers and players can't see what changed, and writing notes by hand after each upload gets forgotten. Everything needed is already in git: each release is tagged `v<x.y.z>`, and the merged pull requests since the previous tag are exactly what the new release contains. Issue #73.

## What Changes

- The deployment workflow assesses the commits between the previous release tag and the released commit and generates player-facing release notes from them.
- Each merged feature pull request becomes one note, taken from its PR title with the change-name prefix, issue references and `+semver:` markers removed.
- Commits can steer the notes with a `Release-note: <text>` trailer (replaces the PR title, or adds a note for a commit that reached `main` without a PR) and `Release-note: none` (leaves a PR out).
- Merges of `development`, other merge commits and commits without a trailer don't produce notes, so developer wording never reaches players.
- The notes respect the Play Console's 500-character limit by dropping whole lines and adding a "…and more improvements" line, and fall back to `Bug fixes and improvements.` when nothing qualifies.
- The Play release is created with the notes (`en-US`). Generating notes never fails a deployment.
- The notes also appear in the run's step summary and in the release tag's message.
- `docs/deployment.md` documents the rules, and the `openspec-work-on-change` skill writes PR titles that read as a release note.

## Capabilities

### New Capabilities
- `play-release-notes`: how release notes for a Play Console release are derived from the released commits, formatted within Play's limits, and attached to the release.

### Modified Capabilities
None. The deployment capability (`app-deployment`) is still in the unarchived change `app-deployment-workflow-in-github`; this change adds a separate capability rather than editing it.

## Impact

- `.github/workflows/deploy-play-internal.yml`: new steps to test and run the generator; `whatsNewDirectory` on the upload step; notes in the step summary and tag message.
- New `scripts/generate-release-notes.sh` and `scripts/generate-release-notes.test.sh` (bash and git only, no new dependencies).
- `docs/deployment.md`: new "Release notes" section and troubleshooting entry.
- `.claude/skills/openspec-work-on-change/SKILL.md` (synced to `.github/skills/`): PR-title guidance.
- No app (`src/`) code changes.
