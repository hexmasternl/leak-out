# app-deployment Specification

## Purpose
TBD - created by archiving change app-deployment-workflow-in-github. Update Purpose after archive.

## Requirements

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

### Requirement: Required secrets are checked up front
Before building, the workflow SHALL check that the secrets `PLAY_UPLOAD_KEYSTORE_BASE64`, `PLAY_UPLOAD_KEYSTORE_PASSWORD`, `PLAY_UPLOAD_KEY_ALIAS`, `PLAY_UPLOAD_KEY_PASSWORD` and `PLAY_SERVICE_ACCOUNT_JSON` are all non-empty. If any is empty, the run SHALL fail with a message naming every missing secret and referring to `docs/deployment.md`, and SHALL NOT print any secret value.

#### Scenario: Missing secret
- **WHEN** the workflow runs and `PLAY_UPLOAD_KEY_PASSWORD` is not set
- **THEN** the run fails before the build, and the log names `PLAY_UPLOAD_KEY_PASSWORD` and `docs/deployment.md`

#### Scenario: All secrets present
- **WHEN** all five secrets are set
- **THEN** the check passes without printing their values

### Requirement: Secrets are only available to deployments from main
The five secrets SHALL be stored in a GitHub environment named `play-store` whose deployment branches are limited to `main`, and the deployment job SHALL use that environment. The workflow SHALL NOT write any secret to the log, to the job summary or to an uploaded artifact, and SHALL delete the decoded keystore file at the end of the job whether the job succeeded or failed.

#### Scenario: Keystore is removed after the job
- **WHEN** the job finishes, successfully or not
- **THEN** the decoded keystore file no longer exists on the runner

#### Scenario: Workflow from another branch cannot read the secrets
- **WHEN** the workflow is run from a branch other than `main`
- **THEN** the `play-store` environment refuses the deployment and no secret is exposed

### Requirement: Release bundle is signed with the upload key
`app/build.gradle.kts` SHALL sign the `release` build with the upload key when the environment variable `LEAKOUT_UPLOAD_STORE_FILE` is set, using `LEAKOUT_UPLOAD_STORE_PASSWORD`, `LEAKOUT_UPLOAD_KEY_ALIAS` and `LEAKOUT_UPLOAD_KEY_PASSWORD`. If `LEAKOUT_UPLOAD_STORE_FILE` is set and any of the other three is missing, the build SHALL fail with a message naming the missing variable. When `LEAKOUT_UPLOAD_STORE_FILE` is not set, the release build SHALL stay unsigned, and debug builds SHALL be unaffected in every case. The workflow SHALL verify that the built bundle is signed before uploading it, and fail otherwise.

#### Scenario: Signed bundle in CI
- **WHEN** `:app:bundleRelease` runs with all four `LEAKOUT_UPLOAD_*` variables set to a valid keystore
- **THEN** `app-release.aab` is produced and `jarsigner -verify` reports it as signed by the upload key

#### Scenario: Local release build without a keystore
- **WHEN** `:app:bundleRelease` runs without `LEAKOUT_UPLOAD_STORE_FILE`
- **THEN** the build succeeds and produces an unsigned bundle

#### Scenario: Incomplete signing configuration
- **WHEN** `LEAKOUT_UPLOAD_STORE_FILE` is set but `LEAKOUT_UPLOAD_KEY_ALIAS` is not
- **THEN** the build fails and names `LEAKOUT_UPLOAD_KEY_ALIAS`

#### Scenario: Unsigned bundle is never uploaded
- **WHEN** the bundle built in the workflow is not signed
- **THEN** the run fails before the upload step

