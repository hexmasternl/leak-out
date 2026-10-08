---
name: triage-github-issues
description: Autonomously triage GitHub issues of the leak-out-server repository. Use when the user asks to triage, groom, refine, label or flesh out GitHub issues (by number, by URL, or "the open/untriaged issues"). Assesses each issue, applies labels and the issue type (Feature, Bug or Task), labels unclear issues `triage`, rewrites clear issues into a detailed description of the problem and the fix, and — when the issue is likely to be implemented — creates an OpenSpec change for it and links the issue and the change to each other. Works without asking for confirmation.
license: MIT
compatibility: Requires git, the GitHub CLI (`gh`) authenticated with write access to hexmasternl/leak-out-server, and the OpenSpec CLI (`openspec`). Works from PowerShell or Git Bash on Windows.
metadata:
  author: hexmasternl
  version: "1.0"
---

Triage GitHub issues of `hexmasternl/leak-out-server`, the ASP.NET Core Web API that stores the best score per player per level for the Leak Out game (players authenticate through Google Play Games Services).

**This skill is autonomous.** Do not ask the user to confirm titles, labels, types, bodies or OpenSpec changes: decide, apply, and report afterwards. The only reasons to stop early are the prerequisites in step 0 failing or `gh`/`git` refusing an operation. Where a decision would normally need the maintainer, record it as an open question or a suggestion in the report instead of asking.

**Input**: one or more issue numbers or URLs. Without input, triage every open issue that has no issue type, or that carries the `triage` label and received a new comment after the last triage edit:

```bash
gh issue list --state open --limit 100 --json number,title,labels,issueType,updatedAt
```

Process issues in ascending number order, so later issues can refer to the changes and decisions of earlier ones.

## 0. Prerequisites (once per run)

```bash
gh auth status
openspec --version
git fetch origin
git ls-tree --name-only origin/development openspec/config.yaml   # OpenSpec must be committed on development
```

If `openspec/config.yaml` is not on `origin/development`, triage still runs (labels, type, body), but no OpenSpec change is created: say so in the report and tell the user to commit the `openspec/` folder to `development` first.

Make sure every label used below exists; create the missing ones once:

```bash
gh label list --limit 100 --json name
gh label create triage --color FBCA04 --description "Unclear: needs input from the reporter or maintainer before work can start"
gh label create feature --color 0E8A16 --description "Adds something that does not exist yet"
gh label create "area:<area>" --color C5DEF5 --description "<area description>"
```

## 1. Read the issue and its surroundings

```bash
gh issue view <n> --json number,title,body,labels,issueType,state,author,comments,url
```

Read the comments too: they often hold the real request or the answer to an earlier open question. Then build the context needed to write an accurate, detailed issue instead of a guessed one:

- **Repository guidance**: `CLAUDE.md` (architecture, commands, conventions).
- **Coding standards**: the HexMaster coding standards via the `hexmaster-coding-standards` MCP server (`list_documents`, `find_documents_by_tag`, `get_document`). The fix you describe must comply with them, in particular: modular monolith layout (ADR 0002), CQRS handlers (ADR 0004), Minimal APIs (ADR 0005), feature slices `HexMaster.LeakOut.{Module}.Features.{FeatureName}` with DTOs in `{Module}.Abstractions.DataTransferObjects` (ADR 0009), and resolving the Google Play player id (the external subject id) to an internal `Guid` UserId at the API boundary, owned by the Players module (ADR 0010). If the MCP server is unavailable, read the documents from `https://github.com/hexmasternl/hexmaster-coding-standards/tree/main/docs`.
- **Current behaviour**: `openspec/specs/<capability>/spec.md` describes what the server does today. This decides `feature` versus `enhancement` and supplies the right names.
- **Code**: the modules under `src/` (`Core`, `Players`, `Scores`, `Aspire`, and any newer module folder). Read the code that the issue touches, so the fix names real projects, namespaces, endpoints and types.
- **The game** (client side): <https://github.com/hexmasternl/leak-out>, when the issue depends on what the Android app sends or expects.
- **Related issues**: `gh issue list --state all --search "<keywords>" --json number,title,state,labels` for duplicates and dependencies (for example, posting a score depends on authentication).

## 2. Assess: clear or unclear, likely to be implemented or not

Decide one of these outcomes per issue:

| Outcome | When | What happens |
| --- | --- | --- |
| **Clear, will be implemented** | In scope for the score server, actionable, and the problem and the expected outcome can be stated without guessing. Minor gaps can be filled with reasonable, recorded assumptions. | Full detailed body, labels, type, OpenSpec change created and linked. |
| **Clear, not to be implemented now** | `duplicate`, `invalid`, out of scope for this server (belongs to the game repo, for example), or it conflicts with the HexMaster standards. | Short body explaining why, labels, type. No OpenSpec change. Suggest closing in the report. |
| **Unclear** | The problem, the expected behaviour or the scope cannot be determined, or a missing decision would materially change the API contract, the stored data, security, or the acceptance criteria. | Add `triage`. Body gets the clarification questions. No OpenSpec change. |

