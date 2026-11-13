# Leak Out game server

The score server of the Leak Out game: a .NET 11 minimal API orchestrated by .NET Aspire, with PostgreSQL. It lives in `server/` of the Leak Out repository, next to the Android app in `app/`; until October 2026 it had its own repository, `hexmasternl/leak-out-server` (archived), whose history was imported here. Paths in this file are relative to `server/` unless they start with `.github/`; how the two sides are built and released apart: [docs/repository-layout.md](../docs/repository-layout.md).

## Running locally

Requirements: the .NET 11 SDK and Docker (Aspire runs PostgreSQL in a container).

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

## Daily pipe

One new level a day that every player gets in exactly the same form: the `DailyPipes` module (`src/DailyPipes/`, database `dailypipesdb`). The day is always the server's **UTC date**; the device clock is never used.

**Generation.** The console project `HexMaster.LeakOut.DailyPipes.Generator` runs the `GenerateDailyPipe` command once and exits: 0 when today's pipe is published (or already was), 1 when the run failed. In Azure it is the Container Apps job `<resource group>-dailypipe` with the schedule `0 0 * * *` (UTC, 3 retries, 10 minutes timeout); locally the Aspire AppHost runs it once at startup as the resource `daily-pipe-generator`, so a developer always has a pipe. A run:

