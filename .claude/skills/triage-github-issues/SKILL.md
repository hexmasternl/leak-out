---
name: triage-github-issues
description: Triage GitHub issues for this repository. Use when the user asks to triage, groom, refine, clean up, label or flesh out one or more GitHub issues (by number, by URL, or "the open/untriaged issues"). Rewrites each issue into a complete story with acceptance criteria while keeping the reporter's original text, applies labels (feature, enhancement, bug, documentation, duplicate, invalid, question, wontfix — combinable), sets the issue type (Feature, Bug or Task), and links the OpenSpec change that implements the issue, proposing a new change autonomously when none exists in openspec/changes.
license: MIT
compatibility: Requires git and the GitHub CLI (`gh`), authenticated with write access to the repository. Works from PowerShell or Git Bash on Windows.
metadata:
  author: hexmasternl
  version: "1.0"
---

Triage GitHub issues: turn each one into a good, complete story, label it, give it a type, and link the OpenSpec change that covers it, creating that change when there is none.

**Only untriaged issues are triaged.** An issue is **untriaged** when it has no labels at all, or no issue type. An issue with both at least one label and an issue type is triaged and is left alone.

**Input**: one or more issue numbers or URLs. If none are given, triage the open untriaged issues (`gh issue list --state open --json number,title,labels,issueType`, then keep those with an empty `labels` list or no `issueType`), and tell the user which ones you picked. Triage all of them, oldest first, even if the list is long. If the user names an issue that is already triaged, skip it and say so in your report; re-triage it only when the user explicitly asks for that issue to be triaged again.

## What a triaged issue looks like

1. A **title** that says what the issue is about in a few words, in the imperative for work items ("Add haptic feedback") or as the symptom for bugs ("Game crashes when the stack is empty"). Keep the original title if it is already good.
2. A **body** that follows the matching template below, with the reporter's text kept verbatim at the bottom.
3. One or more **labels** from the label set.
4. Exactly one **issue type**: `Feature`, `Bug` or `Task`.
5. A link to the **OpenSpec change** that implements it. When there is none, triage creates one (step 2), so every actionable issue ends up with a change.

## Steps

### 1. Read the issue and its surroundings

```bash
gh issue view <n> --json number,title,body,labels,issueType,state,author,comments,url
```

Read the comments too: they often hold the reproduction steps, screenshots or the real request. Then gather context so the story is accurate rather than guessed:

- **Project scope**: `docs/project-plan.md` (roadmap and milestones), `docs/design-system.md`, `docs/level-format.md`.
- **Current behaviour**: `openspec/specs/<capability>/spec.md` describes what the app does today. Search it for the feature the issue talks about; this decides `feature` vs `enhancement` and supplies the right names for screens and concepts.
- **Code**, only when needed to pin down a bug or name the affected area (`src/core`, `src/app`).
- **Possible duplicates**: `gh issue list --state all --search "<keywords>" --json number,title,state`.

### 2. Find the OpenSpec change

Look for a change, active or archived, that implements this issue. Proposals reference their issue as `#<n>` or by URL (for example "Tracked in [#85](https://github.com/hexmasternl/leak-out/issues/85)"):

```bash
grep -rlE "(#|issues/)<n>\b" openspec/changes --include=proposal.md
```

- A hit is only a match if the proposal is **about** this issue (it says "Tracked in #n", "Fixes #n" or the like). Proposals also mention other issues as dependencies ("Depends on ... (#80)"); those are not matches.
- If grep finds nothing, also scan the folder names and the `## Why` sections of the active changes in `openspec/changes/` (skip `archive/`) for the same topic. Only link a change found this way if it clearly covers the issue, and say in your report that the match is by topic.
- Active change → link `openspec/changes/<change-name>/`. Archived change → link `openspec/changes/archive/<date>-<change-name>/` and mention that it has been implemented and archived; the issue can probably be closed (ask, do not close it yourself).
- No change → create one, see below. Skip this only for issues labelled `duplicate`, `invalid` or `wontfix`; those get no change and nothing about OpenSpec in the issue.

#### Creating the change when none exists

Do this autonomously, as part of the triage, without asking for approval:

1. Make sure you are on an up-to-date `development` (`git fetch origin`, `git switch development`, `git merge --ff-only origin/development`). Never create a change on `main`. If the main checkout has uncommitted work on another branch, add a temporary worktree for `development` under `../leak-out-worktrees/` instead of switching.
2. Pick a kebab-case name that follows the existing changes (`app-<area>-<topic>` for app work, `repo-<topic>` for repository/CI work, and so on), and check that it is not already taken in `openspec/changes/` or `openspec/changes/archive/`.
3. Follow the `openspec-propose` skill to create every artifact up to apply-ready (proposal, design, specs, tasks), with these differences: never stop to ask the user; decide from the issue, the specs and the code, and put whatever needs the maintainer's decision under **Open Questions** in `design.md` (the same questions as in the issue).
4. Link the change to the issue: the proposal's `## Why` section ends with `Tracked in [#<n>](https://github.com/hexmasternl/leak-out/issues/<n>).`, and the issue body gets the **OpenSpec change** section (step 5).
5. Run `openspec validate <change-name>` and fix what it reports.
6. Commit only the change folder on `development` with the message `<change-name>: propose change for #<n>` (plus the attribution trailer), and push `development`. The link in the issue only works once the push has landed.

Link to the folder on the `development` branch, where changes are created:

```
https://github.com/hexmasternl/leak-out/tree/development/openspec/changes/<change-name>
```

### 3. Choose labels

Labels may be combined. Use only these:

