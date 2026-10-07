# Tasks

## 1. Telemetry and container image in the code

- [x] 1.1 Add `Azure.Monitor.OpenTelemetry.AspNetCore` to `HexMaster.LeakOut.Aspire.ServiceDefaults` and enable `UseAzureMonitor()` when `APPLICATIONINSIGHTS_CONNECTION_STRING` is set; verify a ServiceDefaults test shows the exporter is registered only when the setting is present
- [x] 1.2 Add the container properties (`ContainerRepository` `hexmaster-leakout-api`) to `HexMaster.LeakOut.Api.csproj` and verify `dotnet publish src/HexMaster.LeakOut.Api -c Release /t:PublishContainer` produces a local image that serves `/alive` on port 8080

## 2. Bicep templates

- [x] 2.1 Add `infrastructure/modules/monitoring.bicep` (Log Analytics `PerGB2018`, 30 days, 1 GB daily cap; workspace-based Application Insights) with outputs for the workspace id/key and the connection string; verify `az bicep build` succeeds
- [x] 2.2 Add `infrastructure/modules/postgres.bicep` (Flexible Server 17, `Standard_B1ms` Burstable, 32 GiB, no HA, no geo backup, `AllowAllAzureServices` rule, databases `playersdb` and `scoresdb`) with the FQDN as output; verify `az bicep build` succeeds
- [x] 2.3 Add `infrastructure/modules/containerapps-env.bicep` (Consumption environment logging to the workspace); verify `az bicep build` succeeds
- [x] 2.4 Add `infrastructure/modules/api.bicep` (container app `hexmaster-leakout-api`: registry credentials, secrets, environment variables including the Google sign-in secrets `google-client-id`, `google-client-secret` and `auth-signing-key` as `secretRef`s for `GooglePlayGames__ClientId`, `GooglePlayGames__ClientSecret` and `Authentication__SigningKey`, external HTTPS ingress on 8080, 0..`maxReplicas` with the `http-concurrency` rule, `/alive` and `/health` probes); verify `az bicep build` succeeds
- [x] 2.5 Add `infrastructure/resources.bicep` (resource group scope) wiring the modules with `@secure()` parameters for every secret, including `googleClientId`, `googleClientSecret` and `authSigningKey` (`@minLength(32)`) passed from `main.bicep` to `api.bicep`; verify `az bicep build` succeeds
- [x] 2.6 Add `infrastructure/main.bicep` (`targetScope = 'subscription'`) that creates the resource group `resourceGroupName` in `location` and deploys `resources.bicep` into it, plus `infrastructure/main.bicepparam`; verify `az bicep build` and `az deployment sub what-if` against a test subscription show the new resource group and only the expected resources in it
- [x] 2.7 Document the infrastructure, the parameters, a manual `az deployment sub create` and the one-time setup (subscription-level role for the deploying identity, first migration) in `infrastructure/README.md`; verify the documented commands match the templates

## 3. Integration checks

The GitHub Actions workflows that publish and deploy are delivered by `platform-github-actions-workflows` (#8); these checks use a manual deployment.


- [ ] 3.1 Deploy at subscription scope with a resource group name that does not exist yet and verify the resource group is created, every resource from the spec exists inside it, `/health` returns 200 over HTTPS, `POST /api/players/sessions` with a valid Google Play Games server auth code returns an access token, and a request appears in Application Insights
- [ ] 3.2 Redeploy with the same parameters and verify `what-if` reports no changes
- [ ] 3.3 Leave the app idle and verify it scales to 0 replicas, then verify a request starts a replica and is answered
- [x] 3.4 Search the repository for the secret values used in the deployment and verify none are committed
