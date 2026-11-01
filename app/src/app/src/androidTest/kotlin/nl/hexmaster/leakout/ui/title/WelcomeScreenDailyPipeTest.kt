package nl.hexmaster.leakout.ui.title

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.hexmaster.leakout.core.library.NextLevel
import nl.hexmaster.leakout.ui.daily.DailyPipeEntryState
import nl.hexmaster.leakout.ui.daily.DailyPipeProblem
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The Daily Pipe button on the welcome screen (specs `daily-pipe` and `welcome-screen`). */
@RunWith(AndroidJUnit4::class)
class WelcomeScreenDailyPipeTest {
    @get:Rule
    val compose = createComposeRule()

    private val entry = mutableStateOf(DailyPipeEntryState())
    private var taps = 0

    private fun showScreen(widthDp: Int = 800, heightDp: Int = 360) {
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(widthDp.dp, heightDp.dp)) {
                    WelcomeScreen(
                        resolveNextLevel = { NextLevel(levelId = 1, page = 1, isUnlocked = true, highestCompleted = 0) },
                        onStartLevel = {},
                        onOpenLevelLibrary = {},
                        onSettings = {},
                        onExit = {},
                        dailyPipe = entry.value,
                        onDailyPipe = { taps++ },
                    )
                }
            }
        }
    }

    private val button get() = compose.onNodeWithTag(WelcomeScreenTags.DAILY_PIPE)
    private val status get() = compose.onNodeWithTag(WelcomeScreenTags.DAILY_PIPE_STATUS)

    @Test
    fun signedOutThereIsNoDailyPipeButton() {
        showScreen()

        button.assertDoesNotExist()
        status.assertDoesNotExist()
        compose.onNodeWithTag(WelcomeScreenTags.PLAY).assertIsDisplayed()
    }

    @Test
    fun signingInWhileOnTheScreenShowsTheButton() {
        showScreen()
        button.assertDoesNotExist()

        entry.value = DailyPipeEntryState(visible = true)

        button.assertIsDisplayed().assertIsEnabled().assertTextEquals("DAILY PIPE")
        status.assertDoesNotExist()
    }

    @Test
    fun signedInTheButtonsArePlayDailyPipeSettingsExitWithTouchTargets() {
        entry.value = DailyPipeEntryState(visible = true)
        showScreen(widthDp = 640, heightDp = 320)

        val lefts = listOf(WelcomeScreenTags.PLAY, WelcomeScreenTags.DAILY_PIPE, WelcomeScreenTags.SETTINGS, WelcomeScreenTags.EXIT)
            .map { tag ->
                val bounds = compose.onNodeWithTag(tag).assertIsDisplayed().getUnclippedBoundsInRoot()
                assertTrue("$tag is shorter than 48 dp", bounds.bottom - bounds.top >= 48.dp)
                bounds.left
            }
        assertEquals(lefts.sorted(), lefts)
    }

    @Test
    fun offlineTheButtonIsDisabledWithAHintUntilTheConnectionReturns() {
        entry.value = DailyPipeEntryState(visible = true, online = false)
        showScreen()

        button.assertIsDisplayed().assertIsNotEnabled()
        status.assertIsDisplayed().assertTextEquals("The Daily Pipe needs an internet connection")
        button.performClick()
        compose.runOnIdle { assertEquals(0, taps) }

        entry.value = DailyPipeEntryState(visible = true, online = true)
        button.assertIsEnabled()
        status.assertDoesNotExist()
    }

    @Test
    fun aTapAsksTheHostToOpenTheDailyPipe() {
        entry.value = DailyPipeEntryState(visible = true)
        showScreen()

        button.performClick()

        compose.runOnIdle { assertEquals(1, taps) }
    }

    @Test
    fun loadingAndProblemsAreShownUnderTheRow() {
        entry.value = DailyPipeEntryState(visible = true, loading = true)
        showScreen()
        button.assertIsNotEnabled()
        status.assertTextEquals("Loading today's Daily Pipe…")

        entry.value = DailyPipeEntryState(visible = true, problem = DailyPipeProblem.NotPublished)
        button.assertIsEnabled()
        status.assertTextEquals("No Daily Pipe yet, try again later")

        entry.value = DailyPipeEntryState(visible = true, problem = DailyPipeProblem.Unsupported)
        status.assertTextEquals("This Daily Pipe needs a newer version of Leak Out")
    }
}
