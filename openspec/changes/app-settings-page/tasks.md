## 1. Settings persistence

- [x] 1.1 Add `androidx.datastore:datastore-preferences` (latest stable, pinned exactly) to `gradle/libs.versions.toml` and the app module
- [x] 1.2 Add the `Settings` data class (`soundEnabled`, `soundVolume`, `musicEnabled`, `musicVolume`, `vibrationEnabled`) with the defaults on, 0.8, on, 0.6, on
- [x] 1.3 Define the `SettingsRepository` interface: `settings: Flow<Settings>` and one suspend setter per field
- [x] 1.4 Implement `DataStoreSettingsRepository` (file `settings`, keys `sound_enabled`, `sound_volume`, `music_enabled`, `music_volume`, `vibration_enabled`; missing keys read as defaults; volumes clamped to 0.0-1.0 on write and read)
- [x] 1.5 Add `InMemorySettingsRepository` for tests and previews, with the same clamping
- [ ] 1.6 Provide a single DataStore-backed instance to the app from `MainActivity`

## 2. Clearing level history

- [x] 2.1 Add `@Query("DELETE FROM level_history") suspend fun deleteAll()` to `LevelHistoryDao`
- [x] 2.2 Add `suspend fun clearAll()` to `LevelHistoryRepository` and implement it in `RoomLevelHistoryRepository` and `InMemoryLevelHistoryRepository`

## 3. Settings state

- [ ] 3.1 Add `AppInfo(versionName, versionCode)`, `ProgressSummary(levelsFinished, starsEarned)` and `SettingsUiState`
- [ ] 3.2 Implement `SettingsViewModel` combining `SettingsRepository.settings` with `LevelHistoryRepository.observeAll()` into a `StateFlow<SettingsUiState?>` (null while loading), with `onVibrationChange` and `onResetConfirmed`

## 4. Settings screen UI

- [ ] 4.1 Create `ui/settings/SettingsScreen.kt`: a top bar with a 48 dp back icon button and the title "SETTINGS", two independently scrolling columns, and a `BackHandler` to leave
- [ ] 4.2 Build a reusable settings row (design system §5.8: 56 dp, `body.md` label, leading icon, trailing pill switch) and a 48 dp slider row
- [ ] 4.3 Build the Audio panel: Sound effects switch and slider, Music switch and slider, all disabled through an `AUDIO_AVAILABLE = false` flag with 40 % opacity and disabled semantics, and the "Coming soon" note
- [ ] 4.4 Build the Feel panel with a working Vibration switch
- [ ] 4.5 Build the Progress panel: the summary ("N levels finished · M ★" or "No levels finished yet") and the destructive "Reset progress" button, disabled when the history is empty
- [ ] 4.6 Build the reset confirm dialog: "Reset progress?", the consequence body, glass "Cancel" and destructive "Reset"; outside taps and Back cancel
- [ ] 4.7 Build the About panel: "Leak Out", "Version <name> (<code>)", "Made by HexMaster" and the privacy line
- [ ] 4.8 Add Compose previews for the screen with progress, without progress and with the dialog open

## 5. Navigation

- [ ] 5.1 Add `Screen.Settings` to `MainActivity`, set it from the welcome screen's `onSettings`, and return to `Screen.Welcome` from the screen's back action
- [ ] 5.2 Read `AppInfo` from `packageManager.getPackageInfo` in `MainActivity` and pass it to the Settings screen

## 6. Tests

- [x] 6.1 JVM test `Settings` defaults and `InMemorySettingsRepository`: setters, observed changes, volume clamping above and below range
- [x] 6.2 JVM test `InMemoryLevelHistoryRepository.clearAll`: records removed, observers get an empty list
- [ ] 6.3 JVM test `SettingsViewModel`: the progress summary from the history, the vibration change written, reset clearing the history but not the settings
- [ ] 6.4 Instrumented test `DataStoreSettingsRepository`: defaults on an empty file, values persist across a new repository instance on the same file, and clamping of out-of-range stored values
- [ ] 6.5 Instrumented test `LevelHistoryDao.deleteAll` on an in-memory Room database
- [ ] 6.6 Compose UI test: panels and titles shown, audio controls disabled and unchanged on tap, "Coming soon" shown, vibration toggles, reset button disabled with empty history, the dialog opens and cancels by button and Back, confirming clears the summary, and the About version text
- [ ] 6.7 Compose UI test for navigation: welcome Settings opens the screen, and the back button and system Back return to the welcome screen
- [ ] 6.8 Run `:app:testDebugUnitTest` and the affected instrumented tests, and record the results

## 7. Docs

- [ ] 7.1 Update `docs/project-plan.md` §2.10 with the full settings key list and defaults, and §1.11 with the Settings screen