| Label           | When                                                                                                    |
| --------------- | ------------------------------------------------------------------------------------------------------- |
| `feature`       | Adds something new that does not exist in the app yet (no spec in `openspec/specs` covers it).          |
| `enhancement`   | Changes or extends an existing feature.                                                                  |
| `bug`           | Something does not work as specified or as a player would reasonably expect, and the issue asks for a fix. |
| `documentation` | The work is (also) about docs: `docs/`, `README`, specs, instructions.                                  |
| `duplicate`     | Another issue already asks for the same thing.                                                           |
| `invalid`       | Not actionable or not about this project (spam, wrong repo, nothing to do).                              |
| `question`      | The reporter asks something rather than requests work, or the issue cannot be turned into a story without an answer from the reporter. |
| `wontfix`       | Only when the user (the maintainer) has decided it will not be done. Never decide this yourself; propose it. |

Rules of thumb:
- `feature` and `enhancement` exclude each other; pick by whether the thing already exists.
- `bug` combines with `enhancement` when fixing it also changes how a feature behaves, and with `documentation` when the docs are wrong.
- `duplicate` goes with a link to the original issue in the body; `invalid`, `duplicate` and `wontfix` issues get a short body explaining why, not a full story.
- Remove labels that the triage contradicts (for example `enhancement` on something that turns out to be a `feature`), and mention it in your report.

### 4. Choose the issue type

Exactly one, set from what the work is:

| Type      | When                                                                                                         |
| --------- | ------------------------------------------------------------------------------------------------------------ |
| `Bug`     | The issue is labelled `bug`: something is broken and must be fixed.                                          |
| `Feature` | The issue is labelled `feature`, or `enhancement` with a change a player can see or feel.                     |
| `Task`    | Work without a player-visible behaviour change: dependency or SDK upgrades, Play Console policy warnings, build/CI, refactoring, documentation-only, performance work without new behaviour. |

Play Console reports ("Your app uses deprecated APIs", "outdated SDK version") are usually `Task`; a crash or ANR report from the Play Console is a `Bug`.

### 5. Write the story

Write in plain English, short sentences, concrete terms from the specs and the design system (the playing field, the stack, sections, the HUD, the start and finish pipes). Do not invent requirements: anything you assume goes under **Open questions**, and if the issue cannot become a story without the reporter's answer, add the `question` label.

**Feature / enhancement / task template:**

```markdown
## Story

As a <player | maintainer | ...>, I want <capability>, so that <benefit>.

## Context

<Why this matters, what exists today (link the spec in openspec/specs if relevant), what triggered the issue.>

## Acceptance criteria

- [ ] <Observable, testable outcome>
- [ ] <...>

## Out of scope

- <What this issue deliberately does not cover, if anything is likely to be assumed.>

## Open questions

- <Anything that needs a decision. Omit the section if there are none.>

## OpenSpec change

<Found or created in step 2; omit for duplicate, invalid and wontfix issues:> Implemented by [`<change-name>`](https://github.com/hexmasternl/leak-out/tree/development/openspec/changes/<change-name>).

---

<details>
<summary>Original report</summary>

<the original body, verbatim; "(empty)" if there was none>

</details>
```

**Bug template:** replace *Story* with these sections and keep the rest (Acceptance criteria, Open questions, OpenSpec change, Original report):

```markdown
## Problem

<One or two sentences: what goes wrong and the impact on the player.>

## Steps to reproduce

1. <...>

## Expected behaviour

<What should happen, referring to the spec where possible.>

## Actual behaviour

<What happens instead.>

## Environment

<Device, Android version, app version, if known. Otherwise "Unknown" and add it to Open questions.>
```

Acceptance criteria are the heart of the story: each one is something a tester can check, written so it can become a unit or instrumented test. For bugs, include "a test reproduces the problem and passes after the fix".

If the issue already follows this format (an explicit re-triage, or an issue that lost its labels or type), update it in place instead of nesting a second "Original report".

### 6. Apply

Triage runs autonomously: apply the title, labels, type and body straight away, without showing a draft or asking for approval first. The maintainer has authorised this; anything that needs their decision goes under **Open questions** in the issue and in your report, not into a question that blocks the triage.

Write the body to a file in your scratchpad (not the repository), then:

```bash
gh issue edit <n> --title "<title>" --body-file <scratchpad>/issue-<n>.md \
  --add-label "feature,documentation" --remove-label "enhancement"

# Issue type (the org defines Feature, Bug and Task)
gh api -X PATCH repos/hexmasternl/leak-out/issues/<n> -f type=Feature
```

If the REST call does not accept `type`, fall back to GraphQL:

```bash
gh api graphql -f query='{repository(owner:"hexmasternl",name:"leak-out"){issue(number:<n>){id} owner{... on Organization{issueTypes(first:20){nodes{id name}}}}}}'
gh api graphql -f query='mutation($id:ID!,$type:ID!){updateIssue(input:{id:$id,issueTypeId:$type}){issue{number issueType{name}}}}' -f id=<issue-id> -f type=<type-id>
```

Labels outside the table above are left alone (for example `good first issue`, `help wanted`). If one of the table's labels is missing from the repository, create it with `gh label create` and mention it.

Never close, lock, assign or transfer an issue as part of triage. For a duplicate, an archived (done) OpenSpec change, or `invalid`/`wontfix`, suggest closing and let the user decide.

### 7. Verify and report

Re-read each issue (`gh issue view <n> --json title,labels,issueType`) to confirm the changes landed. Then report per issue, in one line each where possible:

```
#85 Haptic feedback — Feature · feature · linked app-game-haptic-feedback
#91 Reduce battery usage during play — Bug · bug, enhancement · proposed app-battery-frame-budget · 2 open questions
#88 Upgrade androidx.fragment — Task · enhancement · linked app-play-console-policy-fixes (by topic)
```

Follow with anything that needs the user: issues skipped because they were already triaged, open questions, suggested closes, possible duplicates, labels removed.
