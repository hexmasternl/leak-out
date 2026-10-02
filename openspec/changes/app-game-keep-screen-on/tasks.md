# Tasks

Tracking issue: [#52 When in game (the game screen is open) always on](https://github.com/hexmasternl/leak-out/issues/52).

## 1. Keep the screen on

- [ ] 1.1 Add `KeepScreenOn()` in `app/.../ui/components/KeepScreenOn.kt`: a `DisposableEffect` on `LocalView.current` that sets `keepScreenOn = true` and resets it to `false` on dispose, with KDoc on its scope (design decision 1); verify `./gradlew :app:testDebugUnitTest` compiles and passes
- [ ] 1.2 Call `KeepScreenOn()` at the top of `GameScreen` so it covers the whole screen and its dialogs (design decisions 2 and 3); verify the manifest gains no permission and no other screen calls it (`grep -rn KeepScreenOn app/src/main`)

## 2. Instrumented tests

- [ ] 2.1 Add `KeepScreenOnTest` (androidTest, `ui/game`) with `createAndroidComposeRule<ComponentActivity>` hosting `GameScreen` behind a toggle: assert `FLAG_KEEP_SCREEN_ON` is set on `activity.window.attributes.flags` while playing, still set after `GameCommand.Pause` with the pause dialog shown, still set on the level lost dialog, and cleared once the toggle removes the game screen (use `waitUntil`); run it on the emulator and verify it passes
- [ ] 2.2 Extend `MainActivityTest`: the flag is not set on the main menu, is set after starting a level, and is cleared after exiting back to the main menu; run it on the emulator and verify it passes

## 3. Documentation

- [ ] 3.1 Add one line to `docs/project-plan.md` (game screen section): the display stays on while the game screen is open, through `keepScreenOn`, without a wake lock; verify it matches the spec

## 4. Regression

- [ ] 4.1 Run `./gradlew :core:test :app:testDebugUnitTest` from `src/`; verify both pass
- [ ] 4.2 Run the affected instrumented tests (`KeepScreenOnTest`, `MainActivityTest`, `GameScreenTest`); verify they pass and note the result on issue #52