An issue is never both `triage` and linked to a new OpenSpec change. When an issue that carried `triage` becomes clear (for example after the reporter answered), remove `triage` and continue as a clear issue.

## 3. Choose labels

Labels may be combined. Use only these, and leave any other label (`good first issue`, `help wanted`, ...) alone:

| Label | When |
| --- | --- |
| `feature` | Adds something that does not exist in the server yet (no spec in `openspec/specs` covers it). |
| `enhancement` | Changes or extends existing behaviour. Excludes `feature`. |
| `bug` | Something does not work as specified or as a client would reasonably expect. |
| `documentation` | The work is (also) about docs: `README.md`, `CLAUDE.md`, specs, OpenAPI descriptions. |
| `duplicate` | Another issue already asks for the same thing. Link the original in the body. |
| `invalid` | Not actionable or not about this project. |
| `question` | The reporter asks something rather than requests work. |
| `triage` | Unclear, see step 2. |
| `wontfix` | Never set this yourself: it is the maintainer's decision. Suggest it in the report. |
| `area:<area>` | One per affected area: `area:scores`, `area:players` (identity, authentication, the Google Play id → UserId mapping), `area:platform` (Core, Aspire, ServiceDefaults, build, CI, infrastructure). For a newer module under `src/`, use `area:<module-name-lowercase>`. |

Remove labels that the triage contradicts (for example `enhancement` on something that turns out to be a `feature`, or `triage` on an issue that is now clear), and mention it in the report.

## 4. Choose the issue type

Exactly one. The repository defines `Feature`, `Bug` and `Task`.

| Type | When |
| --- | --- |
| `Bug` | Labelled `bug`. |
| `Feature` | Labelled `feature`, or `enhancement` with a change an API client can observe (new or changed endpoint, response, validation, ranking). |
| `Task` | No client-observable change: setup and configuration, dependency upgrades, build/CI, infrastructure, refactoring, documentation only. |

## 5. Write the issue body

Write in plain English, short sentences, and concrete names from the code and the specs (modules, feature slices, endpoints, DTOs, tables). Everything you assume goes under **Assumptions**, so a reader can tell observed facts from decisions you made.

**Clear issue (feature, enhancement, task):**

```markdown
## Summary

<One or two sentences: what this issue delivers and for whom.>

## Problem

<What is missing or wrong today and why it matters to players, the game client or the maintainer. Refer to the current spec or code where relevant.>

## Proposed fix

<Detailed enough that someone new to the code can start. Cover what applies:>
- **Module and feature slice**: <e.g. `Scores` module, `HexMaster.LeakOut.Scores.Features.SubmitScore`, `SubmitScoreCommand` / `SubmitScoreCommandHandler`>
- **Endpoint**: <method, route, authentication, request and response DTOs in `Abstractions.DataTransferObjects`, status codes>
- **Behaviour**: <rules, edge cases, e.g. "a lower or equal score is ignored and returns 200 with the stored best score">
- **Identity**: <how the Google Play player id is resolved to the internal UserId, if relevant>
- **Persistence**: <tables, columns, indexes, migrations in `{Module}.Data.PostgreSQL`>
- **Configuration and orchestration**: <settings, secrets, Aspire AppHost resources>
- **Tests**: <which handlers, domain rules and endpoints get xUnit tests>

## Acceptance criteria

- [ ] <Observable, testable outcome>
- [ ] <...>

## Dependencies

- <Issues or changes this depends on, e.g. "Depends on #1 (authentication)". Omit the section if none.>

## Out of scope

- <What this issue deliberately does not cover.>

## Assumptions

- <Decisions made during triage that the maintainer may want to revisit. Omit if none.>

## OpenSpec change

Planned in [`<change-name>`](https://github.com/hexmasternl/leak-out-server/tree/development/openspec/changes/<change-name>).

---

<details>
<summary>Original report</summary>

<the original body, verbatim; "(empty)" if there was none>

</details>
```

**Bug**: replace *Summary* and *Problem* with **Problem** (what goes wrong and its impact), **Steps to reproduce**, **Expected behaviour** (referring to the spec) and **Actual behaviour**; keep the rest. Include the acceptance criterion "a test reproduces the problem and passes after the fix".

**Unclear issue**: keep it short. A **Summary** of what you understood, a **What is unclear** section with numbered, concrete questions the reporter or maintainer can answer, optionally a **Possible directions** section, and the original report. Then the `triage` label.

