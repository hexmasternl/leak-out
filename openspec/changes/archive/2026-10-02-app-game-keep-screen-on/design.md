# Design

## Context

The app is a single activity (`MainActivity`) that swaps Compose screens with a `Screen` state (`Welcome`, `Library`, `Settings`, `Game`). `GameScreen` is composed exactly while a level is open, whatever its state: playing, paused (the pause dialog), the leak and win effects, and the result dialogs. The pause, lost and complete dialogs are Compose `Dialog`s with their own window on top of the activity window, which stays visible behind them. On `ON_STOP` the game screen already pauses a running level.

## Goals / Non-Goals

**Goals:**
- Display on for the whole lifetime of the game screen, including its dialogs.
- Normal timeout restored the moment the game screen leaves the composition.

**Non-Goals:**
- A setting to turn this off (can be added later if players ask for it).
- Keeping the screen on in the main menu, level library, settings or the debug Pipe Lab.
- Keeping the device awake in the background (no wake lock).
- Auto-dimming after a long pause.

## Decisions

1. **`View.keepScreenOn` on the Compose host view, scoped by a `DisposableEffect`.** A `KeepScreenOn()` composable reads `LocalView.current`, sets `keepScreenOn = true` and resets it to `false` in `onDispose`. `GameScreen` calls it once at the top. Android turns a visible view's `keepScreenOn` into the window's `FLAG_KEEP_SCREEN_ON`, so the request is tied to the composition: leaving the screen, finishing the activity or a configuration change cleans up without extra code.
   - *Alternative:* `window.addFlags(FLAG_KEEP_SCREEN_ON)` in `MainActivity` when `screen is Screen.Game`. Works, but spreads the rule over the activity and needs manual clearing on every navigation path. Rejected.
   - *Alternative:* `android:keepScreenOn` in a layout/theme, or a `PowerManager` wake lock. The first applies to every screen; the second needs the `WAKE_LOCK` permission and can leak into the background. Rejected.

2. **Scope is "game screen open", not "level running".** The issue asks for playing and paused; the end effects and result dialogs sit in between, and a player reading their score should not have the screen go dark either. One rule for the whole screen is simpler and leaves no gaps between states.

3. **Dialogs need nothing extra.** A Compose `Dialog` is a separate window, but the activity window behind it is still visible and still carries the flag, and Android keeps the screen on while any visible window does. The instrumented test covers the pause and lost dialogs to prove it.

4. **Tests are instrumented.** The behaviour lives in the Android window, which JVM tests cannot see (the project has no Robolectric). The tests check that a view in the hosting activity's hierarchy has `keepScreenOn` set, and wait for it to be set or cleared with `waitUntil`. They do not read `window.attributes.flags`: the view root adds `FLAG_KEEP_SCREEN_ON` to its own copy of the layout params, so `window.attributes` never shows it. A one-off probe on a device confirmed that the window manager's flag (`dumpsys window`) tracks the view check exactly. `KeepScreenOn` has no logic of its own to unit test; `GameScreen`'s logic stays covered by the existing JVM tests.

## Risks / Trade-offs

- [A player leaves the game paused on the table and the screen stays on until they return] → This is what the issue asks for; the screen still turns off with the power button or when the app goes to the background, and the system timeout returns as soon as they exit to the menu. A later change can add an idle limit if needed.
- [Battery use goes up during long sessions] → Limited to the game screen, which is exactly when the player is looking at it.
- [Another screen later also sets `keepScreenOn` on the same view] → The `onDispose` reset would clear it. Only one screen is composed at a time, so this cannot happen today; the composable's KDoc notes it.
