# Spec Delta

## Purpose

Automates verification and delivery of the score server: every pull request into `development` is built, tested and held to the 80% coverage gate, and every merge to `main` is published as a container image and deployed to Azure.

## ADDED Requirements

### Requirement: Pull requests into development are verified
Every pull request that targets `development` SHALL trigger a check that restores, builds the solution and runs all unit tests. The check SHALL fail when the build fails or any test fails.

#### Scenario: Passing pull request
- **WHEN** a pull request into `development` builds and all tests pass with at least 80% line coverage
- **THEN** the check succeeds

#### Scenario: Failing test
- **WHEN** a pull request into `development` contains a failing test
- **THEN** the check fails and reports the failing test

#### Scenario: New commits re-run the check
- **WHEN** a new commit is pushed to the pull request's branch
- **THEN** the check runs again for that commit

### Requirement: Coverage gate of 80%
The verification SHALL measure total line coverage across all test projects, merged into one report, and SHALL fail when it is below 80%. The measured percentage SHALL be visible in the run summary.

#### Scenario: Coverage below the gate
- **WHEN** merged line coverage is 79.9%
- **THEN** the check fails and the summary shows the measured coverage and the 80% threshold

#### Scenario: Coverage at the gate
- **WHEN** merged line coverage is exactly 80%
- **THEN** the coverage step passes

### Requirement: Pull request check is mandatory for merging
The pull request check SHALL have a stable name so it can be configured as a required status check on `development`, so that a pull request cannot be merged until it succeeds.

#### Scenario: Merge blocked on a failing check
- **WHEN** the check has failed on a pull request into `development` and branch protection requires it
- **THEN** GitHub does not allow the pull request to be merged

### Requirement: Infrastructure templates are validated on pull requests
The pull request check SHALL compile the Bicep templates and fail when they do not compile.

#### Scenario: Broken template
- **WHEN** a pull request contains a Bicep syntax error
- **THEN** the check fails

### Requirement: Merges to main are built, tested and published
Every push to `main` SHALL build the solution, run all tests with the same coverage gate, and, only when that succeeds, publish the API container image tagged with the commit SHA to the existing container registry.

#### Scenario: Successful publish
- **WHEN** a merge to `main` builds and passes the tests and the coverage gate
- **THEN** an image of the API tagged with the commit SHA is present in the existing registry

#### Scenario: Tests fail on main
- **WHEN** a test fails on a push to `main`
- **THEN** no image is published and no deployment starts

### Requirement: Merges to main are deployed to Azure at subscription scope
After the image is published, the workflow SHALL run a subscription-scope deployment of the infrastructure templates that creates the resource group if needed and deploys the published image tag. Azure login SHALL use OpenID Connect, without a stored Azure client secret.

#### Scenario: Deployment of a new image
- **WHEN** the image for a commit on `main` has been published
- **THEN** the subscription-scope deployment runs and the container app serves that image tag

#### Scenario: Manual redeploy
- **WHEN** the maintainer starts the deploy workflow manually
- **THEN** the same build, publish and deploy steps run for the selected commit

### Requirement: Secrets stay out of the repository and logs
The workflows SHALL take all credentials from GitHub secrets and SHALL NOT print secret values in logs.

#### Scenario: Secret masking
- **WHEN** a workflow run log is inspected
- **THEN** no registry password, database password or application secret value appears in it
