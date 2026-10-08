# Leak Out game server

The score server of the Leak Out game: a .NET 10 minimal API orchestrated by .NET Aspire, with PostgreSQL. It lives in `server/` of the Leak Out repository, next to the Android app in `app/`; until October 2026 it had its own repository, `hexmasternl/leak-out-server` (archived), whose history was imported here. Paths in this file are relative to `server/` unless they start with `.github/`; how the two sides are built and released apart: [docs/repository-layout.md](../docs/repository-layout.md).

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
3. Every other endpoint, except the public ones below, requires that token as `Authorization: Bearer <token>`. When it expires, the game requests a new server auth code and signs in again.

`src/HexMaster.LeakOut.Api/HexMaster.LeakOut.Api.http` has a sign-in sample.

## Public endpoints

A few read-only endpoints need no token, for the public website ([website/README.md](../website/README.md)). They opt out of the fallback policy with `AllowAnonymous()` and live in `src/HexMaster.LeakOut.Api/Endpoints/PublicLevelScoreEndpoints.cs`.

| Endpoint | Returns |
| --- | --- |
| `GET /api/public/levels/{level}/scores/top` | The 25 best scores of a level in ranking order: `{ "level": 3, "entries": [{ "position": 1, "displayName": "Ann", "score": 600, "achievedAt": "2026-10-01T14:30:00+00:00" }] }`. No player ids. 400 with a validation error when `level` is below 1. |

