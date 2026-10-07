# leak-out-server

The score server of the Leak Out game: a .NET 10 minimal API orchestrated by .NET Aspire, with PostgreSQL.

## Running locally

Requirements: the .NET 10 SDK and Docker (Aspire runs PostgreSQL in a container).

The server needs three secrets. They are Aspire parameters of the AppHost and are stored in its user secrets, never in `appsettings.json`:

| Parameter | Passed to the API as | What it is |
| --- | --- | --- |
| `google-client-id` | `GooglePlayGames:ClientId` | The client id of the **web** OAuth client in the Google Cloud project linked to the game. |
| `google-client-secret` | `GooglePlayGames:ClientSecret` | That web OAuth client's secret. |
| `authentication-signing-key` | `Authentication:SigningKey` | A random key of at least 32 bytes that signs the server's access tokens. |

From `src/`:

```bash
dotnet user-secrets set "Parameters:google-client-id" "<web-client-id>.apps.googleusercontent.com" --project HexMaster.LeakOut.Aspire/HexMaster.LeakOut.Aspire.AppHost
dotnet user-secrets set "Parameters:google-client-secret" "<web-client-secret>" --project HexMaster.LeakOut.Aspire/HexMaster.LeakOut.Aspire.AppHost
dotnet user-secrets set "Parameters:authentication-signing-key" "$(openssl rand -base64 48)" --project HexMaster.LeakOut.Aspire/HexMaster.LeakOut.Aspire.AppHost

dotnet run --project HexMaster.LeakOut.Aspire/HexMaster.LeakOut.Aspire.AppHost
```

In PowerShell, generate the signing key with `[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))`. The API refuses to start, with an options-validation error naming the setting, when one of them is missing or the signing key is too short.

**The game's `serverClientId` must be this same web client id.** The game requests its server auth code with `GamesSignInClient.requestServerSideAccess(serverClientId, false)`; Google only lets the server exchange codes that were requested for its own client id.

See [docs/auth-setup.md](docs/auth-setup.md) for the full Google Cloud / Play Console setup.

## Authentication

1. The game signs in to Play Games Services and requests a one-time server auth code.
2. It posts the code to `POST /api/players/sessions`. The server exchanges it with Google, registers the player on their first sign-in (or refreshes their display name), and returns an access token valid for one hour, its expiry time, the internal player id and the display name.
3. Every other endpoint requires that token as `Authorization: Bearer <token>`. When it expires, the game requests a new server auth code and signs in again.

`src/HexMaster.LeakOut.Api/HexMaster.LeakOut.Api.http` has a sign-in sample.

## Continuous integration

Every pull request into `development` runs [`.github/workflows/pull-request.yml`](.github/workflows/pull-request.yml) with two checks:

| Check | What it does |
| --- | --- |
| `build-test / build-test` | Restores and builds the solution, runs every test project with code coverage (the Scores tests start PostgreSQL with Testcontainers), merges the reports and fails when total line coverage is below 80%. The coverage appears in the run summary; the merged report and the `.trx` test results are attached as artifacts. |
| `bicep` | Compiles `infrastructure/main.bicep` and its modules. |

Both are defined in [`.github/workflows/build-test.yml`](.github/workflows/build-test.yml) (shared with the deploy workflow) and `pull-request.yml`. Do not rename those jobs: the check names are what branch protection refers to.

To make them mandatory (once, by a repository admin): **Settings → Branches → Add branch ruleset** (or classic rule) for `development`, enable **Require status checks to pass**, and add `build-test / build-test` and `bicep`. A check only appears in the list after it has run once, so open a pull request first. Protecting `main` the same way is recommended, because `main` should only change through the `development` → `main` pull request.

## Deployment

Every push to `main` (and a manual run of **Actions → deploy → Run workflow**) runs [`.github/workflows/deploy.yml`](.github/workflows/deploy.yml):

