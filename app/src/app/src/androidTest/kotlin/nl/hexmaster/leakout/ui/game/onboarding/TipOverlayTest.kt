package nl.hexmaster.leakout.ui.game.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.unit.width
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.settings.TipId
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import androidx.compose.ui.test.performTouchInput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The overlay on its own: the reduced-motion drag and tap hints, the button layout and taps on the buttons. */
@RunWith(AndroidJUnit4::class)
class TipOverlayTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun withAnimationsOffThePlacingStepShowsAStaticArrow() {
        val slot = Rect(Offset(20f, 200f), Size(60f, 60f))
        val cell = Rect(Offset(300f, 150f), Size(80f, 80f))
        compose.mainClock.autoAdvance = false
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(640.dp, 360.dp)) {
                    TipOverlay(
                        content = TipContent(TipId.Intro, 3, "Placing Sections", "Drag the next section.", "next section"),
                        spotlights = listOf(slot, cell),
                        hint = DragHint(PipeSection.H, slot, cell),
                        onNext = {}, onSkip = {}, onDone = {},
                        reducedMotion = true,
                    )
                }
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithTag(TipOverlayTags.HINT_ARROW).assertIsDisplayed()
        compose.onNodeWithTag(TipOverlayTags.HINT_GHOST).assertIsDisplayed()
    }

    private fun showTapStep(reducedMotion: Boolean) {
        val cell = Rect(Offset(300f, 150f), Size(80f, 80f))
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(640.dp, 360.dp)) {
                    TipOverlay(
                        content = TipContent(TipId.Intro, 4, "Pro Tip: Tap to Place", "Tap an empty cell.", "an empty cell"),
                        spotlights = listOf(cell),
                        hint = TapHint(cell),
                        onNext = {}, onSkip = {}, onDone = {},
                        reducedMotion = reducedMotion,
                    )
                }
            }
        }
    }

    @Test
    fun withAnimationsOffTheTapToPlaceStepShowsAStillRingAndNoDragHint() {
        compose.mainClock.autoAdvance = false
        showTapStep(reducedMotion = true)
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithTag(TipOverlayTags.HINT_TAP).assertIsDisplayed()
        compose.onNodeWithTag(TipOverlayTags.HINT_GHOST).assertDoesNotExist()
        compose.onNodeWithTag(TipOverlayTags.HINT_ARROW).assertDoesNotExist()
        compose.onNodeWithTag(TipOverlayTags.NEXT).assertIsDisplayed()
    }

    @Test
    fun thePulsingTapHintDoesNotRecomposeTheOverlayEveryFrame() {
        compose.mainClock.autoAdvance = false
        showTapStep(reducedMotion = false)
        compose.mainClock.advanceTimeBy(1_000)
        val settled = TipOverlayProbe.compositions
        compose.mainClock.advanceTimeBy(2_000)
        assertTrue("recomposed ${TipOverlayProbe.compositions - settled} times", TipOverlayProbe.compositions - settled <= 1)
        compose.onNodeWithTag(TipOverlayTags.HINT_TAP).assertIsDisplayed()
    }

    private fun showStartStep(
        onNext: () -> Unit = {},
        onSkip: () -> Unit = {},
        spotlight: () -> Rect = { Rect(Offset(40f, 40f), Size(60f, 60f)) },
    ) {
        compose.setContent {
            LeakOutTheme {
                // A Samsung Galaxy S21 in landscape.
                Box(Modifier.size(914.dp, 411.dp)) {
                    TipOverlay(
                        content = TipContent(TipId.Intro, 0, "The Start", "The liquid comes out here.", "start valve"),
                        spotlights = listOf(spotlight()),
                        hint = null,
                        onNext = onNext, onSkip = onSkip, onDone = {},
                        reducedMotion = false,
                    )
                }
            }
        }
    }

    @Test
    fun aFingerTapWithSmallMovesRunsNextAndSkipTour() {
        var next = 0
        var skip = 0
        showStartStep(onNext = { next++ }, onSkip = { skip++ })
        compose.onNodeWithTag(TipOverlayTags.NEXT, useUnmergedTree = true).performTouchInput { fingerTap() }
        compose.onNodeWithTag(TipOverlayTags.SKIP, useUnmergedTree = true).performTouchInput { fingerTap() }
        compose.waitForIdle()
        assertEquals(1, next)
        assertEquals(1, skip)
    }

    @Test
    fun aFingerTapOnTheScrimRunsNothing() {
        var calls = 0
        showStartStep(onNext = { calls++ }, onSkip = { calls++ })
        compose.onNodeWithTag(TipOverlayTags.OVERLAY).performTouchInput { fingerTap(Offset(width - 10f, height - 10f)) }
        compose.waitForIdle()
        assertEquals(0, calls)
    }

    private fun bounds(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true).getBoundsInRoot()

    @Test
    fun onceSettledTheCardStaysWhereItIs() {
        compose.mainClock.autoAdvance = false
        showStartStep()
        compose.mainClock.advanceTimeBy(1_000)
        val settled = bounds(TipOverlayTags.CARD)
        repeat(6) {
            compose.mainClock.advanceTimeBy(250)
            assertEquals(settled, bounds(TipOverlayTags.CARD))
        }
    }

    @Test
    fun aShownTipDoesNotRecomposeEveryFrame() {
        compose.mainClock.autoAdvance = false
        showStartStep()
        compose.mainClock.advanceTimeBy(1_000)
        val settled = TipOverlayProbe.compositions
        // Two whole glow pulses: well over a hundred frames.
        compose.mainClock.advanceTimeBy(2_000)
        assertTrue("recomposed ${TipOverlayProbe.compositions - settled} times", TipOverlayProbe.compositions - settled <= 1)
    }

    @Test
    fun theCardStaysUnderAFingerWhileTheSpotlightMovesAndTheTapStillCounts() {
        var spotlight by mutableStateOf(Rect(Offset(40f, 40f), Size(60f, 60f)))
        var skips = 0
        showStartStep(onSkip = { skips++ }, spotlight = { spotlight })
        val skip = compose.onNodeWithTag(TipOverlayTags.SKIP, useUnmergedTree = true)
        val before = skip.getBoundsInRoot()
        skip.performTouchInput { down(center) }
        // Over to the far corner, so the card would go to the other side of it.
        spotlight = Rect(Offset(1_800f, 800f), Size(60f, 60f))
        compose.waitForIdle()
        assertEquals(before, skip.getBoundsInRoot())
        skip.performTouchInput { up() }
        compose.waitForIdle()
        assertEquals(1, skips)
        assertNotEquals("the card catches up after the release", before, skip.getBoundsInRoot())
    }

    @Test
    fun aTourStepShowsAFullWidthNextAboveSkipTour() {
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(640.dp, 360.dp)) {
                    TipOverlay(
                        content = TipContent(TipId.Intro, 0, "The Start", "The liquid comes out here.", "start valve"),
                        spotlights = listOf(Rect(Offset(40f, 40f), Size(60f, 60f))),
                        hint = null,
                        onNext = {}, onSkip = {}, onDone = {},
                        reducedMotion = true,
                    )
                }
            }
        }
        val next = compose.onNodeWithTag(TipOverlayTags.NEXT).assertIsDisplayed().getBoundsInRoot()
        val skip = compose.onNodeWithTag(TipOverlayTags.SKIP).assertIsDisplayed().getBoundsInRoot()
        assertTrue("Next is above Skip tour", next.bottom <= skip.top)
        assertTrue("Next is at least as wide as Skip tour", next.width >= skip.width)
        assertTrue("Next has a real width", next.width > 100.dp)
    }
}
