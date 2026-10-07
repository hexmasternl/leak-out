# Design

## Context

See proposal.md - Why. There is no `.github/workflows` folder yet. The solution is `src/leak-out-server.slnx`; `global.json` pins SDK `10.0.400` (roll forward on feature) and the test runner to Microsoft.Testing.Platform (xUnit v3). Coverage uses `Microsoft.Testing.Extensions.CodeCoverage` with `CodeCoverage.settings.xml` at the repository root, and the git-workflow skill (A3) defines the gate: run `dotnet test --solution src/leak-out-server.slnx --coverage --coverage-output-format cobertura`, merge the per-project reports with `dotnet-coverage merge`, and require `line-rate >= 0.80`. Test projects: `HexMaster.LeakOut.Api.Tests`, `HexMaster.LeakOut.Players.Tests`, `HexMaster.LeakOut.Scores.Tests`. Some tests (repository, migrations) need PostgreSQL.

`platform-azure-infrastructure` (#6) defines the Bicep entry point `infrastructure/main.bicep` (subscription scope), `main.bicepparam`, the container image settings and the secrets the deployment needs. Its planned workflow tasks move to this change.

## Goals / Non-Goals

**Goals:**
- One shared definition of build, test and the coverage gate, used by both workflows.
- A check name that is stable enough to mark as required in branch protection.

**Non-Goals:**
- Configuring branch protection, environments, secrets or the Azure federated credential (maintainer actions, documented).
- Running EF Core migrations against Azure (follow-up from #6).
- A workflow for pull requests into `main` (the `development` → `main` PR is reviewed by the maintainer; can be added later by pointing `pull-request.yml` at `main` too).
- Release notes, versioning or image tags other than the commit SHA.

## Decisions

### D1. Three workflow files
- `build-test.yml` - reusable (`on: workflow_call`), one job `build-test` on `ubuntu-latest`:
  1. `actions/checkout`, `actions/setup-dotnet` with `global-json-file: global.json`, NuGet cache keyed on `**/*.csproj`.
  2. `dotnet restore src/leak-out-server.slnx`, `dotnet build --no-restore -c Release`.
  3. `dotnet test --solution src/leak-out-server.slnx --no-build -c Release --coverage --coverage-output-format cobertura --coverage-output coverage.cobertura.xml --coverage-settings CodeCoverage.settings.xml`.
  4. `dotnet tool install -g dotnet-coverage`, `dotnet-coverage merge --output coverage/merged.cobertura.xml --output-format cobertura "src/**/TestResults/**/coverage.cobertura.xml"`.
  5. A shell step reads `line-rate` from the merged report, writes the percentage and threshold to `$GITHUB_STEP_SUMMARY`, and exits non-zero below `0.80`. Integer arithmetic on the rate times 10000 avoids floating point differences, so exactly 80% passes.
  6. Upload the merged report and test results (`.trx`) as artifacts.
- `pull-request.yml` - `on: pull_request: branches: [development]`, `concurrency` per PR with cancel-in-progress. Jobs `build-test` (`uses: ./.github/workflows/build-test.yml`) and `bicep` (`az bicep build --file infrastructure/main.bicep`, skipped with success when the folder does not exist yet so the check can be required before #6 lands). The required check names are `build-test / build-test` and `bicep`.
- `deploy.yml` - `on: push: branches: [main]` and `workflow_dispatch`, `concurrency: deploy-production` without cancel-in-progress, so deployments never overlap. Jobs `build-test` (reusable), `publish` (needs build-test), `deploy` (needs publish, `environment: production`).

*Alternative considered*: one workflow file with conditional jobs. Rejected: separate triggers and permissions are easier to read, and a reusable workflow keeps the gate identical.

*Alternative considered*: a third-party coverage action (ReportGenerator, Codecov). Rejected: `dotnet-coverage` is already the tool of the git workflow, and the gate needs only the `line-rate` attribute; no external service sees the code.

### D2. PostgreSQL for tests
Tests that need PostgreSQL use the same mechanism they use locally. When they use Testcontainers, the Ubuntu runner's Docker is enough. When they expect a connection string, `build-test.yml` adds a `services: postgres` container (`postgres:17`) and passes the connection string as an environment variable. The implementer picks the one matching the existing tests; the workflow must not skip them.

### D3. Publish job
`docker/login-action` with `registry: ${{ secrets.ACR_ENDPOINT }}`, `ACR_USERNAME`, `ACR_PASSWORD`, then `dotnet publish src/HexMaster.LeakOut.Api -c Release /t:PublishContainer -p:ContainerRegistry=${{ secrets.ACR_ENDPOINT }} -p:ContainerImageTags=${{ github.sha }}`. Outputs the full image reference for the deploy job.

### D4. Deploy job
`permissions: id-token: write, contents: read`. `azure/login` with `client-id`, `tenant-id`, `subscription-id` (OIDC). Then:
```
az deployment sub create --name leakout-${{ github.run_id }} --location <location from main.bicepparam>
  --template-file infrastructure/main.bicep --parameters infrastructure/main.bicepparam
  --parameters imageName=<image> registryServer=... registryUsername=... registryPassword=...
               postgresAdminPassword=... googleClientId=... googleClientSecret=... authSigningKey=...
```
Secret parameters are passed only as `@secure()` Bicep parameters and are masked by GitHub. After deployment the job prints the container app FQDN and calls `/alive` with retries as a smoke test.

The `production` environment lets the maintainer add required reviewers or wait timers later without changing the workflow, and the federated credential is bound to `environment:production`.

### D5. Permissions
Every workflow sets top-level `permissions: contents: read`; only the deploy job adds `id-token: write`. Actions are pinned to major versions from GitHub and Azure (`actions/*`, `azure/login`, `docker/login-action`).

## Risks / Trade-offs

- [The required check name changes when a job is renamed] → Keep job names fixed and document them in the README.
- [Coverage measured on Linux may differ slightly from Windows runs] → Same tool and settings; the gate applies to the CI value.
- [`main` can be pushed directly, bypassing PR checks] → `deploy.yml` re-runs build, tests and the gate before publishing; branch protection on `main` is recommended.
- [Deploy depends on #6] → Implement #6 first, or land `deploy.yml` with #6; `pull-request.yml` can land before it.
- [Secrets misconfigured] → The deploy job fails before changing Azure; the README lists every secret.

## Migration Plan

1. Merge the workflows into `development`; the PR check runs on its own pull request.
2. Maintainer marks `build-test / build-test` and `bicep` as required checks on `development`.
3. Maintainer creates the `production` environment, secrets and federated credential (see #6).
4. The next merge to `main` publishes and deploys. Rollback: re-run `deploy.yml` for an earlier commit with `workflow_dispatch`.
