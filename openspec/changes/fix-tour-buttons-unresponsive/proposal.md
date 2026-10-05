# Proposal

## Why

On a Samsung Galaxy S21 the intro tour shows on first launch, but its "Next" and "Skip tour" buttons do not respond. Tapping them, even rapidly, gives visible press feedback, so the touches arrive, yet no step changes and the tour never closes. Back is the only way out, and few new players will try it. The first level is unplayable until the player gets past the tour, so this breaks the first-time experience on a popular device.

## What Changes

- Find the root cause on S21-class hardware (2400×1080, ~420 dpi, 120 Hz, landscape, immersive edge-to-edge with a punch-hole camera) and add a regression test that fails before the fix.
- Make the tip callout buttons ("Next", "Skip tour", "Let's go", "Got it") complete a tap reliably. A tap that shows press feedback must also run its action: the card must not move or be rebuilt under the player's finger during a press, and running animations (spotlight glide, glow pulse, card entry, field reveal pan) must not cancel a tap.
- Keep the callout card and its buttons clear of the display cutout and the system gesture areas, so the system does not take a tap on a button.
- Make rapid taps predictable: each completed tap on "Next" advances exactly one step, and once "Skip tour" has closed the tour, later taps do nothing.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `game-play-onboarding`: the "Moving through and skipping the intro tour" requirement gains scenarios saying a tap on a tip button always takes effect, also while the tip animates and on rapid taps. A new requirement keeps the callout buttons inside the safe tappable area of the screen.

## Impact

- `app/src/main/kotlin/nl/hexmaster/leakout/ui/game/onboarding/TipOverlay.kt`: overlay pointer handling, callout placement (`CalloutLayer` writes `pointer` state during layout), glow and entry animations read during composition.
- `app/src/main/kotlin/nl/hexmaster/leakout/ui/game/GameScreen.kt` (`GameTips`): spotlight rects recomputed while the field pans to reveal a target.
- Possibly `ui/components/FlowButton.kt` and `ui/game/onboarding/CalloutPlacement.kt` (safe area insets).
- Tests: `TipOverlayTest`, `GameTipsTest` (instrumented, run on an S21-sized emulator profile) and JVM tests for `CalloutPlacement`.
- Tracked in GitHub issue [#78](https://github.com/hexmasternl/leak-out/issues/78).
