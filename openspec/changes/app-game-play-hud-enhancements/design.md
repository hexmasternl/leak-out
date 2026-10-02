## Context

Tracking issue: [#38 HUD enhancements](https://github.com/hexmasternl/leak-out/issues/38). See proposal.md for the motivation.

The HUD today is `GameHud` (`app/.../ui/game/GameHud.kt`): a 48 dp `Row` on `SurfaceDeep` with the level, a speed icon plus "0.8/s", a spacer, `HudStars` and the score, and the cog wheel. `GameScreen` feeds it `levelId`, `flowSpeed`, `stars` and `score` from the `GameSession`. Text formatting lives in the Android-free `HudFormat`, sizes in `HudLayout`.

The engine already has everything the new HUD needs: `FlowState.Waiting.remainingMs` is the countdown in flow time (so it already freezes on pause and runs 10× in fast forward), the level has `flow.initialDelayMs` and `stars.one/two/three`, and `GameState.score` is the score. No `core` change is needed.

`docs/design-system.md` section 5.3 is out of date: it describes a 64 dp bar with a countdown ring that was never built. The issue asks for the look to be proposed in the design system and approved before implementation.

## Goals / Non-Goals

**Goals:**
- A HUD that reads at a glance and looks like a finished game: speed as one number, a visible countdown, progress towards the next star.
- All HUD maths (fractions, warning level, which thresholds a fill change passes, speed rounding, countdown seconds) in pure functions with JVM unit tests.
- Design system section 5.3 matches what ships.

**Non-Goals:**
- No change to scoring, star thresholds, flow rules or the level format.
- No change to the level complete dialog's stars (`WinStars` / `StarIcon` stay).
- No new sounds for the threshold sparkle (can be a follow-up).

## Decisions

### 1. Design system first, as a gate
Task group 1 rewrites `docs/design-system.md` §5.3 HUD bar and adds the countdown bar and score bar to §7.2, with a Compose preview mock-up screenshot attached to the PR / issue for review. Implementation (group 3 onward) does not start until the product owner approves. The proposal in the design system covers at least:
- **Panel**: depth level 1 (`line.highlight` top edge, shadow `0 4 12`), `surface.raised` fill over a subtle vertical gradient from `surface.raised` to `surface.deep`, `radius.lg` on the bottom corners, 16 dp horizontal padding. Height stays 48 dp unless the design picks something else that still passes the "largest stack fits" scenario on a 360 dp screen.
- **Countdown bar**: a `radius.pill` track in `line.subtle`, 8 dp tall, filling the free gap (min 96 dp), fill colour by warning level with a `G` glow in that colour.
- **Score bar**: a fixed-width (about 200 dp) `radius.pill` track, 20 dp tall, fill `brass.500 → brass.300` gradient, 2 dp marker lines in `line.strong` at the one and two-star positions, score number in `numeric.md` `text.primary` with a 1 dp `surface.abyss` text shadow so it stays legible over the fill.
- **Sparkle**: a short procedural burst of 6–8 `liquid.glow`/`brass.300` particles plus a brief glow flash at the threshold position, about 600 ms.

Alternative: implement first and adjust the design system after. Rejected because the issue explicitly requires approval first.

### 2. A pure `HudModel` for the maths
A new Android-free object (next to `HudFormat`) with:
- `speedNumber(cellsPerSecond): Int` – `BigDecimal(cellsPerSecond.toString()) × 10`, `RoundingMode.HALF_UP`. Going through the decimal string avoids `0.85 × 10 = 8.499…`.
- `countdownFraction(remainingMs, initialDelayMs): Float` – clamped 0..1, 0 when the delay is 0.
- `countdownWarning(remainingMs): Warning` – `Calm` (> 5,000), `Brass` (≤ 5,000), `Red` (≤ 2,000).
- `countdownSeconds(remainingMs): Int` – rounded up, for the content description.
- `scoreFraction(score, three): Float` and `markerFractions(stars)`.
- `thresholdsPassed(fromFraction, toFraction, stars): List<Threshold>` – the thresholds whose position lies in `(from, to]`, in order.

Alternative: compute inline in composables. Rejected: the spec scenarios map one-to-one to unit tests on these functions.

### 3. Sparkles follow the animated fill, not the score
The score bar keeps an `Animatable` for the fill (600 ms `FastOutSlowInEasing`, 160 ms under reduced motion). On each animation frame the previous and current fill values go through `thresholdsPassed`, and each passed threshold starts its sparkle once per attempt (a set of sparkled thresholds, reset with the attempt key). This makes multi-threshold jumps sparkle in sequence and keeps the sparkle in sync with what the player sees. The attempt key that already restarts the stack and field also resets the fill to 0 with `snapTo`.

Alternative: trigger on `state.stars` changes. Rejected: it fires before the fill gets there and can't sparkle the second threshold of a jump separately.

### 4. HUD inputs
`GameHud` gets `countdownRemainingMs: Long?` (null once the flow is no longer `Waiting`), `initialDelayMs: Long`, `stars: StarThresholds` (the level's) and `score: Int`; it drops the `stars: Int` count, which it derives for the content description. `GameScreen` selects `(state.flow as? FlowState.Waiting)?.remainingMs`. The countdown bar is always laid out (weight 1f slot) and only its alpha changes, so nothing moves when it disappears.

### 5. Removing `HudStars`
`HudStars`/`HudStar` and the `STAR_*` constants in `HudLayout` are deleted; `StarIcon` and `WinStars` move nothing and stay in `HudStars.kt` (renamed to `Stars.kt`) for the level complete dialog. The `hud_stars` test tag and the star semantics keys go away from the HUD tests.

## Risks / Trade-offs

- [Speed shown as an integer hides the small `speedRampPerCell` increases] → Accepted per issue; the number still changes once the ramp adds up to 0.1 cells per second.
- [Score number unreadable over the brass fill] → Text shadow and contrast check in the design system review against §2.8.
- [Fixed-width score bar plus a countdown bar may not fit on narrow landscape screens] → Countdown bar takes the flexible slot with a minimum width; the instrumented layout test runs at 640 dp width.
- [Per-frame threshold checks cost] → Three comparisons per frame, negligible.
- [Design approval delays the change] → Group 1 is small and produces a preview mock-up to review quickly.
