# Proposal

Tracked in [#110 Show the global top 10 of a level](https://github.com/hexmasternl/leak-out/issues/110).

## Why

Scores of won levels now reach the Leak Out score server (`app-game-post-scores-on-win`), but the player never sees them: there is no way to find out how a score compares to other players or what score to aim for. The score server already serves the 10 best scores of a level (`GET /api/levels/{level}/scores/top`, with the signed-in player's own entry marked), so the game only has to ask for it and show it.

## What Changes

- **Design system first, gated on approval.** Before any app code is written, the top 10 dialog is designed in the Leak Out design system (`docs/design-system.md` §5.6 Dialogs and sheets, §7.2 / §7.4 motion, and the mock-ups in `docs/design-system.html`): the panel, the ranked list (rank, player name, score, the player's own entry highlighted), the loading, empty, signed-out and error states, the **Top 10** button in the pause dialog and in both result dialogs, and how it all fits a 640 × 360 dp landscape screen. The design is posted on issue #110 and **implementation does not start until the product owner has approved it there**. Requested changes are written back into the design system and the specs first.
- **Top 10 dialog.** A modal dialog on the opaque pause backdrop (the `surface.abyss` → `surface.deep` gradient), so the game cannot be seen behind it. It shows the title, the level number and up to 10 entries from the highest score down, each with its rank, the player's display name and the score. The signed-in player's own entry is highlighted.
- **Where it opens.**
  - During play: from a new **Top 10** button in the pause dialog. The game is already paused there, so it stays frozen; closing the top 10 returns to the pause dialog.
  - After a win: from a **Top 10** button in the level complete dialog, once its buttons are enabled. Closing returns to the level complete dialog.
  - After a loss: from a **Top 10** button in the level lost dialog. Closing returns to the level lost dialog.
- **States.** While the scores load, the dialog shows a loading indicator. A level with no scores yet shows an empty message. When the player is not signed in to Play Games, the dialog says that signing in is needed to see the top 10 and offers the existing manual sign-in. When the scores cannot be loaded (offline, timeout, server error), it shows an error message with a **Try again** action.
- **Only for library levels.** The Top 10 button is not shown for a test play from the level designer, or when the score server is not configured in the build, matching score posting.
- **Score server client.** The existing `ScoreServerApi` gets a call for `GET /api/levels/{level}/scores/top`, using the same session handling as score posting (sign in again once on a 401).

## Capabilities

### New Capabilities
- `level-top-scores`: the global top 10 of a level: when the Top 10 button is available, loading the scores from the score server, the dialog's content and states, closing it, accessibility, and fitting a small landscape screen.

### Modified Capabilities
- `game-play-pause`: the pause dialog gets a **Top 10** button.
- `game-play-level-complete`: the level complete dialog gets a **Top 10** button.
- `game-play-level-lost`: the level lost dialog gets a **Top 10** button.

## Impact

- **Design system**: `docs/design-system.md` §5.6 (Top 10 dialog, the Top 10 button in the pause and result dialogs, the states), §7.2 / §7.4 (entry motion and reduced motion of the list); `docs/design-system.html` mock-ups. Must be approved on #110 before implementation.
- **App code** (`app/`): `scores/online/ScoreServerApi.kt` and `HttpScoreServerApi.kt` (top scores call and JSON parsing); a new top scores loader using `ScoreServerSession`; a new `ui/game/TopScoresDialog.kt` composable; `PauseDialog`, `LevelCompleteDialog`, `LevelLostDialog` and `GameScreen` (the button and dialog state); strings.
- **Server**: none; uses the existing `GET /api/levels/{level}/scores/top` of `leak-out-server`, which requires a signed-in player.
- **Depends on** `app-game-post-scores-on-win` (score server client, session and configuration).
- No `core` change, no new dependencies, no new permissions.
