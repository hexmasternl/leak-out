package nl.hexmaster.leakout

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
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

    @Test
    fun welcomeScreenIsShownOnLaunch() {
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
        compose.onNodeWithTag(WelcomeScreenTags.EXIT).performClick()
        compose.waitForIdle()
        assertTrue(compose.activity.isFinishing || compose.activity.isDestroyed)
    }
}
