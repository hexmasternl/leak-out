# Tasks

## 1. Solution and Core foundations

- [x] 1.1 Add `Core/HexMaster.LeakOut.Core`, the four `Players/*` and the four `Scores/*` projects to `src/leak-out-server.slnx` and verify `dotnet build src/leak-out-server.slnx` succeeds
- [x] 1.2 Add `ICommandHandler<TCommand, TResult>`, `ICommandHandler<TCommand>`, `IQueryHandler<TQuery, TResult>`, `IClock`, `SystemClock` and `DomainException` to `HexMaster.LeakOut.Core` (no web or EF references) and verify the solution builds
- [ ] 1.3 Remove the `/weatherforecast` endpoint and `WeatherForecast` record from `Program.cs` and its sample from `HexMaster.LeakOut.Api.http`, and verify the API starts and `GET /weatherforecast` returns 404

## 2. Players domain

- [ ] 2.1 Add `DomainModels/Player.cs` (`Id`, `DisplayName`, `CreatedAt`, `LastSignedInAt`, `Register`, `RecordSignIn`) with `PlayerTests` and a Bogus `PlayerFaker` in `HexMaster.LeakOut.Players.Tests`, and verify the tests pass
- [ ] 2.2 Add the ports `IPlayerRepository`, `IGooglePlayGamesClient` (with `GooglePlayGamesPlayer`, `GooglePlayGamesAuthenticationException`, `GooglePlayGamesUnavailableException`) and `IAccessTokenIssuer` to `HexMaster.LeakOut.Players` and verify the project builds

## 3. Google Play Games client and access tokens

- [ ] 3.1 Implement `GooglePlayGamesClient` as a typed `HttpClient` (token exchange at `oauth2.googleapis.com/token`, then `games/v1/players/me`) bound to `GooglePlayGamesOptions` (`ClientId`, `ClientSecret`), with `GooglePlayGamesClientTests` using a fake `HttpMessageHandler` for success, `invalid_grant` and 5xx, and verify the tests pass
- [ ] 3.2 Implement `JwtAccessTokenIssuer` bound to `AccessTokenOptions` (`Issuer` `leak-out-server`, `Audience` `leak-out-game`, `SigningKey` ≥ 32 bytes, `Lifetime` 1 hour, validated on start) with `JwtAccessTokenIssuerTests` checking claims and expiry, and verify the tests pass

## 4. Sign-in feature slice

- [ ] 4.1 Add `CreateSessionRequest` and `SessionDto` sealed records to `HexMaster.LeakOut.Players.Abstractions/DataTransferObjects` and verify the Abstractions project has no reference to the module or EF
- [ ] 4.2 Add `Features/SignInPlayer/SignInPlayerCommand.cs` and `SignInPlayerCommandHandler.cs` with `SignInPlayerCommandHandlerTests` (new player, returning player with a renamed display name, rejected code, Google unavailable, null command), and verify the tests pass
- [ ] 4.3 Add `Observability/PlayersMetrics.cs` and the `players.sign_in` activity with outcome and new-player tags, without subject ids or tokens, and verify a handler test asserts the counter is recorded

## 5. User resolver

- [ ] 5.1 Add `IUserResolver` to `HexMaster.LeakOut.Players.Abstractions/Services` and implement `UserResolver` with `IMemoryCache` (sliding 10 minutes, no negative caching), with `UserResolverTests` (hit, miss, cached second call), and verify the tests pass
- [ ] 5.2 Add `PlayersModuleRegistration.AddPlayersModule()` registering handler, ports, options, `HttpClient` and resolver, and verify a registration test resolves `ICommandHandler<SignInPlayerCommand, SessionDto>` and `IUserResolver`

## 6. Persistence

- [ ] 6.1 Add `PlayersDbContext`, `PlayerEntity` (table `players`, unique index on `subject_id`) and `PlayerRepository` to `HexMaster.LeakOut.Players.Data.PostgreSQL` using `Aspire.Npgsql.EntityFrameworkCore.PostgreSQL`, and verify repository tests (EF Core in-memory SQLite or Testcontainers) cover insert, lookup by subject id and update
- [ ] 6.2 Add the initial EF Core migration and apply migrations at startup in Development, and verify `dotnet ef migrations list` shows it and the `players` table exists after `aspire run`

## 7. Aspire orchestration and configuration

- [ ] 7.1 Add `AddPostgres("postgres").WithDataVolume()` and the `playersdb` database to `AppHost.cs`, referenced into the API with `.WithReference(...).WaitFor(...)`, and verify the Aspire dashboard shows PostgreSQL healthy and the API waiting for it
- [ ] 7.2 Add Aspire parameters for `GooglePlayGames:ClientId`, `GooglePlayGames:ClientSecret` (secret) and `Authentication:SigningKey` (secret), passed to the API as environment variables, and verify the API fails fast with a clear options-validation error when one is missing

## 8. API authentication and endpoints

- [ ] 8.1 Configure `AddAuthentication().AddJwtBearer()` (issuer, audience, lifetime with 1 minute skew, signing key), `AddAuthorization` with a fallback policy requiring an authenticated user, anonymous `/health`, `/alive` and `/openapi`, and a bearer security scheme in the OpenAPI document, and verify `/health` still returns 200 anonymously in Development
- [ ] 8.2 Add `Endpoints/PlayerEndpoints.cs` with `MapPlayerEndpoints()` (group `/api/players`, `POST /sessions`, `AllowAnonymous`, 400 for a blank code, 401 for `GooglePlayGamesAuthenticationException`, 503 for `GooglePlayGamesUnavailableException`) and call it from `Program.cs`, and verify with endpoint tests
- [ ] 8.3 Add the `ResolveUserIdAsync` helper (or endpoint filter) that reads `sub`, calls `IUserResolver` and returns 401 for an unknown player, and verify with a test endpoint in `HexMaster.LeakOut.Api.Tests`
- [ ] 8.4 Create `HexMaster.LeakOut.Api.Tests` with `WebApplicationFactory<Program>` tests for no token, expired token, foreign-signed token and valid token on a protected endpoint, and verify they pass
- [ ] 8.5 Add a sign-in sample to `HexMaster.LeakOut.Api.http` and document the Google web client id/secret, the signing key and the game's `serverClientId` in `README.md` and `CLAUDE.md`, and verify the documented `dotnet user-secrets` commands work

## 9. Integration check

- [ ] 9.1 Run all tests with coverage (`dotnet test --solution src/leak-out-server.slnx --coverage`) and verify every test passes and merged line coverage is at least 80%
