# Design

## Context

- `PlayingField.drawCell` draws `Cell.Start` and `Cell.Finish` with one `drawEndpoint(rect, openSide, color)`: an 18 % tint of the colour over the tile, a `Steel300` butt line 0.22 c wide from the centre to the open edge, a disc of radius 0.26 c and a `Steel900` dot of 0.10 c. Start uses `Brass500`, finish the slim `SuccessGreen` (`#43A047`), not the design system's `success.500` (`#3DDC97`).
- Placed sections are drawn from SVG assets (`app/src/main/assets/pipes/<id>.svg` plus `<id>-glass.svg`) through `PipeArtLibrary.prepared(section)` and `drawPipe(art, rect, fills, scratch)`, with the liquid drawn along the asset's channels between its `under` and `over` groups.
- The flow starts in the start cell: `FlowSimulation.startFlow` puts the head at the start with entry `start.openSide.opposite()` on a single channel, so the head runs through the whole start cell before it leaves through the open side. `FieldLiquid.fills` only produces fills for `Cell.Pipe`, so today no liquid is ever drawn in the start.
- The win is detected the moment the head enters the finish, so the head never runs through the finish cell. The finish flash (`finishFlashFrame`, `drawFinishRing`) is drawn over the finish tile while `winAgeMs` is set.
- The design system (§6.4 and the `pipeSVG` `START` / `FINISH` mock-ups) already describes the valve and the drain; only the grate, the stub material, the cell tint and the glow lifecycle are open.

## Goals / Non-Goals

**Goals:**
- Start and finish are recognisable at first sight, at every zoom, and visibly connect to the sections placed against them.
- The design is written down in the design system before the app follows it, and the app matches it exactly.

**Non-Goals:**
- Any change to the level format, the flow, the win or leak rules.
- Endpoints with more than one open side, or endpoints in the stack.
- Liquid in the finish cell (the head never travels through it).
- A new SVG asset format for endpoints (see decision 2).

## Decisions

### 1. Checker pattern instead of a grate
The finish keeps the design system's ring (radius 0.24 c, stroke 0.05 c `success.500`, inner radius 0.215 c) and replaces the three-bar grate by filling the whole disc inside the ring with a checker pattern, in cell units *c* relative to the cell centre:
- **Checker**: a 6 × 6 grid of squares 0.43 c / 6 ≈ 0.072 c wide, spanning the ring's inner diameter and aligned on the cell centre (the centre is a corner shared by four squares). Squares alternate `text.primary` (`#F2F7FB`) and `steel.900` (`#22303F`); the grid's top-left square is light. The pattern is flat (no wave) and clipped to the inner disc.
- **Ring**: drawn on top of the checker, covering the clipped squares at the edge. No separate disc fill shows.
The same geometry at 24 dp becomes the `finish-checkered` icon in §8.
*Alternatives considered*: a checkered flag on a pole inside the ring (rejected by the product owner on issue #44: the finish is just the checker pattern in the pulsing ring), and a pattern that moves over time (the ring already pulses; two motions on one tile compete).

### 2. Stubs drawn from the straight section's art
The stub is drawn by clipping the existing `H` (open left/right) or `V` (open top/bottom) art to the half of the cell between the centre and the open side and calling `drawPipe` for it, so body, shadow, bore, specular, flange and glass are pixel-identical to a placed straight. The valve wheel or ring covers the cut end at the centre (radius 0.24 c > half the 0.36 c body width).
*Alternatives considered*: new `start.svg` / `finish.svg` assets rotated per open side (the specular and glass highlight would rotate away from the top-left light), or one asset per open side (eight new files that duplicate `h.svg` and `v.svg`). Clipping reuses what exists and cannot drift.

### 3. Valve wheel and chevron
As in the design-system mock-up, in cell units: wheel disc radius 0.24 c with a `brass.300 → brass.500` radial gradient, rim stroke 0.03 c `brass.700`, two spokes (a cross) 0.045 c `brass.900` with round caps from −0.20 c to +0.20 c, hub radius 0.08 c `brass.700` with a 0.02 c `brass.900` edge, and a small `brass.100` highlight at 45 % at (−0.08 c, −0.10 c). The chevron is a 0.04 c `brass.300` round-capped stroke, 0.16 c wide, its tip 0.32 c from the centre towards the open side, i.e. on the stub between the wheel and the flange.

### 4. No cell tint
Both endpoint cells are drawn on the same empty-cell tile as a pipe cell (`SurfaceCard` at 6 %), without the 18 % colour wash. The colour lives only in the wheel and the ring, which keeps the design system's rule that brass marks what the player acts on (the valve is where the flow starts and what the fast-forward acts on), and keeps the finish flash the only full-tile success effect.

### 5. Liquid in the start's stub
`FieldLiquid` gets a fill for the start: while the head is in the start cell with progress *p*, the stub's channel (centre → open side) is filled to *p*; once the head has left, it is full; before the flow starts, there is none. The fill is drawn by the same `drawPipe` call as decision 2, entering from the centre end of the clipped `H`/`V` channel: the full channel is filled from the side opposite the open side to `0.5 + p / 2`, and the clip hides the first half. The head's rounded cap and glow come for free and cross into the next section without a jump, as `game-play-playing-field` requires.

### 6. Glow lifecycle
Pure functions of game time, unit tested:
- `valveGlow(nowMs, flowing, reducedMotion)`: while waiting, alpha `0.45 + 0.45 × (1 − cos(2π t / 1600)) / 2` (ease-in-out, `motion.drift`); once flowing or under reduced motion, a steady 0.6.
- `finishGlow(nowMs, won, reducedMotion)`: until won, the same curve on 2 000 ms (`motion.pulse`); under reduced motion a steady 0.6; once won, no glow pulse, because `drawFinishRing` takes over and then keeps the ring solid (`FinishRing`).
The glow is drawn as a soft circle behind the wheel or ring (radius 0.24 c + 12 dp, radial gradient from the colour at `alpha` to transparent), not as a `BlurMaskFilter`, so it is cheap and identical on all API levels. Using the field's game clock (`nowMs`) makes the pulses freeze on pause, like the popups.

### 7. Semantics for tests
The start and finish cells keep their content descriptions ("Start", "Finish") so existing tests keep finding them. Two test-only semantics are added: `endpointGlow` (the current glow alpha, rounded to two decimals) and `startFilled` (the start stub's fill 0–1), so instrumented tests can assert the pulse, the steady state and the liquid without screenshots.

## Risks / Trade-offs

- [Clipping the `H`/`V` art leaves the glass rim visible at the cut] → The wheel and ring cover the centre; the design-system mock-up is checked at 44 dp and 120 dp cells before the app work starts.
- [Two pulsing glows plus the countdown bar draw attention during the countdown] → The valve stops pulsing the moment the liquid flows, and the finish pulse is slower and smaller; reduced motion turns both off.
- [A light-and-dark checker may look busy at the 44 dp minimum cell] → 6 × 6 squares are about 3.2 dp each at 44 dp; the gate in task 1 checks legibility at 44 dp and falls back to 4 × 4 squares if it does not read, recorded in the design system.
- [The finish flash today draws a rounded square over the tile, not the ring] → Kept as is: it is specified in `game-play-level-complete` and works on the new tile; the ring simply stops pulsing once it starts.
