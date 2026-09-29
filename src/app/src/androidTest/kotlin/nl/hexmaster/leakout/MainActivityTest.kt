package nl.hexmaster.leakout

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.hexmaster.leakout.ui.splash.SplashScreenTags
import nl.hexmaster.leakout.ui.splash.SplashTiming
import nl.hexmaster.leakout.ui.title.WelcomeScreenTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    /** Runs the splash's minimum duration and cross-fade on the test clock. */
    private fun skipSplash() {
        compose.mainClock.advanceTimeBy(SplashTiming.MIN_DURATION_MS + SplashTiming.CROSSFADE_MS + 200)
        compose.waitForIdle()
    }

    @Test
    fun splashWordmarkIsShownBeforeTheWelcomeScreen() {
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag(SplashScreenTags.SCREEN).assertIsDisplayed()
        compose.onNodeWithTag(SplashScreenTags.WORDMARK).assertIsDisplayed().assertTextEquals("LEAK\nOUT")
        compose.onNodeWithTag(WelcomeScreenTags.SCREEN).assertDoesNotExist()

        skipSplash()

        compose.onNodeWithTag(SplashScreenTags.SCREEN).assertDoesNotExist()
        compose.onNodeWithTag(WelcomeScreenTags.SCREEN).assertIsDisplayed()
    }

    @Test
    fun recreatingTheActivityDoesNotReplayTheSplash() {
        compose.mainClock.autoAdvance = false
        skipSplash()
        compose.onNodeWithTag(WelcomeScreenTags.SCREEN).assertIsDisplayed()

        compose.activityRule.scenario.recreate()

        // No clock advance: the restored state goes straight to the welcome screen.
        compose.onNodeWithTag(SplashScreenTags.SCREEN).assertDoesNotExist()
        compose.onNodeWithTag(WelcomeScreenTags.SCREEN).assertIsDisplayed()
    }

    @Test
    fun welcomeScreenIsShownOnLaunch() {
        skipSplash()
        compose.onNodeWithTag(WelcomeScreenTags.SCREEN).assertIsDisplayed()
        compose.onNodeWithTag(WelcomeScreenTags.TITLE).assertIsDisplayed()
        compose.onNodeWithTag(WelcomeScreenTags.PLAY).assertIsDisplayed()
        compose.onNodeWithTag(WelcomeScreenTags.SETTINGS).assertIsDisplayed()
        compose.onNodeWithTag(WelcomeScreenTags.EXIT).assertIsDisplayed()
    }

    @Test
    fun manifestLocksActivityToSensorLandscape() {
        val activity = compose.activity
        // The int-flag overload works on every API level the app supports (min 26).
        @Suppress("DEPRECATION")
        val info = activity.packageManager.getActivityInfo(activity.componentName, 0)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, info.screenOrientation)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, activity.requestedOrientation)
    }

    @Test
    fun activityIsLaidOutInLandscape() {
        val config = compose.activity.resources.configuration
        assertEquals(Configuration.ORIENTATION_LANDSCAPE, config.orientation)
        assertTrue(config.screenWidthDp > config.screenHeightDp)
    }

    @Test
    fun exitFinishesTheActivity() {
        skipSplash()
        compose.onNodeWithTag(WelcomeScreenTags.EXIT).performClick()
        compose.waitForIdle()
        assertTrue(compose.activity.isFinishing || compose.activity.isDestroyed)
    }
}
