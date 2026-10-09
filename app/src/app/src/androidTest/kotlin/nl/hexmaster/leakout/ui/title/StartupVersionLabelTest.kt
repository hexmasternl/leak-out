package nl.hexmaster.leakout.ui.title

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.hexmaster.leakout.core.library.NextLevel
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupVersionLabelTest {

    @get:Rule
    val compose = createComposeRule()

    private var settings = 0

    /** The host's process-lifetime flag, as `MainActivity` keeps it. */
    private var labelShown by mutableStateOf(false)

    /** Whether the host shows the welcome screen or another screen (Settings). */
    private var onWelcome by mutableStateOf(true)

    private fun showScreen(versionLabel: String? = "v1.4.0 (57)") {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(800.dp, 360.dp)) {
                    if (onWelcome) {
                        WelcomeScreen(
                            resolveNextLevel = { NextLevel(levelId = 1, page = 1, isUnlocked = true, highestCompleted = 0) },
                            onStartLevel = {},
                            onOpenLevelLibrary = {},
                            onSettings = { settings++ },
                            onExit = {},
                            versionLabel = if (labelShown) null else versionLabel,
                            onVersionLabelShown = { labelShown = true },
                        )
                    }
                }
            }
        }
        compose.mainClock.advanceTimeByFrame()
    }

    private fun label() = compose.onNodeWithTag(WelcomeScreenTags.VERSION, useUnmergedTree = true)

    @Test
    fun showsTheVersionInTheBottomRightCornerAtStart() {
        showScreen()
        label().assertIsDisplayed().assertTextEquals("v1.4.0 (57)")
        val bounds = label().getUnclippedBoundsInRoot()
        val play = compose.onNodeWithTag(WelcomeScreenTags.PLAY).getUnclippedBoundsInRoot()
        assertTrue("right half", bounds.left > 400.dp)
        assertTrue("below the buttons", bounds.top > play.bottom)
        assertTrue(labelShown)
    }

    @Test
    fun staysForFiveSecondsThenFadesOutOver300Ms() {
        showScreen()
        compose.mainClock.advanceTimeBy(4_900)
        label().assertIsDisplayed()
        compose.mainClock.advanceTimeBy(250)
        // Mid fade: still there.
        label().assertExists()
        compose.mainClock.advanceTimeBy(300)
        compose.mainClock.advanceTimeByFrame()
        label().assertDoesNotExist()
    }

    @Test
    fun reducedMotionFadesOutOver160Ms() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            LeakOutTheme { StartupVersionLabel(text = "v1.4.0 (57)", onShown = {}, reducedMotion = true) }
        }
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(5_050)
        label().assertExists()
        compose.mainClock.advanceTimeBy(200)
        compose.mainClock.advanceTimeByFrame()
        label().assertDoesNotExist()
    }

    @Test
    fun isNotShownAgainAfterReturningToTheWelcomeScreen() {
        showScreen()
        compose.mainClock.advanceTimeBy(6_000)
        label().assertDoesNotExist()
        onWelcome = false
        compose.mainClock.advanceTimeByFrame()
        onWelcome = true
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag(WelcomeScreenTags.SCREEN).assertIsDisplayed()
        label().assertDoesNotExist()
    }

    @Test
    fun leavingBeforeTheFadeAlsoCountsAsShown() {
        showScreen()
        compose.mainClock.advanceTimeBy(1_000)
        onWelcome = false
        compose.mainClock.advanceTimeByFrame()
        onWelcome = true
        compose.mainClock.advanceTimeByFrame()
        label().assertDoesNotExist()
    }

    @Test
    fun keepsFadingWhenTheHostClearsTheLabelWhileItShows() {
        showScreen()
        // labelShown is now true, so the host recomposes with null; the running label stays until it has faded.
        assertTrue(labelShown)
        compose.mainClock.advanceTimeBy(1_000)
        label().assertIsDisplayed()
    }

    @Test
    fun buttonsKeepTheirPlaceAndRespondWhileTheLabelShows() {
        showScreen()
        val tags = listOf(WelcomeScreenTags.PLAY, WelcomeScreenTags.SETTINGS, WelcomeScreenTags.EXIT)
        label().assertIsDisplayed()
        val with = tags.map { compose.onNodeWithTag(it).getUnclippedBoundsInRoot() }
        compose.onNodeWithTag(WelcomeScreenTags.SETTINGS).performClick()
        compose.mainClock.advanceTimeByFrame()
        assertEquals(1, settings)

        compose.mainClock.advanceTimeBy(6_000)
        label().assertDoesNotExist()
        val without = tags.map { compose.onNodeWithTag(it).getUnclippedBoundsInRoot() }
        assertEquals(without, with)
    }

    @Test
    fun nullShowsNothing() {
        showScreen(versionLabel = null)
        label().assertDoesNotExist()
        assertEquals(false, labelShown)
    }
}
