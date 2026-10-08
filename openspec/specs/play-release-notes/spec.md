# play-release-notes Specification

## Purpose
Gives every Play Console release player-facing release notes ("What's new"), derived automatically from the commits the release contains.

## Requirements

### Requirement: Notes cover the commits since the previous release
The release notes SHALL be derived from the commits reachable from the released commit but not from the newest `v*` release tag that is an ancestor of the released commit, ignoring a tag on the released commit itself. When no such tag exists, every commit reachable from the released commit SHALL be considered. Of those commits, only the ones that change a file under `app/` SHALL be considered; a merge commit counts when it changes `app/` compared with its first parent.

#### Scenario: Commits since the last release
- **WHEN** `main` was released as `v0.3.0` and two feature pull requests that change the app have been merged since
- **THEN** the notes for the next release are derived only from the commits after `v0.3.0`, including those two merges

#### Scenario: First release
- **WHEN** no `v*` tag exists in the history of the released commit
- **THEN** the notes are derived from the whole history

#### Scenario: Tag on the released commit is ignored
- **WHEN** the released commit already carries the tag `v0.3.1`
- **THEN** the range starts at the newest earlier tag, not at `v0.3.1`

#### Scenario: Server changes give no notes
- **WHEN** a pull request that only changes files under `server/` was merged into `main` since the previous release
- **THEN** that merge and its commits give no release note

### Requirement: Each merged feature pull request gives one note
Each merge commit of a pull request from a `feature/<change-name>` branch in the range SHALL give one note, taken from the pull request title in the merge commit body. The note SHALL have a leading `<change-name>:` prefix, issue references (`(issue #N)`, `(#N)`) and `+semver: <word>` markers removed, surrounding whitespace trimmed and its first letter capitalised.

#### Scenario: PR title cleaned up
- **WHEN** the range holds the merge of `feature/app-game-more-achievements` titled `app-game-more-achievements: 14 new achievements (issue #64)`
- **THEN** the notes contain the line `• 14 new achievements`

#### Scenario: Semver marker removed
- **WHEN** a merged feature PR is titled `app-splash-screen: show a splash screen +semver: minor`
- **THEN** its note is `Show a splash screen`

### Requirement: Release-note trailers override and add notes
A commit MAY carry `Release-note: <text>` trailers. Trailers on commits within a feature pull request SHALL replace that pull request's title as its notes, one note per trailer. A trailer on a commit not within any feature pull request SHALL add a note of its own. `Release-note: none` SHALL leave the pull request, or the commit, out of the notes.

#### Scenario: Trailers replace the PR title
- **WHEN** a merged feature PR contains commits with `Release-note: Pipes glow when they fill` and `Release-note: New level pack`
- **THEN** the notes contain those two lines and not the PR title

#### Scenario: PR left out
- **WHEN** a commit in a merged feature PR carries `Release-note: none`
- **THEN** that PR contributes no note

#### Scenario: Direct commit with a trailer
- **WHEN** a commit that reached `main` without a feature PR carries `Release-note: Fixed a crash on start`
- **THEN** the notes contain `• Fixed a crash on start`

### Requirement: Other commits produce no notes
Merge commits of branches other than `feature/*` (such as `development` into `main`, or `Merge remote-tracking branch …`) and non-merge commits without a `Release-note:` trailer SHALL NOT produce notes. Notes SHALL be ordered oldest first, and a note text that occurs more than once SHALL be listed once, at its first position.

#### Scenario: Developer commits are filtered
- **WHEN** the range holds `app-welcome-exit-button: archive the change and sync its specs` and a merge of `development` titled `Development`, neither with a trailer
- **THEN** neither appears in the notes

#### Scenario: Duplicates listed once
- **WHEN** two notes have the same text
- **THEN** the notes list it once

### Requirement: Notes fit the Play Console limit
The notes SHALL be plain text with one note per line, each starting with `• `, and SHALL be at most 500 characters. When the notes are longer, whole lines SHALL be dropped from the end and a final `• …and more improvements` line added so the result is at most 500 characters. A note SHALL never be cut in half.

#### Scenario: Long notes truncated on whole lines
- **WHEN** the qualifying notes add up to more than 500 characters
- **THEN** the notes end with `• …and more improvements`, are at most 500 characters, and every other line is a complete note

#### Scenario: Short notes untouched
- **WHEN** the qualifying notes add up to 500 characters or fewer
- **THEN** all of them are listed and no "…and more improvements" line is added

### Requirement: Fallback text when nothing qualifies
When no commit in the range qualifies for a note, the notes SHALL be `Bug fixes and improvements.`

#### Scenario: Only internal changes
- **WHEN** every commit in the range is filtered out or marked `Release-note: none`
- **THEN** the notes are `Bug fixes and improvements.`

### Requirement: The Play release carries the notes
The deployment workflow SHALL create the Play Console release with the generated notes as its `en-US` release notes.

#### Scenario: Release created with notes
- **WHEN** a push to `main` deploys successfully
- **THEN** the release on the configured track shows the generated notes under "What's new" for `en-US`

### Requirement: Release notes never block a deployment
If generating the notes fails, the workflow SHALL log a warning, use `Bug fixes and improvements.` as the notes, and continue the deployment.

#### Scenario: Generator fails
- **WHEN** the notes generator exits with an error during a deployment
- **THEN** the run logs a warning, uploads the bundle with the fallback notes, and does not fail because of the notes

### Requirement: Notes are visible on GitHub
The workflow SHALL add the notes to the run's step summary and to the message of the annotated `v<x.y.z>` tag it pushes.

#### Scenario: Notes in summary and tag
- **WHEN** a deployment finishes and pushes `v0.3.1`
- **THEN** the run summary shows the notes and `git show v0.3.1` shows them in the tag message
