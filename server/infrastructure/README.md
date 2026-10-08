# Infrastructure

Bicep templates that host the Leak Out server in Azure. One subscription-scope deployment creates the resource group and everything inside it. The container registry is the only thing it does not create; the API image is pulled from the existing registry.

| File | Scope | Contents |
| --- | --- | --- |
| `main.bicep` | subscription | Creates the resource group `resourceGroupName` in `location` and deploys `resources.bicep` into it |
| `main.bicepparam` | | Non-secret defaults (resource group, region, max replicas) |
| `resources.bicep` | resource group | Wires the modules and builds the database connection strings |
| `modules/monitoring.bicep` | resource group | Log Analytics workspace (`PerGB2018`, 30 days retention, 1 GB daily cap) and workspace-based Application Insights |
| `modules/postgres.bicep` | resource group | PostgreSQL Flexible Server 17, `Standard_B1ms` Burstable, 32 GiB, no HA, no geo backup, TLS required, firewall rule `AllowAllAzureServices`, databases `playersdb` and `scoresdb` |
| `modules/containerapps-env.bicep` | resource group | Consumption-only Container Apps environment that sends logs to the workspace |
| `modules/api.bicep` | resource group | Container app `<resource group>-api` (container and OpenTelemetry service `hexmaster-leakout-api`, like the Aspire resource): external HTTPS ingress on port 8080, 0 to `maxReplicas` replicas on the `http-concurrency` rule (20 concurrent requests), `/alive` startup and liveness probes, `/health` readiness probe |

Every resource is named `<resource group name>-<kind>`, so with the default resource group `rg-leakout-prod-swe` they are `rg-leakout-prod-swe-log`, `-appi`, `-psql`, `-cae` and `-api`. The names stay the same across redeploys. The PostgreSQL server name is also its host name and must be unique across Azure; the resource group name makes it so. Keep the resource group name at most 28 characters, because Container Apps and their environments allow at most 32.

## Parameters

`main.bicepparam` sets the non-secret values and reads everything else from environment variables named after the GitHub secrets, so no value is stored in the repository or passed on the command line:

