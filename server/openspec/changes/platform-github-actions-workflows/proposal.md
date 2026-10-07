# Proposal

## Why

Tracked in [#8](https://github.com/hexmasternl/leak-out-server/issues/8). Depends on #6 (Azure infrastructure, change `platform-azure-infrastructure`) for the Bicep templates the deployment runs.

The repository has no automation. The 80% coverage gate of the git workflow is enforced only by hand before a pull request is opened, and nothing builds, publishes or deploys the server when `main` changes. Two GitHub Actions workflows close both gaps: a required check on every pull request into `development`, and a build-and-deploy run on every merge to `main`.

## What Changes

- New workflow `.github/workflows/pull-request.yml`: on pull requests into `development`, restore, build, run all tests with code coverage, and fail when total line coverage is below 80%. It also validates the Bicep templates. It is meant to be a required status check on `development`.
- New workflow `.github/workflows/deploy.yml`: on push to `main` (and manual dispatch), build and test, publish the API container image with `dotnet publish /t:PublishContainer`, push it to the existing registry (`ACR_ENDPOINT`, `ACR_USERNAME`, `ACR_PASSWORD`), and run a subscription-scope deployment of `infrastructure/main.bicep` that creates the resource group and its resources (#6).
- A reusable workflow `.github/workflows/build-test.yml` holds the shared build, test and coverage steps, so both workflows apply the same gate.
- The workflow and the deployment pipeline previously planned in `platform-azure-infrastructure` move to this change.

## Capabilities

### New Capabilities
- `ci-cd`: Automated verification of pull requests (build, tests, coverage gate) and automated publishing and deployment of `main` to Azure.

### Modified Capabilities

## Impact

- **New files**: `.github/workflows/build-test.yml`, `.github/workflows/pull-request.yml`, `.github/workflows/deploy.yml`.
- **Repository settings** (maintainer): branch protection on `development` requiring the pull request check; GitHub environment `production`; repository secrets for the registry, Azure OIDC login and application secrets.
- **Azure**: an app registration with a federated credential for `main` / environment `production` and a subscription-level role (see #6).
- **Code**: none, apart from what #6 adds (`ContainerRepository` in the API project).
