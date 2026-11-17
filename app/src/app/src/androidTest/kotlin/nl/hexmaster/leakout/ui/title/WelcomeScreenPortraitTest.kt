package nl.hexmaster.leakout.ui.title

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.abs
import nl.hexmaster.leakout.core.library.NextLevel
import nl.hexmaster.leakout.ui.daily.DailyPipeEntryState
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The portrait welcome screen (spec welcome-screen, "Portrait menu layout"). */
@RunWith(AndroidJUnit4::class)
class WelcomeScreenPortraitTest {

    @get:Rule
    val compose = createComposeRule()

    private fun showScreen(fontScale: Float = 1f, dailyPipe: Boolean = false, widthDp: Int = 360, heightDp: Int = 640) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                LeakOutTheme {
                    Box(Modifier.size(widthDp.dp, heightDp.dp)) {
                        WelcomeScreen(
                            resolveNextLevel = { NextLevel(levelId = 1, page = 1, isUnlocked = true, highestCompleted = 0) },
                            onStartLevel = {},
                            onOpenLevelLibrary = {},
                            onSettings = {},
                            onExit = {},
                            versionLabel = "1.0.10",
                            dailyPipe = DailyPipeEntryState(visible = dailyPipe),
                        )
                    }
                }
            }
        }
    }

    private fun bounds(tag: String): DpRect = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    private fun assertStackedAndCentredAtTheBottom(tags: List<String>) {
        val screen = bounds(WelcomeScreenTags.SCREEN)
        val title = bounds(WelcomeScreenTags.TITLE)
        val buttons = tags.map { compose.onNodeWithTag(it).assertIsDisplayed(); bounds(it) }
        val centre = (screen.left + screen.right) / 2

        // Title at the top, fully on screen and above the stack.
        assertTrue("title $title", title.top >= screen.top && title.bottom <= buttons.first().top)
        for ((upper, lower) in buttons.zipWithNext()) {
            assertTrue("$upper overlaps $lower", upper.bottom <= lower.top)
        }
        for (button in buttons) {
            assertTrue("not centred: $button", abs(((button.left + button.right) / 2 - centre).value) < 2f)
            assertTrue("touch target: $button", button.bottom - button.top >= 48.dp && button.right - button.left >= 48.dp)
            assertTrue("clipped: $button", button.left >= screen.left && button.right <= screen.right)
        }
        // The stack sits at the bottom: its last button is in the lowest fifth and on screen.
        val last = buttons.last()
        assertTrue("last $last", last.bottom <= screen.bottom && last.bottom >= screen.bottom - (screen.bottom - screen.top) / 5)
    }

    @Test
    fun buttonsAreStackedAndCentredAtTheBottom() {
        showScreen()
        assertStackedAndCentredAtTheBottom(listOf(WelcomeScreenTags.PLAY, WelcomeScreenTags.SETTINGS, WelcomeScreenTags.EXIT))
    }

    @Test
    fun theDailyPipeButtonJoinsTheStackRightAfterPlay() {
        showScreen(dailyPipe = true)
        assertStackedAndCentredAtTheBottom(
            listOf(WelcomeScreenTags.PLAY, WelcomeScreenTags.DAILY_PIPE, WelcomeScreenTags.SETTINGS, WelcomeScreenTags.EXIT),
        )
    }

    @Test
    fun everythingFitsAt130PercentFontScale() {
        showScreen(fontScale = 1.3f, dailyPipe = true)
        assertStackedAndCentredAtTheBottom(
            listOf(WelcomeScreenTags.PLAY, WelcomeScreenTags.DAILY_PIPE, WelcomeScreenTags.SETTINGS, WelcomeScreenTags.EXIT),
        )
    }

    @Test
    fun theVersionLabelStaysClearOfTheButtons() {
        showScreen(dailyPipe = true)
        val version = bounds(WelcomeScreenTags.VERSION)
        val play = bounds(WelcomeScreenTags.PLAY)
        assertTrue("version $version overlaps the stack", version.bottom <= play.top)
    }
}
