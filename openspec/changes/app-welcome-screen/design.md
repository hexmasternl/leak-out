## Context

The repository contains only docs; there is no Android code. The project plan specifies Kotlin, Jetpack Compose (Material 3), min SDK 26, and a `ui/title/` package in the `app` module. The design system defines the palette (Hydro Blue, Brass Yellow, ink outlines), Fredoka for display text, and a 3-layer cartoon button recipe with a pressed state. The plan currently assumes portrait; this change makes the app landscape.

## Goals / Non-Goals

**Goals:**
- A welcome screen shown at launch with background art, a large centered title, and Play / Settings / Exit buttons below it.
- Landscape enforced app-wide.
- Reusable themed button and theme tokens that later screens can share.
- Screen is stateless and previewable; navigation is delegated via callbacks.

**Non-Goals:**
- The game, level select, and settings screens themselves (Play/Settings only emit callbacks).
- Audio, saved progress, or animated liquid simulation.
- Tablet/foldable-specific layouts beyond scaling.

## Decisions

- **Compose `WelcomeScreen(onPlay, onSettings, onExit)`** in `ui/title/`. Stateless; `MainActivity` wires callbacks (Exit calls `finishAffinity()`; Play/Settings are no-op stubs until those screens exist). Alternative: a ViewModel + navigation graph now — rejected as premature with a single destination.
- **Landscape via manifest**: `android:screenOrientation="sensorLandscape"` on `MainActivity`. Lets the device flip between both landscape sides while never showing portrait, and applies before the first frame (no rotation flicker). Alternative: `requestedOrientation` in code — rejected, it runs after launch and can flash portrait. Plain `landscape` was rejected because it blocks reverse-landscape.
- **Background drawn in Compose `Canvas`** (sky-blue gradient using `color-primary-bg`→`color-primary-soft`, stylized steel pipes with ink outlines, a few liquid bubbles/drops) rather than a bundled bitmap. Scales to any landscape aspect ratio without asset density variants, stays on-palette, and adds no binary assets. Alternative: PNG illustration — rejected for now (density/aspect handling), can replace the composable's internals later without changing its API.
- **Layout**: `Box` filling the screen; a centered `Column` with the title (Fredoka Bold, uppercase, outlined/shadowed text, ~48sp+ scaled to width) and below it a `Row`/`Column` of three buttons. In landscape height is scarce, so buttons are laid out in a horizontal row with fixed min touch target of 48dp; on very short heights the content scales down via `BoxWithConstraints`.
- **`CartoonButton` composable** in `ui/theme` or `ui/components`: 3px ink border, 6dp bottom offset shadow, inner shine; pressed state translates down 4dp and shrinks shadow to 2dp (via `interactionSource`). Play uses secondary (brass yellow), Settings primary (blue), Exit error-red variant.
- **Fonts**: bundle Fredoka in `res/font/` (OFL licence). Fallback to `FontFamily.Default` bold if loading fails.
- **System bars**: enable edge-to-edge and hide system bars (immersive) for the welcome screen so the art fills the display; respect display cutouts for button placement via safe-drawing insets.
- **Back button**: on the welcome screen back exits the app (default behavior), same as Exit.

## Risks / Trade-offs

- [Plan says portrait, game board may want portrait] → Landscape is an explicit product decision; update `docs/project-plan.md` and revisit the game screen layout in its own change.
- [Canvas-drawn art may look less rich than illustration] → Keep drawing isolated in `WelcomeBackground` so it can be swapped for an image asset.
- [Short landscape heights on small phones may clip title+buttons] → Scale via `BoxWithConstraints` and verify on a small (e.g. 640×360dp) preview.
- [Exit via `finishAffinity()` leaves process alive] → Acceptable on Android; no cleanup needed yet.
- [Repo has no Gradle project] → Tasks include minimal scaffolding of the `app` module; keep it aligned with the plan's version catalog.
