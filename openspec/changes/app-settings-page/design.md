## Context

`MainActivity` switches between screens with a small `Screen` sealed interface (Welcome, Library, Game). The welcome screen's Settings button emits `onSettings`, which is still a TODO. The plan (`docs/project-plan.md` §2.10) keeps settings in DataStore (Preferences) and level progress in Room (`level_history`, behind `LevelHistoryRepository`). DataStore is not a dependency yet. Audio (`SoundPool`, `MediaPlayer`) and the haptic placement tick are Milestone 5 work and do not exist. The design system already defines the settings row (§5.8: 56 dp, `body.md` label, pill switch), the glass secondary and destructive button styles ("Reset progress"), and the sound, music and settings icons (§8).

## Goals / Non-Goals

**Goals:**
- A Settings screen reachable from the welcome screen, with a back button and system Back.
- A complete audio panel (two switches, two sliders) that shows stored values but is disabled.
- A working, persisted vibration switch.
- Reset progress with a confirm dialog that wipes level history and nothing else.
- An about section with the real version name and code.
- One `SettingsRepository` that the later audio and haptics changes read without changing its shape.

**Non-Goals:**
- Playing any sound or music, or vibrating. This change only stores the preferences.
- A settings entry in the pause dialog.
- An in-app reduced-motion or colour-blind option. Motion follows the system setting (design system §7.4).
- Language selection, accounts, cloud backup, licences screen.

## Decisions

**Persistence: DataStore Preferences behind an interface.** `SettingsRepository` exposes `val settings: Flow<Settings>` and suspend setters (`setSoundEnabled`, `setSoundVolume`, `setMusicEnabled`, `setMusicVolume`, `setVibrationEnabled`). `Settings` is an immutable data class with defaults: sound on at 0.8, music on at 0.6, vibration on. `DataStoreSettingsRepository` maps it to Preferences keys (`sound_enabled`, `sound_volume`, `music_enabled`, `music_volume`, `vibration_enabled`) in a file named `settings`. A missing key reads as its default, so first launch needs no setup step. Volumes are clamped to 0.0-1.0 on write and on read, so a corrupt value cannot break the screen. `InMemorySettingsRepository` mirrors `InMemoryLevelHistoryRepository` for tests and previews. *Alternatives:* adding a Room table (rejected: the plan already reserves DataStore for flags) and SharedPreferences (rejected: synchronous API, no Flow).

**Disabled audio controls still have setters.** The repository supports writing audio values today, but the screen passes `enabled = false` to all four audio controls and shows a "Coming soon" note in `text.muted` under the panel title. When audio lands, enabling the panel is a one-flag change (`AUDIO_AVAILABLE` in the settings UI) plus the audio engine. The controls are drawn with the design system's disabled treatment (40 % opacity) and get `disabled()` semantics, so TalkBack reads them as unavailable. *Alternative:* hide the panel (rejected: the user asked to build it complete).

**Vibration is stored only.** The switch writes `vibrationEnabled`. No `Vibrator` call is added, because there is nothing to vibrate yet. The haptic tick change reads the flag.

**Reset progress clears `level_history` only.** `LevelHistoryRepository` gains `suspend fun clearAll()`. Room implements it with `@Query("DELETE FROM level_history")` and the in-memory repository empties its map. Settings are deliberately kept, because a player who resets to replay the levels does not expect their vibration choice to change. The level library observes history (`observeAll`), so it shows every level locked except level 1 after a reset, and no extra signalling is needed. No schema change and no migration are needed.

**Confirm dialog.** The dialog has the title "Reset progress?", the body "All finished levels, highscores and stars are deleted. This cannot be undone.", a glass "Cancel" button and a destructive "Reset" button. Tapping outside and Back both cancel. Reset runs `clearAll()` and closes the dialog, and the summary in the progress panel updates from the history Flow to "No levels finished yet". When there is no progress, the Reset progress button is disabled so the dialog cannot be opened for nothing. A one-word confirmation is enough, because the button is already destructive-styled and the dialog states the consequence. Type-to-confirm would be too much for a puzzle game.

**State holder.** `SettingsViewModel(settingsRepository, historyRepository)` exposes one `StateFlow<SettingsUiState>`. The state combines the `Settings` with a `ProgressSummary(levelsFinished, starsEarned)` derived from `observeAll()`. The ViewModel also holds the event methods (`onVibrationChange`, `onResetConfirmed`). `SettingsScreen(state, appInfo, callbacks…)` is stateless apart from the dialog's open flag, so Compose tests drive it directly with fake state. This follows the `LevelLibraryViewModel` pattern.

**Version info is passed in.** `MainActivity` reads `packageManager.getPackageInfo(packageName, 0)` once into `AppInfo(versionName, versionCode)` and passes it to the screen. Shown as "Version 0.1.0 (1)". *Alternative:* enabling `BuildConfig` (rejected: a build-feature change only for two strings, and the screen would be harder to test).

**Layout.** The screen is landscape with a top bar holding the back icon button (48 dp) and the title "SETTINGS" in `display.lg`. Below it are two columns of level-1 panels:

```
┌───────────────────────────────────────────────────────────────┐
│  ◀  SETTINGS                                                  │
├──────────────────────────────┬────────────────────────────────┤
│ AUDIO          Coming soon   │ PROGRESS                       │
│ ♪ Sound effects      [━━○]   │ 12 levels finished · 31 ★      │
│   ───────●────────           │ [ RESET PROGRESS ]             │
│ ♫ Music              [━━○]   │                                │
│   ─────●──────────           │ ABOUT                          │
│ FEEL                         │ Leak Out                       │
│ 〰 Vibration          [●━━]   │ Version 0.1.0 (1)              │
│                              │ Made by HexMaster              │
│                              │ No account. Your data stays    │
│                              │ on this device.                │
└──────────────────────────────┴────────────────────────────────┘
```

Each column scrolls vertically on its own when content exceeds the height, which covers small screens and 130 % font scale. Rows follow §5.8, and each slider sits on a 48 dp tall row under its switch.

**Navigation.** `Screen.Settings` is added to `MainActivity`. `onSettings` sets it, and the back button and `BackHandler` return to `Screen.Welcome`. A navigation library is still not worth adding for this.

## Risks / Trade-offs

- [A disabled panel looks broken to players] → The "Coming soon" note sits next to the panel title, and the disabled controls still show sensible values.
- [Players expect the vibration switch to do something now] → Accepted for the short time until the haptic tick ships. The spec states the switch only stores a value.
- [Reset is irreversible] → The dialog states the consequence, Cancel is the default safe action, and the button is disabled with nothing to reset.
- [DataStore read on first composition flickers defaults] → The UI state starts as `null` (loading), and the panels render once the first `Settings` arrives. This is usually within one frame.
- [New dependency] → `androidx.datastore:datastore-preferences` is a standard AndroidX library, pinned exactly in `libs.versions.toml`. Use the latest stable release at implementation time.

## Open Questions

- The maker credit is shown as "Made by HexMaster". Confirm the exact wording during implementation if it should differ.
