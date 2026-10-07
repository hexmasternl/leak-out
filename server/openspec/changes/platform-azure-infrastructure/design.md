# Design

## Context

See proposal.md - Why. Today there is no deployment at all: no Dockerfile, no `.github/workflows`, no infrastructure folder. The API (`src/HexMaster.LeakOut.Api`) calls `AddServiceDefaults()` and `MapDefaultEndpoints()`; `/health` and `/alive` are mapped in every environment, and the OTLP exporter is enabled only when `OTEL_EXPORTER_OTLP_ENDPOINT` is set. The Azure Monitor exporter in `ServiceDefaults/Extensions.cs` is commented out.

Changes #1 (`players-google-play-auth`) and #2 (`scores-submit-score`) define the runtime configuration the hosted API needs: connection strings `playersdb` and `scoresdb`, `GooglePlayGames:ClientId`, `GooglePlayGames:ClientSecret` and `Authentication:SigningKey`. They also state that production migrations are a deployment step.

## Goals / Non-Goals

**Goals:**
- One subscription-scope `az deployment sub create` creates the resource group and provisions everything in it, except the container registry.
- Lowest reasonable monthly cost for a low-traffic hobby game server.
- Telemetry in Application Insights without changing any handler code.

**Non-Goals:**
- Running EF Core migrations against the Azure databases (separate follow-up; see Risks).
- Custom domain, certificates, VNet integration, private endpoints, Key Vault, multiple environments (staging/production).
- Creating the container registry or the Azure service principal.
- Generating the infrastructure from the Aspire AppHost.

## Decisions

### D1. Hand-written Bicep under `infrastructure/`
```
infrastructure/
  main.bicep                 # targetScope = 'subscription': creates the resource group, deploys resources.bicep into it
  main.bicepparam            # non-secret defaults; image, registry and secrets read from environment variables
  resources.bicep            # resourceGroup scope, wires the modules
  modules/
    monitoring.bicep         # Log Analytics + Application Insights
    postgres.bicep           # Flexible Server, databases, firewall rule
    containerapps-env.bicep  # managed environment
    api.bicep                # container app
```
`main.bicep` declares `targetScope = 'subscription'`, creates `Microsoft.Resources/resourceGroups` named `resourceGroupName` in `location`, and deploys `resources.bicep` as a module with `scope: resourceGroup`. Splitting into a subscription-scope entry point and a resource-group-scope module is required because Bicep resources other than the resource group itself cannot be declared directly at subscription scope.

