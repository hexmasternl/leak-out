# Proposal

## Why

The score server runs on an Azure Container App that scales to zero when idle, and waking it up can take about 15 seconds. The ingress accepts the connection straight away and holds the request while a replica starts, so the app's 10 second read timeout cuts the call off first. The first request after the server has gone idle therefore often fails, and a retry a moment later succeeds. Players see the score dot fail and the top 10 show its error state for no real reason.

## What Changes

- A request to the score server waits up to 30 seconds for the server's answer once connected (was 10 s), and gives up after at most 35 seconds in total (was 15 s). Connecting still gives up after 10 seconds, so an unreachable server fails as fast as before.
- The per-request limit in `online-score-submission` goes from 15 to 35 seconds, and a scenario covers a server that is starting up.
- The top 10 dialog keeps showing its loading indicator while it waits; no UI changes.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `online-score-submission`: the requirement "A failed send changes nothing else" raises the per-request limit from 15 to 35 seconds and requires the app to wait long enough for a server that is starting up.

## Impact

- `app/src/main/kotlin/nl/hexmaster/leakout/scores/online/HttpScoreServerApi.kt`: timeout values, and the read timeout made injectable for tests.
- `app/src/test/kotlin/nl/hexmaster/leakout/scores/online/HttpScoreServerApiTest.kt`: new tests.
- Affects every score server call: creating a session, submitting a score and loading the top 10.
- `online-score-submission` is still in the unarchived change `app-game-post-scores-on-win`. That change must be archived before this one.