1. does nothing when today already has a pipe, so a rerun or retry is harmless;
2. generates random candidates (`Generation/DailyLevelGenerator.cs`, with a `DifficultyPreset` per weekday, easiest on Monday) until one passes `LevelRules` and `LevelSolver`, C# ports of the app's `LevelValidator` and `LevelSolver`, at most 200 attempts;
3. derives the head start and the star thresholds (0.6, 0.9 and 1.2 times the points of the solver's route) and the highest score a submission may claim from that route (`DailyPipeBuilder`);
4. publishes the level with a 53-bit stack seed and, in the same transaction, deletes the scores of every earlier day and the pipes older than 7 days. Streaks are never deleted.

**Keeping the port in step with the app.** `src/DailyPipes/HexMaster.LeakOut.DailyPipes.Tests/Fixtures/` holds the bundled levels, hand-made invalid and unsolvable levels and their expected verdicts (`verdicts.json`), and one generated level per weekday (`generated/`). The server tests and the app's `core` test `DailyPipeConformanceTest` both check them, so a difference between the C# and Kotlin rules fails one of the two test suites. When the generator changes, `GeneratedFixturesTests` fails until the generated levels are written again: run it with `LEAKOUT_WRITE_FIXTURES=1`, then run `./gradlew :core:test` in `app/src`. When a bundled level or the app's rules change, copy the level into `Fixtures/levels/` and update its verdict.

**Running the generator by hand.** Locally, restart the `daily-pipe-generator` resource in the Aspire dashboard, or from `src/`:

```bash
ConnectionStrings__dailypipesdb="Host=localhost;Port=<port>;Database=dailypipesdb;Username=postgres;Password=<password>" \
  dotnet run --project DailyPipes/HexMaster.LeakOut.DailyPipes.Generator
```

In Azure, after a failed night or to publish the first pipe after the first deployment:

```bash
az containerapp job start --name rg-leakout-prod-neu-dailypipe --resource-group rg-leakout-prod-neu
az containerapp job execution list --name rg-leakout-prod-neu-dailypipe --resource-group rg-leakout-prod-neu --output table
```

**Endpoints** (all for a signed-in player; `src/HexMaster.LeakOut.Api/Endpoints/DailyPipeEndpoints.cs`, samples in the `.http` file):

| Endpoint | Returns |
| --- | --- |
| `GET /api/daily-pipes/today` | `{ "date": "2026-11-02", "level": { …level-format JSON… }, "stackSeed": 123, "streak": { "current": 3, "best": 7, "wonToday": false }, "bestScore": null }`, or `{ "score": 380, "stars": 2 }` as `bestScore`. 404 until today's pipe is published; an older pipe is never returned. |
| `POST /api/daily-pipes/{date}/scores` | Body `{ "score": 380, "stars": 2 }`. Keeps the higher of this and the player's stored score; a result with at least one star is a win for the streak. Returns `{ "date", "bestScore": { "score", "stars" }, "isNewBest", "rank", "streak" }`. 409 when `{date}` is not today's pipe (the day has ended), 400 naming `score` (below 0 or above the pipe's maximum) or `stars` (not 0 to 3). |
| `GET /api/daily-pipes/{date}/scores/top` | `{ "date", "entries": [{ "position", "displayName", "score", "stars", "isCurrentPlayer" }], "player": { "position", "score", "stars" } \| null }`: the 10 best results, by score and then the earliest achievement. 404 for a date without a pipe. |

**Streaks.** The current streak counts consecutive UTC days with a win. A second win on the same day changes nothing; a win the day after the last one adds 1; any later win restarts it at 1. A streak whose last win is before yesterday is reported as 0 (the stored row is only corrected on the next win), and the best streak is kept.

## Observability

The API emits OpenTelemetry traces, metrics and logs (`HexMaster.LeakOut.Aspire.ServiceDefaults`). ASP.NET Core requests, outgoing HTTP calls (Google) and EF Core / Npgsql database calls are instrumented automatically. Every operation adds its own span, metrics and logs on top.

**Locally**, open the Aspire dashboard (its URL is printed when the AppHost starts) and select the `hexmaster-leakout-api` resource:

- **Traces**: a leaderboard request shows the HTTP span, `players.resolve_user`, the handler span (for example `scores.get_level_top_scores`), `players.get_display_names` and the database spans nested under them.
- **Metrics**: the `HexMaster.LeakOut.Players`, `HexMaster.LeakOut.Scores` and `HexMaster.LeakOut.DailyPipes` meters, next to the ASP.NET Core, HttpClient and runtime meters. The `daily-pipe-generator` resource shows the generation runs.
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
| `daily_pipes.generate` | A generator run (cloud role `daily-pipe-generator`); `daily_pipe.attempts` and `daily_pipe.published` |
| `daily_pipes.get_today` | Downloading today's daily pipe; `daily_pipe.found` |
| `daily_pipes.submit_score` | Submitting a daily pipe result; `daily_pipe.outcome` |
| `daily_pipes.get_top_scores` | Top 10 of a daily pipe |

| Metric | Type | Attributes |
| --- | --- | --- |
| `leakout.players.sign_ins` | counter | `outcome` (`success`, `rejected`, `unavailable`, `error`), `new_player` |
| `leakout.players.sign_in.duration` | histogram (s) | `outcome` |
| `leakout.players.user_resolutions` | counter | `outcome` (`cache_hit`, `cache_miss`, `unknown`, `error`) |
| `leakout.scores.submissions` | counter | `outcome` (`new_best`, `not_improved`, `error`) |
| `leakout.scores.submission.duration` | histogram (s) | `outcome` |
| `leakout.scores.leaderboard.requests` | counter | `leaderboard.kind` (`top`, `around_player`, `public_top`), `outcome` (`success`, `error`) |
| `leakout.scores.leaderboard.duration` | histogram (s) | `leaderboard.kind`, `outcome` |
| `leakout.daily_pipes.generation.runs` | counter | `outcome` (`published`, `already_published`, `failed`) |
| `leakout.daily_pipes.generation.attempts` | histogram | `outcome`: candidates generated per run |
| `leakout.daily_pipes.generation.duration` | histogram (s) | `outcome` |
| `leakout.daily_pipes.downloads` | counter | `outcome` (`served`, `not_found`, `error`) |
| `leakout.daily_pipes.submissions` | counter | `outcome` (`new_best`, `not_improved`, `ended`, `rejected`, `error`) |
| `leakout.daily_pipes.submission.duration` | histogram (s) | `outcome` |
| `leakout.daily_pipes.leaderboard.requests` | counter | `outcome` (`served`, `not_found`, `error`) |

Logs record a new player, a rejected sign-in, a new best score, a published or failed daily pipe and unexpected failures, and carry the trace id of their request. A failed generator run logs an error, which fires the alert `<resource group>-dailypipe-failed` ([infrastructure/README.md](infrastructure/README.md)).

**Privacy**: no span attribute, metric attribute or log contains the Google Play Games player id, a server auth code, an access token or a display name. Spans and logs identify a player by the internal player id only. Metric attributes never contain player ids, levels or scores. `/alive` and `/health` are not traced, so platform probes don't count against the 1 GB daily ingestion cap.

## Continuous integration

Every pull request into `development` runs [`.github/workflows/server-pull-request.yml`](../.github/workflows/server-pull-request.yml). A first job, `server / changes`, looks at the files the pull request changes; the two checks below only do their work when it changes `server/`, a `.github/workflows/server-*.yml` workflow or `.github/scripts/server-coverage-gate.sh`. Otherwise (an app-only pull request) they are skipped, which counts as passed, so they never block app work:

| Check | What it does |
| --- | --- |
| `server / build-test` | Restores and builds the solution, runs every test project with code coverage (the Scores and DailyPipes tests start PostgreSQL with Testcontainers), merges the reports and fails when total line coverage is below 80%. The coverage appears in the run summary; the merged report and the `.trx` test results are attached as artifacts. |
| `server / bicep` | Compiles `infrastructure/main.bicep` and its modules. |

Both are defined in [`.github/workflows/server-build-test.yml`](../.github/workflows/server-build-test.yml) (shared with the deploy workflow; its `enabled` input skips the job while keeping the check name) and `server-pull-request.yml`. Do not rename those jobs (`server` calling `build-test`, and `server / bicep`): the check names are what branch protection refers to.

To make them mandatory (once, by a repository admin): **Settings → Branches → Add branch ruleset** (or classic rule) for `development`, enable **Require status checks to pass**, and add `server / build-test` and `server / bicep`. A check only appears in the list after it has run once, so open a pull request first. Protecting `main` the same way is recommended, because `main` should only change through the `development` → `main` pull request.

## Deployment

Every push to `main` that changes `server/`, a server workflow or the coverage gate script (and a manual run of **Actions → server-deploy → Run workflow**) runs [`.github/workflows/server-deploy.yml`](../.github/workflows/server-deploy.yml). Pushes that only change the app do not deploy the server, and server pushes do not release the app.

1. **server / build-test**: the same build, tests and 80% gate as the pull request check. A failure stops the run; nothing is published or deployed.
2. **publish**: calculates the version with [GitVersion](GitVersion.yml) (`configFilePath: server/GitVersion.yml`), publishes the API and daily pipe generator container images with `dotnet publish /t:PublishContainer` (no Dockerfile) and pushes them as `nvv54gsk4pteu.azurecr.io/leak-out/game-server-api:<version>` and `…/leak-out/daily-pipe-generator:<version>`, then fails if either image would run as root.
3. **deploy** (environment `production`): logs in to Azure with OpenID Connect and runs the subscription-scope deployment of [`infrastructure/main.bicep`](infrastructure/README.md) with the new images (the API container app and the daily pipe job), then calls `https://<api-host>/alive` until it answers with this build's `X-API-Version` header.
4. **release**: only after a healthy deploy, tags the commit `server-v<version>` and creates its GitHub release, with the commits since the previous `server-v*` tag that change `server/` as notes ([`scripts/publish-release.sh`](../scripts/publish-release.sh), see [docs/repository-layout.md](../docs/repository-layout.md#versions-and-tags)).

### Versions and image tags

The image is tagged `<semver>-<short commit sha>`, for example `1.1.0-9e7b048`. The semantic version comes from GitVersion (`GitVersion.yml`) and starts from the newest `server-v*` tag. Every successful deployment pushes that tag, so the next merge to `main` gets the next version. The commit SHA also keeps every image tag unique, so even a re-run of the same version changes the container app's image and rolls out a new revision. Before the SHA was added, a reused tag left the previous image running after a successful deploy. Put `+semver: minor` or `+semver: major` in a commit message (for example in the `development` → `main` merge) to bump the minor or major version. Don't create, move or delete `server-v*` tags by hand: the workflow owns them. Only `server-v*` tags count (`tag-prefix: 'server-v'`): the app's `v*` release tags never move the server's version. GitVersion uses GitHub Flow, like the app and website, and the workflow reads its `MajorMinorPatch` (see `docs/repository-layout.md#versions-and-tags` for why mainline versioning was dropped). The version is also the assembly version. The informational version is `<semver>+<short commit sha>`, and the API returns it on every response in the `X-API-Version` header, so `curl -sI https://<api-host>/alive` shows which build is running.

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

The repository **variable** `ALERT_EMAIL` (optional, **Settings → Secrets and variables → Actions → Variables**) is the e-mail address that gets the daily pipe generator's failure alert; without it the alert only shows in Azure Monitor.

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
