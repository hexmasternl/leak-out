---
name: sync-agent-config
description: Keep Claude Code and GitHub Copilot configuration in sync. Use whenever you create, edit, rename or delete CLAUDE.md, AGENTS.md, .github/copilot-instructions.md, a skill (.claude/skills or .github/skills), a slash command or prompt file (.claude/commands, .github/prompts), or a custom agent (.claude/agents, .github/agents); also use when a sync hook reports a conflict or the user asks whether the two tools' configuration matches.
license: MIT
compatibility: Requires Node.js 18+ and git. Works from PowerShell or Git Bash on Windows.
metadata:
  author: hexmasternl
  version: "1.0"
---

This repository is worked on with both **Claude Code** and **GitHub Copilot**. Each tool reads its own copy of the instructions, skills, commands and agents, and the copies must never drift apart. This skill describes how they are kept in sync and what you must do when you touch any of them.

The sync itself is done by `scripts/sync-agent-config.mjs`. **Never copy or hand-edit counterpart files yourself**: edit one side, then let the script produce the other side.

## What is kept in sync

| Claude Code                         | GitHub Copilot                             | How                                   |
| ----------------------------------- | ------------------------------------------ | ------------------------------------- |
| `CLAUDE.md`                         | `AGENTS.md`, `.github/copilot-instructions.md` | identical copies (all three)      |
| `.claude/skills/<skill>/**`         | `.github/skills/<skill>/**`                | identical copies, every file          |
| `.claude/commands/<dir>/<name>.md`  | `.github/prompts/<dir>-<name>.prompt.md`   | body + `description`, `argument-hint` |
| `.claude/agents/<name>.md`          | `.github/agents/<name>.agent.md`           | body + `name`, `description`          |

For commands/prompts and agents, only the body and the shared frontmatter keys above are copied. Keys that only one tool understands (for example `tools`, `model`, `category`, `tags` on the Claude side, or `agent`, `tools`, `model`, `handoffs` on the Copilot side) stay on their own side and are never overwritten. Put tool-specific settings in those keys, not in the body.

Creating, editing or deleting a file on either side is propagated: a new skill gets its counterpart folder, a deleted prompt deletes its command.

## Automatic sync (hooks)

Both tools run the script as a hook, so normally you don't have to do anything:

- **Claude Code**: `.claude/settings.json`. It runs after every `Write`/`Edit`/`MultiEdit`, and again when Claude stops, which catches edits made through the shell.
- **Copilot (VS Code agent mode)**: `.github/hooks/sync-agent-config.json`. It runs after every tool call that touches a synced file, and when the agent stops.
- **git** (optional safety net): `.githooks/pre-commit` refuses a commit while the two sides differ. Enable it once per clone with `git config core.hooksPath .githooks`.

When a hook syncs something, it reports it (`Agent config sync: update ... (from ...)`). Mention the synced files to the user in your summary.

## Manual sync

If hooks are unavailable (Copilot outside VS Code, hooks disabled, files edited in an editor), run the script yourself after changing a synced file:

```bash
node scripts/sync-agent-config.mjs                          # sync every change since the last sync
node scripts/sync-agent-config.mjs --from CLAUDE.md         # this file is the version to keep
node scripts/sync-agent-config.mjs --check                  # report drift only; exit 1 if out of sync
```

Always finish a task that touched synced files by running `node scripts/sync-agent-config.mjs --check` and confirming it reports *in sync*.

## How the script decides which side is the source

It compares each file with its **baseline**: the content at the last sync, stored per clone or worktree in `.git/agent-sync-state.json`, or the committed `HEAD` version if there is no record yet.

- Only one side differs from its baseline: that side is copied to the others.
- A file was deleted and its counterpart is unchanged: the counterpart is deleted too.
- A counterpart doesn't exist yet: it is created.
- `--from <file>` (hooks pass the edited file automatically) always wins for that file's group.

## Resolving conflicts

The script refuses to guess, and exits with `CONFLICT`, when:

- both sides were changed in different ways since the last sync,
- one side was changed and the other deleted, or
- the sides differ and neither changed since the last sync (for example, drift that was committed).

To resolve one:

1. Show the difference: `git diff --no-index <claude file> <copilot file>`.
2. Decide which version is correct. Merge by hand into **one** of the files if both contain wanted changes. If the right version isn't clear from the conversation, ask the user.
3. Run `node scripts/sync-agent-config.mjs --from <the file you kept>`.
4. Re-run with `--check` to confirm everything is in sync.

`--prefer-newest` resolves every conflict by most recent modification time. Use it only when the user asks for it.

## Generated files

Tools such as `openspec init` / `openspec update` regenerate skills and commands for each tool separately, and can overwrite repository-specific additions (like the worktree step in `openspec-apply-change`). After running such a generator, run the script, inspect every conflict, and keep the repository-specific version unless the user says otherwise.

## Extending the sync

To sync a new kind of file, add a group to `discoverGroups()` and a path rule to `isManaged()` in `scripts/sync-agent-config.mjs`. Then update the table above. This skill is itself synced, so edit it in one place only.
