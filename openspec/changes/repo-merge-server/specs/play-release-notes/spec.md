## MODIFIED Requirements

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
