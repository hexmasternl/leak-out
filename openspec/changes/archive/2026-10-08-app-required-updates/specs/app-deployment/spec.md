## ADDED Requirements

### Requirement: Pull requests into main carry exactly one update label
The repository SHALL have the labels `update-optional` and `update-required`, and a GitHub Actions workflow that runs for pull requests into `main` when they are opened, reopened, synchronized, labeled or unlabeled. It SHALL fail unless the pull request has exactly one of these two labels, with a message naming both. It SHALL need no secrets.

#### Scenario: No update label
- **WHEN** a pull request into `main` has neither label
- **THEN** the check fails and its message names `update-optional` and `update-required`

#### Scenario: Both update labels
- **WHEN** a pull request into `main` has both labels
- **THEN** the check fails

#### Scenario: Label added later
- **WHEN** the check failed and the maintainer adds `update-required`
- **THEN** the check runs again and passes

#### Scenario: Pull requests into other branches
- **WHEN** a pull request into `development` is opened without an update label
- **THEN** the check does not run

### Requirement: Release update priority follows the update label
Before building, the deployment SHALL decide the release's in-app update priority. For a push, it SHALL find the pull request merged as the pushed commit and use priority 5 for `update-required` and 0 for `update-optional`. For a manual run, a required boolean `required-update` input SHALL decide (5 or 0). If no pull request, no update label or both labels are found, the run SHALL fail before building. The upload SHALL use that priority, and the job summary SHALL show it and its source.

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

### Requirement: Setup guide documents required updates
`docs/deployment.md` SHALL explain the two update labels and what each does for players, how to make the label check a required status check on `main`, the `required-update` input of a manual run, and that Google Play does not allow changing a release's priority after upload, so a release that must become required is shipped again with `update-required`.

#### Scenario: Guide covers the labels
- **WHEN** a contributor reads `docs/deployment.md`
- **THEN** it names `update-optional` and `update-required`, the required status check setup and the `required-update` input
