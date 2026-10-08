# Design

## Context

See proposal.md for the motivation. `LevelScore.AchievedAt` (column `level_scores.achieved_at`, `timestamp with time zone`, `NOT NULL` since the scores database's initial migration) is set by `SubmitScoreCommandHandler` from the server's `IClock` when a submission becomes the player's best score, and is left alone for equal or lower scores. It is already the leaderboard tie-breaker. So the value the issue asks for exists; it is just not returned by `RankedLevelScore` or the public DTO.

The public top scores are cached in memory for 30 minutes per level, and the website runs in the visitor's browser in five languages (en, nl, de, fr, es).

## Goals / Non-Goals

**Goals:**
- Return `achievedAt` on the public top scores only.
- Show it on the website as localized relative time, computed in the browser.

**Non-Goals:**
- A new column or a migration (see Decisions).
- Showing dates in the app, or adding them to the app's endpoints.
- Recording every submission: the moment belongs to the best score, not to the last submission.

## Decisions

- **Reuse `achieved_at` instead of adding a "received at" column.** It already holds exactly "the moment the score was received by the server" (server clock, not the client's) and is `NOT NULL`. The issue's migration instruction (fill existing rows with UTC now, make it mandatory) is therefore already met, and better: existing rows keep their real moments instead of all showing "now". Alternative considered: a second column backfilled with `now()`; rejected because it duplicates data and would make every existing score look brand new.
- **Add `AchievedAt` to `RankedLevelScore`, keep the app DTOs unchanged.** `GetRankedAsync` already orders by `achieved_at`, so selecting it costs nothing. `LeaderboardEntryDto` (app) stays without it, and an endpoint test asserts the app JSON has no `achievedAt`.
- **JSON shape:** `achievedAt` as an ISO 8601 string with offset in UTC (`2026-10-01T14:30:00+00:00`), System.Text.Json's default for `DateTimeOffset`.
- **Relative time in the browser with `Intl.RelativeTimeFormat(locale, { numeric: "auto" })`.** The browser translates it for every page language, so the only new text is the column header. Computing it client-side from the absolute moment keeps it correct despite the 30-minute server cache. Units: years (≥ 365 days), months (≥ 30 days), weeks (≥ 7 days), days, hours, minutes, otherwise `format(0, "second")` ("now"). A pure function `relativeTime(achievedAt, now, locale)` is exported so tests use a fixed `now`.
- **Full date via `<time datetime="…" title="…">`** with `Intl.DateTimeFormat(locale, { dateStyle: "medium", timeStyle: "short" })`, so it shows on hover and is available to assistive technology.

## Risks / Trade-offs

- [Server and browser clocks differ, giving a moment in the future] → Clamp negative ages to "now".
- [The current website receives the new field] → `parseTopScores` maps known fields only, so the extra field is ignored; server and website can deploy in either order.
- [Month and year lengths are approximate] → Acceptable for "n months ago"; the exact date is on hover.

## Migration Plan

No schema migration. Deploy the server (additive response field), then the website. Either side can be rolled back on its own.

## Open Questions

- Should the "when" column be hidden on narrow phone screens if the table no longer fits 360 px, or should the table scroll horizontally? Decide during implementation from how it looks.
