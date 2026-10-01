package nl.hexmaster.leakout.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.hexmaster.leakout.history.InMemoryLevelHistoryRepository
import nl.hexmaster.leakout.history.LevelHistory
import nl.hexmaster.leakout.settings.InMemorySettingsRepository
import nl.hexmaster.leakout.settings.Settings
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private var backs = 0
    private lateinit var settings: InMemorySettingsRepository
    private lateinit var history: InMemoryLevelHistoryRepository

    /** 12 finished levels with 31 stars. */
    private val twelveLevels = (1..10).map { LevelHistory(it, 100, 3) } + LevelHistory(11, 50, 1) + LevelHistory(12, 5, 0)

    private fun showScreen(
        stored: Settings = Settings.Default,
        records: List<LevelHistory> = twelveLevels,
        widthDp: Int = 800,
        heightDp: Int = 360,
        fontScale: Float? = null,
    ) {
        settings = InMemorySettingsRepository(stored)
        history = InMemoryLevelHistoryRepository(records)
        val viewModel = SettingsViewModel(settings, history)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale ?: density.fontScale)) {
                LeakOutTheme {
                    Box(Modifier.size(widthDp.dp, heightDp.dp)) {
                        SettingsScreen(viewModel = viewModel, appInfo = AppInfo("0.1.0", 1), onBack = { backs++ })
                    }
                }
            }
        }
    }

    private fun stored(): Settings = settings.settings.value

    private fun SemanticsNodeInteraction.assertSliderValue(expected: Float) = assert(
        SemanticsMatcher("slider at $expected") {
            val info = it.config.getOrNull(SemanticsProperties.ProgressBarRangeInfo)
            info != null && abs(info.current - expected) < 0.001f
        },
    )

    private fun soundSlider() = compose.onNodeWithContentDescription("Sound effects volume")

    private fun musicSlider() = compose.onNodeWithContentDescription("Music volume")

    @Test
    fun panelsAndTitlesAreShown() {
        showScreen()
        compose.onNodeWithTag(SettingsTags.TITLE).assertIsDisplayed().assertTextEquals("SETTINGS")
        for (tag in listOf(SettingsTags.AUDIO_PANEL, SettingsTags.FEEL_PANEL, SettingsTags.PROGRESS_PANEL, SettingsTags.ABOUT_PANEL)) {
            compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
        }
        for (title in listOf("AUDIO", "FEEL", "PROGRESS", "ABOUT")) {
            compose.onNodeWithText(title).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun audioControlsShowTheStoredValues() {
        showScreen(Settings(soundEnabled = true, soundVolume = 0.8f, musicEnabled = false, musicVolume = 0.6f))
        compose.onNodeWithTag(SettingsTags.SOUND_SWITCH).assertIsOn()
        soundSlider().assertSliderValue(0.8f)
        compose.onNodeWithTag(SettingsTags.MUSIC_SWITCH).assertIsOff()
        musicSlider().assertSliderValue(0.6f)
    }

    @Test
    fun audioControlsAreDisabledAndCannotBeChanged() {
        showScreen()
        listOf(
            compose.onNodeWithTag(SettingsTags.SOUND_SWITCH),
            compose.onNodeWithTag(SettingsTags.MUSIC_SWITCH),
            soundSlider(),
            musicSlider(),
        ).forEach { it.assertIsNotEnabled() }

        compose.onNodeWithTag(SettingsTags.SOUND_SWITCH).performClick()
        compose.onNodeWithTag(SettingsTags.MUSIC_SWITCH).performClick()
        soundSlider().performTouchInput { swipeLeft() }
        musicSlider().performTouchInput { swipeRight() }
        compose.waitForIdle()

        compose.onNodeWithTag(SettingsTags.SOUND_SWITCH).assertIsOn()
        compose.onNodeWithTag(SettingsTags.MUSIC_SWITCH).assertIsOn()
        soundSlider().assertSliderValue(0.8f)
        musicSlider().assertSliderValue(0.6f)
        compose.runOnIdle {
            assertEquals(emptyList<String>(), settings.writes)
            assertEquals(Settings.Default, stored())
        }
    }

    @Test
    fun comingSoonIsShownInTheAudioPanel() {
        showScreen()
        compose.onNodeWithTag(SettingsTags.COMING_SOON).assertIsDisplayed().assertTextEquals("Coming soon")
    }

    @Test
    fun vibrationSwitchReflectsAndChangesTheStoredValue() {
        showScreen()
        val vibration = compose.onNodeWithTag(SettingsTags.VIBRATION_SWITCH).performScrollTo()
        vibration.assertIsEnabled().assertIsOn()
        vibration.performClick()
        vibration.assertIsOff()
        compose.runOnIdle {
            assertFalse(stored().vibrationEnabled)
            assertEquals(listOf("vibrationEnabled"), settings.writes)
        }
        vibration.performClick()
        vibration.assertIsOn()
        compose.runOnIdle { assertTrue(stored().vibrationEnabled) }
    }

    @Test
    fun vibrationStoredOffIsShownOff() {
        showScreen(Settings(vibrationEnabled = false))
        compose.onNodeWithTag(SettingsTags.VIBRATION_SWITCH).assertIsOff()
    }

    @Test
    fun progressSummaryCountsLevelsAndStars() {
        showScreen()
        compose.onNodeWithTag(SettingsTags.PROGRESS_SUMMARY).assertTextEquals("12 levels finished · 31 ★")
        compose.onNodeWithTag(SettingsTags.RESET_PROGRESS).assertIsEnabled()
    }

    @Test
    fun resetIsDisabledWithAnEmptyHistory() {
        showScreen(records = emptyList())
        compose.onNodeWithTag(SettingsTags.PROGRESS_SUMMARY).assertTextEquals("No levels finished yet")
        compose.onNodeWithTag(SettingsTags.RESET_PROGRESS).assertIsNotEnabled().performClick()
        compose.onNodeWithTag(SettingsTags.RESET_DIALOG).assertDoesNotExist()
    }

    private fun openDialog() {
        compose.onNodeWithTag(SettingsTags.RESET_PROGRESS).performScrollTo().performClick()
        compose.onNodeWithTag(SettingsTags.RESET_DIALOG).assertIsDisplayed()
        compose.onNodeWithText("Reset progress?").assertIsDisplayed()
        compose.onNodeWithText("All finished levels, highscores and stars are deleted. This cannot be undone.").assertIsDisplayed()
        compose.onNodeWithTag(SettingsTags.RESET_CANCEL).assertIsDisplayed()
        compose.onNodeWithTag(SettingsTags.RESET_CONFIRM).assertIsDisplayed()
        compose.runOnIdle { assertEquals(twelveLevels, runBlocking { history.observeAllNow() }) }
    }

    private fun assertCancelled() {
        compose.waitForIdle()
        compose.onNodeWithTag(SettingsTags.RESET_DIALOG).assertDoesNotExist()
        compose.onNodeWithTag(SettingsTags.SCREEN).assertIsDisplayed()
        compose.onNodeWithTag(SettingsTags.PROGRESS_SUMMARY).assertTextEquals("12 levels finished · 31 ★")
        compose.runOnIdle {
            assertEquals(twelveLevels, runBlocking { history.observeAllNow() })
            assertEquals(0, backs)
        }
    }

    @Test
    fun cancelClosesTheDialogAndKeepsTheHistory() {
        showScreen()
        openDialog()
        compose.onNodeWithTag(SettingsTags.RESET_CANCEL).performClick()
        assertCancelled()
    }

    @Test
    fun backClosesTheDialogAndKeepsTheHistory() {
        showScreen()
        openDialog()
        Espresso.onView(isRoot()).inRoot(isDialog()).perform(ViewActions.pressBack())
        assertCancelled()
    }

    @Test
    fun aTapOutsideClosesTheDialogAndKeepsTheHistory() {
        showScreen()
        openDialog()
        // The dialog root is the panel itself, centred on the screen; a tap near the screen edge is outside it.
        val outside = GeneralClickAction(
            Tap.SINGLE,
            { view ->
                val origin = IntArray(2).also(view::getLocationOnScreen)
                floatArrayOf(8f, origin[1] + view.height / 2f)
            },
            Press.FINGER,
            0,
            0,
        )
        Espresso.onView(isRoot()).inRoot(isDialog()).perform(outside)
        assertCancelled()
    }

    @Test
    fun confirmingClearsTheHistoryButKeepsTheSettings() {
        val kept = Settings(soundEnabled = false, soundVolume = 0.3f, musicEnabled = false, musicVolume = 0.2f, vibrationEnabled = false)
        showScreen(stored = kept)
        openDialog()
        compose.onNodeWithTag(SettingsTags.RESET_CONFIRM).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(SettingsTags.RESET_DIALOG).assertDoesNotExist()
        compose.onNodeWithTag(SettingsTags.PROGRESS_SUMMARY).assertTextEquals("No levels finished yet")
        compose.onNodeWithTag(SettingsTags.RESET_PROGRESS).assertIsNotEnabled()
        compose.onNodeWithTag(SettingsTags.VIBRATION_SWITCH).assertIsOff()
        compose.runOnIdle {
            assertTrue(runBlocking { history.observeAllNow() }.isEmpty())
            assertEquals(kept, stored())
            assertEquals(emptyList<String>(), settings.writes)
        }
    }

    @Test
    fun aboutShowsNameVersionMakerAndPrivacy() {
        showScreen()
        compose.onNodeWithTag(SettingsTags.ABOUT_VERSION).performScrollTo().assertIsDisplayed().assertTextEquals("Version 0.1.0 (1)")
        compose.onNodeWithText("Leak Out").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Made by HexMaster").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("No account. Your data stays on this device.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun backButtonLeaves() {
        showScreen()
        compose.onNodeWithTag(SettingsTags.BACK).assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, backs) }
    }

    @Test
    fun systemBackLeavesWhenNoDialogIsOpen() {
        showScreen()
        Espresso.pressBack()
        compose.runOnIdle { assertEquals(1, backs) }
    }

    @Test
    fun controlsHaveTouchTargetsOfAtLeast48dp() {
        showScreen()
        compose.onNodeWithTag(SettingsTags.BACK).assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        listOf(
            SettingsTags.SOUND_SWITCH, SettingsTags.SOUND_VOLUME, SettingsTags.MUSIC_SWITCH,
            SettingsTags.MUSIC_VOLUME, SettingsTags.VIBRATION_SWITCH, SettingsTags.RESET_PROGRESS,
        ).forEach { tag -> compose.onNodeWithTag(tag).performScrollTo().assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp) }
    }

    @Test
    fun columnsScrollAtLargeFontsSoNothingIsOutOfReach() {
        showScreen(widthDp = 640, heightDp = 320, fontScale = 1.3f)
        compose.onNodeWithTag(SettingsTags.VIBRATION_SWITCH).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag(SettingsTags.ABOUT_VERSION).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag(SettingsTags.RESET_PROGRESS).performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag(SettingsTags.RESET_DIALOG).assertIsDisplayed()
    }
}

private suspend fun InMemoryLevelHistoryRepository.observeAllNow(): List<LevelHistory> =
    observeAll().first()
