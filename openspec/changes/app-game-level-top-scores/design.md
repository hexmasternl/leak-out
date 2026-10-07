# Design

## Context

- `app-game-post-scores-on-win` added the score server client in `app/scores/online`: `ScoreServerApi` / `HttpScoreServerApi` (`HttpURLConnection` + kotlinx.serialization), `ScoreServerSession` (access token kept in memory, signs in with a Play Games server auth code, `invalidate()` on 401), `ScoreServerConfig` (blank URL or client id turns it off) and `ScoreSubmitter`.
- The score server (`leak-out-server`) already serves `GET /api/levels/{level}/scores/top`. It requires a signed-in player and returns `{ level, entries: [{ position, displayName, score, isCurrentPlayer }] }`, at most 10 entries, highest first. The level is the same level number that score posting sends.
- `PlayGamesAuth` knows whether the player is signed in and offers the manual sign-in used by the achievements screen.
- The pause dialog sits on the opaque `surface.abyss` → `surface.deep` backdrop; the level complete and level lost dialogs sit over the see-through `surface.scrim` and switch between a Regular and a Compact layout (`ResultDialogLayout`) to fit 360 dp high screens.
- `docs/design-system.md` §5.6 describes every dialog the game shows. There is no top 10 dialog and no Top 10 button in it yet.

## Goals / Non-Goals

**Goals:**
- Design the top 10 dialog and its entry points in the design system, and get that design approved on #110 before building it.
- Show the global top 10 of the level from the pause dialog, the level complete dialog and the level lost dialog.
- Reuse the existing score server client, session and configuration.

**Non-Goals:**
- A leaderboard screen outside the game (level select, main menu).
- The "around me" view (`GET /api/levels/{level}/scores/around-me`), friends or time-based leaderboards.
- Caching scores on the device or showing them offline.
- Play Games Services leaderboards.

## Decisions

1. **Design first, behind an approval gate.** Task group 1 writes the top 10 dialog and the Top 10 buttons into `docs/design-system.md` and the `docs/design-system.html` mock-ups, renders Compose `@Preview` mock-ups at 640 × 360 dp and 1280 × 800 dp (and at a 130 % font scale), and posts them on #110. No other task group starts until the product owner has approved the design there. Requested changes go back into the design system and these specs before implementation. Earlier dialog work (#38, #40, #44, #51) followed the same gate.

2. **During play, the top 10 opens from the pause dialog, not from a HUD button.** The issue asks for the game to pause while the top 10 is shown, and the pause dialog already freezes the game and hides the field. A Top 10 button there (glass Secondary, between Restart and the separator) adds no new pause path, keeps the crowded landscape HUD unchanged, and closing returns to the pause dialog, from which the player chooses to resume. *Alternative:* a trophy button in the HUD that pauses and opens the top 10 directly. It is one tap shorter, but it needs room in the HUD and a second way to pause and resume. The design review on #110 may still choose this; it then changes task 1.2 and the `game-play-pause` delta.

3. **In the result dialogs, Top 10 is a Tertiary button.** Next Level / Restart stay the main actions. Top 10 is added as a Tertiary button next to Exit (Regular layout: in one row with Exit; Compact layout: in the right-hand button column). The exact placement is part of the design to be approved and must keep the Compact layout within 328 dp on 640 × 360 dp.

4. **One dialog on the opaque backdrop, over whichever dialog opened it.** `TopScoresDialog` draws its own full-screen backdrop (the pause gradient), so the result dialog under it is fully hidden, as the issue asks. The opening dialog stays composed underneath with its state (tally done, buttons enabled), so closing simply returns to it. `GameScreen` holds a `showTopScores` flag; Back closes the top 10 first.

5. **Loading lives in a small `TopScoresLoader`, separate from the submitter.** `ScoreServerApi` gets `suspend fun topScores(accessToken: String, level: Int): LevelLeaderboard`, with JSON parsing in `ScoreServerJson` so it can be unit tested. The loader checks sign-in, gets a token from `ScoreServerSession`, calls the server, and on `Unauthorized` invalidates the session and retries once. It returns a sealed `TopScoresState`: `Loading`, `Loaded(entries)`, `Empty`, `SignedOut`, `Failed`. A screen-scoped state holder runs it in a coroutine when the dialog opens and on Try again; closing the dialog cancels the load. Nothing is stored.

6. **Signed-out players get the sign-in, not an empty list.** The server needs a signed-in player, so without Play Games the dialog shows the signed-out state with a sign-in button that uses `PlayGamesAuth`'s manual sign-in; once signed in it loads. When Play Games is not available on the device, the sign-in button is not shown and the message stays.

7. **Availability is decided once per game screen.** The Top 10 button is shown when the level comes from the level library and `ScoreServerConfig` is enabled, the same rule score posting uses, so a designer test play and a build without a score server never show a button that cannot work. Being offline or signed out does not hide the button; the dialog explains those states instead.

8. **The list scrolls, the frame does not.** The title row (title, level) and the Close button are fixed; the 10 rows are in a `LazyColumn` so the dialog fits 360 dp high screens and large fonts. Each row merges its semantics into one item ("Rank 2, Ada, 1450 points"; the own entry adds "your score").

## Risks / Trade-offs

- **Display names come from the server.** They can be long or contain any characters; rows ellipsize the name on one line. Content moderation is the server's concern.
- **Extra network use and latency.** One small GET per opening. A slow server shows the loading state; the HTTP timeouts of `HttpScoreServerApi` bound it, then the error state with Try again.
- **Score just posted may not show yet.** After a win the top 10 can be opened while the score is still being sent (the status dot is blinking). The dialog shows whatever the server has at that moment; the player can close and reopen it. A refresh when the post completes is not in scope.
- **The design may change the entry point.** If the approval on #110 moves the during-play entry to the HUD (decision 2), the pause dialog delta and tasks for it are replaced before implementation starts.