| Parameter | Secure | Value in `main.bicepparam` |
| --- | --- | --- |
| `resourceGroupName` | | `rg-leakout-prod-swe` |
| `location` | | `northeurope` |
| `maxReplicas` | | `3` |
| `imageName` | | `API_IMAGE`: full image reference with tag, e.g. `nvv54gsk4pteu.azurecr.io/leak-out/game-server-api:<version>` |
| `serviceVersion` | | `SERVICE_VERSION`: version of the release, set by `server-deploy.yml` from the GitVersion `SemVer` (the image tag); reported as the OpenTelemetry `service.version` |
| `registryServer` | | `ACR_LOGIN_SERVER` |
| `registryUsername` | | `ACR_LOGIN_USERNAME` |
| `registryPassword` | yes | `ACR_LOGIN_PASSWORD` |
| `postgresAdminPassword` | yes | `POSTGRES_ADMIN_PASSWORD`: PostgreSQL admin password (login `leakoutadmin`) |
| `googleClientId` | yes | `GOOGLE_CLIENT_ID`: the **web** OAuth client id of the Google Cloud project linked to the game (the game's `serverClientId`) |
| `googleClientSecret` | yes | `GOOGLE_CLIENT_SECRET` |
| `authSigningKey` | yes | `GOOGLE_SIGNING_KEY`: the API's own JWT signing key, at least 32 characters |

A missing environment variable (`BCP427`) or a signing key shorter than 32 characters fails the deployment while the parameters are compiled, before anything is sent to Azure. To check the templates without the variables, build `main.bicep` (`az bicep build --file infrastructure/main.bicep`).

The container app gets its configuration as container app secrets, exposed as environment variables with the same keys the Aspire AppHost uses locally:

| Container app secret | Environment variable |
| --- | --- |
| `playersdb-connection` | `ConnectionStrings__playersdb` |
| `scoresdb-connection` | `ConnectionStrings__scoresdb` |
| `google-client-id` | `GooglePlayGames__ClientId` |
| `google-client-secret` | `GooglePlayGames__ClientSecret` |
| `auth-signing-key` | `Authentication__SigningKey` |
| `appinsights-connection` | `APPLICATIONINSIGHTS_CONNECTION_STRING` |
| `registry-password` | (registry credentials) |

Besides these, the container app sets plain environment variables: `ASPNETCORE_ENVIRONMENT=Production`, `OTEL_SERVICE_NAME=hexmaster-leakout-api` and `OTEL_RESOURCE_ATTRIBUTES=service.version=<serviceVersion>`. Application Insights shows the service name as the cloud role and the version as `application_Version`.

## One-time setup

1. **Deploying identity.** The deployment creates the resource group, so the identity that runs it needs `Contributor` on the **subscription** (or a custom role that can create resource groups, PostgreSQL Flexible Servers, Container Apps and monitoring resources). For GitHub Actions this is the app registration behind `AZURE_PROD_CLIENT_ID`:

   ```bash
   az role assignment create --assignee <client-id> --role Contributor --scope /subscriptions/<subscription-id>
   ```

2. **Resource providers.** Make sure the subscription has them registered (once):

   ```bash
   for ns in Microsoft.App Microsoft.DBforPostgreSQL Microsoft.OperationalInsights Microsoft.Insights; do
     az provider register --namespace "$ns"
   done
   ```

3. **Container image.** Push the API image to the existing registry (the GitHub Actions workflow of #8 does this on every push to `main`):

   ```bash
   az acr login --name nvv54gsk4pteu   # or: docker login nvv54gsk4pteu.azurecr.io
   dotnet publish src/HexMaster.LeakOut.Api -c Release -t:PublishContainer \
     -p:ContainerImageTags=<version>   # registry, repository and non-root user come from the project
   ```

## Manual deployment

From the repository root, logged in with `az login` to the target subscription. Export the environment variables `main.bicepparam` reads; `read -s` keeps the secrets out of the shell history:

```bash
az account set --subscription <subscription-id>

export API_IMAGE="nvv54gsk4pteu.azurecr.io/leak-out/game-server-api:<version>"
export SERVICE_VERSION="<version>"
export ACR_LOGIN_SERVER="nvv54gsk4pteu.azurecr.io"
export ACR_LOGIN_USERNAME="<registry-user>"
read -rsp "Registry password: " ACR_LOGIN_PASSWORD; echo; export ACR_LOGIN_PASSWORD
read -rsp "PostgreSQL admin password: " POSTGRES_ADMIN_PASSWORD; echo; export POSTGRES_ADMIN_PASSWORD
read -rsp "Google client id: " GOOGLE_CLIENT_ID; echo; export GOOGLE_CLIENT_ID
read -rsp "Google client secret: " GOOGLE_CLIENT_SECRET; echo; export GOOGLE_CLIENT_SECRET
read -rsp "Signing key (>= 32 chars): " GOOGLE_SIGNING_KEY; echo; export GOOGLE_SIGNING_KEY

# Preview the changes
az deployment sub what-if --location northeurope \
  --template-file infrastructure/main.bicep \
  --parameters infrastructure/main.bicepparam

# Deploy
az deployment sub create --location northeurope --name "leakout-$(date +%Y%m%d%H%M%S)" \
  --template-file infrastructure/main.bicep \
  --parameters infrastructure/main.bicepparam \
  --query properties.outputs
```

`--location` is where the subscription deployment's metadata is stored; always use the same value as `location` in `main.bicepparam`, because ARM rejects reusing a deployment name in another location. The outputs contain the resource group name and the API's host name (`apiFullyQualifiedDomainName`); `https://<host>/health` should return `Healthy`.

Redeploying with the same parameters changes nothing. A new image tag creates a new revision of the container app.

## Database migrations

The API applies pending EF Core migrations of `playersdb` and `scoresdb` when it starts, in every environment, before it answers `/alive`. A new deployment therefore needs no manual migration step. If a migration fails, the new revision never passes its startup probe and the previous revision keeps serving; the container app's console logs show the error. Migrations must stay backwards-compatible with the previous release, because the old revision briefly runs against the new schema during a rollout.

## Removing everything

```bash
az group delete --name rg-leakout-prod-swe
```
