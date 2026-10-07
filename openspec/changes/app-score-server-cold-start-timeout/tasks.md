# Tasks

## 1. Timeouts

- [x] 1.1 In `app/.../scores/online/HttpScoreServerApi.kt`, replace `CONNECT_TIMEOUT_MS` with `CONNECT_TIMEOUT_MS = 10_000` and `READ_TIMEOUT_MS = 30_000`, raise `TIMEOUT_MS` to `35_000L`, and add `connectTimeoutMs` / `readTimeoutMs` constructor parameters defaulting to them; update the KDoc. Verify that `./gradlew :app:testDebugUnitTest --tests "*HttpScoreServerApiTest"` still passes.
- [x] 1.2 Add JVM tests to `HttpScoreServerApiTest`: a server that answers after a delay longer than the connect timeout but shorter than the read timeout succeeds (scaled down, e.g. connect 200 ms, read 1 500 ms, overall 2 000 ms, answer after 800 ms); a server that answers after the read timeout fails as `Unavailable`; and the production defaults meet the spec (connect ≤ 10 s, read ≥ 30 s, overall ≤ 35 s, read < overall). Verify that `./gradlew :app:testDebugUnitTest` passes.

## 2. Docs

- [x] 2.1 Update the timeout values in `openspec/changes/app-game-post-scores-on-win/design.md` (decision 1) if that change is still unarchived, or note them in `docs/` where the score server is described. Verify with a grep for `15 s` / `10 s connect/read` in docs and specs.
- [ ] 2.2 Check that `openspec validate app-score-server-cold-start-timeout` passes once `app-game-post-scores-on-win` has been archived.
