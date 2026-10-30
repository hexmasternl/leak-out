## Why

Every level from the level library has a global Top 10, and the score is built from points per section, a big bonus for the second pass through a cross and a penalty for unused sections. Players do not find this out. The Top 10 is only a button in the pause, level complete and level lost dialogs, and the scoring rules are only hinted at in the cross and reservoir tips. Players therefore settle for the shortest route to the finish instead of building a longer, more sophisticated pipeline that earns more points and a better place on the leaderboard.

Tracked in [#158](https://github.com/hexmasternl/leak-out/issues/158).

## What Changes

- A **How scoring works** dialog explains the points per section type (straight 10, corner 15, reservoir 25, cross 10 + 50 for the second pass), the 3-point penalty per unused section, how stars follow from the score, and that every library level has a global Top 10 where only the player's best score counts. It opens from a new **Scoring** button in the pause dialog and from a small help button next to the score rows of the level complete dialog.
- The first time the player wins a level from the level library, the **How scoring works** dialog is shown once, after the score tally of the level complete dialog is done, as a one-time tip.
- When the top 10 is available, the level complete dialog shows the player's **leaderboard place** for the level ("You are #23 on level 7"), loaded with `GET /api/levels/{level}/scores/around-me` once the score has been sent. When the place cannot be loaded, the line is left out; it never blocks the dialog.
- When the top 10 is available, the level complete dialog shows a short line below the score rows inviting the player to reach the Top 10 ("Longer routes score more. Can you reach the Top 10?"), and the Top 10 button gets the highlighted style.

## Capabilities

### New Capabilities
- `scoring-guide`: the How scoring works dialog, its content, where it opens from (including the new Scoring button in the pause dialog) and the one-time tip after the first win.

### Modified Capabilities
- `game-play-level-complete`: adds the leaderboard place line, the Top 10 invitation and the scoring help button.
## Impact

- **App code** (`app/`): a new `ScoringGuideDialog` composable; a Scoring button in the pause dialog; the level complete dialog gets the place line, the invitation and the help button; the score server client gets an `around-me` call; a "scoring guide seen" flag in the settings DataStore.
- **Core** (`core/`): the point values shown in the guide are read from the scoring rules, not duplicated in UI strings.
- **Strings**: new translatable strings in every supported language.
- **Design system**: entries for the scoring guide dialog, the place line and the invitation.
- **Server**: none; uses the existing `around-me` endpoint.
