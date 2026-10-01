## Why

The welcome screen has a Settings button that does nothing (`MainActivity` leaves `onSettings` as a TODO). Players need a place to turn vibration off, start over by wiping their progress and see which version they run. Audio is planned for Milestone 5. Building its panel and stored preferences now means the audio change only has to read them.

## What Changes

- New **Settings screen**, opened from the welcome screen's Settings button. It is landscape, with two panels and a back button, and Back returns to the welcome screen.
- **Audio panel**, complete but disabled: a "Sound effects" switch with a volume slider and a "Music" switch with a volume slider. The controls show their stored values but cannot be changed. A "Coming soon" note explains why. Audio itself is out of scope.
- **Feel panel**: a working "Vibration" switch that is stored on the device. Nothing vibrates yet; the haptic tick will read this setting when it is built.
- **Progress panel**: a summary of finished levels and stars, and a destructive "Reset progress" button. The button opens a confirm dialog and, once confirmed, deletes all level history. Settings are kept.
- **About panel**: the app name, version (name and code), the maker, and a privacy line saying no account is used and no data leaves the device.
- New `SettingsRepository` backed by Jetpack DataStore (Preferences). It holds `soundEnabled`, `soundVolume`, `musicEnabled`, `musicVolume` and `vibrationEnabled`, each with a default.
- `LevelHistoryRepository` gains a `clearAll()` operation.

## Capabilities

### New Capabilities
- `settings-screen`: the Settings screen, covering navigation to it and back, its layout, the disabled audio panel, the vibration switch, reset progress with confirmation, and the about section.
- `settings-persistence`: the stored settings, covering their keys, defaults, value ranges, and how they survive restarts and progress resets.

### Modified Capabilities
<!-- None: the welcome screen spec already requires the Settings button to emit its event; this change only gives that event a destination. -->

## Impact

- `app/`: a new `ui/settings/` package (screen, panels, confirm dialog), a new `settings/` package (repository interface, DataStore and in-memory implementations), `MainActivity` (a `Screen.Settings` destination and the `onSettings` wiring), and `history/` (`clearAll` on the repository, the DAO, Room and the in-memory implementations).
- Dependencies: adds `androidx.datastore:datastore-preferences`, pinned in `gradle/libs.versions.toml`.
- Docs: `docs/project-plan.md` §2.10 lists the extended settings keys.
- No changes to `core`.
