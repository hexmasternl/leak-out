package nl.hexmaster.leakout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.hexmaster.leakout.ui.designer.DesignerTags
import nl.hexmaster.leakout.ui.game.GameScreenTags
import nl.hexmaster.leakout.ui.game.HudTags
import nl.hexmaster.leakout.ui.game.PauseTags
import nl.hexmaster.leakout.ui.splash.SplashTiming
import nl.hexmaster.leakout.ui.title.WelcomeScreenTags
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The level designer in the app, reached from the welcome screen in a debug build (flag on). */
@RunWith(AndroidJUnit4::class)
class MainActivityDesignerTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun skipSplash() {
        compose.mainClock.advanceTimeBy(SplashTiming.MIN_DURATION_MS + SplashTiming.CROSSFADE_MS + 200)
        compose.waitForIdle()
    }

    private fun openDesigner() {
        skipSplash()
        compose.onNodeWithTag(WelcomeScreenTags.LEVEL_DESIGNER).assertIsDisplayed().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(DesignerTags.SCREEN).assertIsDisplayed()
    }

    /** Changes the design to an 8 by 5 grid, so a kept design can be told from a new blank one. */
    private fun growColumns() {
        compose.onNodeWithTag(DesignerTags.COLUMNS_UP).performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(DesignerTags.COLUMNS_VALUE).assertTextEquals("8")
    }

    @Test
    fun debugBuildHasTheFlagOn() {
        assertTrue(FeatureFlags.levelDesigner)
    }

    @Test
    fun welcomeOpensTheDesignerAndBackReturns() {
        openDesigner()
        Espresso.pressBack()
        compose.waitForIdle()
        compose.onNodeWithTag(WelcomeScreenTags.SCREEN).assertIsDisplayed()
        compose.onNodeWithTag(DesignerTags.SCREEN).assertDoesNotExist()
    }

    @Test
    fun testPlayThenExitReturnsToTheSameDesign() {
        openDesigner()
        growColumns()
        compose.onNodeWithTag(DesignerTags.TEST_PLAY).performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(GameScreenTags.SCREEN).assertIsDisplayed()

        compose.onNodeWithTag(HudTags.PAUSE).performClick()
        compose.onNodeWithTag(PauseTags.EXIT).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(GameScreenTags.SCREEN).assertDoesNotExist()
        compose.onNodeWithTag(DesignerTags.SCREEN).assertIsDisplayed()
        compose.onNodeWithTag(DesignerTags.COLUMNS_VALUE).performScrollTo().assertTextEquals("8")
    }

    @Test
    fun aRecreatedActivityReturnsToTheDesignerWithTheDesign() {
        openDesigner()
        growColumns()
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithTag(DesignerTags.SCREEN).assertIsDisplayed()
        compose.onNodeWithTag(DesignerTags.COLUMNS_VALUE).performScrollTo().assertTextEquals("8")
    }
}
