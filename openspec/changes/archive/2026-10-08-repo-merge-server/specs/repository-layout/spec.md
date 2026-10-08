## Purpose

Defines how the repository holds both the Android app and the online game server: where each lives, which workflows build and release each of them, and how their versions, secrets and checks are kept apart.

## ADDED Requirements

### Requirement: App and server live in their own folders
The repository SHALL hold the Android app's Gradle root (settings, wrapper, `gradle/libs.versions.toml`, and the `core` and `app` modules) in `app/src/`, the game server's sources (the .NET solution and its projects) in `server/src/`, and the server's Bicep templates in `server/infrastructure/`. No source SHALL live in `src/` at the repository root.

#### Scenario: App builds from app/src/
- **WHEN** a contributor runs `./gradlew :core:test :app:testDebugUnitTest` from `app/src/`
- **THEN** the build finds every module and the tests run

#### Scenario: Server builds from server/src/
- **WHEN** a contributor runs `dotnet build` on the server solution in `server/src/`
- **THEN** the solution builds with the SDK pinned by `server/global.json`

### Requirement: History is kept across the move
The app's files SHALL keep their history when moved to `app/`, and the server's files SHALL arrive in `server/` with the full commit history of the `leak-out-server` repository.

#### Scenario: Following an app file
- **WHEN** a contributor runs `git log --follow` on a file under `app/`
- **THEN** commits from before the move are listed

#### Scenario: Following a server file
- **WHEN** a contributor runs `git log` on a file under `server/`
- **THEN** commits made in the `leak-out-server` repository are listed

### Requirement: Server workflows only run for server changes
The server's build, pull request and deploy workflows SHALL live in `.github/workflows/` with a `server-` prefix and SHALL build and test from `server/src/` and deploy the infrastructure from `server/infrastructure/`. The server deployment SHALL run for pushes to `main` that change `server/` or a server workflow, and by hand; it SHALL NOT run for pushes that only change the app.

#### Scenario: App-only changes do not deploy the server
- **WHEN** a commit that only changes files under `app/` is pushed to `main`
- **THEN** the server deployment does not start

#### Scenario: Server change deploys the server
- **WHEN** a commit that changes a file under `server/` is pushed to `main`
- **THEN** the server is built, tested and deployed to Azure, and the smoke test passes

### Requirement: Pull request checks never block on an untouched side
The server's pull request check SHALL report a result on every pull request into `development`. It SHALL build and test the server when the pull request changes `server/` or a server workflow, and SHALL succeed without building otherwise, so it can be a required status check without blocking pull requests that only change the app.

#### Scenario: App-only pull request
- **WHEN** a pull request into `development` only changes files under `app/`
- **THEN** the server check reports success without building the server

#### Scenario: Server pull request with a failing test
- **WHEN** a pull request into `development` changes `server/` and a server test fails
- **THEN** the server check fails

### Requirement: App and server versions are independent
The app SHALL keep versioning from `v<major>.<minor>.<patch>` tags. The server SHALL version from its own configuration and SHALL only take `server-v<major>.<minor>.<patch>` tags into account, so an app release tag never changes the server's version and a server tag never changes the app's.

#### Scenario: App release does not move the server version
- **WHEN** the app is released and `main` is tagged `v0.9.0`
- **THEN** the next server deployment's version is calculated without that tag

#### Scenario: Server tag does not move the app version
- **WHEN** `main` carries a `server-v2.0.0` tag
- **THEN** the app's next version name is calculated without that tag

### Requirement: Server secrets and environment live in this repository
The repository SHALL have a `production` environment holding every secret the server workflows read (`ACR_LOGIN_PASSWORD`, `ACR_LOGIN_SERVER`, `ACR_LOGIN_USERNAME`, `AZURE_PROD_CLIENT_ID`, `AZURE_PROD_SUBSCRIPTION_ID`, `AZURE_PROD_TENANT_ID`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_SIGNING_KEY`, `POSTGRES_ADMIN_PASSWORD`), and Azure SHALL trust this repository's `production` environment for the server's sign-in. No secret value SHALL be committed.

#### Scenario: Deployment signs in to Azure
- **WHEN** the server deployment runs from this repository
- **THEN** the Azure sign-in succeeds with the federated credential and no stored password

#### Scenario: Missing secret fails early
- **WHEN** one of the server's secrets is not set
- **THEN** the deployment fails before anything changes in Azure, naming the missing secret

### Requirement: Repository layout is documented
The repository SHALL contain `docs/repository-layout.md`, describing the `app/` folder (sources in `app/src/`) and the `server/` folder (sources in `server/src/`, Bicep in `server/infrastructure/`), which workflow builds and releases each, how their versions and tags differ, and where each side's secrets are documented. `CLAUDE.md` SHALL describe the same layout and link to it, and no instruction, doc, skill or agent SHALL refer to `src/` as the app's location.

#### Scenario: No stale src/ references
- **WHEN** the repository's instructions, docs, skills, agents and scripts are searched for `src/` as the app's Gradle root
- **THEN** nothing is found
