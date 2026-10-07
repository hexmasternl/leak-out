# Design

## Context

`HttpScoreServerApi` gives each request three limits: a 10 s `connectTimeout`, a 10 s `readTimeout` (both from one `CONNECT_TIMEOUT_MS` constant), and an overall 15 s `withTimeout` around the whole call. The Azure Container Apps ingress accepts the TCP (and TLS) connection while the app has zero replicas and holds the request until one is ready. So during a cold start the connect succeeds and the wait falls on the read, which gives up at 10 s, before the 15 s overall limit. See proposal.md for the symptom.

## Goals / Non-Goals

**Goals:**
- The first request after the server went idle survives a cold start of about 15 s, with room to spare.
- An unreachable server (no network, DNS failure, refused connection) still fails within 10 s.

**Non-Goals:**
- Retrying failed requests automatically. The queue already resends on the next trigger, and the top 10 has Try again.
- Warming the server up ahead of time, or changing the container app's scaling (`minReplicas`).
- Any UI change. The top 10 dialog's loading state already covers a slow answer.

## Decisions

### 1. Separate connect and read timeouts: 10 s connect, 30 s read, 35 s overall
The connect timeout stays at 10 s because a cold start does not affect it, and a failed connect should be reported quickly. The read timeout goes to 30 s, about twice the observed cold start. The overall limit goes to 35 s, so it sits above the read timeout plus a normal connect and the request body. The read timeout then decides the cold-start case, and the overall limit only bounds a pathological call (a slow connect followed by a slow read).

*Alternative:* only raise the overall limit. Rejected: the 10 s read timeout would still fire first.
*Alternative:* drop the socket timeouts and rely on `withTimeout` alone. Rejected: `runInterruptible` cannot break a blocking socket read on every Android version, so the socket timeouts are the dependable bound.

### 2. Timeouts are constructor parameters
`HttpScoreServerApi` gets `connectTimeoutMs` and `readTimeoutMs` parameters next to the existing `timeoutMs`, defaulting to the production constants. The JVM tests can then prove the relationship at a small scale (a server that answers later than the old 10/15 ratio would allow, but within the read timeout) without sleeping for 15 s. A separate test pins the production defaults against the spec (read ≥ 30 s, overall ≤ 35 s, connect ≤ 10 s, read < overall).

## Risks / Trade-offs

- [A dead but connectable server makes the top 10 spin for up to 35 s] → The dialog shows a loading indicator and can be closed at any time. Score posting runs in the background and never blocks the game.
- [A cold start longer than 30 s still fails the first request] → The queue resends it later and the top 10 offers Try again, as today. The value is a single constant if it needs tuning.
- [`online-score-submission` is not archived yet] → Archive `app-game-post-scores-on-win` before archiving this change, so the MODIFIED delta has a main spec to apply to.
