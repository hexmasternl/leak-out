# server-ci-cd Specification

## Purpose

Automates verification and delivery of the score server, which lives in `server/` of the Leak Out repository next to the app: every pull request into `development` that changes the server is built, tested and held to the 80% coverage gate, and every merge to `main` that changes the server is published as a container image and deployed to Azure. The workflows are `.github/workflows/server-build-test.yml` (reusable), `server-pull-request.yml` and `server-deploy.yml`; they build from `server/src/` and deploy `server/infrastructure/` (see `docs/repository-layout.md`).

## Requirements

### Requirement: Pull requests into development are verified
Every pull request that targets `development` SHALL trigger a check. When the pull request changes a file under `server/`, a server workflow (`.github/workflows/server-*.yml`) or the coverage gate script (`.github/scripts/server-coverage-gate.sh`), the check SHALL restore and build the solution in `server/src/` and run all unit tests, and SHALL fail when the build fails or any test fails. Otherwise the check SHALL succeed without building.

#### Scenario: Passing pull request
- **WHEN** a pull request into `development` builds and all tests pass with at least 80% line coverage
- **THEN** the check succeeds

#### Scenario: Failing test
- **WHEN** a pull request into `development` contains a failing test
- **THEN** the check fails and reports the failing test

#### Scenario: App-only pull request
- **WHEN** a pull request into `development` only changes files under `app/`
- **THEN** the check succeeds without building or testing the server

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
The pull request check SHALL report its result under the stable names `server / build-test` and `server / bicep` so they can be configured as required status checks on `development`, so that a pull request cannot be merged until they succeed. They SHALL report a result on every pull request into `development`, including those that do not change the server.

#### Scenario: Merge blocked on a failing check
- **WHEN** the check has failed on a pull request into `development` and branch protection requires it
- **THEN** GitHub does not allow the pull request to be merged

### Requirement: Infrastructure templates are validated on pull requests
When the pull request changes the server, the pull request check SHALL compile the Bicep templates in `server/infrastructure/` and fail when they do not compile.

#### Scenario: Broken template
- **WHEN** a pull request contains a Bicep syntax error
- **THEN** the check fails

### Requirement: Merges to main are built, tested and published
Every push to `main` that changes a file under `server/`, a server workflow or the coverage gate script SHALL build the solution, run all tests with the same coverage gate, and, only when that succeeds, publish the API container image as `nvv54gsk4pteu.azurecr.io/leak-out/game-server-api:<version>` to the existing container registry, where `<version>` is the semantic version GitVersion calculates for the commit with `server/GitVersion.yml`, authenticating with the `ACR_LOGIN_USERNAME` and `ACR_LOGIN_PASSWORD` secrets. Pushes to `main` that only change the app SHALL NOT start the server workflow.

#### Scenario: Successful publish
- **WHEN** a merge to `main` builds and passes the tests and the coverage gate
- **THEN** `nvv54gsk4pteu.azurecr.io/leak-out/game-server-api` is present in the registry with the commit's semantic version as tag

#### Scenario: Every merge gets a new version
- **WHEN** two successive merges to `main` are published
- **THEN** the second image has a higher semantic version than the first, and no existing tag is overwritten

#### Scenario: App-only push
- **WHEN** a commit that only changes files under `app/` is pushed to `main`
- **THEN** the server workflow does not start and no image is published

#### Scenario: Tests fail on main
- **WHEN** a test fails on a push to `main`
- **THEN** no image is published and no deployment starts

### Requirement: The API container runs without elevated permissions
The API container image SHALL run as a non-root user and SHALL listen on an unprivileged port. The non-root user SHALL be declared in the API project's container configuration, and the publish job SHALL fail when the published image is configured to run as root.

#### Scenario: Container user
- **WHEN** the configuration of a published API image is inspected
- **THEN** its user is `app`, not `root` or empty, and the API listens on port 8080

### Requirement: Merges to main are deployed to Azure at subscription scope
After the image is published, the workflow SHALL run a subscription-scope deployment of the infrastructure templates in `server/infrastructure/` that creates the resource group if needed and deploys the published image tag. The deploy job SHALL run in the repository's `production` environment, which only accepts `main`. Azure login SHALL use OpenID Connect with the `AZURE_PROD_CLIENT_ID`, `AZURE_PROD_TENANT_ID` and `AZURE_PROD_SUBSCRIPTION_ID` secrets, trusting the subject `repo:hexmasternl/leak-out:environment:production`, without a stored Azure client secret.

#### Scenario: Deployment of a new image
- **WHEN** the image for a commit on `main` has been published
- **THEN** the subscription-scope deployment runs and the container app serves that image tag

#### Scenario: Manual redeploy
- **WHEN** the maintainer starts the deploy workflow manually
- **THEN** the same build, publish and deploy steps run for the selected commit

### Requirement: The deployment configures Google Play Games sign-in
The deploy workflow SHALL pass the `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` and `GOOGLE_SIGNING_KEY` secrets to the subscription-scope deployment as the secure parameters `googleClientId`, `googleClientSecret` and `authSigningKey`, and SHALL fail before deploying when one of them is empty.

#### Scenario: Sign-in works after a deployment
- **WHEN** the deploy workflow completes and a player signs in with a valid Google Play Games server auth code on the hosted API
- **THEN** the API exchanges the code with Google using the configured web client and returns an access token signed with the configured key

#### Scenario: Missing Google secret
- **WHEN** the deploy workflow runs and `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` or `GOOGLE_SIGNING_KEY` is not set
- **THEN** the deploy job fails before the Azure deployment starts and names the missing secret

### Requirement: Server versions are independent of the app
The server's version SHALL only take `server-v<major>.<minor>.<patch>` tags into account, never the app's `v<major>.<minor>.<patch>` release tags, and every merge to `main` that is deployed SHALL get a higher version than the previous one.

#### Scenario: App release tag
- **WHEN** `main` carries the app's tag `v9.9.9` and no newer `server-v*` tag
- **THEN** the server's next version is calculated from the newest `server-v*` tag, not from `v9.9.9`

### Requirement: Secrets stay out of the repository and logs
The workflows SHALL take all credentials from the secrets of the `production` environment and SHALL NOT print secret values in logs.

#### Scenario: Secret masking
- **WHEN** a workflow run log is inspected
- **THEN** no registry password, database password or application secret value appears in it
