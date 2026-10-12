# Design

## Context

The intro tour is five fixed steps driven by one step index: `INTRO_STEPS` in `ui/game` gives the count, `tipContent` in `ui/game/onboarding/TipOverlay.kt` maps the index to string resources, and the game screen picks the spotlight rects and, for step 3, the `DragHint` from `TipTargets` and the hint cell that `TipScheduler.hintCell` computes (the empty cell the start's exit opens onto). The overlay draws the drag ghost itself. Tapping a cell already places the next section (spec `game-play-playing-field`, "Tapping a tile places the next section"); nothing in the tour mentions it. Only English strings exist in the app; other languages come from Play Console's automatic translation (spec `app-localization`). The design system describes every step in section 5.9.

## Goals / Non-Goals

**Goals:**
- One new tour step that teaches tapping, placed where the player has just learned to drag, so the two ways of placing are shown together.
- Reuse the tour's step machinery, animations, accessibility and tests; the new step is one more index, not a new kind of tip.

**Non-Goals:**
- Showing the new step to players who have already finished the tour. The tour is one "seen" flag and stays that way.
- Changing how tapping places a section, or the placing rules.
- A new single tip (like the cross tip) for tapping.

## Decisions

- **Position: step 5 of 6, between placing and zoom and pan.** The step builds on the placing step: same hint cell, "you can also tap it". Putting it last, after zoom and pan, would separate the two placing methods and make the pro tip the tour's closing thought instead of the field navigation it ends with today. Alternative considered: fold the sentence into the placing step's text. Rejected: that step's text is already two sentences and its animation shows dragging; a second method with its own animation needs its own step, and the issue asks for a step.
- **Target: the hint cell alone.** The placing step highlights the next slot and the hint cell; the tap step highlights only the hint cell, so the spotlight visibly moves from the stack to the field and the message is "tap here". The cell is the one `TipScheduler.hintCell` already computes, so the pan and reveal logic of the placing step carries over unchanged.
- **A tap hint, not the drag ghost.** The overlay gets a second hint kind, `TapHint(cell: Rect)`, drawn as a finger-sized ring that scales up and fades on the cell, looping on `motion.pulse` like the drag hint. With reduced motion it is a static ring, the counterpart of the drag hint's static arrow. The hint becomes a sealed type (`DragHint` or `TapHint`) rather than two nullable fields, so the overlay cannot show both.
- **Texts.** Title "Pro Tip: Tap to Place", text "You can also tap an empty cell to place the next section there. It is much faster than dragging.", target "an empty cell". English only, in `res/values/strings.xml`, following the `tip_intro_*` naming. The step counter and "Step n of 6" use `INTRO_STEPS`, so raising the constant updates them.
- **Mockups and previews.** `TipMockupStep` gets a `Tap` entry so the Compose preview shows the new step as it does the placing step.

## Risks / Trade-offs

- [The tour gets longer, and more players skip it] → One short step with a one-line pro tip; "Skip tour" is unchanged. The step sits where it adds to what the player just learned, not at the end.
- [Existing players never learn about tapping] → Accepted for this change; the tour is a one-time flag. A "replay tour" setting would solve it and is out of scope.
- [Instrumented tests that count "Next" taps to walk the tour break] → The tour tests use `INTRO_STEPS` or skip the tour; the ones that count taps are updated in this change.
- [Automatic translation of "Pro Tip" reads oddly in some languages] → Same mechanism as every other string; the title can be simplified to "Tap to Place" if a translation turns out wrong.

## Open Questions

- Should the step come right after the placing step (this design) or be the tour's last step? The design picks "after placing"; swapping the order is a one-line change to the step index mapping if the maintainer prefers the pro tip last.
