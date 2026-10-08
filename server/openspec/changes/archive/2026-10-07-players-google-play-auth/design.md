# Design

## Context

See proposal.md (Why). Today `src/HexMaster.LeakOut.Api/Program.cs` only maps the template `/weatherforecast` endpoint. The module projects already exist on disk (`Core`, `Players`, `Scores`, each with `Abstractions`, `Data.PostgreSQL` and `Tests` projects) but are empty and not listed in `src/leak-out-server.slnx`. The AppHost registers only the API (`hexmaster-leakout-api`). Shared build settings live in the root `Directory.Build.props` (`net10.0`, nullable, `TreatWarningsAsErrors`).

The game (hexmasternl/leak-out) signs in to Play Games Services v2 (`PlayGamesAuth.kt`, silent sign-in plus a manual Sign in button). PGS v2 gives a game server no ID token; the supported server flow is a one-time **server auth code** from `GamesSignInClient.requestServerSideAccess(serverClientId, forceRefreshToken)`, which the server exchanges with Google.

HexMaster standards that shape this design: modular monolith (ADR 0002), lightweight CQRS with handler interfaces in Core (ADR 0004), Minimal APIs organised in `Endpoints/` files (ADR 0005 and the endpoint organisation standard), feature slices with DTOs in `Abstractions/DataTransferObjects` (ADR 0009), Aspire for orchestration (ADR 0003), OpenTelemetry (ADR 0008), and resolving the external subject id to an internal `Guid` at the API boundary, owned by the Players module (ADR 0010).

## Goals / Non-Goals

