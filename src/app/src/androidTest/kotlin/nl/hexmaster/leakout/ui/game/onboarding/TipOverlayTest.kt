package nl.hexmaster.leakout.ui.game.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.settings.TipId
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The overlay on its own: the reduced-motion drag hint. */
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
}
