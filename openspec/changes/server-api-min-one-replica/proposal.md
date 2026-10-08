# Proposal

## Why

The game server's container app scales down to 0 replicas when idle. That is cheap, but the next request has to wait for a cold start of about 15 seconds, and many of those requests fail with HTTP timeouts in the app. Keeping one replica running at all times removes the cold start, at the cost of one always-on replica. The maximum stays at 3.

Tracked in [#131](https://github.com/hexmasternl/leak-out/issues/131).

## What Changes

- The container app's scale rule keeps at least 1 replica (was 0) and at most 3 (unchanged).
- The minimum becomes a Bicep parameter `minReplicas` (default `1`) next to `maxReplicas`, passed through `main.bicep` → `resources.bicep` → `modules/api.bicep`, so it can be set back to 0 without editing a module.
- `server/infrastructure/README.md` and the header comment of `modules/api.bicep` describe the new scaling.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `server-azure-hosting`: the requirement "The API scales with HTTP traffic and down to zero" is replaced by "The API scales with HTTP traffic and keeps a warm replica", with a configurable minimum of 1 and a configurable maximum of 3.

## Impact

- `server/infrastructure/main.bicep`, `main.bicepparam`, `resources.bicep`, `modules/api.bicep`, `server/infrastructure/README.md`.
- Azure cost: one replica runs all the time.
- The app-side change `app-score-server-cold-start-timeout` (longer client timeouts) stays useful for restarts and scale-out, but is no longer the main defence against cold starts.
