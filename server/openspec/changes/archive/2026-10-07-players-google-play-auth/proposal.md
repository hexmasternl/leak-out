# Proposal

## Why

Tracked in [#1](https://github.com/hexmasternl/leak-out-server/issues/1).

The Leak Out score server has no notion of who is calling it: the API is still the template project and every endpoint is anonymous. Before a score can be stored per player (#2) or shown on a leaderboard (#3, #4), the server must know which player sends a request. Players already sign in to Google Play Games Services in the Android game, so that identity is the one the server should trust.

## What Changes

- Add a sign-in endpoint, `POST /api/players/sessions`, that accepts a one-time Google Play Games server auth code, verifies it with Google, and returns a short-lived access token issued by the server.
- Register a player the first time they sign in, and refresh their display name on every later sign-in. The Players module owns the mapping from the Google Play player id to an internal `Guid` player id.
- Require a valid access token on every other API endpoint, and resolve the token's subject to the internal player id at the API boundary before any command or query runs.
- Lay the groundwork the following issues build on: the shared CQRS handler interfaces and `IClock` in `HexMaster.LeakOut.Core`, the Core/Players/Scores projects in the solution file, PostgreSQL orchestrated by Aspire with a `playersdb` database, and endpoint files under `src/HexMaster.LeakOut.Api/Endpoints/`.
- **BREAKING** (template only): remove the scaffolded `/weatherforecast` endpoint.

## Capabilities

### New Capabilities

- `player-authentication`: how a player of the Leak Out game proves their identity to the score server, how a player is registered, and how the server protects its endpoints.

### Modified Capabilities

None.

## Impact

- **Code**: `HexMaster.LeakOut.Core` (handler interfaces, `IClock`), `HexMaster.LeakOut.Players` (domain, `SignInPlayer` feature slice, Google Play Games client, token issuer, user resolver), `HexMaster.LeakOut.Players.Abstractions` (DTOs, `IUserResolver`), `HexMaster.LeakOut.Players.Data.PostgreSQL` (EF Core, `players` table, migrations), `HexMaster.LeakOut.Players.Tests`, `HexMaster.LeakOut.Api` (authentication, `Endpoints/PlayerEndpoints.cs`), `HexMaster.LeakOut.Aspire.AppHost` (PostgreSQL, secrets as parameters), `src/leak-out-server.slnx`.
- **External systems**: Google OAuth 2.0 token endpoint and the Play Games Services REST API (`games/v1/players/me`).
- **Configuration and secrets**: the Google web OAuth client id and secret, and the token signing key.
- **Game client** (hexmasternl/leak-out): must call `GamesSignInClient.requestServerSideAccess` with the server's web client id and send the code to the new endpoint. That work belongs to the game repository.
