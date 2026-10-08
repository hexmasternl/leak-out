package nl.hexmaster.leakout.ui.game

import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.score.FinalScore
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The level complete and level lost dialogs on their own, in landscape on the 640 × 360 dp-class
 * phone emulator, where the Regular layout does not fit and the dialogs use Compact.
 */
@RunWith(AndroidJUnit4::class)
class ResultDialogsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val score = FinalScore(earned = 120, unusedSections = List(4) { Position(it, 0) }, total = 108)

    @Before
    fun landscape() {
        compose.runOnUiThread { compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitForIdle()
    }

    @Test
    fun winDialogShowsTheLevelAndTheNewRowsAfterTheTally() {
        showWin(WinTally.totalDurationMs(score, 2))
        compose.onNodeWithText("LEVEL 7").assertIsDisplayed()
        compose.onNodeWithText("Level Complete!").assertIsDisplayed()
        compose.onNodeWithTag(LevelCompleteTags.EARNED).assertContentDescriptionEquals("Points 120")
        compose.onNodeWithTag(LevelCompleteTags.PENALTY).assertContentDescriptionEquals("Penalty minus 12")
        compose.onNodeWithTag(LevelCompleteTags.TOTAL).assertContentDescriptionEquals("Score 108")
    }

    @Test
    fun winDialogUsesCompactWithAllButtonsDisplayed() {
        showWin(WinTally.totalDurationMs(score, 2))
        compose.onNodeWithTag(LevelCompleteTags.LAYOUT_COMPACT).assertExists()
        compose.onNodeWithTag(LevelCompleteTags.LAYOUT_REGULAR).assertDoesNotExist()
        for (tag in listOf(LevelCompleteTags.NEXT, LevelCompleteTags.RESTART, LevelCompleteTags.EXIT)) {
            compose.onNodeWithTag(tag).assertIsDisplayed().assertIsEnabled()
        }
    }

    @Test
    fun winButtonsStayDisabledDuringTheTallyAndTheLayoutStaysPut() {
        var age by mutableLongStateOf(0L)
        compose.setContent { LeakOutTheme { WinDialog(age) } }
        for (tag in listOf(LevelCompleteTags.NEXT, LevelCompleteTags.RESTART, LevelCompleteTags.EXIT)) {
            compose.onNodeWithTag(tag).assertIsDisplayed().assertIsNotEnabled()
        }
        compose.onNodeWithTag(LevelCompleteTags.LAYOUT_COMPACT).assertExists()
        age = WinTally.totalDurationMs(score, 2)
        compose.waitForIdle()
        compose.onNodeWithTag(LevelCompleteTags.LAYOUT_COMPACT).assertExists()
        compose.onNodeWithTag(LevelCompleteTags.NEXT).assertIsEnabled()
    }

    @Test
    fun lostDialogShowsTheLevelScoreAndCompactButtons() {
        var restarts = 0
        var exits = 0
        compose.setContent { LeakOutTheme { LevelLostDialog(level = 7, score = 85, onRestart = { restarts++ }, onExit = { exits++ }) } }
        compose.onNodeWithText("LEVEL 7").assertIsDisplayed()
        compose.onNodeWithTag(LevelLostTags.SCORE).assertContentDescriptionEquals("Score 85")
        compose.onNodeWithTag(LevelLostTags.SPLASH, useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(LevelLostTags.LAYOUT_COMPACT).assertExists()
        compose.onNodeWithTag(LevelLostTags.RESTART).assertIsDisplayed().performClick()
        compose.onNodeWithTag(LevelLostTags.EXIT).assertIsDisplayed().performClick()
        Espresso.pressBack()
        compose.runOnIdle {
            assertEquals(1, restarts)
            assertEquals(2, exits)
        }
    }

    @Test
    fun splashDropletsFlyOutAndComeToRest() {
        compose.mainClock.autoAdvance = false
        compose.setContent { LeakOutTheme { LeakSplash(reducedMotion = false, modifier = Modifier.testTag(SPLASH)) } }
        compose.mainClock.advanceTimeByFrame()
        val start = progress()
        compose.mainClock.advanceTimeBy(300)
        val middle = progress()
        compose.mainClock.advanceTimeBy(1_000)
        assertTrue("droplets should move: $start -> $middle", start < middle)
        compose.onNodeWithTag(SPLASH).assert(SemanticsMatcher.expectValue(SplashProgress, 1f))
    }

    @Test
    fun splashIsStillWithAnimationsOff() {
        compose.mainClock.autoAdvance = false
        compose.setContent { LeakOutTheme { LeakSplash(reducedMotion = true, modifier = Modifier.testTag(SPLASH)) } }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag(SPLASH).assert(SemanticsMatcher.expectValue(SplashProgress, 1f))
    }

    private fun progress(): Float =
        compose.onNodeWithTag(SPLASH).fetchSemanticsNode().config[SplashProgress]

    private fun showWin(age: Long) {
        compose.setContent { LeakOutTheme { WinDialog(age) } }
    }

    @Composable
    private fun WinDialog(age: Long) {
        LevelCompleteDialog(
            level = 7,
            score = score,
            frame = winFrame(age, score, stars = 2),
            hasNextLevel = true,
            onNextLevel = {},
            onRestart = {},
            onExit = {},
        )
    }

    private companion object {
        const val SPLASH = "splash"
    }
}