**Goals:**
- A player who is signed in to Play Games in the game can get an access token for the server without extra UI.
- Every later endpoint (#2, #3, #4) gets the caller's internal `Guid` player id the same way, with one line of endpoint code.
- The foundations those issues need are in place: Core handler interfaces, PostgreSQL in Aspire, the solution file, the endpoint file layout.

**Non-Goals:**
- Refresh tokens or token revocation. When a token expires the game requests a new server auth code and signs in again.
- Other identity providers, accounts without Play Games, account linking.
- Anti-cheat or request signing.
- Changes to the game client; they are tracked in hexmasternl/leak-out.

## Decisions

### D1. Exchange a server auth code and issue our own JWT
The game posts the server auth code to `POST /api/players/sessions` (anonymous). The server exchanges it at `https://oauth2.googleapis.com/token` (grant `authorization_code`, the web OAuth client id and secret, empty `redirect_uri`), then calls `GET https://games.googleapis.com/games/v1/players/me` with the Google access token to get `playerId` and `displayName`. The server then issues its own JWT and discards Google's tokens.

- *Why*: a server auth code is single-use, so it cannot authenticate every request. Our own short-lived JWT lets ASP.NET Core's standard JWT bearer handler protect all endpoints.
- *Alternatives*: sending the Google access token on every request and validating it with Google each time (one Google round trip per request, and PGS access tokens are not meant to be forwarded); requesting an OpenID ID token through extra scopes (adds a consent screen to the game and is not the documented PGS v2 path).

### D2. Token format
HS256-signed JWT, issuer `leak-out-server`, audience `leak-out-game`, lifetime 1 hour, `sub` = the Google Play Games player id, `jti` random. Signing key from `Authentication:SigningKey` (at least 32 bytes). Options bound to `AccessTokenOptions` (`Issuer`, `Audience`, `SigningKey`, `Lifetime`) and validated on start (`ValidateOnStart`).

- *Why `sub` = Play Games player id*: ADR 0010 expects `sub` to be the identity provider's subject and the API boundary to resolve it; keeping that shape means a later move to another issuer (or to Google tokens directly) does not change the endpoints.
- *Why symmetric*: one server issues and validates; no key distribution is needed. Asymmetric keys can replace it later without changing the API contract.

### D3. Feature slice `SignInPlayer` in the Players module
- `HexMaster.LeakOut.Players.Features.SignInPlayer`: `SignInPlayerCommand(string ServerAuthCode)` and `SignInPlayerCommandHandler : ICommandHandler<SignInPlayerCommand, SessionDto>`.
- Handler steps: call `IGooglePlayGamesClient.GetPlayerAsync(code, ct)`; load the player through `IPlayerRepository.FindBySubjectIdAsync`; register a new `Player` or call `player.RecordSignIn(displayName, clock.UtcNow)`; save; ask `IAccessTokenIssuer.Issue(subjectId)` for the token; return `SessionDto(AccessToken, ExpiresAt, PlayerId, DisplayName)`.
- `IGooglePlayGamesClient` returns `GooglePlayGamesPlayer(string PlayerId, string DisplayName)`, or throws `GooglePlayGamesAuthenticationException` (rejected code, mapped to 401) or `GooglePlayGamesUnavailableException` (mapped to 503).
- DTOs in `HexMaster.LeakOut.Players.Abstractions.DataTransferObjects`: `CreateSessionRequest(string ServerAuthCode)`, `SessionDto(string AccessToken, DateTimeOffset ExpiresAt, Guid PlayerId, string DisplayName)`. All messages and DTOs are `sealed record`s.
- `IGooglePlayGamesClient`, `IAccessTokenIssuer` and `IPlayerRepository` are ports in the module project (ADR 0002); `GooglePlayGamesClient` is a typed `HttpClient` registered with `AddHttpClient`, so the ServiceDefaults standard resilience handler applies; `JwtAccessTokenIssuer` lives in the module's `Services/` folder.
- `AddPlayersModule()` in `PlayersModuleRegistration.cs` registers handlers, ports and options.

### D4. Domain model and persistence
- `Player` (in `DomainModels/`): `Id` (`Guid`, `Guid.CreateVersion7()`), `DisplayName`, `CreatedAt`, `LastSignedInAt`; factory `Player.Register(displayName, now)` and `RecordSignIn(displayName, now)`. A blank display name is stored as an empty string (the leaderboard falls back to a placeholder, #3).
- `HexMaster.LeakOut.Players.Data.PostgreSQL`: `PlayersDbContext`, record `PlayerEntity` and table `players` (`id uuid` PK, `subject_id text not null` unique index, `display_name text not null`, `created_at timestamptz`, `last_signed_in_at timestamptz`), `PlayerRepository` implementing `IPlayerRepository`, EF Core migrations. The subject id lives only in this data record, never on the domain `Player`.
- Npgsql via `Aspire.Npgsql.EntityFrameworkCore.PostgreSQL` (`builder.AddNpgsqlDbContext<PlayersDbContext>("playersdb")`), which brings health checks and telemetry. Migrations are applied at startup in Development only; production migrations are a deployment step.
- AppHost: `var postgres = builder.AddPostgres("postgres").WithDataVolume(); var playersDb = postgres.AddDatabase("playersdb");` referenced into the API with `.WithReference(playersDb).WaitFor(playersDb)`. `scoresdb` is added by #2.

### D5. Resolving the caller at the API boundary (ADR 0010)
- `IUserResolver` in `HexMaster.LeakOut.Players.Abstractions.Services`: `Task<Guid?> ResolveAsync(string subjectId, CancellationToken ct)`. `UserResolver` in the Players module looks the id up through the repository and caches hits in `IMemoryCache` per subject id (sliding 10 minutes). No negative caching, so a player registered a second ago resolves.
- An API helper (`HttpContextUserExtensions.ResolveUserIdAsync` or an endpoint filter that stores the id in `HttpContext.Items`) reads `sub`, calls the resolver, and returns 401 when the result is `null`. Endpoints of #2–#4 use it before dispatching their command or query.
- *ADR 0010 nuance*: the subject id appears only in endpoint code, in the `IUserResolver` contract and implementation, and inside the Players module that owns the mapping: the Google client result, the sign-in handler and repository, the token issuer, and the `PlayerEntity` data record. It never appears on a command, a query, a domain model property, an Abstractions DTO, or in another module.

### D6. API host and endpoints
- Single API host `HexMaster.LeakOut.Api`; no per-module `.Api` projects. The team is small, all modules ship together, and `modular-solution-structure` allows one API in that case.
- `Program.cs` only composes: `AddServiceDefaults`, `AddPlayersModule`, `AddAuthentication(JwtBearerDefaults.AuthenticationScheme).AddJwtBearer(...)` validating issuer, audience, lifetime (clock skew 1 minute) and signing key, `AddAuthorization`, `UseAuthentication`, `UseAuthorization`, `MapPlayerEndpoints()`. A fallback authorization policy requiring an authenticated user makes protected the default; sign-in, `/health`, `/alive` and `/openapi` opt out with `AllowAnonymous`.
- `Endpoints/PlayerEndpoints.cs`: group `/api/players`, tag `Players`; `POST /sessions` (`AllowAnonymous`, name `CreateSession`, `Produces<SessionDto>(200)`, 400, 401, 503). The endpoint validates that `ServerAuthCode` is not blank, then maps the request to `SignInPlayerCommand`.
- The OpenAPI document gets a bearer security scheme so protected endpoints show the lock.
- `/weatherforecast` and the `WeatherForecast` record are removed; `HexMaster.LeakOut.Api.http` gets a sign-in sample instead.

### D7. Core
`HexMaster.LeakOut.Core` gets `ICommandHandler<TCommand, TResult>`, `ICommandHandler<TCommand>`, `IQueryHandler<TQuery, TResult>`, `IClock` (`DateTimeOffset UtcNow`) and `SystemClock`, plus `DomainException`. Core stays free of ASP.NET Core and EF references.

### D8. Configuration and secrets
- `GooglePlayGames:ClientId`, `GooglePlayGames:ClientSecret` (the **web** OAuth client from the Play Console / Google Cloud project linked to the game) and `Authentication:SigningKey`.
- In the AppHost they are Aspire parameters (`builder.AddParameter("google-client-secret", secret: true)` etc.) passed to the API as environment variables, so they come from user secrets locally and never from `appsettings.json`.

### D9. Observability
Activities on `HexMaster.LeakOut.Players` for sign-in (`players.sign_in`) and a counter `leakout.players.sign_ins` tagged `outcome` (`success`, `rejected`, `unavailable`) and `new_player`. The subject id and tokens are never logged or added as tags.

### D10. Tests
`HexMaster.LeakOut.Players.Tests` (xUnit v3, Moq, Bogus): `Player` domain tests; `SignInPlayerCommandHandlerTests` (new player, existing player with renamed display name, rejected code, Google unavailable, null command); `UserResolverTests` (hit, miss, cache); `GooglePlayGamesClientTests` with a fake `HttpMessageHandler` (success, 400 `invalid_grant`, 5xx); `JwtAccessTokenIssuerTests`. An API test project (`HexMaster.LeakOut.Api.Tests`) with `WebApplicationFactory<Program>` covers 401 without, with expired and with foreign tokens, and the sign-in endpoint with mocked handler. Coverage at least 80%.

## Risks / Trade-offs

- [The server auth code is bound to the web client id configured in the game] → document in the README that the game's `serverClientId` must be the web client id configured here.
- [Symmetric signing key leaks] → keep it in secrets only, at least 32 bytes, rotate by redeploying; tokens live one hour.
- [Google outage blocks sign-in] → existing tokens keep working for up to an hour; sign-in returns 503 so the game can retry later.
- [Resolver cache keeps a deleted player resolvable for 10 minutes] → acceptable: no player deletion exists yet.
- [Display names from Google can change or be empty] → refreshed on every sign-in; consumers handle empty names.

## Migration Plan

New database and endpoints; nothing to migrate. Deploying requires the three secrets and a PostgreSQL database. Rollback is redeploying the previous build; the `players` table can stay.

## Open Questions

- Exact token lifetime in production (1 hour is the default in `AccessTokenOptions` and can be tuned by configuration).