- **`achievedAt`.** The moment the server received that best score (`level_scores.achieved_at`, from the server's clock), as ISO 8601 with offset in UTC. A later equal or lower score does not change it. Only the public top scores carry it: the app's leaderboards (`/api/levels/{level}/scores/top`, `/around-me`) have no dates or times.

- **Caching.** Each level's result is kept in the API's memory cache for **30 minutes** (`GetPublicLevelTopScoresQueryHandler.CacheDuration`), so a new score can take up to 30 minutes to appear, and each replica reads the database at most once per level in that time. A failed read is not cached. Responses also carry `Cache-Control: public, max-age=60`.
- **CORS.** Only the origins in `Website:AllowedOrigins` may call them from a browser, with GET and no credentials. `appsettings.json` allows `https://leakout.hexmaster.nl`; another origin, for example the Static Web App's own `*.azurestaticapps.net` address, can be added there or as the app setting `Website__AllowedOrigins__1`. The authenticated endpoints send no CORS headers.

## Observability

The API emits OpenTelemetry traces, metrics and logs (`HexMaster.LeakOut.Aspire.ServiceDefaults`). ASP.NET Core requests, outgoing HTTP calls (Google) and EF Core / Npgsql database calls are instrumented automatically. Every operation adds its own span, metrics and logs on top.

**Locally**, open the Aspire dashboard (its URL is printed when the AppHost starts) and select the `hexmaster-leakout-api` resource:

- **Traces**: a leaderboard request shows the HTTP span, `players.resolve_user`, the handler span (for example `scores.get_level_top_scores`), `players.get_display_names` and the database spans nested under them.
- **Metrics**: the `HexMaster.LeakOut.Players` and `HexMaster.LeakOut.Scores` meters, next to the ASP.NET Core, HttpClient and runtime meters.
- **Structured logs**: filter by trace to see the logs of one request.

**In Azure**, everything goes to the Application Insights component `<resource group>-appi`, under cloud role `hexmaster-leakout-api`. The deployed version (`service.version`, the image tag) appears as `application_Version`. Requests and dependencies show in **Transaction search** and **Application map**. Custom spans are in the `dependencies` table, custom metrics in `customMetrics` and logs in `traces`:

```kusto
dependencies | where name in ("players.resolve_user", "scores.get_level_top_scores", "scores.get_level_scores_around_player", "scores.get_public_level_top_scores")
customMetrics | where name startswith "leakout."
requests | summarize count() by application_Version, bin(timestamp, 1h)
```

| Span | Operation |
| --- | --- |
| `players.sign_in` | Sign-in with a Google Play Games server auth code |
| `players.resolve_user` | Resolving the access token's subject to the internal player id (every authenticated request) |
| `players.get_display_names` | Looking up display names for a leaderboard |
| `scores.submit_score` | Submitting a score |
| `scores.get_level_top_scores` | Top 10 of a level |
| `scores.get_level_scores_around_player` | The scores around the player's own score |
| `scores.get_public_level_top_scores` | Public top 25 of a level for the website; `leaderboard.cache_hit` tells whether it came from the 30-minute memory cache |

| Metric | Type | Attributes |
| --- | --- | --- |
| `leakout.players.sign_ins` | counter | `outcome` (`success`, `rejected`, `unavailable`, `error`), `new_player` |
| `leakout.players.sign_in.duration` | histogram (s) | `outcome` |
| `leakout.players.user_resolutions` | counter | `outcome` (`cache_hit`, `cache_miss`, `unknown`, `error`) |
| `leakout.scores.submissions` | counter | `outcome` (`new_best`, `not_improved`, `error`) |
| `leakout.scores.submission.duration` | histogram (s) | `outcome` |
| `leakout.scores.leaderboard.requests` | counter | `leaderboard.kind` (`top`, `around_player`, `public_top`), `outcome` (`success`, `error`) |
| `leakout.scores.leaderboard.duration` | histogram (s) | `leaderboard.kind`, `outcome` |

Logs record a new player, a rejected sign-in, a new best score and unexpected failures, and carry the trace id of their request.

**Privacy**: no span attribute, metric attribute or log contains the Google Play Games player id, a server auth code, an access token or a display name. Spans and logs identify a player by the internal player id only. Metric attributes never contain player ids, levels or scores. `/alive` and `/health` are not traced, so platform probes don't count against the 1 GB daily ingestion cap.

## Continuous integration

Every pull request into `development` runs [`.github/workflows/server-pull-request.yml`](../.github/workflows/server-pull-request.yml). A first job, `server / changes`, looks at the files the pull request changes; the two checks below only do their work when it changes `server/`, a `.github/workflows/server-*.yml` workflow or `.github/scripts/server-coverage-gate.sh`. Otherwise (an app-only pull request) they are skipped, which counts as passed, so they never block app work:

| Check | What it does |
| --- | --- |
| `server / build-test` | Restores and builds the solution, runs every test project with code coverage (the Scores tests start PostgreSQL with Testcontainers), merges the reports and fails when total line coverage is below 80%. The coverage appears in the run summary; the merged report and the `.trx` test results are attached as artifacts. |
| `server / bicep` | Compiles `infrastructure/main.bicep` and its modules. |

Both are defined in [`.github/workflows/server-build-test.yml`](../.github/workflows/server-build-test.yml) (shared with the deploy workflow; its `enabled` input skips the job while keeping the check name) and `server-pull-request.yml`. Do not rename those jobs (`server` calling `build-test`, and `server / bicep`): the check names are what branch protection refers to.

To make them mandatory (once, by a repository admin): **Settings → Branches → Add branch ruleset** (or classic rule) for `development`, enable **Require status checks to pass**, and add `server / build-test` and `server / bicep`. A check only appears in the list after it has run once, so open a pull request first. Protecting `main` the same way is recommended, because `main` should only change through the `development` → `main` pull request.

## Deployment

Every push to `main` that changes `server/`, a server workflow or the coverage gate script (and a manual run of **Actions → server-deploy → Run workflow**) runs [`.github/workflows/server-deploy.yml`](../.github/workflows/server-deploy.yml). Pushes that only change the app do not deploy the server, and server pushes do not release the app.

1. **server / build-test**: the same build, tests and 80% gate as the pull request check. A failure stops the run; nothing is published or deployed.
2. **publish**: calculates the version with [GitVersion](GitVersion.yml) (`configFilePath: server/GitVersion.yml`), publishes the API container image with `dotnet publish /t:PublishContainer` (no Dockerfile) and pushes it as `nvv54gsk4pteu.azurecr.io/leak-out/game-server-api:<version>`, then fails if the image would run as root.
3. **deploy** (environment `production`): logs in to Azure with OpenID Connect and runs the subscription-scope deployment of [`infrastructure/main.bicep`](infrastructure/README.md) with the new image, then calls `https://<api-host>/alive` until it answers with this build's `X-API-Version` header.

### Versions and image tags

The image is tagged `<semver>-<short commit sha>`, for example `1.1.0-9e7b048`. The semantic version comes from GitVersion (`GitVersion.yml`), which does not move on every merge to `main`: two merges can get the same version. The commit SHA keeps every tag unique, so no tag is overwritten and every deployment changes the container app's image, which rolls out a new revision. Before the SHA was added, a reused tag left the previous image running after a successful deploy. Put `+semver: minor` or `+semver: major` in a commit message (for example in the `development` → `main` merge) to bump the minor or major version; a Git tag `server-vX.Y.Z` on `main` sets a new base. Only `server-v*` tags count (`tag-prefix: 'server-v'`): the app's `v*` release tags never move the server's version. The merge that imported the server into this repository has no merge base, which mainline versioning cannot walk past, so that merge commit carries the tag `server-v0.3.2` (the last version deployed from `leak-out-server`); it must exist before the first deploy from this repository. The version is also the assembly version. The informational version is `<semver>+<short commit sha>`, and the API returns it on every response in the `X-API-Version` header, so `curl -sI https://<api-host>/alive` shows which build is running.

The registry and repository are set in `src/HexMaster.LeakOut.Api/HexMaster.LeakOut.Api.csproj` (`ContainerRegistry`, `ContainerRepository`); the workflow reads them from there. `ContainerUser` pins the non-root `app` user (UID 1654) of the .NET base image, which listens on the unprivileged port 8080.

To roll back, run the deploy workflow manually on an earlier commit of `main`; it rebuilds and redeploys that commit's version.

### Secrets and settings

Secrets of the `hexmasternl/leak-out` repository (**Settings → Secrets and variables → Actions**). GitHub never shows a secret's value, so they were re-entered by hand from their sources when the server moved here (Azure portal, Google Cloud console, the registry's *Access keys* page, the password manager for `POSTGRES_ADMIN_PASSWORD`). The app's secrets are separate: they live in the `play-store` environment ([docs/deployment.md](../docs/deployment.md)).

