# Tasks

Tracking issue: [#110 Show the global top 10 of a level](https://github.com/hexmasternl/leak-out/issues/110).

## 1. Design system proposal (gate: no other group starts before 1.6 is done)

- [x] 1.1 Add a "Top 10" dialog to `docs/design-system.md` §5.6: opaque backdrop (pause gradient), level 3 `surface.base` panel, title "Top 10" in `display.lg`, eyebrow "LEVEL 7", rows with rank, display name (one line, ellipsized) and score, the own entry highlighted, the fixed title and Close button with a scrolling list, the Regular (one column) and Compact (two columns of 5) layouts, and the loading, empty, signed-out (with sign-in button) and error (with Try again) states; verify every token named exists in §2–§4
- [x] 1.2 Add the Top 10 button to the pause dialog (glass Secondary sharing Restart's row) and to the win and leak dialogs (Tertiary sharing Exit's row, Regular and Compact) in §5.6 and §5.1's "Use" column; verify the pause and Compact result dialogs still fit within 328 dp on 640 × 360 dp
- [x] 1.3 Add the dialog entry, the list's appearance and their reduced-motion variants to §7.2 and §7.4; verify durations use existing motion tokens
- [ ] 1.4 Add mock-ups of the top 10 dialog (list with own entry, loading, empty, signed out, error) and the updated pause and result dialogs to `docs/design-system.html`; verify the page renders in a browser with no layout break
- [ ] 1.5 Build static Compose `@Preview` mock-ups of the top 10 dialog states and the dialogs with the Top 10 button at 640 × 360 dp and 1280 × 800 dp, plus the list at 130 % font, and attach screenshots to issue #110; verify the previews render
- [ ] 1.6 Get the product owner's approval of the design on issue #110 and record any requested changes back into the design system, the specs and these tasks (including design decision 2, the during-play entry point); verify approval is noted on the issue before starting group 2

## 2. Score server client

- [ ] 2.1 Add `LevelLeaderboard` / `LeaderboardEntry` (position, display name, score, isCurrentPlayer) and `ScoreServerJson.leaderboard(body)` that ignores unknown fields and throws `Unavailable` on an unreadable body; verify JVM unit tests for a full list, an empty list, an unknown field and a broken body
- [ ] 2.2 Add `topScores(accessToken, level)` to `ScoreServerApi` and `HttpScoreServerApi` (`GET /api/levels/{level}/scores/top`, bearer token, same timeouts and error mapping as `submitScore`); verify JVM unit tests against a local HTTP test server for 200, 401, 404 and 500
- [ ] 2.3 Add `TopScoresLoader` returning `TopScoresState` (`Loading`, `Loaded`, `Empty`, `SignedOut`, `Failed`): signed out → `SignedOut` without a request, 401 → invalidate session and retry once, other failures → `Failed`; wire it in `AppContainer`; verify JVM unit tests with fake API, session and auth for every state and the single retry

## 3. Top 10 dialog

- [ ] 3.1 Build `TopScoresDialog` per the approved design: backdrop, panel, title and level, scrolling list with highlighted own entry, the four other states, Close, Try again and sign-in, entry motion and reduced motion; Back and Close call `onClose`, a backdrop tap does nothing; expose test tags; verify the `@Preview`s from 1.5 now use the real dialog
- [ ] 3.2 Add the screen-scoped state holder that loads on open and on Try again, starts the manual Play Games sign-in from the signed-out state and loads after a successful sign-in, and cancels on close; verify JVM unit tests with a fake loader
- [ ] 3.3 Add strings (title, rank/name/score content description, "your score", empty, signed out, error, Try again, Close, Top 10); verify `./gradlew :app:assembleDebug` succeeds

## 4. Entry points

- [ ] 4.1 Decide availability in `GameScreen` (library level and `ScoreServerConfig` enabled) and pass it to the three dialogs; verify a JVM unit test of the rule (library + configured, test play, blank URL)
- [ ] 4.2 Add the Top 10 button to `PauseDialog`; opening keeps the game paused and closing returns to the pause dialog; verify an instrumented test that the game time does not advance while the top 10 is open and that Close shows the pause dialog again
- [ ] 4.3 Add the Top 10 button to `LevelCompleteDialog` (enabled with the other buttons, both layouts) and `LevelLostDialog`; closing returns to the unchanged dialog; verify instrumented tests for opening and closing from both dialogs and that all buttons are fully visible in the Compact layout on 640 × 360 dp
- [ ] 4.4 Hide the Top 10 button for designer test plays and unconfigured builds; verify instrumented tests that no Top 10 button exists in those cases

## 5. Dialog behaviour tests

- [ ] 5.1 Instrumented tests of `TopScoresDialog` with a fake loader: 10 entries in order with the own entry highlighted, 3 entries, empty, signed out with sign-in, error then Try again, Back closes, backdrop tap does not; verify they pass
- [ ] 5.2 Instrumented accessibility and fit tests: each row is one semantics node with rank, name and score, the own row says "your score", Close is at least 48 dp, and on 640 × 360 dp at 130 % font Close is displayed and the last entry can be scrolled to; verify they pass

## 6. Regression

- [ ] 6.1 Run `./gradlew :core:test :app:testDebugUnitTest` and fix failures; verify both pass
- [ ] 6.2 Run the affected instrumented tests on the emulator (the new top 10 tests, `PauseDialog` tests, `LevelCompleteTest`, `LevelCompleteNewBestTest`, `LevelLostTest`); verify they pass
- [ ] 6.3 Open the top 10 from the pause, win and leak dialogs against the live score server on the emulator and post screenshots on issue #110; verify the comment links them
