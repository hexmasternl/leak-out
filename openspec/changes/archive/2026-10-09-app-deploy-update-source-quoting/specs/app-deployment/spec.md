## MODIFIED Requirements

### Requirement: Release update priority follows the update label
Before building, the deployment SHALL decide the release's in-app update priority. For a push, it SHALL find the pull request merged as the pushed commit and use priority 5 for `update-required` and 0 for `update-optional`. For a manual run, a required boolean `required-update` input SHALL decide (5 or 0). If no pull request, no update label or both labels are found, the run SHALL fail before building. The upload SHALL use that priority, and the job summary SHALL show it and its source. The decided values SHALL be read back as data, never run as shell code.

#### Scenario: Required release
- **WHEN** a pull request labelled `update-required` is merged into `main`
- **THEN** the release is uploaded with in-app update priority 5, and the job summary says required, from the pull request label

#### Scenario: Optional release
- **WHEN** a pull request labelled `update-optional` is merged into `main`
- **THEN** the release is uploaded with in-app update priority 0

#### Scenario: Direct push without a pull request
- **WHEN** a commit is pushed to `main` without a pull request
- **THEN** the run fails before building, and nothing is uploaded or tagged

#### Scenario: Manual run
- **WHEN** a maintainer starts the workflow by hand with `required-update` set to true
- **THEN** the release is uploaded with in-app update priority 5

#### Scenario: Source with spaces is carried through
- **WHEN** the priority is decided by a pull request label or by the manual input
- **THEN** the step completes, and the job summary and later steps receive the source exactly as `pull request label` or `manual input`