1. **build-test**: the same build, tests and 80% gate as the pull request check. A failure stops the run; nothing is published or deployed.
2. **publish**: calculates the version with [GitVersion](GitVersion.yml), publishes the API container image with `dotnet publish /t:PublishContainer` (no Dockerfile) and pushes it as `nvv54gsk4pteu.azurecr.io/leak-out/game-server-api:<version>`, then fails if the image would run as root.
3. **deploy** (environment `production`): logs in to Azure with OpenID Connect and runs the subscription-scope deployment of [`infrastructure/main.bicep`](infrastructure/README.md) with the new image, then calls `https://<api-host>/alive` until it answers.

### Versions and image tags

The image is tagged with the semantic version GitVersion calculates (mainline versioning, `GitVersion.yml`): every merge to `main` increments the patch version (`0.0.3`, `0.0.4`, ...), so each deployment has its own tag and no tag is overwritten. Put `+semver: minor` or `+semver: major` in a commit message (for example in the `development` → `main` merge) to bump the minor or major version; a Git tag `vX.Y.Z` on `main` sets a new base. The version is also the assembly version, and the informational version includes the commit SHA.

The registry and repository are set in `src/HexMaster.LeakOut.Api/HexMaster.LeakOut.Api.csproj` (`ContainerRegistry`, `ContainerRepository`); the workflow reads them from there. `ContainerUser` pins the non-root `app` user (UID 1654) of the .NET base image, which listens on the unprivileged port 8080.

To roll back, run the deploy workflow manually on an earlier commit of `main`; it rebuilds and redeploys that commit's version.

### Secrets and settings

Repository secrets (**Settings → Secrets and variables → Actions**):

| Secret | Used by | Purpose |
| --- | --- | --- |
| `ACR_LOGIN_USERNAME` | publish, deploy | User of the container registry `nvv54gsk4pteu.azurecr.io`; also passed to the container app to pull the image |
| `ACR_LOGIN_PASSWORD` | publish, deploy | Password of that registry user |
| `AZURE_PROD_CLIENT_ID` | deploy | Client id of the app registration the workflow logs in with (OIDC, no client secret) |
| `AZURE_PROD_TENANT_ID` | deploy | Entra tenant id |
| `AZURE_PROD_SUBSCRIPTION_ID` | deploy | Subscription the deployment runs in |
| `POSTGRES_ADMIN_PASSWORD` | deploy | Admin password of the PostgreSQL server (login `leakoutadmin`). **Still to be created.** |
| `GOOGLE_CLIENT_ID` | deploy | Client id of the **web** OAuth client (the game's `serverClientId`), becomes `GooglePlayGames:ClientId` |
| `GOOGLE_CLIENT_SECRET` | deploy | Secret of that web OAuth client, becomes `GooglePlayGames:ClientSecret` |
| `GOOGLE_SIGNING_KEY` | deploy | The API's own JWT signing key (at least 32 characters), becomes `Authentication:SigningKey`. Not a Google credential; rotating it signs every player out. |

The deploy job fails before logging in to Azure when one of the registry, PostgreSQL or Google secrets is empty, and names it. The `ACR_LOGIN_SERVER` secret is no longer used: the registry comes from the API project.

Once, in GitHub and Azure:

1. Create the environment **production** (**Settings → Environments**). Add required reviewers there if deployments should wait for approval, and limit its deployment branches to `main`.
2. Give the app registration behind `AZURE_PROD_CLIENT_ID` a federated credential for GitHub Actions with entity type **Environment**, i.e. subject `repo:hexmasternl/leak-out-server:environment:production`, audience `api://AzureADTokenExchange`:

   ```bash
   az ad app federated-credential create --id <client-id> --parameters '{
     "name": "github-production",
     "issuer": "https://token.actions.githubusercontent.com",
     "subject": "repo:hexmasternl/leak-out-server:environment:production",
     "audiences": ["api://AzureADTokenExchange"]
   }'
   ```

3. Give it `Contributor` on the subscription and register the resource providers (see [infrastructure/README.md](infrastructure/README.md), One-time setup).
