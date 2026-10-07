---
name: git-workflow
description: The git branching, worktree, commit, test/coverage and pull request workflow of the leak-out-server repository. Use whenever you implement a GitHub issue or apply an OpenSpec change (opsx apply), create an OpenSpec change (opsx propose, issue triage), archive an OpenSpec change (opsx archive), commit, push, or open a pull request. Covers which branch work happens on, where worktrees live, how often to commit, the 80% coverage gate, and closing the linked GitHub issue when a change is archived.
license: MIT
compatibility: Requires git, the .NET 10 SDK, the GitHub CLI (`gh`) authenticated with write access to hexmasternl/leak-out-server, the OpenSpec CLI (`openspec`) and the `dotnet-coverage` global tool. Works from PowerShell or Git Bash on Windows.
metadata:
  author: hexmasternl
  version: "1.0"
---

The git workflow of `hexmasternl/leak-out-server`. Follow it for every change to the repository; other skills (`openspec-propose`, `openspec-apply-change`, `openspec-archive-change`, `triage-github-issues`) do their work inside the branches this skill prescribes.

## Branches

| Branch | Role | Who writes to it |
| --- | --- | --- |
| `main` | Production. | Never commit or push to it directly. It only changes through a pull request from `development`, opened and merged by the maintainer. |
| `development` | Integration branch, created from `main`. Feature pull requests target it. | Feature pull requests, plus the two OpenSpec bookkeeping actions below (proposing and archiving changes), which are committed to it directly. |
| `feature/<name>`, `fix/<name>` | One per issue or OpenSpec change, created from `development` in its own worktree. | You, while implementing. |

If `development` does not exist yet on `origin`, create it from `main` once and push it:

```bash
git fetch origin
git ls-remote --exit-code --heads origin development || (git branch development origin/main && git push -u origin development)
```

## Decide which flow applies

| Situation | Flow |
| --- | --- |
| Implementing a GitHub issue, or applying an OpenSpec change (`/opsx:apply`) | **A. Implement in a worktree** |
| Creating an OpenSpec change (`/opsx:propose`, or an issue triage that creates a change) | **B. Bookkeeping on `development`** |
| Archiving an OpenSpec change (`/opsx:archive`) | **B. Bookkeeping on `development`**, then **C. Close the linked issue** |

Only flow B commits directly to `development`. Anything else that touches code, tests, configuration, docs or specs outside of proposing/archiving goes through flow A.

## A. Implement in a worktree

### A1. Create the worktree and branch

Worktrees always live in `D:\projects\github.com\hexmasternl\leak-out-server-worktrees` (Git Bash: `/d/projects/github.com/hexmasternl/leak-out-server-worktrees`), one folder per branch, named after the branch without its prefix.

Branch name:

- OpenSpec change: `feature/<change-name>` (the change folder name, e.g. `feature/scores-submit-score`).
- GitHub issue without a change: `feature/<n>-<short-kebab-title>`, or `fix/<n>-<short-kebab-title>` for a bug.

First check whether the branch or worktree already exists (an earlier session may have started it), and reuse it if so:

```bash
git fetch origin
git worktree list
git ls-remote --heads origin "<branch>"
```

Otherwise create it from the latest `development`:

```bash
WT=/d/projects/github.com/hexmasternl/leak-out-server-worktrees
git worktree add -b <branch> "$WT/<name>" origin/development
```

If the branch exists only on `origin`, attach a worktree to it instead: `git worktree add "$WT/<name>" <branch>`.

From here on, **do all work inside the worktree**: edit files under it, and run every `git`, `dotnet` and `openspec` command from it (or with `git -C "$WT/<name>"`). Never implement in the main working tree at `D:\projects\github.com\hexmasternl\leak-out-server`.

### A2. Implement and commit often

Commit every logical change as soon as it builds, so the history reads as a clear trail of how the feature came together. A logical change is one coherent step, for example:

- add the domain entity and its tests;
- add the command, handler and handler tests;
- wire the endpoint;
- add the EF Core configuration and migration;
- register the resource in the Aspire AppHost;
- tick off completed tasks in `openspec/changes/<change-name>/tasks.md`.

Rules:

- Small, focused commits; never one big commit at the end. Do not mix unrelated changes in one commit.
- Stage explicit paths (`git add <paths>`), not `git add -A`, so stray files such as `TestResults/` or coverage output are never committed.
- Imperative, descriptive subject line of at most ~72 characters (`Add SubmitScore command handler`), optionally a body explaining why. Reference the issue where it helps (`Validate level number in SubmitScore (#2)`).
- Push the branch regularly (`git push -u origin <branch>` the first time) so work is not lost.

### A3. Finish: tests and the 80% coverage gate

