# Tasks

## 1. Shared build, test and coverage gate

- [ ] 1.1 Add `.github/workflows/build-test.yml` (reusable): setup .NET from `global.json`, restore, build, `dotnet test --solution` with Cobertura coverage and `CodeCoverage.settings.xml`; verify with `actionlint` and a run that all three test projects execute
- [ ] 1.2 Provide PostgreSQL to the tests that need it (Testcontainers on the runner's Docker or a `postgres:17` service container, matching how the tests connect); verify the repository and migration tests run instead of being skipped
- [ ] 1.3 Add the merge with `dotnet-coverage` and the gate step that writes the coverage percentage to the job summary and fails below 80% (exactly 80% passes); verify by running the step against a sample report at 0.7999 (fails) and 0.8000 (passes)
- [ ] 1.4 Upload the merged coverage report and test results as artifacts; verify they are attached to a run

## 2. Pull request workflow

- [ ] 2.1 Add `.github/workflows/pull-request.yml` (`pull_request` into `development`, per-PR concurrency) calling `build-test.yml`; verify on a test pull request that the check `build-test / build-test` succeeds, and fails when a test is broken on purpose
- [ ] 2.2 Add the `bicep` job (`az bicep build --file infrastructure/main.bicep`, success when `infrastructure/` does not exist yet); verify it fails on a broken template
- [ ] 2.3 Document the required checks and how to enable branch protection on `development` in `README.md`; verify the documented check names match the jobs

## 3. Deploy workflow

- [ ] 3.1 Add `.github/workflows/deploy.yml` (push to `main`, `workflow_dispatch`, `concurrency: deploy-production`) with `build-test` and a `publish` job that logs in to the registry with `ACR_LOGIN_SERVER`, `ACR_LOGIN_USERNAME` and `ACR_LOGIN_PASSWORD` and runs `dotnet publish /t:PublishContainer` tagged with the commit SHA; verify with `workflow_dispatch` that the image appears in the registry
- [ ] 3.2 Add the `deploy` job (environment `production`, OIDC `azure/login` with `AZURE_PROD_CLIENT_ID`, `AZURE_PROD_TENANT_ID` and `AZURE_PROD_SUBSCRIPTION_ID`, `az deployment sub create` of `infrastructure/main.bicep` with the image and secure parameters, `/alive` smoke test); verify a manual run creates or updates the resource group and the container app serves the new tag
- [ ] 3.3 Pass `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` and `GOOGLE_SIGNING_KEY` to the deployment as `googleClientId`, `googleClientSecret` and `authSigningKey` (as step `env:` variables that `main.bicepparam` reads), with a guard that fails the job when one is empty; verify the container app shows the three secrets and `POST /api/players/sessions` with a real server auth code returns a token
- [ ] 3.4 Document the secrets (the existing `ACR_LOGIN_*`, `AZURE_PROD_*` and `GOOGLE_*` secrets, and the PostgreSQL admin password still to be created), the `production` environment and the federated credential subject in `README.md`; verify every secret the workflow reads is listed

## 4. Integration checks

- [ ] 4.1 Verify a failing test on `main` stops the run before `publish` and `deploy`
- [ ] 4.2 Verify no secret value appears in the logs of a full deploy run