Parameters: `resourceGroupName` (default `rg-leakout`), `location` (required; default `westeurope` in `main.bicepparam`, because a subscription deployment has no resource group location to inherit), `namePrefix` (default `leakout`), `imageName` (full image reference including tag), `registryServer`, `registryUsername`, secure `registryPassword`, secure `postgresAdminPassword`, secure `googleClientId`, secure `googleClientSecret`, secure `authSigningKey`, `maxReplicas` (default `3`). `main.bicepparam` assigns every parameter without a default with `readEnvironmentVariable()`, using the names of the GitHub secrets: `API_IMAGE` (`imageName`), `ACR_LOGIN_SERVER`, `ACR_LOGIN_USERNAME`, `ACR_LOGIN_PASSWORD`, `POSTGRES_ADMIN_PASSWORD`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` and `GOOGLE_SIGNING_KEY` (#8 sets them in the step `env:`). A `.bicepparam` file must assign every required parameter and the Azure CLI checks that before it applies inline `--parameters` overrides, so the values cannot be left out of the file and passed on the command line; reading them from the environment also keeps them off the command line. A missing variable fails the deployment while the parameters are compiled (`BCP427`). `authSigningKey` has `@minLength(32)`, matching the API's startup check, so a too-short key fails the deployment instead of the container. Resource names are `${namePrefix}-<kind>-${uniqueString(resourceGroup().id)}` so they are stable across redeploys and unique where Azure requires it.

*Alternative considered*: Aspire's own Azure publishing (`aspire publish`/`azd`), which generates Bicep from the AppHost. Rejected for now: the issue asks for Bicep that is owned in the repository, the existing registry with username/password does not fit the azd default of a managed registry, and generated templates are harder to review and tune for cost. ADR 0003 still holds: Aspire remains the local orchestrator and the hosted configuration uses the same resource names and connection string keys, so no deviation ADR is needed for local orchestration.

### D2. Monitoring
Log Analytics workspace, SKU `PerGB2018`, retention 30 days, `dailyQuotaGb` 1 to cap cost. Application Insights `kind: web`, workspace-based on that workspace. The Container Apps environment uses `appLogsConfiguration.destination = 'log-analytics'` with the workspace id and shared key.

ServiceDefaults gets the `Azure.Monitor.OpenTelemetry.AspNetCore` package and the existing commented block is enabled: `if (!string.IsNullOrEmpty(builder.Configuration["APPLICATIONINSIGHTS_CONNECTION_STRING"])) builder.Services.AddOpenTelemetry().UseAzureMonitor();`. This keeps exporter selection configuration-driven (ADR 0008) and leaves local Aspire runs unchanged.

### D3. PostgreSQL Flexible Server
`Microsoft.DBforPostgreSQL/flexibleServers`, version `17`, SKU `Standard_B1ms` (tier `Burstable`), storage 32 GiB with auto-grow off, backup retention 7 days, geo-redundant backup and high availability disabled. Password authentication with admin login `leakoutadmin` and the secure `postgresAdminPassword`. Databases `playersdb` and `scoresdb` (`UTF8`, `en_US.utf8`). TLS is required by default (`require_secure_transport` on).

Network: public access with the firewall rule `AllowAllAzureServices` (`0.0.0.0`-`0.0.0.0`). Consumption Container Apps have no fixed outbound IP, and VNet integration needs a workload-profile environment and private DNS, which adds cost and complexity disproportionate to the load. Access is protected by the password and TLS.

### D4. Container Apps environment and app
Consumption-only managed environment. Container app `hexmaster-leakout-api` (matching the Aspire resource name), 0.25 vCPU / 0.5 GiB, external ingress on target port 8080, `allowInsecure: false`, single active revision mode.

- Registry: `registries: [{ server, username, passwordSecretRef: 'registry-password' }]`.
- Secrets: `registry-password`, `playersdb-connection`, `scoresdb-connection`, `google-client-id`, `google-client-secret`, `auth-signing-key`, `appinsights-connection`.
- Environment variables: `ConnectionStrings__playersdb`, `ConnectionStrings__scoresdb` (Npgsql format: `Host=<fqdn>;Database=<db>;Username=leakoutadmin;Password=...;Ssl Mode=Require`), `GooglePlayGames__ClientId`, `GooglePlayGames__ClientSecret`, `Authentication__SigningKey`, `APPLICATIONINSIGHTS_CONNECTION_STRING`, `OTEL_SERVICE_NAME=hexmaster-leakout-api`, `ASPNETCORE_ENVIRONMENT=Production`.
- Google Play Games sign-in: the secure parameters flow through `resources.bicep` into `api.bicep` and are stored only as container app secrets, never as plain environment values:

  | GitHub secret | Bicep parameter | Container app secret | Environment variable | API configuration key |
  |---|---|---|---|---|
  | `GOOGLE_CLIENT_ID` | `googleClientId` | `google-client-id` | `GooglePlayGames__ClientId` (`secretRef`) | `GooglePlayGames:ClientId` |
  | `GOOGLE_CLIENT_SECRET` | `googleClientSecret` | `google-client-secret` | `GooglePlayGames__ClientSecret` (`secretRef`) | `GooglePlayGames:ClientSecret` |
  | `GOOGLE_SIGNING_KEY` | `authSigningKey` | `auth-signing-key` | `Authentication__SigningKey` (`secretRef`) | `Authentication:SigningKey` |

  These are the same keys the AppHost sets locally from `Parameters:google-client-id`, `Parameters:google-client-secret` and `Parameters:authentication-signing-key`, so the API needs no production-specific code. The client id must be the **web** OAuth client of the Google Cloud project linked to the game (the game's `serverClientId`). Without any of the three the API fails at startup, so a revision deployed without them never becomes ready.
- Scale: `minReplicas: 0`, `maxReplicas: maxReplicas`, rule `http-concurrency` with `concurrentRequests: '20'`.
- Probes: liveness `GET /alive`, readiness `GET /health`, startup `GET /alive` with a generous failure threshold for cold starts.

*Alternative considered*: a managed identity with `AcrPull` instead of registry username/password. Rejected because the issue specifies the existing registry's username and password, and the registry may live outside this subscription.

### D5. Container image without a Dockerfile
`HexMaster.LeakOut.Api.csproj` gets `<ContainerRepository>hexmaster-leakout-api</ContainerRepository>` and uses the SDK's default chiseled ASP.NET base image. Publishing runs `dotnet publish src/HexMaster.LeakOut.Api -c Release /t:PublishContainer -p:ContainerRegistry=<registry> -p:ContainerImageTags=<tag>`. The SDK's container support is the .NET default and avoids maintaining a Dockerfile.

### D6. Deployment entry point, pipeline in #8
The templates are deployed with `az deployment sub create --location <location> --name leakout-<id> -f infrastructure/main.bicep -p infrastructure/main.bicepparam`, with the image reference, the registry credentials and the secrets in the environment variables listed in D1. The deploying identity needs `Contributor` on the subscription (or a custom role that can create resource groups and the resources in them), because it creates the resource group. `infrastructure/README.md` documents a manual deployment with these commands.

The GitHub Actions workflows that build, test, publish the image and run this deployment, including the Bicep check on pull requests, are designed in `platform-github-actions-workflows` (#8).

## Risks / Trade-offs

- [Databases are empty after deployment: migrations do not run in Production] → Track a follow-up issue for a migration step (EF Core migration bundle executed by the pipeline). Until then the maintainer runs `dotnet ef database update` once against Azure.
- [Scale to zero adds a cold start of a few seconds to the first request] → Acceptable for a game leaderboard; the startup probe tolerates it. Set `minReplicas` to 1 if it becomes a problem.
- [Firewall allows all Azure services] → TLS and a strong generated password; move to VNet integration if the threat model changes.
- [Burstable B1ms has limited CPU credits] → Fine for expected load; the SKU is a parameter.
- [Secrets visible in deployment history] → Secure parameters (`@secure()`) are not logged by ARM; secrets are passed from GitHub secrets only.
- [This change can be implemented before #1 and #2] → Templates and env vars can exist before the code consumes them; the app simply ignores unused configuration.

- [The pipeline identity has subscription-wide rights] → Scope the role assignment to the smallest that works (a custom role limited to resource groups, PostgreSQL, Container Apps and monitoring), and restrict the federated credential to the `main` branch and the `production` environment.
- [Subscription deployments are named per location] → Use a deployment name per run and always deploy to the same `location`; ARM rejects reusing a subscription deployment name in a different location.

## Migration Plan

1. Maintainer creates the federated credential with a subscription-level role assignment and the GitHub secrets. The resource group is created by the deployment.
2. Merge to `development`, then `main`; the workflow from #8 builds, pushes and deploys (or deploy manually as documented in `infrastructure/README.md`).
3. Apply database migrations once (see Risks).
4. Rollback: redeploy the previous image tag with `workflow_dispatch`, or delete the resource group to remove everything.

## Open Questions

- Azure region and resource group name: defaults are `westeurope` and `rg-leakout` in `main.bicepparam`; the maintainer can change them before the first deployment.
