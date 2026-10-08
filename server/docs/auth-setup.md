# Setting up Google Play Games authentication

This guide sets up everything the score server needs to sign players in with their Google Play Games account: the Google Cloud and Play Console configuration, the game, and the server's secrets. Follow it once per environment (your machine, a test server, production).

## How sign-in works

```
 Leak Out game (Android)                  leak-out-server                     Google
 ───────────────────────                  ───────────────                     ──────
 1. Signs in to Play Games
 2. requestServerSideAccess(WEB_CLIENT_ID)  ──────────────────────────────▶  issues a one-time
    ◀──────────────────────────────────────────────────────────────────────  server auth code
 3. POST /api/players/sessions { serverAuthCode }  ─▶
                                           4. exchanges the code  ────────▶  oauth2.googleapis.com/token
                                              (web client id + secret)
                                           5. reads the player     ────────▶  games/v1/players/me
                                           6. registers the player or refreshes the display name
    ◀─ { accessToken, expiresAt, playerId, displayName }
 7. Calls other endpoints with
    Authorization: Bearer <accessToken>
```

The server issues its own access token (a JWT, valid for one hour) and throws Google's tokens away. When the token expires, the game requests a new server auth code and signs in again. A server auth code works **once**; never cache or reuse it.

Three values connect the pieces:

| Value | Where it comes from | Who uses it |
| --- | --- | --- |
| Web client id | The **web** OAuth client of the Google Cloud project linked to the game | The game (as `serverClientId`) **and** the server (`GooglePlayGames:ClientId`) |
| Web client secret | The same web OAuth client | The server only (`GooglePlayGames:ClientSecret`) |
| Signing key | You generate it | The server only (`Authentication:SigningKey`) |

The game and the server must use **the same web client id**. Google only lets a server exchange codes that were requested for its own client id; a mismatch makes every sign-in fail with 401.

## 1. Google Cloud project and Play Games Services

You need a Play Console developer account with the Leak Out game, and access to the Google Cloud project that Play Games Services is linked to. Console menu names change from time to time; the steps below describe what to look for.

1. **Play Games Services configuration.** In the Play Console, open the game, then *Play Games Services → Setup and management → Configuration*. If Play Games Services is not set up yet, create the configuration and let the Play Console create (or link) a Google Cloud project. Note which Cloud project is linked; every credential below lives in it.

2. **Android credential (for the game).** Under *Credentials*, add a credential of type **Android**. It needs the game's package name and the SHA-1 fingerprint of the signing certificate. Add one for each certificate that signs builds you test with:
   - your debug keystore (`keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android`), for local builds;
   - the **app signing key** from *Play Console → Test and release → App integrity* when Play App Signing is used, for builds installed from Google Play.

   Without a matching Android credential, sign-in already fails in the game, before the server is involved.

3. **Game server credential (for this server).** Under *Credentials*, add a credential of type **Game server**. This creates (or links) an OAuth 2.0 client of type *Web application* in the Cloud project. Open it in the Google Cloud Console (*APIs & Services → Credentials*) and copy:
   - the **client id** (`<number>-<random>.apps.googleusercontent.com`);
   - the **client secret** (`GOCSPX-…`).

   This web client needs no authorised redirect URIs or JavaScript origins: the server exchanges codes with an empty `redirect_uri`, which is what Google expects for codes requested on Android.

4. **APIs.** In the Cloud project, make sure the **Google Play Games Services API** (`games.googleapis.com`) is enabled (*APIs & Services → Enabled APIs & services*). The Play Console normally enables it when linking the project.

5. **Testers.** While the Play Games Services configuration is unpublished, only accounts listed under *Play Games Services → Testers* can sign in. Add the Google accounts you test with. Publish the configuration (*Review and publish*) before releasing the game.

## 2. The game

In the game (hexmasternl/leak-out), after the player is signed in to Play Games Services:

```kotlin
val webClientId = "<web client id from step 1.3>"

PlayGames.getGamesSignInClient(activity)
    .requestServerSideAccess(webClientId, /* forceRefreshToken = */ false)
    .addOnSuccessListener { serverAuthCode ->
        // POST {"serverAuthCode": serverAuthCode} to https://<server>/api/players/sessions
        // and keep the returned accessToken in memory until expiresAt.
    }
```

- `webClientId` is the **web** client id, not the Android one.
- Request a fresh code for every sign-in, including when the access token has expired.
- On 401, request a new code and try once more; the code may have expired or been used. On 503, Google is unavailable: retry later and let the player keep playing offline.

## 3. The server's secrets

The values are Aspire parameters of the AppHost and live in its user secrets on your machine. They never go into `appsettings.json` or source control.

Generate a signing key of at least 32 bytes:

