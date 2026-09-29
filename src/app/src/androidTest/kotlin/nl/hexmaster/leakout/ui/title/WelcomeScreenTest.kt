package nl.hexmaster.leakout.ui.title

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WelcomeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private var plays = 0
    private var settings = 0
    private var exits = 0

    private fun showScreen(widthDp: Int = 800, heightDp: Int = 360) {
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(widthDp.dp, heightDp.dp)) {
                    WelcomeScreen(
                        onPlay = { plays++ },
                        onSettings = { settings++ },
                        onExit = { exits++ },
                    )
                }
            }
        }
    }

    @Test
    fun titleIsDisplayedInUppercase() {
        showScreen()
        compose.onNodeWithText("LEAK OUT").assertIsDisplayed()
        compose.onNodeWithTag(WelcomeScreenTags.TITLE).assertIsDisplayed()
    }

    @Test
    fun allThreeButtonsAreDisplayedWithTheirLabels() {
        showScreen()
        listOf(
            WelcomeScreenTags.PLAY to "PLAY",
            WelcomeScreenTags.SETTINGS to "SETTINGS",
            WelcomeScreenTags.EXIT to "EXIT",
        ).forEach { (tag, label) ->
            compose.onNodeWithTag(tag)
                .assertIsDisplayed()
                .assertHasClickAction()
                .assertTextEquals(label)
        }
    }

    @Test
    fun playInvokesOnlyOnPlayOnce() {
        showScreen()
        compose.onNodeWithTag(WelcomeScreenTags.PLAY).performClick()
        compose.runOnIdle { assertCounts(play = 1, settings = 0, exit = 0) }
    }

    @Test
    fun settingsInvokesOnlyOnSettingsOnce() {
        showScreen()
        compose.onNodeWithTag(WelcomeScreenTags.SETTINGS).performClick()
        compose.runOnIdle { assertCounts(play = 0, settings = 1, exit = 0) }
    }

    @Test
    fun exitInvokesOnlyOnExitOnce() {
        showScreen()
        compose.onNodeWithTag(WelcomeScreenTags.EXIT).performClick()
        compose.runOnIdle { assertCounts(play = 0, settings = 0, exit = 1) }
    }

    @Test
    fun buttonsSitBelowTheTitleInPlaySettingsExitOrder() {
        showScreen()
        val title = compose.onNodeWithTag(WelcomeScreenTags.TITLE).getUnclippedBoundsInRoot()
        val play = compose.onNodeWithTag(WelcomeScreenTags.PLAY).getUnclippedBoundsInRoot()
        val settings = compose.onNodeWithTag(WelcomeScreenTags.SETTINGS).getUnclippedBoundsInRoot()
        val exit = compose.onNodeWithTag(WelcomeScreenTags.EXIT).getUnclippedBoundsInRoot()
        listOf(play, settings, exit).forEach { assertTrue(it.top >= title.bottom) }
        assertTrue(play.right <= settings.left)
        assertTrue(settings.right <= exit.left)
    }

    @Test
    fun titleIsHorizontallyCentred() {
        showScreen()
        val title = compose.onNodeWithTag(WelcomeScreenTags.TITLE).getUnclippedBoundsInRoot()
        val screen = compose.onNodeWithTag(WelcomeScreenTags.SCREEN).getUnclippedBoundsInRoot()
        val titleCentre = (title.left + title.right).value / 2f
        val screenCentre = (screen.left + screen.right).value / 2f
        assertEquals(screenCentre, titleCentre, 1f)
    }

    @Test
    fun smallLandscapeKeepsTitleUnclippedAndTargetsAtLeast48dp() {
        showScreen(widthDp = 640, heightDp = 320)
        val screen = compose.onNodeWithTag(WelcomeScreenTags.SCREEN).getUnclippedBoundsInRoot()
        val title = compose.onNodeWithTag(WelcomeScreenTags.TITLE).getUnclippedBoundsInRoot()
        assertTrue(title.left >= screen.left && title.right <= screen.right)
        assertTrue(title.top >= screen.top)
        listOf(WelcomeScreenTags.PLAY, WelcomeScreenTags.SETTINGS, WelcomeScreenTags.EXIT).forEach { tag ->
            val node = compose.onNodeWithTag(tag)
                .assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp)
                .assertWidthIsAtLeast(48.dp)
            assertTrue(node.getUnclippedBoundsInRoot().bottom <= screen.bottom)
        }
    }

    private fun assertCounts(play: Int, settings: Int, exit: Int) {
        assertEquals("onPlay", play, plays)
        assertEquals("onSettings", settings, this.settings)
        assertEquals("onExit", exit, exits)
    }
}
