## Why

The liquid flows slowly on purpose: 0.4 to 2 cells per second, after a countdown of 8 to 20 seconds. That pace gives the player time to build. But once the route from start to finish is complete, there is nothing left to do, and on a large level the player can wait a minute or more just to see the win. A fast forward button lets the player skip that wait as soon as the outcome is certain.

## What Changes

- Each time a section is placed or replaced, the game checks whether the placed sections form a **complete route** from the start to the finish. The route is followed exactly as the liquid would flow: out of the start's open side, through each section's channel, into the finish through the finish's open side. A cross is passed straight through, a reservoir only from left to right or right to left.
- When the route is complete, a **fast forward button** appears at the bottom right of the screen, over the corner of the playing field.
- When the route is no longer complete (a replacement broke it) before the player taps the button, the button disappears again.
- Tapping the button turns fast forward on: the liquid flows at **10 times** its current speed. If the countdown is still running, the countdown also runs 10 times faster. The button then turns into a **slow button**.
- Tapping the slow button turns fast forward off again: the liquid goes back to the level's normal speed, and the button turns back into the fast forward button. The player can switch between the two as often as they like.
- While fast forward is on, the route is locked: the player can no longer drag sections from the stack or replace sections on the field. This keeps the win certain and stops a player from breaking the route at 10x speed. Pan and zoom still work. Turning fast forward off unlocks placing again.
- Fast forward is kept over pause and resume, and turned off by restart.
- Fast forward changes only how fast the liquid moves. Points, the order of the popups and the result of the level are the same as without it.

## Capabilities

### New Capabilities
- `game-play-fast-forward`: The complete-route check after each placement, when the fast forward button is shown and hidden, where it is placed, what turning it on does to the flow, the countdown and placing, and how it behaves on pause, resume and restart.

### Modified Capabilities

None. The stack, HUD, pause, playing-field and placement specs are all still in open changes and not in `openspec/specs/`. Their requirements stay the same: this change adds a new reason why no tile accepts a drop (the route is locked), which the placement rule gets as one more rejection.

## Impact

- New pure Kotlin code in `core`: `PipeSection.exitFor(entry: Side): Side?` (the channel rule the flow will also use) and a `RouteTracer` in `nl.hexmaster.leakout.core.route` that follows the route from the start over a `Board`, with JVM unit tests.
- `GameSession` (from `app-game-play-hud`, changed by `app-game-play-playing-field`) gets `routeComplete`, `fastForward` and `flowTimeScale` (1 or 10), runs the route check after every accepted placement, and refuses placements while fast forward is on.
- New Compose code in `app` (`ui/game/`): a `FastForwardButton` in the bottom right corner of the game screen, with a vector icon, string resources, and unit and instrumented tests.
- There is no flow simulation yet. The flow change multiplies its time step by `flowTimeScale` for the countdown and the liquid head. Until then, the button can be shown and turned on, but the liquid does not move.
- This change builds on `app-game-play-stack`, `app-game-play-hud` and `app-game-play-playing-field` and must merge after them.
- `docs/project-plan.md` sections 1.8 and 1.11 are updated with the fast forward button.
