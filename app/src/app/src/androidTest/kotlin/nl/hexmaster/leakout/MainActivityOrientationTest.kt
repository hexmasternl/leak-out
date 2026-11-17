package nl.hexmaster.leakout

import android.app.UiAutomation
import android.content.res.Configuration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import nl.hexmaster.leakout.ui.game.GameScreenTags
import nl.hexmaster.leakout.ui.game.HudTags
import nl.hexmaster.leakout.ui.game.PauseTags
import nl.hexmaster.leakout.ui.game.onboarding.TipOverlayTags
import nl.hexmaster.leakout.ui.splash.SplashScreenTags
import nl.hexmaster.leakout.ui.splash.SplashTiming
import nl.hexmaster.leakout.ui.title.PlayMenuTags
import nl.hexmaster.leakout.ui.title.WelcomeScreenTags
import kotlin.math.abs
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The menu follows the device and a level is landscape (openspec/specs/landscape-orientation). The
 * device rotation is frozen through [UiAutomation], the same as turning a phone with auto-rotate on.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityOrientationTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val automation: UiAutomation get() = InstrumentationRegistry.getInstrumentation().uiAutomation

    @Before
    fun holdInPortrait() {
        rotate(UiAutomation.ROTATION_FREEZE_0)
    }

    @After
    fun releaseRotation() {
        automation.setRotation(UiAutomation.ROTATION_UNFREEZE)
    }

    private fun rotate(rotation: Int) {
        automation.setRotation(rotation)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun waitForOrientation(orientation: Int) {
        compose.waitUntil(10_000) { compose.activity.resources.configuration.orientation == orientation }
        compose.waitForIdle()
    }

    private fun skipSplash() {
        compose.mainClock.advanceTimeBy(SplashTiming.MIN_DURATION_MS + SplashTiming.CROSSFADE_MS + 200)
        compose.waitForIdle()
    }

    private fun bounds(tag: String) = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    @Test
    fun welcomeScreenIsPortraitWithTheButtonsStackedAtTheBottom() {
        skipSplash()
        waitForOrientation(Configuration.ORIENTATION_PORTRAIT)
        compose.onNodeWithTag(WelcomeScreenTags.SCREEN).assertIsDisplayed()

        val play = bounds(WelcomeScreenTags.PLAY)
        val settings = bounds(WelcomeScreenTags.SETTINGS)
        val exit = bounds(WelcomeScreenTags.EXIT)
        val title = bounds(WelcomeScreenTags.TITLE)
        val screen = bounds(WelcomeScreenTags.SCREEN)
        // One column, in order, under the title.
        assertTrue(play.bottom <= settings.top && settings.bottom <= exit.top)
        assertTrue(title.bottom <= play.top)
        // Centred horizontally and in the lower part of the screen.
        val centre = (screen.left + screen.right) / 2
        assertTrue(abs(((play.left + play.right) / 2 - centre).value) < 2f)
        assertTrue(exit.bottom > (screen.top + screen.bottom) * 3 / 4)
    }

    @Test
    fun theSplashFollowsTheDeviceAndRotatingItHandsOverOnce() {
        compose.mainClock.autoAdvance = false
        waitForOrientation(Configuration.ORIENTATION_PORTRAIT)
        compose.onNodeWithTag(SplashScreenTags.WORDMARK).assertIsDisplayed()
        val wordmark = bounds(SplashScreenTags.WORDMARK)
        val screen = bounds(SplashScreenTags.SCREEN)
        assertTrue(abs(((wordmark.left + wordmark.right) / 2 - (screen.left + screen.right) / 2).value) < 2f)
        assertTrue(wordmark.left >= screen.left && wordmark.right <= screen.right)

        val activity = compose.activity
        compose.mainClock.advanceTimeBy(SplashTiming.MIN_DURATION_MS / 2)
        rotate(UiAutomation.ROTATION_FREEZE_90)
        waitForOrientation(Configuration.ORIENTATION_LANDSCAPE)
        // Rotation did not recreate the activity, so the splash keeps its saved start and hands over once.
        // The clock runs again here: Compose test roots are not found across a rotation with a frozen clock.
        assertSame(activity, compose.activity)
        compose.mainClock.autoAdvance = true
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(WelcomeScreenTags.SCREEN).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag(SplashScreenTags.SCREEN).assertDoesNotExist()
        compose.onNodeWithTag(WelcomeScreenTags.SCREEN).assertIsDisplayed()
        assertSame(activity, compose.activity)
    }

    @Test
    fun rotatingTheWelcomeScreenKeepsTheActivityTheDialogAndNoSplash() {
        skipSplash()
        waitForOrientation(Configuration.ORIENTATION_PORTRAIT)
        val activity = compose.activity
        compose.onNodeWithTag(WelcomeScreenTags.PLAY).performClick()
        compose.onNodeWithTag(PlayMenuTags.DIALOG).assertIsDisplayed()

        rotate(UiAutomation.ROTATION_FREEZE_90)
        waitForOrientation(Configuration.ORIENTATION_LANDSCAPE)

        assertSame(activity, compose.activity)
        compose.onNodeWithTag(PlayMenuTags.DIALOG).assertIsDisplayed()
        compose.onNodeWithTag(SplashScreenTags.SCREEN).assertDoesNotExist()
        // Landscape again: the buttons are back in one row.
        val play = bounds(WelcomeScreenTags.PLAY)
        val settings = bounds(WelcomeScreenTags.SETTINGS)
        assertTrue(play.right <= settings.left)
    }

    @Test
    fun aLevelStartedInPortraitIsPlayedInLandscapeAndLeavingItFollowsTheDeviceAgain() {
        skipSplash()
        waitForOrientation(Configuration.ORIENTATION_PORTRAIT)
        val activity = compose.activity

        compose.onNodeWithTag(WelcomeScreenTags.PLAY).performClick()
        compose.onNodeWithTag(PlayMenuTags.CONTINUE).performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(GameScreenTags.SCREEN).fetchSemanticsNodes().isNotEmpty() }
        waitForOrientation(Configuration.ORIENTATION_LANDSCAPE)

        // The switch did not recreate the activity, so the level is still the one that was opened.
        assertSame(activity, compose.activity)
        compose.onNodeWithTag(GameScreenTags.SCREEN).assertIsDisplayed()

        if (compose.onAllNodesWithTag(TipOverlayTags.SKIP).fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag(TipOverlayTags.SKIP).performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithTag(TipOverlayTags.TAP_GUARD).fetchSemanticsNodes().isEmpty() }
        }
        compose.onNodeWithTag(HudTags.PAUSE).performClick()
        compose.onNodeWithTag(PauseTags.EXIT).performClick()
        compose.waitForIdle()

        // Back on the welcome screen with the device still held in portrait.
        compose.onNodeWithTag(WelcomeScreenTags.SCREEN).assertIsDisplayed()
        waitForOrientation(Configuration.ORIENTATION_PORTRAIT)
    }
}
