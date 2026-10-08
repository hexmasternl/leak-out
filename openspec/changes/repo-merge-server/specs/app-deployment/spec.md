## MODIFIED Requirements

### Requirement: Deployment runs on every push to main
The repository SHALL have a GitHub Actions workflow, `.github/workflows/deploy-play-internal.yml`, that runs on every push to `main` that changes a file under `app/` or the workflow file itself, and can be started by hand with `workflow_dispatch`. It SHALL NOT run for pushes to any other branch, for pull requests, or for pushes to `main` that only change files outside `app/` (such as `server/`). At most one run of the workflow SHALL be in progress at a time; a run started while another is in progress SHALL wait for it instead of cancelling it.

#### Scenario: Merge into main deploys
- **WHEN** a commit that changes a file under `app/` is pushed to `main`
- **THEN** the deployment workflow starts

#### Scenario: Server-only changes do not deploy the app
- **WHEN** a commit that only changes files under `server/` is pushed to `main`
- **THEN** the deployment workflow does not start

#### Scenario: Other branches do not deploy
- **WHEN** a commit is pushed to `development` or to a `feature/*` branch, or a pull request is opened
- **THEN** the deployment workflow does not start

#### Scenario: Manual run
- **WHEN** a maintainer starts the workflow with *Run workflow* on `main`
- **THEN** it builds and uploads exactly as for a push

#### Scenario: Deployments do not overlap
- **WHEN** a second push to `main` happens while a deployment is running
- **THEN** the second deployment waits until the first has finished, and neither is cancelled

### Requirement: Tests pass before anything is uploaded
The workflow SHALL run `:core:test` and `:app:testDebugUnitTest` from `app/src/` with JDK 21 before building the release bundle. If any test fails, the workflow SHALL fail and SHALL NOT upload anything.

#### Scenario: Failing unit test stops the deployment
- **WHEN** a unit test fails in the workflow
- **THEN** the run fails and no bundle is uploaded to the Play Console