When the implementation is complete (all tasks of the change done, or the issue's acceptance criteria met), run all unit tests with code coverage from the worktree root. The repository uses xUnit v3 on Microsoft.Testing.Platform (`global.json`) with `Microsoft.Testing.Extensions.CodeCoverage`:

```bash
find src -type d -name TestResults -prune -exec rm -rf {} +; rm -rf coverage
dotnet test --solution src/leak-out-server.slnx \
  --coverage --coverage-output-format cobertura --coverage-output coverage.cobertura.xml
```

Every test must pass. Then merge the per-project reports and read the total line coverage:

```bash
dotnet tool list -g | grep -q dotnet-coverage || dotnet tool install -g dotnet-coverage
dotnet-coverage merge --output coverage/merged.cobertura.xml --output-format cobertura "src/**/TestResults/**/coverage.cobertura.xml"
grep -o -m1 'line-rate="[0-9.]*"' coverage/merged.cobertura.xml
```

The line rate must be **0.80 (80%) or higher**. If tests fail or coverage is below 80%:

- fix the failing code or tests, or add meaningful tests for the uncovered code (follow the `unit-testing-with-xunit-moq-bogus` skill);
- commit those fixes as their own logical commits;
- run A3 again.

Never lower the threshold, skip, disable or delete tests, or exclude production code from coverage to pass the gate. If the gate cannot be met (for example the build is broken by something outside this change), stop and report it instead of opening a pull request.

Delete the coverage output afterwards (`find src -type d -name TestResults -prune -exec rm -rf {} +; rm -rf coverage`); it is never committed.

### A4. Commit, push and open the pull request

```bash
git status                      # nothing left uncommitted
git push -u origin <branch>
gh pr create --base development --head <branch> --title "<title>" --body-file <scratchpad>/pr-body.md
```

Write the PR body in your scratchpad directory, never in the repository. It contains:

- a summary of what the change delivers;
- `Tracked in #<n>` for the linked issue (a PR into `development` does not auto-close issues; the issue is closed when the change is archived, see C);
- the OpenSpec change name, if any;
- the test result and the measured coverage percentage.

The pull request always targets `development`, never `main`. Report the PR URL to the user.

Keep the worktree until the pull request is merged. After the merge, clean up:

```bash
git worktree remove "$WT/<name>"
git branch -d <branch>
```

## B. OpenSpec bookkeeping on `development`

**Only** two actions are committed directly to `development`, without a worktree, branch or pull request:

1. **Creating an OpenSpec change** (a proposal: `proposal.md`, `design.md`, specs, `tasks.md`).
2. **Archiving an OpenSpec change** (moving it to `openspec/changes/archive/` and syncing its delta specs into `openspec/specs/`).

Each change is committed on its own: one proposal or one archive per commit, nothing else in that commit. Do it in the main working tree:

```bash
git switch development
git pull --ff-only origin development
# ... create the change (openspec-propose) or archive it (openspec-archive-change) ...
git add openspec/changes/<change-name>                     # proposal
git commit -m "Propose <change-name>"                      # add " for #<n>" when it tracks an issue
# or, for an archive:
git add openspec/changes openspec/specs
git commit -m "Archive <change-name>"
git push origin development
```

Before starting, check `git status`: if the main working tree has uncommitted changes, do not switch branches or commit them along; stop and report.

**Archiving happens after implementation is merged.** Before archiving, confirm the change's implementation has been merged into `development` (`git log origin/development` contains the feature branch, or `gh pr list --state merged --head feature/<change-name>`). If it has not, stop and report rather than archive.

## C. Close the linked GitHub issue on archive

After the archive commit is pushed, close the issue the change tracks. This is fully autonomous: do not ask for confirmation.

1. Find the issue link in the archived proposal. The `## Why` section of `proposal.md` starts with `Tracked in [#<n>](https://github.com/hexmasternl/leak-out-server/issues/<n>)`:

   ```bash
   grep -oE "Tracked in \[?#[0-9]+" openspec/changes/archive/*-<change-name>/proposal.md
   ```

   Only `Tracked in` links count. `Depends on #<m>` references are never closed.

2. If an issue is linked and still open, close it as completed with a comment pointing at the archived change:

   ```bash
   gh issue view <n> --json state -q .state
   gh issue close <n> --reason completed --comment "Implemented and archived in OpenSpec change \`<change-name>\` (commit <short-sha> on \`development\`)."
   ```

3. No link, or the issue is already closed: do nothing, and say so in the report.

## Never

- Commit or push to `main`, or open a pull request against `main`.
- Implement features or fixes directly on `development` or in the main working tree.
- Create worktrees anywhere other than `D:\projects\github.com\hexmasternl\leak-out-server-worktrees`.
- Force-push `development` or `main`, or rewrite their history.
- Bypass hooks (`--no-verify`) or the coverage gate.
