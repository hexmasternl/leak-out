# Design

## Context

See proposal.md - Why. Today there is no deployment at all: no Dockerfile, no `.github/workflows`, no infrastructure folder. The API (`src/HexMaster.LeakOut.Api`) calls `AddServiceDefaults()` and `MapDefaultEndpoints()`; `/health` and `/alive` are mapped in every environment, and the OTLP exporter is enabled only when `OTEL_EXPORTER_OTLP_ENDPOINT` is set. The Azure Monitor exporter in `ServiceDefaults/Extensions.cs` is commented out.

Changes #1 (`players-google-play-auth`) and #2 (`scores-submit-score`) define the runtime configuration the hosted API needs: connection strings `playersdb` and `scoresdb`, `GooglePlayGames:ClientId`, `GooglePlayGames:ClientSecret` and `Authentication:SigningKey`. They also state that production migrations are a deployment step.

## Goals / Non-Goals

**Goals:**
- One `az deployment group create` provisions everything except the container registry and the resource group.
- Lowest reasonable monthly cost for a low-traffic hobby game server.
- Telemetry in Application Insights without changing any handler code.

**Non-Goals:**
- Running EF Core migrations against the Azure databases (separate follow-up; see Risks).
- Custom domain, certificates, VNet integration, private endpoints, Key Vault, multiple environments (staging/production).
- Creating the resource group, the container registry or the Azure service principal.
- Generating the infrastructure from the Aspire AppHost.

## Decisions

### D1. Hand-written Bicep under `infrastructure/`
```
infrastructure/
  main.bicep                 # resourceGroup scope, wires the modules
  main.bicepparam            # non-secret defaults; secrets come from the pipeline
  modules/
    monitoring.bicep         # Log Analytics + Application Insights
    postgres.bicep           # Flexible Server, databases, firewall rule
    containerapps-env.bicep  # managed environment
    api.bicep                # container app
```
Parameters: `location` (default `resourceGroup().location`), `namePrefix` (default `leakout`), `imageName` (full image reference including tag), `registryServer`, `registryUsername`, secure `registryPassword`, secure `postgresAdminPassword`, secure `googleClientId`, `googleClientSecret`, `authSigningKey`, `maxReplicas` (default `3`). Resource names are `${namePrefix}-<kind>-${uniqueString(resourceGroup().id)}` so they are stable across redeploys and unique where Azure requires it.

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
- Scale: `minReplicas: 0`, `maxReplicas: maxReplicas`, rule `http-concurrency` with `concurrentRequests: '20'`.
- Probes: liveness `GET /alive`, readiness `GET /health`, startup `GET /alive` with a generous failure threshold for cold starts.

*Alternative considered*: a managed identity with `AcrPull` instead of registry username/password. Rejected because the issue specifies the existing registry's username and password, and the registry may live outside this subscription.

### D5. Container image without a Dockerfile
`HexMaster.LeakOut.Api.csproj` gets `<ContainerRepository>hexmaster-leakout-api</ContainerRepository>` and uses the SDK's default chiseled ASP.NET base image. The pipeline runs `dotnet publish src/HexMaster.LeakOut.Api -c Release /t:PublishContainer -p:ContainerRegistry=$ACR_ENDPOINT -p:ContainerImageTags=<sha>` after `docker login`. The SDK's container support is the .NET default and avoids maintaining a Dockerfile.

### D6. GitHub Actions workflow `.github/workflows/deploy.yml`
Triggers: push to `main` and `workflow_dispatch`. Jobs:
1. `build` - setup .NET from `global.json`, `docker login` with the registry secrets, publish the container tagged with `github.sha`.
2. `deploy` (needs `build`, environment `production`) - `azure/login` with OpenID Connect (`AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID`), then `az deployment group create -g $AZURE_RESOURCE_GROUP -f infrastructure/main.bicep -p infrastructure/main.bicepparam` with the image and secrets passed as parameters.

Repository secrets: `ACR_ENDPOINT`, `ACR_USERNAME`, `ACR_PASSWORD`, `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID`, `AZURE_RESOURCE_GROUP`, `POSTGRES_ADMIN_PASSWORD`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `AUTH_SIGNING_KEY`. A pull request workflow step runs `az bicep build` to catch template errors before merge.

*Alternative considered*: a service principal secret for Azure login. OIDC federated credentials avoid a long-lived secret.

## Risks / Trade-offs

- [Databases are empty after deployment: migrations do not run in Production] → Track a follow-up issue for a migration step (EF Core migration bundle executed by the pipeline). Until then the maintainer runs `dotnet ef database update` once against Azure.
- [Scale to zero adds a cold start of a few seconds to the first request] → Acceptable for a game leaderboard; the startup probe tolerates it. Set `minReplicas` to 1 if it becomes a problem.
- [Firewall allows all Azure services] → TLS and a strong generated password; move to VNet integration if the threat model changes.
- [Burstable B1ms has limited CPU credits] → Fine for expected load; the SKU is a parameter.
- [Secrets visible in deployment history] → Secure parameters (`@secure()`) are not logged by ARM; secrets are passed from GitHub secrets only.
- [This change can be implemented before #1 and #2] → Templates and env vars can exist before the code consumes them; the app simply ignores unused configuration.

## Migration Plan

1. Maintainer creates the resource group, the federated credential and the GitHub secrets.
2. Merge to `development`, then `main`; the workflow builds, pushes and deploys.
3. Apply database migrations once (see Risks).
4. Rollback: redeploy the previous image tag with `workflow_dispatch`, or delete the resource group to remove everything.

## Open Questions

- Azure region: defaults to the resource group's location; the maintainer picks it when creating the group.
