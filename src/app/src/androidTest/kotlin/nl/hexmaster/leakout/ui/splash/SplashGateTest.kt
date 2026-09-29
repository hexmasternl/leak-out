package nl.hexmaster.leakout.ui.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SplashGateTest {

    @get:Rule
    val compose = createComposeRule()

    /** Shows the gate on a 800x360 dp box, driven entirely by the paused test clock. */
    private fun showGate(reducedMotion: Boolean, widthDp: Int = 800, heightDp: Int = 360) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            Box(Modifier.size(widthDp.dp, heightDp.dp)) {
                SplashGate(reducedMotion = reducedMotion, clock = { compose.mainClock.currentTime }) {
                    Box(Modifier.fillMaxSize().testTag(CONTENT))
                }
            }
        }
    }

    @Test
    fun splashShowsTheTwoLineWordmarkAsOneCentredTextNode() {
        showGate(reducedMotion = false)

        val wordmark = compose.onNodeWithTag(SplashScreenTags.WORDMARK)
        wordmark.assertIsDisplayed().assertTextEquals("LEAK\nOUT")

        val screen = compose.onNodeWithTag(SplashScreenTags.SCREEN).getUnclippedBoundsInRoot()
        val text = wordmark.getUnclippedBoundsInRoot()
        assertEquals(((screen.left + screen.right) / 2).value, ((text.left + text.right) / 2).value, 1f)
        assertEquals(((screen.top + screen.bottom) / 2).value, ((text.top + text.bottom) / 2).value, 1f)
    }

    @Test
    fun splashStaysForTheMinimumThenCrossFadesIntoContent() {
        showGate(reducedMotion = false)

        compose.mainClock.advanceTimeBy(SplashTiming.MIN_DURATION_MS - 100)
        compose.onNodeWithTag(SplashScreenTags.SCREEN).assertIsDisplayed()
        compose.onNodeWithTag(CONTENT).assertDoesNotExist()

        // Halfway through the cross-fade both screens are composed.
        compose.mainClock.advanceTimeBy(100L + SplashTiming.CROSSFADE_MS / 2)
        compose.onNodeWithTag(SplashScreenTags.SCREEN).assertExists()
        compose.onNodeWithTag(CONTENT).assertExists()

        compose.mainClock.advanceTimeBy(SplashTiming.CROSSFADE_MS.toLong())
        compose.onNodeWithTag(SplashScreenTags.SCREEN).assertDoesNotExist()
        compose.onNodeWithTag(CONTENT).assertIsDisplayed()
    }

    @Test
    fun reducedMotionReplacesTheSplashWithoutACrossFade() {
        showGate(reducedMotion = true)

        compose.mainClock.advanceTimeBy(SplashTiming.MIN_DURATION_MS - 100)
        compose.onNodeWithTag(SplashScreenTags.SCREEN).assertIsDisplayed()
        compose.onNodeWithTag(CONTENT).assertDoesNotExist()

        // Two frames after the minimum: a cross-fade would still show the splash for ~300 ms.
        compose.mainClock.advanceTimeBy(100L + 2 * FRAME_MS)
        compose.onNodeWithTag(SplashScreenTags.SCREEN).assertDoesNotExist()
        compose.onNodeWithTag(CONTENT).assertIsDisplayed()
    }

    @Test
    fun wordmarkFitsTheSmallScreenAtLargeFontScale() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.3f)) {
                Box(Modifier.size(640.dp, 320.dp)) { SplashScreen() }
            }
        }

        val screen = compose.onNodeWithTag(SplashScreenTags.SCREEN).getUnclippedBoundsInRoot()
        val text = compose.onNodeWithTag(SplashScreenTags.WORDMARK).getUnclippedBoundsInRoot()
        assertTrue(text.left >= screen.left && text.right <= screen.right)
        assertTrue(text.top >= screen.top && text.bottom <= screen.bottom)
    }

    private companion object {
        const val CONTENT = "gate_content"
        const val FRAME_MS = 16L
    }
}
