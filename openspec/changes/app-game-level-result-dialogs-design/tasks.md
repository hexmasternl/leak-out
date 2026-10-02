# Tasks

Tracking issue: [#40 Win and loss dialogs design](https://github.com/hexmasternl/leak-out/issues/40).

## 1. Design system proposal (gate)

- [x] 1.1 Rewrite `docs/design-system.md` §5.6 "Win" and "Leak": eyebrow "LEVEL 7" (`success.500` / `leak.500`), headlines "Level Complete!" / "Leaked!", message lines, stars / splash, the **Points · Penalty · Score** row (replacing Pipes · Bonus · Score; Penalty "−12" in `leak.500`, Score in `brass.300`), buttons Success "Next Level" + glass "Restart" + tertiary "Exit" (win) and Primary "Restart" + tertiary "Exit" (leak), the 72 % scrim kept on purpose; verify every token named exists in §2–§4
- [x] 1.2 Add the **Regular** and **Compact** panel layouts to §5.6 (widths, paddings, button sizes, which elements shrink or drop in Compact, the 200 dp button column and divider, the measuring rule and the scrolling fallback) with a small ASCII sketch of each; verify the stated heights fit: Compact ≤ 328 dp on 640 × 360 dp
- [x] 1.3 Add the dialog entry, leak splash and their reduced-motion variants to §7.2 and §7.4; update §5.1's "Use" column so Success, glass, Primary and Tertiary name the result dialog buttons; verify durations match design decision 6
- [x] 1.4 Update the dialog mock-ups in `docs/design-system.html` (score row labels, button names, a Compact variant for both dialogs); verify the page renders in a browser with no layout break
- [x] 1.5 Build static Compose `@Preview` mock-ups of both dialogs in both layouts (640 × 360 dp and 1280 × 800 dp, plus the win dialog at 130 % font) and attach screenshots to issue #40; verify the previews render in Android Studio
- [x] 1.6 Get the product owner's approval of the design on issue #40 and record any requested changes back into the design system and spec; verify approval is noted on the issue before starting group 2

## 2. Buttons and tokens

- [ ] 2.1 If `app-game-pause-dialog-enhancements` has not landed: add its missing colour tokens and the `FlowButton` component (Primary, Secondary glass, Destructive) exactly as its tasks 1.2, 2.1 and 2.2 describe, with their tests; verify `./gradlew :app:testDebugUnitTest` passes
- [ ] 2.2 Add the **Success** (gradient `success.500 → success.700`, `text.onStatus`, lip `success.700`, success glow) and **Tertiary** (no fill, `liquid.300` label, no lip, 48 dp touch target) variants to `FlowButtonVariant`; verify JVM unit tests of their colours against §5.1 and that Tertiary has no lip, and an instrumented test that a Tertiary button is at least 48 dp high and clickable

## 3. Shared layout

- [ ] 3.1 Add `ResultDialogLayout.choose(availableHeightPx, regularHeightPx)` returning Regular or Compact in `app/ui/game/ResultDialogLayout.kt`; verify JVM unit tests: fits exactly → Regular, 1 px over → Compact, 360 dp window with 440 dp content → Compact, 800 dp window → Regular
- [ ] 3.2 Add the shared `ResultDialogPanel` (level 3 shadow, `radius.lg`, `surface.base`, `line.highlight` top edge, no outline), the scrim (`surface.scrim`, platform dim off), the `SubcomposeLayout` that measures the Regular content with final values once per window size and font scale, and the Compact two-column scaffold with a scrolling info column; verify an instrumented test that the info column scrolls while the buttons column is fully displayed when the content is forced taller than the window
- [ ] 3.3 Add the entry motion (scrim 160 ms fade, panel 0.92 → 1 `motion.pop`; reduced motion: 160 ms fade, no scale); verify a JVM test of the motion spec selection and an instrumented test with the clock paused that the panel starts at scale 0.92 and ends at 1

## 4. Level complete dialog

- [ ] 4.1 Rename the row strings to "Points", "Penalty", "Score" with descriptions "Points %1$s", "Penalty minus %1$s", "Score %1$s", add the "Level %1$d" eyebrow, Title Case "Level Complete!" and "Next Level"; verify `./gradlew :app:assembleDebug` succeeds and no other screen uses the renamed strings
- [ ] 4.2 Rebuild `LevelCompleteDialog` on the shared panel in both layouts: eyebrow, headline, message / "New best!", `WinStars` (40 dp / 32 dp), the three-column score row with the existing tags and merged semantics, `FlowButton`s Success / glass / Tertiary; pass the level number from `GameScreen`; keep all `LevelCompleteTags` and expose `win_layout_regular` / `win_layout_compact`; verify the `@Preview`s from 1.5 now use the real dialog
- [ ] 4.3 Update `LevelCompleteTest`, `LevelCompleteNewBestTest` and any text lookups of the old labels; add instrumented tests: "Level 7" eyebrow shown, rows read "Points 120", "Penalty minus 12", "Score 108" after the tally, Compact layout with all three buttons fully displayed on the 640 × 360 dp emulator, and buttons still disabled during the tally; verify they pass

## 5. Level lost dialog

- [ ] 5.1 Add the `LeakSplash` composable (centre blob, 6 droplets flying out over 600 ms once; static sprite with animations off) tagged `lost_splash`; verify an instrumented test with the clock paused that the droplets move and come to rest, and that with animator scale 0 the still image is drawn
- [ ] 5.2 Rebuild `LevelLostDialog` on the shared panel in both layouts: eyebrow "LEVEL 7" in `leak.500`, splash (72 dp / 48 dp beside the headline), "Leaked!", message, single Score column (`lost_score`), Primary "Restart" and Tertiary "Exit"; pass the level number from `GameScreen`; expose `lost_layout_regular` / `lost_layout_compact`; replace the old `LeakingPipe` drawing
- [ ] 5.3 Update `LevelLostTest` and add instrumented tests: "Level 7" eyebrow, score 85 shown, Compact layout with Restart and Exit fully displayed on 640 × 360 dp, Restart and Exit still work, Back still exits; verify they pass

## 6. Regression

- [ ] 6.1 Run `./gradlew :core:test :app:testDebugUnitTest` and fix failures; verify both pass
- [ ] 6.2 Run the affected instrumented tests on the emulator (`LevelCompleteTest`, `LevelCompleteNewBestTest`, `LevelLostTest`, `GameEngineScreenTest`, `GameSoundEffectsTest` and the new layout, splash and `FlowButton` tests); verify they pass
- [ ] 6.3 Win and lose a level on the emulator (640 × 360 dp) and on a tablet-sized emulator, take screenshots of both dialogs in both layouts and post them on issue #40; verify the comment links all four screenshots
