# Proposal

Tracking issue: [#44 Start and end position re-design](https://github.com/hexmasternl/leak-out/issues/44).

## Why

On the playing field the start and the finish are hard to tell apart and do not look like part of the pipe network. Both are drawn by the same `drawEndpoint`: a tinted tile, a flat `steel.300` bar from the centre to the open side, a filled disc and a dark dot, differing only in colour (brass vs. the old slim-palette `SuccessGreen` `#43A047`). The bar does not match the pipe art the sections are drawn with, so a section next to the start or the finish does not visibly connect to it. Nothing says "the liquid comes out here" or "get it here".

The design system (`docs/design-system.md` §6.4, mock-up in `docs/design-system.html`) already describes a much clearer pair: a brass **start valve** wheel with a chevron pointing out of its open side, and a **finish drain**, a pulsing `success.500` ring. The only weak spot is the drain's grate of three bars, which reads as "a drain" but not as "the finish". Filling the ring with a **checker pattern** instead of the grate gives the player two unmistakable markers: a valve wheel where the flow starts, a checkered finish where it has to end.

## What Changes

- **Design system first** (`docs/design-system.md` §6.4, §7.1, §7.2, §7.4, §8 and the `docs/design-system.html` gallery):
  - The finish drain's grate is replaced by a **checker pattern** filling the disc inside the pulsing `success.500` ring: a flat 6 × 6 grid of `text.primary` and `steel.900` squares clipped to the disc, with no flag or pole. The drain is renamed **Checkered finish** where the name is shown to people (the gallery caption) but keeps "finish drain" as its technical name.
  - Both endpoints' **stubs are made of the real pipe art**: the half of the straight section (`H` or `V`) between the centre of the cell and the open side, with its body, bore, flange and glass, so a section placed next to the start or the finish joins it seamlessly, exactly like two sections join.
  - The cell itself has no tint: like a pipe cell, the endpoints are drawn on the empty-cell tile, so only the valve and the ring carry colour.
  - The liquid is visible as it leaves the valve: the start's stub fills from the centre to the open side while the liquid head is in the start cell.
  - Motion is specified completely: the valve's brass glow pulses on `motion.drift` while the countdown runs and stops (static glow at 60 %) once the liquid flows; the finish ring's success glow pulses on `motion.pulse` until the level is won, then the ring fills solid with the existing finish flash; reduced motion gives static glows.
  - `finish-checkered` joins the icon set in §8 with the same geometry, so other screens (onboarding, the level designer) can show the same finish symbol.
- **App complies with the design system**: `PlayingField` draws the start valve and the checkered finish as specified, replacing `drawEndpoint`. The finish uses `success.500` instead of the slim `SuccessGreen`.
- No `core` or level-format change: start and finish keep their position and open side, flow and win detection are unchanged.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `game-play-playing-field`: the start cell is drawn as a brass valve wheel with a chevron and a pipe stub out of its open side, the finish cell as a success ring filled with a checker pattern and a pipe stub into its open side; both stubs use the straight section's pipe art; the liquid is drawn in the start's stub; the valve and ring glows pulse and stop as specified.

## Impact

- `docs/design-system.md` §6.2 (endpoint cells), §6.4 (start valve, finish drain with checker pattern, stubs from pipe art), §7.1/§7.2/§7.4 (glow pulses, when they stop, reduced motion), §8 (`finish-checkered`); `docs/design-system.html` (`pipeSVG` `START` / `FINISH` and the gallery card).
- `app/ui/game/PlayingField.kt`: `drawEndpoint` replaced by start valve and checkered finish drawing; the start's liquid is drawn; glow pulses timed by the game clock.
- New `app/ui/game/Endpoints.kt` (or similar): the pure geometry of the valve, chevron, ring and checker pattern in cell units, and the pulse functions, unit tested.
- `app/ui/game/FieldLiquid.kt`: a fill for the start cell's stub.
- `app/ui/theme` colours: uses existing `Brass*`, `Success500`, `Steel*`, `SurfaceBase` tokens; adds `TextPrimary` if missing.
- Instrumented tests in `PlayingFieldTest` for the endpoint semantics; existing tests that find the start and finish by content description ("Start", "Finish") keep working.