**Not to be implemented** (`duplicate`, `invalid`, out of scope): a short explanation and, for a duplicate, a link to the original issue, followed by the original report.

If the issue was triaged before (it already follows this format), update it in place: never nest a second "Original report".

## 6. Create the OpenSpec change (clear issues that will be implemented)

Skip this step when step 0 found no OpenSpec on `development`.

Creating a change is OpenSpec bookkeeping: it is committed directly to `development`, one change per commit, following flow B of the `git-workflow` skill (`.claude/skills/git-workflow/SKILL.md`). No worktree or feature branch is created now; that happens when the change is implemented (`/opsx:apply`).

1. **Look for an existing change first.** A change is linked to an issue when its `proposal.md` says `Tracked in #<n>` (or the issue URL):

   ```bash
   grep -rlE "(#|issues/)<n>\b" openspec/changes --include=proposal.md
   git grep -lE "(#|issues/)<n>\b" origin/development -- 'openspec/changes/*/proposal.md' 'openspec/changes/archive/*/proposal.md'
   ```

   Found one → do not create a second change; link the existing one (an archived change means it is implemented: suggest closing the issue in the report).

2. **Switch to an up-to-date `development`** in the main working tree; stop and report if `git status` shows uncommitted changes. Derive a kebab-case change name from the issue (`scores-submit-score`, `players-google-play-auth`), prefixed with the area.

   ```bash
   git status --porcelain
   git switch development
   git pull --ff-only origin development
   ```

3. **Write the change** by following the `openspec-propose` skill (`.claude/skills/openspec-propose/SKILL.md`) for the change name, with the triaged issue as the request. Two overrides apply because this skill is autonomous:
   - Do not ask the user about ambiguities. Make reasonable assumptions and record them in `design.md`. If an ambiguity is so material that you cannot write the specs, the issue was not clear: delete the uncommitted change folder (`rm -rf openspec/changes/<change-name>`) and treat the issue as unclear (step 2).
   - The planning boundary of `openspec-propose` still applies: artifacts only, no project code.

   The proposal's `## Why` section must start with: `Tracked in [#<n>](https://github.com/hexmasternl/leak-out-server/issues/<n>).` Mention dependencies on other issues as `Depends on #<m>`, never as `Tracked in`. The design must comply with the HexMaster coding standards (step 1).

4. **Validate, commit and push** only the change folder:

   ```bash
   openspec validate <change-name> --type change --strict
   git add openspec/changes/<change-name>
   git commit -m "Propose <change-name> for #<n>"
   git push origin development
   ```

   One commit per change: when triaging several issues, commit each change separately.

5. **Link the issue to the change** through the `## OpenSpec change` section of the body (step 5), pointing at the folder on the `development` branch. The proposal links back to the issue through its `Tracked in` line.

## 7. Apply to GitHub

Write the body to a file in your scratchpad directory (never in the repository), then:

```bash
gh issue edit <n> --title "<title>" --body-file <scratchpad>/issue-<n>.md \
  --add-label "feature,area:scores" --remove-label "enhancement"

gh api -X PATCH repos/hexmasternl/leak-out-server/issues/<n> -f type=Feature
```

If the REST call does not accept `type`, use GraphQL:

```bash
gh api graphql -f query='{repository(owner:"hexmasternl",name:"leak-out-server"){issue(number:<n>){id} issueTypes(first:20){nodes{id name}}}}'
gh api graphql -f query='mutation($id:ID!,$type:ID!){updateIssue(input:{id:$id,issueTypeId:$type}){issue{number issueType{name}}}}' -f id=<issue-id> -f type=<type-id>
```

Keep the title short: imperative for work items ("Submit a level score"), the symptom for bugs ("Lower score overwrites the best score"). Keep the original title when it is already good.

Never close, lock, assign or transfer an issue, and never apply `wontfix`. Those are the maintainer's decisions: suggest them in the report.

## 8. Verify and report

Re-read every issue (`gh issue view <n> --json title,labels,issueType,body`) and confirm the edits landed and the OpenSpec link resolves (`git ls-tree --name-only origin/development openspec/changes/<change-name>/proposal.md`). Then report one line per issue:

```
#1 Set up Google Play Games authentication — Task · feature, area:players, area:platform · change players-google-play-auth
#2 Submit a level score — Feature · feature, area:scores · change scores-submit-score · depends on #1
#4 Show the scores around a player — Feature · feature, area:scores · triage: 2 questions for the reporter
```

Follow with anything the maintainer should look at: questions on `triage` issues, assumptions recorded in changes, suggested closes, possible duplicates, labels removed or created, and any step that failed.
