# Proposal

## Why

Tracked in [#6](https://github.com/hexmasternl/leak-out-server/issues/6). Builds on the database names introduced by #1 (`playersdb`, change `players-google-play-auth`) and #2 (`scoresdb`, change `scores-submit-score`).

The server only runs locally through the Aspire AppHost today. The game needs a hosted endpoint, so the API must run in Azure, and the infrastructure must be reproducible and reviewable as code rather than clicked together in the portal. The expected load is low, so the hosting must stay cheap and scale to zero when idle.

## What Changes

- New `infrastructure/` folder with Bicep templates deployed at **subscription scope**: the deployment creates the resource group itself and then provisions, inside that new resource group:
  - a Log Analytics workspace and a workspace-based Application Insights component;
  - an Azure Database for PostgreSQL Flexible Server on the cheapest burstable tier, with the `playersdb` and `scoresdb` databases;
  - an Azure Container Apps environment (Consumption) that sends logs to the workspace;
  - the `hexmaster-leakout-api` container app, pulling its image from the existing container registry, with an HTTP scale rule that scales from 0 up to a small maximum.
- The API project is configured for the .NET SDK container support (`dotnet publish /t:PublishContainer`, no Dockerfile); the container app pulls the image from the existing registry at `ACR_LOGIN_SERVER` with `ACR_LOGIN_USERNAME` / `ACR_LOGIN_PASSWORD`.
- The container app is configured for Google Play Games sign-in: secure Bicep parameters `googleClientId`, `googleClientSecret` and `authSigningKey` (supplied from the GitHub secrets `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` and `GOOGLE_SIGNING_KEY`) become container app secrets exposed as `GooglePlayGames__ClientId`, `GooglePlayGames__ClientSecret` and `Authentication__SigningKey`.
- Publishing the image and running the deployment from GitHub Actions is out of this change: it is tracked in #8 (change `platform-github-actions-workflows`).
- ServiceDefaults exports OpenTelemetry to Application Insights when `APPLICATIONINSIGHTS_CONNECTION_STRING` is set (the Azure Monitor exporter that is commented out in `Extensions.cs` today).

## Capabilities

### New Capabilities
- `azure-hosting`: How the server is provisioned and run in Azure: infrastructure as code, container image publishing, telemetry, database, configuration and scaling.

### Modified Capabilities

## Impact

- **New files**: `infrastructure/main.bicep`, `infrastructure/main.bicepparam`, `infrastructure/resources.bicep`, `infrastructure/modules/*.bicep`, `infrastructure/README.md`.
- **Code**: `src/HexMaster.LeakOut.Aspire/HexMaster.LeakOut.Aspire.ServiceDefaults/Extensions.cs` (Azure Monitor exporter), `HexMaster.LeakOut.Api.csproj` (container image properties).
- **Dependencies**: `Azure.Monitor.OpenTelemetry.AspNetCore` in ServiceDefaults.
- **Azure**: a subscription-scope deployment that creates a new resource group and all resources in it; the existing container registry is used, not created. The deploying identity needs rights at subscription level.
- **Secrets**: secure Bicep parameters for the registry password, the PostgreSQL admin password and the application secrets from #1 (`googleClientId`, `googleClientSecret`, `authSigningKey`); #8 supplies them from GitHub secrets (`GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_SIGNING_KEY` for the Google sign-in parameters).
- **API behaviour**: unchanged; no endpoint or contract changes.