```bash
openssl rand -base64 48
```

```powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
```

Store the three values, from `src/`:

```bash
dotnet user-secrets set "Parameters:google-client-id" "<web client id>" --project HexMaster.LeakOut.Aspire/HexMaster.LeakOut.Aspire.AppHost
dotnet user-secrets set "Parameters:google-client-secret" "<web client secret>" --project HexMaster.LeakOut.Aspire/HexMaster.LeakOut.Aspire.AppHost
dotnet user-secrets set "Parameters:authentication-signing-key" "<generated key>" --project HexMaster.LeakOut.Aspire/HexMaster.LeakOut.Aspire.AppHost
```

Check them with `dotnet user-secrets list --project HexMaster.LeakOut.Aspire/HexMaster.LeakOut.Aspire.AppHost`. If one is missing when you start the AppHost, the Aspire dashboard asks for it.

The AppHost passes them to the API as environment variables:

| AppHost parameter | API setting | Environment variable |
| --- | --- | --- |
| `google-client-id` | `GooglePlayGames:ClientId` | `GooglePlayGames__ClientId` |
| `google-client-secret` | `GooglePlayGames:ClientSecret` | `GooglePlayGames__ClientSecret` |
| `authentication-signing-key` | `Authentication:SigningKey` | `Authentication__SigningKey` |

**Outside Aspire** (a deployed server, or the API run on its own), set those environment variables directly, together with the database connection string `ConnectionStrings__playersdb`. Store them in the host's secret store (for example Key Vault or the container platform's secrets), not in files in the image.

Optional settings, with their defaults: `Authentication:Issuer` (`leak-out-server`), `Authentication:Audience` (`leak-out-game`) and `Authentication:Lifetime` (`01:00:00`, between 1 minute and 1 day).

The API validates all settings at startup. If one is missing, or the signing key is shorter than 32 bytes, it refuses to start with an error such as:

```
OptionsValidationException: DataAnnotation validation failed for 'GooglePlayGamesOptions' members: 'ClientSecret' with the error: 'The ClientSecret field is required.'
```

Changing the signing key invalidates every issued access token; players then sign in again automatically on their next request. Use a different key per environment.

## 4. Run and verify

1. Start Docker, then the AppHost from `src/`:

   ```bash
   dotnet run --project HexMaster.LeakOut.Aspire/HexMaster.LeakOut.Aspire.AppHost
   ```

   In the Aspire dashboard, `postgres` and `playersdb` become healthy first, then `hexmaster-leakout-api` starts. The API applies the database migrations itself when it starts, in every environment.

2. Get a server auth code from a debug build of the game (log it once while testing; never log codes in release builds) and send it within a few minutes, using `src/HexMaster.LeakOut.Api/HexMaster.LeakOut.Api.http` or:

   ```bash
   curl -X POST http://localhost:5090/api/players/sessions \
        -H "Content-Type: application/json" \
        -d '{"serverAuthCode":"<code>"}'
   ```

3. A 200 response contains `accessToken`, `expiresAt` (one hour from now), `playerId` and `displayName`. Signing in again with a new code returns the same `playerId`.

4. Send the token to a protected endpoint as `Authorization: Bearer <accessToken>`. In Development, the OpenAPI document at `/openapi/v1.json` lists the endpoints and marks the protected ones.

## Troubleshooting

| Symptom | Likely cause | Fix |
| --- | --- | --- |
| The API does not start: `OptionsValidationException` | A secret is missing or the signing key is shorter than 32 bytes | Set it as in section 3 |
| Sign-in returns **400** | The request has no `serverAuthCode` | Send `{"serverAuthCode": "…"}` |
| Sign-in returns **401** | Google rejected the code (`invalid_grant`): it was already used, expired, or requested for another client id | Request a fresh code; make sure the game's `serverClientId` equals `GooglePlayGames:ClientId` |
| Sign-in returns **503**, and the API log shows `token exchange failed with status 401 (invalid_client)` | The client id or secret configured on the server is wrong | Copy both again from the **web** OAuth client in the Cloud project |
| Sign-in returns **503**, and the log shows `players/me failed with status 403` | The Play Games Services API is not enabled, or the account is not a tester of an unpublished configuration | Enable the API (step 1.4) or add the tester (step 1.5) |
| Sign-in returns **503** without such a log line | Google could not be reached | Retry later; check the server's outbound network access |
| The game cannot sign in to Play Games at all | No Android credential matches the build's package name and signing certificate | Add the SHA-1 of the certificate that signed the build (step 1.2) |
| Protected endpoints return **401** with a fresh token | The token was issued with another signing key, issuer or audience (for example by another environment), or the player is not registered there | Sign in against this server again |
