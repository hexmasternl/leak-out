package nl.hexmaster.leakout.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.hexmaster.leakout.audio.BundledMusic
import nl.hexmaster.leakout.audio.Playlist
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
    private val previews = mutableListOf<Float>()
    private val musicPreviews = mutableListOf<Float>()
    private val bundledCredits: Playlist? = BundledMusic.load(ApplicationProvider.getApplicationContext<android.content.Context>().assets)
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
        credits: Playlist? = bundledCredits,
    ) {
        settings = InMemorySettingsRepository(stored)
        history = InMemoryLevelHistoryRepository(records)
        val viewModel = SettingsViewModel(settings, history, credits)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale ?: density.fontScale)) {
                LeakOutTheme {
                    Box(Modifier.size(widthDp.dp, heightDp.dp)) {
                        SettingsScreen(
                            viewModel = viewModel,
                            appInfo = AppInfo("0.1.0", 1),
                            onBack = { backs++ },
                            onSoundPreview = { previews += it },
                            onMusicVolumePreview = { musicPreviews += it },
                        )
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

    /**
     * Drags the slider's thumb to 0, as a player does. Inside the scrolling settings columns the
     * slider only follows a drag that starts on its thumb, so a swipe from the track's end is ignored.
     */
    private fun SemanticsNodeInteraction.dragToStart() {
        // On a short screen the slider can sit partly below its column's visible part.
        performScrollTo()
        val current = fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current
        performTouchInput {
            down(Offset(width * current, centerY))
            repeat(DRAG_STEPS) { moveBy(Offset(-width * current / DRAG_STEPS, 0f)) }
            moveTo(Offset(0f, centerY))
            up()
        }
    }

    @Test
    fun panelsAndTitlesAreShown() {
        showScreen()
        compose.onNodeWithTag(SettingsTags.TITLE).assertIsDisplayed().assertTextEquals("SETTINGS")
        for (tag in listOf(SettingsTags.AUDIO_PANEL, SettingsTags.FEEL_PANEL, PlayGamesPanelTags.PANEL, SettingsTags.PROGRESS_PANEL, SettingsTags.ABOUT_PANEL)) {
            compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
        }
        for (title in listOf("AUDIO", "FEEL", "PLAY GAMES", "PROGRESS", "ABOUT")) {
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
    fun soundEffectsControlsAreEnabled() {
        showScreen()
        compose.onNodeWithTag(SettingsTags.SOUND_SWITCH).assertIsEnabled()
        soundSlider().assertIsEnabled()
    }

    @Test
    fun soundEffectsSwitchChangesTheStoredValue() {
        showScreen()
        val sound = compose.onNodeWithTag(SettingsTags.SOUND_SWITCH)
        sound.performClick()
        sound.assertIsOff()
        compose.runOnIdle {
            assertFalse(stored().soundEnabled)
            assertEquals(listOf("soundEnabled"), settings.writes)
        }
    }

    @Test
    fun soundEffectsSliderStoresOnReleaseAndPreviews() {
        showScreen()
        soundSlider().dragToStart()
        compose.waitForIdle()
        soundSlider().assertSliderValue(0f)
        compose.runOnIdle {
            assertEquals(0f, stored().soundVolume)
            assertEquals(listOf("soundVolume"), settings.writes)
            assertEquals(listOf(0f), previews)
        }
    }

    @Test
    fun noPreviewWhileSoundEffectsAreOff() {
        showScreen(Settings(soundEnabled = false))
        soundSlider().dragToStart()
        compose.runOnIdle {
            assertEquals(0f, stored().soundVolume)
            assertEquals(emptyList<Float>(), previews)
        }
    }

    @Test
    fun musicControlsAreEnabled() {
        showScreen()
        compose.onNodeWithTag(SettingsTags.MUSIC_SWITCH).assertIsEnabled()
        musicSlider().assertIsEnabled()
    }

    @Test
    fun musicSwitchChangesTheStoredValue() {
        showScreen()
        val music = compose.onNodeWithTag(SettingsTags.MUSIC_SWITCH)
        music.assertIsOn()
        music.performClick()
        music.assertIsOff()
        compose.runOnIdle {
            assertFalse(stored().musicEnabled)
            assertEquals(listOf("musicEnabled"), settings.writes)
        }
    }

    @Test
    fun musicSliderFollowsTheDragLiveAndStoresOnRelease() {
        showScreen()
        musicSlider().dragToStart()
        compose.waitForIdle()
        musicSlider().assertSliderValue(0f)
        compose.runOnIdle {
            assertEquals(0f, stored().musicVolume)
            assertEquals(listOf("musicVolume"), settings.writes)
            assertTrue("the music hears the drag", musicPreviews.size > 1)
            assertEquals(0f, musicPreviews.last())
            // Only the sound effects slider plays a sound preview.
            assertEquals(emptyList<Float>(), previews)
        }
    }

    @Test
    fun noComingSoonNoteIsShown() {
        showScreen()
        compose.onNodeWithTag(SettingsTags.MUSIC_COMING_SOON, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag(SettingsTags.SOUND_COMING_SOON, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("Coming soon").assertDoesNotExist()
    }

    @Test
    fun aboutListsTheTenMusicCreditsInOrder() {
        showScreen()
        compose.onNodeWithTag(SettingsTags.MUSIC_CREDITS).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("MUSIC").performScrollTo().assertIsDisplayed()
        val tracks = bundledCredits!!.tracks
        assertEquals(10, tracks.size)
        tracks.forEachIndexed { index, track ->
            compose.onNodeWithTag(musicCreditTag(index)).performScrollTo().assertIsDisplayed().assertTextEquals("${track.title} · ${track.artist}")
        }
        compose.onNodeWithTag(musicCreditTag(0)).assertTextEquals("Ebb and Flow · Fabian Measures")
        compose.onNodeWithTag(musicCreditTag(9)).assertTextEquals("Endless Garden · Unheard Music Concepts")
        compose.onNodeWithTag(musicCreditTag(10)).assertDoesNotExist()
    }

    @Test
    fun theMusicSectionIsLeftOutWithoutCredits() {
        showScreen(credits = null)
        compose.onNodeWithTag(SettingsTags.MUSIC_CREDITS).assertDoesNotExist()
        compose.onNodeWithTag(musicCreditTag(0)).assertDoesNotExist()
        compose.onNodeWithTag(SettingsTags.ABOUT_VERSION).performScrollTo().assertIsDisplayed().assertTextEquals("Version 0.1.0 (1)")
        compose.onNodeWithText("No account needed. If you sign in to Google Play Games, your achievements are shared with Google Play and the scores of the levels you win are sent to the Leak Out score server.").performScrollTo().assertIsDisplayed()
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
        compose.onNodeWithText("All finished levels, highscores and stars are deleted. Achievements are kept. This cannot be undone.").assertIsDisplayed()
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
        compose.onNodeWithText("No account needed. If you sign in to Google Play Games, your achievements are shared with Google Play and the scores of the levels you win are sent to the Leak Out score server.").performScrollTo().assertIsDisplayed()
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
            SettingsTags.MUSIC_VOLUME, SettingsTags.VIBRATION_SWITCH, SettingsTags.RESET_PROGRESS, PlayGamesPanelTags.ACHIEVEMENTS,
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

/** Moves in small steps, so the drag is seen as one, like a finger. */
private const val DRAG_STEPS = 20

private suspend fun InMemoryLevelHistoryRepository.observeAllNow(): List<LevelHistory> =
    observeAll().first()
