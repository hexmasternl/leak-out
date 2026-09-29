## Why

The app has no entry point yet. Players need a first screen that sets the tone of the game and gives access to the main flows (play, settings, exit). Establishing it now also fixes the app-wide screen orientation and the first use of the design system in the Android app.

## What Changes

- Add a welcome screen as the app's launch screen (Jetpack Compose, `ui/title/`).
- The screen shows a full-bleed, illustrated background (cartoon pipes/liquid in the Leak Out style) with the app title "LEAK OUT" large and centered.
- Below the title, three menu buttons are shown: **Play**, **Settings**, **Exit**.
- Force the whole app into landscape orientation (`sensorLandscape`, so both landscape directions work).
- **Play** and **Settings** raise navigation events handled by the host (destinations do not exist yet; they are stubbed). **Exit** closes the app.
- Buttons follow the design system (3-layer cartoon recipe, press animation, Fredoka display font).
- **BREAKING** (against `docs/project-plan.md`): the plan describes portrait orientation for the board; this change makes landscape the app-wide orientation. The plan/game-screen layout must be revisited when the game screen is built.

## Capabilities

### New Capabilities
- `welcome-screen`: The launch screen: background visual, centered title, and the Play/Settings/Exit menu with their behavior.
- `landscape-orientation`: The app is always displayed in landscape and never rotates to portrait.

### Modified Capabilities
<!-- None: no existing specs. -->

## Impact

- New Android `app` module scaffolding as needed (Compose theme, `MainActivity`, manifest) since the repository has no code yet.
- New code: `ui/theme/` (colors, typography from the design system), `ui/title/WelcomeScreen`, background asset(s).
- `AndroidManifest.xml`: `android:screenOrientation="sensorLandscape"` on `MainActivity`.
- Assets: Fredoka font, background illustration (drawn in Compose or a bundled vector/raster).
- Docs: `docs/project-plan.md` orientation statements (sections 1.2, 1.11) become outdated.
