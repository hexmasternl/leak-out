---
name: openspec-work-on-change
description: Work on an OpenSpec change in its own git worktree and feature branch. Use when the user wants to start, resume, or finish implementing an OpenSpec change, and whenever an OpenSpec change is applied. Sets up the worktree under ../leak-out-worktrees/<change-name>, branches feature/<change-name> from development (creating development from main if needed), commits frequently, pushes when done, and opens a pull request into development.
license: MIT
compatibility: Requires git, the GitHub CLI (`gh`) and the openspec CLI. Works from PowerShell or Git Bash on Windows.
metadata:
  author: hexmasternl
  version: "1.0"
---

Work on an OpenSpec change inside a dedicated git worktree, on a feature branch that follows the repository's branch flow. This skill wraps the implementation work of `openspec-apply-change`: it decides **where** and **on which branch** the work happens, then hands the actual task implementation to that skill, and finally commits, pushes and opens a pull request into `development`.

**Input**: Optionally specify a change name. If omitted, infer it from conversation context. If it is vague or ambiguous you MUST prompt for the available changes.

## Branch flow (always maintained)

| Branch                    | Role                                                                                       |
| ------------------------- | ------------------------------------------------------------------------------------------ |
| `main`                    | Represents production. **Never commit to it, never push to it, never rebase or reset it.** |
| `development`             | Integration branch. Branched from `main`. Features are built on top of it.                 |
| `feature/<change-name>`   | One per OpenSpec change, named after the change. Branched from `development`.              |

Rules:
- If `development` does not exist locally or on the remote, create it from `main` and push it.
- A feature branch is always created from `development`, never from `main` and never from another feature branch.
- All implementation commits go on the feature branch only.

## Worktree layout

Worktrees live in a sibling folder of the repository, named `<repo-folder>-worktrees`. For this repository that is `leak-out-worktrees`, next to `leak-out`:

```
D:\projects\github.com\hexmasternl\
├── leak-out\                       ← main repository (stays on main, untouched)
└── leak-out-worktrees\
    ├── app-welcome-screen\         ← worktree for change "app-welcome-screen"
    └── app-game-level-model\       ← worktree for change "app-game-level-model"
```

The worktree folder name is exactly the OpenSpec change name. Derive the paths from git, do not hardcode them:

```powershell
$repoRoot     = (git rev-parse --show-toplevel)            # e.g. D:/projects/github.com/hexmasternl/leak-out
$repoName     = Split-Path $repoRoot -Leaf                  # leak-out
$worktreesDir = Join-Path (Split-Path $repoRoot -Parent) "$repoName-worktrees"
$worktreePath = Join-Path $worktreesDir $change
$branch       = "feature/$change"
```

Bash equivalent:

```bash
repo_root=$(git rev-parse --show-toplevel)
repo_name=$(basename "$repo_root")
worktrees_dir="$(dirname "$repo_root")/${repo_name}-worktrees"
worktree_path="$worktrees_dir/$change"
branch="feature/$change"
```

## Steps

1. **Select the change**

   If a name is provided, use it. Otherwise:
   - Infer from conversation context if the user mentioned a change
   - Auto-select if only one active change exists
   - If ambiguous, run `openspec list --json` and use the **AskUserQuestion tool** to let the user pick

   Validate that `openspec/changes/<change-name>/` exists. Announce: "Working on change: <name> in worktree <path> on branch feature/<name>".

2. **Sync with the remote**

   Run from the main repository:
   ```bash
   git fetch origin --prune
   ```
   Do not check out anything in the main repository. It stays on `main`.

3. **Ensure `development` exists**

   ```bash
   git show-ref --verify --quiet refs/heads/development          # local?
   git show-ref --verify --quiet refs/remotes/origin/development # remote?
   ```
   - Local and remote exist: nothing to do.
   - Only remote exists: `git branch --track development origin/development`
   - Only local exists: `git push -u origin development`
   - Neither exists: create it from `main` and publish it:
     ```bash
     git branch development origin/main
     git push -u origin development
     ```
   Never fast-forward or reset `development` from inside a feature worktree; only the branch creation above touches it.

4. **Create or reuse the worktree**

   Check `git worktree list --porcelain` for an entry whose path equals the worktree path.

   - **Worktree already exists** (resuming earlier work): reuse it. Confirm it is on `feature/<change-name>`; if not, stop and ask the user before switching branches.
   - **Branch exists but no worktree** (e.g. worktree was removed): attach it:
     ```bash
     git worktree add "<worktree-path>" "feature/<change-name>"
     ```
   - **Neither exists** (fresh start): create the branch from `development` and the worktree in one go:
     ```bash
     mkdir -p "<worktrees-dir>"
     git worktree add -b "feature/<change-name>" "<worktree-path>" development
     ```
     If `origin/development` is ahead of the local `development`, base on `origin/development` instead so the feature starts from the latest integration state.

   Then push the new branch immediately so it exists on the remote from the start:
   ```bash
   git -C "<worktree-path>" push -u origin "feature/<change-name>"
   ```

5. **Move into the worktree**

   Every subsequent command runs inside the worktree, not in the main repository. Either `cd` into the worktree path once (the working directory persists between shell calls), or prefix git commands with `git -C "<worktree-path>"`. Read and edit files by their absolute path under the worktree.

   Before starting, verify once:
   ```bash
   git -C "<worktree-path>" rev-parse --abbrev-ref HEAD   # must print feature/<change-name>
   ```
   If it prints `main` or `development`, stop. Something is wrong and the user must decide.

   Note for this repository: the Gradle root is `src/` inside the worktree, so Gradle commands run from `<worktree-path>/src`.

