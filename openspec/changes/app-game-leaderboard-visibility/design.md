## Context

The scoring rules live in `game-play-points` and `game-play-final-score`; the Top 10 lives in `level-top-scores` and opens from the pause, level complete and level lost dialogs. The server already offers `GET /api/levels/{level}/scores/around-me` (`server-level-leaderboard`), which returns the caller's position. Issue #158 asks to make the leaderboard and the way points add up more prominent, and to encourage more sophisticated pipelines.

## Goals / Non-Goals

**Goals:**
- Explain the scoring rules in one place the player can always reach.
- Tell every new player once, at the moment it matters: their first win.
- Make the leaderboard visible right after a win, with the player's own place.

**Non-Goals:**
- Changing the point values, the penalty or the star thresholds.
- A leaderboard screen outside a level, or a ranking over all levels.
- Website changes.

## Decisions

- **Guide as a dialog, not an intro tour step.** The intro tour runs before the first level and is already long; scoring only makes sense once the player has seen points come in. Showing it after the first win's tally reaches the player while they look at their score.
- **Point values come from `core`.** The guide formats the values from the scoring rules, so it cannot drift from the game.
- **Place from `around-me`.** The endpoint already returns the caller's position, so no server change is needed. It is requested after the score submission for the win succeeded, so the place includes the new score. On any failure the line is left out silently.
- **No new navigation.** Everything is reachable from existing dialogs.

## Risks / Trade-offs

- [The place line adds a network call per win] → only when signed in and the top 10 is available; left out on failure, never retried.
- [The first-win guide delays Next Level] → it appears only once, after the tally, and closes with one tap.

## Open Questions

- Should the level library also show the player's place or a Top 10 badge on each level tile?
- Should the app point players to the public website leaderboard?
- Should the level complete dialog say "New personal best!" when the total beats the player's previous best on the level?
