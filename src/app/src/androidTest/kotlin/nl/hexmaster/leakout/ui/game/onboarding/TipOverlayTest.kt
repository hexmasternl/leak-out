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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The overlay on its own: the reduced-motion drag hint, the button layout and taps on the buttons. */
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

    private fun showStartStep(onNext: () -> Unit = {}, onSkip: () -> Unit = {}) {
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(914.dp, 411.dp)) {
                    TipOverlay(
                        content = TipContent(TipId.Intro, 0, "The Start", "The liquid comes out here.", "start valve"),
                        spotlights = listOf(Rect(Offset(40f, 40f), Size(60f, 60f))),
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