### Requirement: Every upload has a higher version code
`app/build.gradle.kts` SHALL take `versionCode` and `versionName` from the Gradle properties `versionCode` and `versionName` when they are given, and SHALL otherwise keep the values written in the build file. A `versionCode` that is not a positive integer, or a `versionName` that is not `<major>.<minor>.<patch>`, SHALL fail the build. The workflow SHALL compute the version with GitVersion (configured in `GitVersion.yml`, GitHub Flow, patch increment on `main`, bumps through `+semver: minor|major`), pass its `MajorMinorPatch` as `versionName` and `major × 1000000 + minor × 1000 + patch` as `versionCode`, show both in the job summary, and tag the released commit `v<major>.<minor>.<patch>` after a successful upload. The run SHALL fail before building when the version is below the newest release tag, its tag already exists on another commit, or minor or patch is above 999 or major above 2099.

#### Scenario: Next release after a tagged release
- **WHEN** `main` is one merge past the release tag `v0.2.0`, with no `+semver:` bump
- **THEN** the bundle has `versionName` `0.2.1` and `versionCode` 2001, and the commit is tagged `v0.2.1` after the upload

#### Scenario: Minor bump
- **WHEN** a commit since `v0.2.1` says `+semver: minor`
- **THEN** the bundle has `versionName` `0.3.0` and `versionCode` 3000

#### Scenario: Version below the newest release
- **WHEN** GitVersion gives a version below the newest `v*` tag
- **THEN** the run fails before building, and nothing is uploaded or tagged

#### Scenario: Local build keeps the file's version
- **WHEN** the app is built without `-PversionCode` and `-PversionName`
- **THEN** the version code and name are the ones written in `app/build.gradle.kts`

#### Scenario: Invalid version code property
- **WHEN** the app is built with `-PversionCode=abc` or `-PversionCode=0`
- **THEN** the build fails with a message about the version code

### Requirement: Bundle is uploaded to the private testing track
The workflow SHALL upload the signed `app-release.aab` for package `nl.hexmaster.leakout` to the Play Console with the service account in `PLAY_SERVICE_ACCOUNT_JSON`. The track SHALL be the repository variable `PLAY_TRACK`, defaulting to `internal` (the internal testing track), and the release status SHALL be the repository variable `PLAY_RELEASE_STATUS`, defaulting to `completed`. If the Play Console rejects the upload, the run SHALL fail. The bundle SHALL also be kept as a workflow artifact for a limited time.

#### Scenario: Default upload
- **WHEN** a deployment succeeds with `PLAY_TRACK` and `PLAY_RELEASE_STATUS` unset
- **THEN** a new completed release with the new bundle is on the internal testing track

#### Scenario: Named private track
- **WHEN** `PLAY_TRACK` is set to the name of a closed testing track
- **THEN** the bundle is released on that track instead

#### Scenario: Draft app
- **WHEN** the app has never been published and `PLAY_RELEASE_STATUS` is `draft`
- **THEN** the upload succeeds as a draft release on the track

#### Scenario: Rejected upload fails the run
- **WHEN** the Play Console rejects the upload, for example because the version code was already used
- **THEN** the run fails

### Requirement: Setup guide documents every secret, key and setting
The repository SHALL contain `docs/deployment.md`, a step-by-step guide that covers, in order: creating and backing up the upload keystore with `keytool`; creating the app in the Play Console with Play App Signing and doing the first upload by hand; adding testers to the internal testing track; creating the Google Cloud service account with the Google Play Android Developer API enabled and its JSON key; granting the service account access to the app in the Play Console; creating the `play-store` environment limited to `main`; adding each of the five secrets (including how to base64-encode the keystore on Windows, macOS and Linux) and each of the three variables with its default; running and checking the workflow; troubleshooting common Play Console API errors; and rotating or replacing keys. Every secret and variable name the workflow reads SHALL appear in the guide, and every name in the guide SHALL be one the workflow reads. `CLAUDE.md` SHALL link to the guide.

#### Scenario: Guide and workflow agree on names
- **WHEN** the secret and variable names used in `deploy-play-internal.yml` are compared with those in `docs/deployment.md`
- **THEN** both sets are identical

#### Scenario: Guide is reachable from the project instructions
- **WHEN** a contributor reads `CLAUDE.md`
- **THEN** it links to `docs/deployment.md`
