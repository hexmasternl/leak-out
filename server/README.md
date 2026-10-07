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
