# Tasks

Tracking issue: [#44 Start and end position re-design](https://github.com/hexmasternl/leak-out/issues/44).

## 1. Design system (gate)

- [ ] 1.1 Rewrite the start valve and finish drain rows of `docs/design-system.md` §6.4: stubs made of the half `H`/`V` art between the centre and the open side (flange and glass included), the valve wheel and chevron geometry (design decision 3), the finish ring with the checkered flag replacing the grate (decision 1); verify every token named exists in §2
- [ ] 1.2 Add the endpoint cells to §6.2 (drawn on the empty-cell tile, no colour wash); add the glow lifecycle to §7.1/§7.2 (valve pulses on `motion.drift` during the countdown and settles at 60 % once the liquid flows; finish ring pulses on `motion.pulse` until the win, then the finish flash and a solid ring) and the steady 60 % glows to §7.4; verify the durations match design decision 6
- [ ] 1.3 Add `flag-checkered` to the §8 required icon set with the flag geometry at 24 dp; update the `brass.500` and `success.500` usage text in §2.4/§2.5 ("Finish ring and flag" instead of "Finish drain ring")
- [ ] 1.4 Update `pipeSVG` `START` and `FINISH` in `docs/design-system.html`: stub drawn as the clipped `H`/`V` layers, checkered flag instead of the grate, gallery caption "Finish flag"; add a start and a finish joined to a placed `H` to the gallery; verify the page renders in a browser at 44 dp and 120 dp cells and the flag reads at 44 dp (else switch to 3 × 2 squares and record it)
- [ ] 1.5 Attach screenshots of the new gallery cards to issue #44 and get the product owner's approval there; verify approval is noted on the issue before starting group 2

## 2. Geometry and glows

- [ ] 2.1 Add the pure endpoint geometry in `app/ui/game/Endpoints.kt`: valve wheel, spokes, hub, chevron for each open side, ring, pole and the waving 4 × 3 cloth squares, in cell units; verify JVM unit tests: the chevron points out of each of the four open sides, the flag's bounding box lies inside the ring's inner radius, the cloth has 12 squares with the top-left one light and neighbours alternating
- [ ] 2.2 Add `valveGlow` and `finishGlow` (design decision 6); verify JVM unit tests: valve at 0 ms and 1 600 ms equal and different from 800 ms while waiting, 0.6 once flowing, 0.6 under reduced motion; finish period 2 000 ms until won, 0.6 under reduced motion
- [ ] 2.3 Add `Success500`, `TextPrimary` and any missing `Brass*` tokens to the app colours if absent; verify `./gradlew :app:testDebugUnitTest` passes

## 3. Drawing

- [ ] 3.1 Replace `drawEndpoint` in `PlayingField.kt`: empty-cell tile, the clipped `H`/`V` stub via `drawPipe`, glow, wheel and chevron for the start, glow, ring and flag for the finish; remove the colour wash and the slim `SuccessGreen`; verify `@Preview`s of both endpoints for all four open sides next to a placed straight match the design-system gallery
- [ ] 3.2 Add the start stub fill to `FieldLiquid` (design decision 5) and draw it; verify JVM unit tests: no fill while waiting, half the stub at head progress 0.5 in the start, full once the head has left, none after a restart
- [ ] 3.3 Time the glows by the field's game clock and pass the waiting / won state and reduced motion into `PlayingField`; expose the `endpointGlow` and `startFilled` test semantics; verify `./gradlew :app:testDebugUnitTest` passes

## 4. Instrumented tests

- [ ] 4.1 Add `PlayingFieldTest` cases: start and finish still found by "Start" / "Finish"; during the countdown the valve glow changes over 800 ms of game time; after the countdown it is 0.6; while paused it does not change; with animations off both glows are 0.6; `startFilled` grows while the head is in the start and is 1 after it leaves and 0 after a restart; verify they pass on the emulator
- [ ] 4.2 Add a screenshot-style check (as in `HudScreenshotTest`) of a start opening right next to a placed `H` and a finish opening left, so a reviewer can compare with the gallery; verify it passes

## 5. Regression

- [ ] 5.1 Run `./gradlew :core:test :app:testDebugUnitTest` from `src/`; verify both pass
- [ ] 5.2 Run the affected instrumented tests on the emulator (`PlayingFieldTest`, `GameEngineScreenTest`, `LevelCompleteTest`, `FastForwardTest` and the new screenshot test); verify they pass