| Secret | Used by | Purpose |
| --- | --- | --- |
| `ACR_LOGIN_USERNAME` | publish, deploy | User of the container registry `nvv54gsk4pteu.azurecr.io`; also passed to the container app to pull the image |
| `ACR_LOGIN_PASSWORD` | publish, deploy | Password of that registry user |
| `AZURE_PROD_CLIENT_ID` | deploy | Client id of the app registration the workflow logs in with (OIDC, no client secret) |
| `AZURE_PROD_TENANT_ID` | deploy | Entra tenant id |
| `AZURE_PROD_SUBSCRIPTION_ID` | deploy | Subscription the deployment runs in |
| `POSTGRES_ADMIN_PASSWORD` | deploy | Admin password of the PostgreSQL server (login `leakoutadmin`) |
| `GOOGLE_CLIENT_ID` | deploy | Client id of the **web** OAuth client (the game's `serverClientId`), becomes `GooglePlayGames:ClientId` |
| `GOOGLE_CLIENT_SECRET` | deploy | Secret of that web OAuth client, becomes `GooglePlayGames:ClientSecret` |
| `GOOGLE_SIGNING_KEY` | deploy | The API's own JWT signing key (at least 32 characters), becomes `Authentication:SigningKey`. Not a Google credential; rotating it signs every player out. |

The deploy job fails before logging in to Azure when one of the registry, PostgreSQL or Google secrets is empty, and names it. `ACR_LOGIN_SERVER` exists but is no longer read: the registry comes from the API project.

Once, in GitHub and Azure:

1. Create the environment **production** (**Settings → Environments**). Add required reviewers there if deployments should wait for approval, and limit its deployment branches to `main`. The deploy job runs in it, which is what the Azure sign-in trusts.
2. Give the app registration behind `AZURE_PROD_CLIENT_ID` a federated credential for GitHub Actions with entity type **Environment**, i.e. subject `repo:hexmasternl/leak-out:environment:production`, audience `api://AzureADTokenExchange`:

   ```bash
   az ad app federated-credential create --id <client-id> --parameters '{
     "name": "github-leak-out-production",
     "issuer": "https://token.actions.githubusercontent.com",
     "subject": "repo:hexmasternl/leak-out:environment:production",
     "audiences": ["api://AzureADTokenExchange"]
   }'
   ```

3. Give it `Contributor` on the subscription and register the resource providers (see [infrastructure/README.md](infrastructure/README.md), One-time setup).