6. **Implement the change**

   Follow the `openspec-apply-change` skill from inside the worktree: run `openspec status` / `openspec instructions apply` there, read the context files there, work through `tasks.md` there. The OpenSpec change folder is part of the repository, so `openspec/changes/<change-name>/tasks.md` checkbox updates are made in the worktree and committed along with the code.

7. **Commit frequently, in logical chunks**

   Commit after every coherent unit of work, at minimum after each completed task. Do not batch the whole change into one commit. A good chunk is one of:
   - A new model or data class plus its unit tests
   - A single UI component or screen
   - A refactor that prepares for the next task
   - Marking a task done in `tasks.md` together with the code that completes it

   Use this commit message shape:
   ```
   <change-name>: <imperative summary of this chunk>

   <optional body: what and why, one or two sentences>

   Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>
   ```
   Stage explicitly (`git add <paths>`), never `git add -A` blindly; check `git status` first so build output or unrelated files do not sneak in. Run the relevant tests before committing when the chunk touches testable code, and say in the commit body if they were not run.

   Push intermediate commits when a natural milestone is reached (for example half the tasks done, or before a long-running step). Pushing early is cheap and protects the work.

8. **Finish: final commit and push**

   When every task in `tasks.md` is checked:
   - Run the full test suite for the touched modules and make sure it passes. Report failures honestly and do not push a red build without telling the user.
   - Make a final commit containing any remaining changes (last task checkboxes, cleanup).
   - Push:
     ```bash
     git -C "<worktree-path>" push origin "feature/<change-name>"
     ```
   - Confirm the push landed: `git -C "<worktree-path>" status -sb` should show the branch in sync with `origin/feature/<change-name>`.

9. **Open the pull request into `development`**

   Once the final push has landed, open a pull request from `feature/<change-name>` into `development` with the GitHub CLI:

   ```bash
   gh pr create --base development --head "feature/<change-name>" \
     --title "<change-name>: <short summary of the change>" \
     --body "<body>"
   ```

   The title becomes a line in the Play Store release notes. The deploy workflow removes the `<change-name>:` prefix, so write the summary for players: short, in plain language, about what they will notice (`app-game-pause: pause the game at any time`, not `app-game-pause: add PauseState and wire the overlay`). Keep issue references like `(issue #64)`: they are removed too. When players won't notice the change at all (refactors, CI, docs, tests), add a commit with the trailer `Release-note: none` before opening the pull request. To word the note differently from the title, use `Release-note: <text>` trailers instead. Full rules: `docs/deployment.md#release-notes`.

   Run it from inside the worktree (or add `--repo hexmasternl/leak-out`). Build the body from the OpenSpec change artifacts:
   - A one-paragraph summary taken from `openspec/changes/<change-name>/proposal.md`
   - A checklist of the completed tasks from `tasks.md`
   - Which tests were run and their result (or why they were not run)
   - A closing line: `Archive with openspec-archive-change after merge.`

   Rules:
   - The base branch is always `development`, never `main`.
   - If a pull request for the branch already exists, do not create a second one: report the existing URL (`gh pr view --json url,state`) and, if the description is stale, update it with `gh pr edit`.
   - Leave the pull request open. Do not merge it, do not delete the branch, and do not remove the worktree on your own.
   - If `gh` is unavailable or unauthenticated, report that and give the user the compare URL `https://github.com/hexmasternl/leak-out/compare/development...feature/<change-name>?expand=1` instead of failing silently.

   Report the pull request URL, then stop. Merging and archiving the change with `openspec-archive-change` remain the user's call.

## Output on start

```
## Working on: <change-name>

**Worktree:** D:\projects\github.com\hexmasternl\leak-out-worktrees\<change-name>
**Branch:**   feature/<change-name>  (from development)
**State:**    created | resumed

Proceeding with implementation (see openspec-apply-change).
```

## Output on completion

```
## Change pushed: <change-name>

**Branch:**   feature/<change-name> → origin/feature/<change-name>
**Commits:**  <n> commits this session
**Progress:** <m>/<m> tasks complete ✓
**Tests:**    <passed | failed: details | not run: reason>
**Pull request:** <url>  (feature/<change-name> → development)

Next steps (your call):
- Review and merge the pull request into development
- Archive the change with openspec-archive-change after merge
- Remove the worktree: git worktree remove "<worktree-path>"
```

## Guardrails

- `main` is read-only. Never `git checkout main` in a worktree, never commit, push, merge, rebase, or reset it.
- `development` is only ever created by this skill, never modified from a feature worktree.
- One change, one worktree, one feature branch. Never work on two changes in the same worktree.
- Never run implementation commands in the main repository folder; if the current directory is the main repository, move to the worktree first.
- If the worktree path already exists on disk but is not a registered worktree, stop and ask. Do not delete it.
- If `git worktree add` fails because the branch is checked out elsewhere, stop and report where. Do not force it.
- Small, frequent commits beat one large commit. When in doubt, commit.
- Never `git push --force` to any branch of this repository.
- Do not merge into `development` or archive the change unless the user asks. Opening the pull request is expected; merging it is not.
