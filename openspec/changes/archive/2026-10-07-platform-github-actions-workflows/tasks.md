# Tasks

## 1. Shared build, test and coverage gate

- [x] 1.1 Add `.github/workflows/build-test.yml` (reusable): setup .NET from `global.json`, restore, build, `dotnet test` per test project with Cobertura coverage and `CodeCoverage.settings.xml`; verify with `actionlint` and a run that all four test projects execute
- [x] 1.2 Provide PostgreSQL to the tests that need it (Testcontainers on the runner's Docker or a `postgres:17` service container, matching how the tests connect); verify the repository and migration tests run instead of being skipped
- [x] 1.3 Add the merge with `dotnet-coverage` and the gate step that writes the coverage percentage to the job summary and fails below 80% (exactly 80% passes); verify by running the step against a sample report at 0.7999 (fails) and 0.8000 (passes)
- [x] 1.4 Upload the merged coverage report and test results as artifacts; verify they are attached to a run

## 2. Pull request workflow

- [x] 2.1 Add `.github/workflows/pull-request.yml` (`pull_request` into `development`, per-PR concurrency) calling `build-test.yml`; verify on a test pull request that the check `build-test / build-test` succeeds, and fails when a test is broken on purpose
- [x] 2.2 Add the `bicep` job (`az bicep build --file infrastructure/main.bicep`, success when `infrastructure/` does not exist yet); verify it fails on a broken template
- [x] 2.3 Document the required checks and how to enable branch protection on `development` in `README.md`; verify the documented check names match the jobs

## 3. Deploy workflow

- [x] 3.0 Add `GitVersion.yml` (`workflow: TrunkBased/preview1`) and set `ContainerRegistry` (`nvv54gsk4pteu.azurecr.io`), `ContainerRepository` (`leak-out/game-server-api`) and `ContainerUser` (`app`) in `HexMaster.LeakOut.Api.csproj`; verify GitVersion yields a release version on `main`, and a local `dotnet publish /t:PublishContainer` to an archive produces an image whose user is `app`
- [x] 3.1 Add `.github/workflows/deploy.yml` (push to `main`, `workflow_dispatch`, `concurrency: deploy-production`) with `build-test` and a `publish` job that runs GitVersion, logs in to `nvv54gsk4pteu.azurecr.io` with `ACR_LOGIN_USERNAME` and `ACR_LOGIN_PASSWORD`, runs `dotnet publish /t:PublishContainer` tagged with the GitVersion `SemVer`, and fails when the pushed image's user is root; verify with `workflow_dispatch` that `leak-out/game-server-api:<version>` appears in the registry
- [x] 3.2 Add the `deploy` job (environment `production`, OIDC `azure/login` with `AZURE_PROD_CLIENT_ID`, `AZURE_PROD_TENANT_ID` and `AZURE_PROD_SUBSCRIPTION_ID`, `az deployment sub create` of `infrastructure/main.bicep` with the image and secure parameters, `/alive` smoke test); verify a manual run creates or updates the resource group and the container app serves the new tag
- [x] 3.3 Pass `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` and `GOOGLE_SIGNING_KEY` to the deployment as `googleClientId`, `googleClientSecret` and `authSigningKey` (as step `env:` variables that `main.bicepparam` reads), with a guard that fails the job when one is empty; verify the container app shows the three secrets and `POST /api/players/sessions` with a real server auth code returns a token
- [x] 3.4 Document the secrets (the existing `ACR_LOGIN_USERNAME`/`ACR_LOGIN_PASSWORD`, `AZURE_PROD_*` and `GOOGLE_*` secrets, and the PostgreSQL admin password still to be created), the `production` environment and the federated credential subject in `README.md`; verify every secret the workflow reads is listed

## 4. Integration checks

- [x] 4.1 Verify a failing test on `main` stops the run before `publish` and `deploy`
- [x] 4.2 Verify no secret value appears in the logs of a full deploy run
