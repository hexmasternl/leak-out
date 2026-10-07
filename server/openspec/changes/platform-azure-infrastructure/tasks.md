# Tasks

## 1. Telemetry and container image in the code

- [ ] 1.1 Add `Azure.Monitor.OpenTelemetry.AspNetCore` to `HexMaster.LeakOut.Aspire.ServiceDefaults` and enable `UseAzureMonitor()` when `APPLICATIONINSIGHTS_CONNECTION_STRING` is set; verify a ServiceDefaults test shows the exporter is registered only when the setting is present
- [ ] 1.2 Add the container properties (`ContainerRepository` `hexmaster-leakout-api`) to `HexMaster.LeakOut.Api.csproj` and verify `dotnet publish src/HexMaster.LeakOut.Api -c Release /t:PublishContainer` produces a local image that serves `/alive` on port 8080

## 2. Bicep templates

- [ ] 2.1 Add `infrastructure/modules/monitoring.bicep` (Log Analytics `PerGB2018`, 30 days, 1 GB daily cap; workspace-based Application Insights) with outputs for the workspace id/key and the connection string; verify `az bicep build` succeeds
- [ ] 2.2 Add `infrastructure/modules/postgres.bicep` (Flexible Server 17, `Standard_B1ms` Burstable, 32 GiB, no HA, no geo backup, `AllowAllAzureServices` rule, databases `playersdb` and `scoresdb`) with the FQDN as output; verify `az bicep build` succeeds
- [ ] 2.3 Add `infrastructure/modules/containerapps-env.bicep` (Consumption environment logging to the workspace); verify `az bicep build` succeeds
- [ ] 2.4 Add `infrastructure/modules/api.bicep` (container app `hexmaster-leakout-api`: registry credentials, secrets, environment variables, external HTTPS ingress on 8080, 0..`maxReplicas` with the `http-concurrency` rule, `/alive` and `/health` probes); verify `az bicep build` succeeds
- [ ] 2.5 Add `infrastructure/main.bicep` and `infrastructure/main.bicepparam` wiring the modules with `@secure()` parameters for every secret; verify `az bicep build` and `az deployment group what-if` against a test resource group show only the expected resources
- [ ] 2.6 Document the infrastructure, the required GitHub secrets and the one-time setup (resource group, OIDC federated credential, first migration) in `infrastructure/README.md` and verify the documented commands match the templates

## 3. Pipeline

- [ ] 3.1 Add `.github/workflows/deploy.yml` (push to `main` and `workflow_dispatch`: build and push the image tagged with the commit SHA, then `az deployment group create` with OIDC login); verify with `workflow_dispatch` that the image appears in the registry and the deployment succeeds
- [ ] 3.2 Add a pull request check that runs `az bicep build infrastructure/main.bicep`; verify it fails on a deliberately broken template and passes on the real one

## 4. Integration checks

- [ ] 4.1 Deploy into an empty resource group and verify every resource from the spec exists, `/health` returns 200 over HTTPS, and a request appears in Application Insights
- [ ] 4.2 Redeploy with the same parameters and verify `what-if` reports no changes
- [ ] 4.3 Leave the app idle and verify it scales to 0 replicas, then verify a request starts a replica and is answered
- [ ] 4.4 Search the repository for the secret values used in the deployment and verify none are committed
