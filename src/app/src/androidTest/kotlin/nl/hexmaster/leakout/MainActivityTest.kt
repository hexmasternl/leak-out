package nl.hexmaster.leakout

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.content.pm.PackageInfoCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.hexmaster.leakout.ui.achievements.AchievementsTags
import nl.hexmaster.leakout.ui.game.GameScreenTags
import nl.hexmaster.leakout.ui.game.HudTags
import nl.hexmaster.leakout.ui.game.PauseTags
import nl.hexmaster.leakout.ui.game.keepsScreenOn
import nl.hexmaster.leakout.ui.game.onboarding.TipOverlayTags
import nl.hexmaster.leakout.ui.levels.LevelLibraryTags
import nl.hexmaster.leakout.ui.levels.levelTileTag
import nl.hexmaster.leakout.ui.settings.PlayGamesPanelTags
import nl.hexmaster.leakout.ui.settings.SettingsTags
import nl.hexmaster.leakout.ui.splash.SplashScreenTags
import nl.hexmaster.leakout.ui.splash.SplashTiming
import nl.hexmaster.leakout.ui.title.PlayMenuTags
import nl.hexmaster.leakout.ui.title.ExitDialogTags
import nl.hexmaster.leakout.ui.title.WelcomeScreenTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    /** Play, then a choice on the play menu; the history is read off the main thread, so wait for [tag]. */
    private fun playThen(choice: String, tag: String) {
        compose.onNodeWithTag(WelcomeScreenTags.PLAY).performClick()
        compose.onNodeWithTag(PlayMenuTags.DIALOG).assertIsDisplayed()
        compose.onNodeWithTag(choice).performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    /**
     * On a fresh install the intro tour covers the game first: skips it, then waits out the double
     * tap after it, in which taps do not reach the game.
     */
    private fun skipTourIfShown() {
        if (compose.onAllNodesWithTag(TipOverlayTags.SKIP).fetchSemanticsNodes().isEmpty()) return
        compose.onNodeWithTag(TipOverlayTags.SKIP).performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(TipOverlayTags.TAP_GUARD).fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun continueStartsALevel() {
        skipSplash()
        playThen(PlayMenuTags.CONTINUE, GameScreenTags.SCREEN)
        compose.onNodeWithTag(PlayMenuTags.DIALOG).assertDoesNotExist()
        compose.onNodeWithTag(GameScreenTags.SCREEN).assertIsDisplayed()
    }

    @Test
    fun selectLevelOpensTheLevelLibraryAndBackReturnsToTheWelcomeScreen() {
        skipSplash()
        playThen(PlayMenuTags.SELECT_LEVEL, LevelLibraryTags.SCREEN)
        compose.onNodeWithTag(PlayMenuTags.DIALOG).assertDoesNotExist()
        compose.onNodeWithTag(LevelLibraryTags.SCREEN).assertIsDisplayed()
        compose.onNodeWithTag(levelTileTag(1)).assertIsDisplayed()

        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()

        compose.onNodeWithTag(LevelLibraryTags.SCREEN).assertDoesNotExist()
        compose.onNodeWithTag(WelcomeScreenTags.SCREEN).assertIsDisplayed()
    }

    @Test
    fun exitFromThePauseMenuShowsTheWelcomeScreen() {
        skipSplash()
        playThen(PlayMenuTags.SELECT_LEVEL, LevelLibraryTags.SCREEN)
        compose.onNodeWithTag(levelTileTag(1)).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(GameScreenTags.SCREEN).assertIsDisplayed()
        skipTourIfShown()

        compose.onNodeWithTag(HudTags.PAUSE).performClick()
        compose.onNodeWithTag(PauseTags.EXIT).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(GameScreenTags.SCREEN).assertDoesNotExist()
        compose.onNodeWithTag(WelcomeScreenTags.SCREEN).assertIsDisplayed()
    }

    @Test
    fun screenIsKeptOnOnlyWhileALevelIsOpen() {
        skipSplash()
        assertFalse(compose.activity.keepsScreenOn())
        playThen(PlayMenuTags.SELECT_LEVEL, LevelLibraryTags.SCREEN)
        assertFalse(compose.activity.keepsScreenOn())

        compose.onNodeWithTag(levelTileTag(1)).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(GameScreenTags.SCREEN).assertIsDisplayed()
        compose.waitUntil(5_000) { compose.activity.keepsScreenOn() }
        skipTourIfShown()

        compose.onNodeWithTag(HudTags.PAUSE).performClick()
        compose.onNodeWithTag(PauseTags.EXIT).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(WelcomeScreenTags.SCREEN).assertIsDisplayed()
        compose.waitUntil(5_000) { !compose.activity.keepsScreenOn() }
    }

    private fun openSettings() {
        compose.onNodeWithTag(WelcomeScreenTags.SETTINGS).performClick()
        // The panels appear once the settings and the history have been read off the main thread.
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(SettingsTags.VIBRATION_SWITCH).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag(SettingsTags.SCREEN).assertIsDisplayed()
        compose.onNodeWithTag(SettingsTags.TITLE).assertTextEquals("SETTINGS")
    }

    @Test
    fun settingsOpensFromTheWelcomeScreenAndTheBackButtonReturns() {
        skipSplash()
        openSettings()
        compose.onNodeWithTag(WelcomeScreenTags.SCREEN).assertDoesNotExist()

        compose.onNodeWithTag(SettingsTags.BACK).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(SettingsTags.SCREEN).assertDoesNotExist()
        compose.onNodeWithTag(WelcomeScreenTags.SCREEN).assertIsDisplayed()
    }

    @Test
    fun systemBackFromSettingsReturnsToTheWelcomeScreen() {
        skipSplash()
        openSettings()

        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()

        compose.onNodeWithTag(SettingsTags.SCREEN).assertDoesNotExist()
        compose.onNodeWithTag(WelcomeScreenTags.SCREEN).assertIsDisplayed()
    }

    private fun openAchievements() {
        compose.onNodeWithTag(PlayGamesPanelTags.ACHIEVEMENTS).performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(AchievementsTags.GRID).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag(AchievementsTags.SCREEN).assertIsDisplayed()
        compose.onNodeWithTag(AchievementsTags.TITLE).assertTextEquals("ACHIEVEMENTS")
        compose.onNodeWithTag(SettingsTags.SCREEN).assertDoesNotExist()
    }

    @Test
    fun achievementsOpenFromSettingsAndSystemBackReturnsToSettings() {
        skipSplash()
        openSettings()
        openAchievements()

        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()

        compose.onNodeWithTag(AchievementsTags.SCREEN).assertDoesNotExist()
        compose.onNodeWithTag(SettingsTags.SCREEN).assertIsDisplayed()
    }

    @Test
    fun theAchievementsBackButtonReturnsToSettings() {
        skipSplash()
        openSettings()
        openAchievements()

        compose.onNodeWithTag(AchievementsTags.BACK).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(AchievementsTags.SCREEN).assertDoesNotExist()
        compose.onNodeWithTag(SettingsTags.SCREEN).assertIsDisplayed()
    }

    @Test
    fun aboutShowsTheInstalledVersion() {
        skipSplash()
        openSettings()
        val activity = compose.activity
        @Suppress("DEPRECATION")
        val info = activity.packageManager.getPackageInfo(activity.packageName, 0)
        compose.onNodeWithTag(SettingsTags.ABOUT_VERSION)
            .performScrollTo()
            .assertTextEquals("Version ${info.versionName} (${PackageInfoCompat.getLongVersionCode(info)})")
    }

    @Test
    fun vibrationSurvivesLeavingTheSettingsScreen() {
        skipSplash()
        openSettings()
        val vibration = { compose.onNodeWithTag(SettingsTags.VIBRATION_SWITCH).performScrollTo() }
        // The device keeps whatever an earlier run stored, so flip it and flip it back afterwards.
        val wasOn = vibration().fetchSemanticsNode().config[SemanticsProperties.ToggleableState] == ToggleableState.On
        vibration().performClick()
        try {
            compose.onNodeWithTag(SettingsTags.BACK).performClick()
            compose.waitForIdle()
            openSettings()
            if (wasOn) vibration().assertIsOff() else vibration().assertIsOn()
        } finally {
            vibration().performClick()
            compose.waitForIdle()
        }
        if (wasOn) vibration().assertIsOn() else vibration().assertIsOff()
    }

    @Test
    fun exitFinishesTheActivity() {
        skipSplash()
        compose.onNodeWithTag(WelcomeScreenTags.EXIT).performClick()
        compose.waitForIdle()
        // The device may hold an earlier opt-out, so the dialog is confirmed only when it is shown.
        if (compose.onAllNodesWithTag(ExitDialogTags.DIALOG).fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag(ExitDialogTags.YES).performClick()
            compose.waitForIdle()
        }
        assertTrue(compose.activity.isFinishing || compose.activity.isDestroyed)
    }
}
