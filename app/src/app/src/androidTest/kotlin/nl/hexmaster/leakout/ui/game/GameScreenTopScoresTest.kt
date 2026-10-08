package nl.hexmaster.leakout.ui.game

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import nl.hexmaster.leakout.achievements.playgames.SignInState
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.StarBenchmarks
import nl.hexmaster.leakout.scores.online.LeaderboardEntry
import nl.hexmaster.leakout.scores.online.TopScores
import nl.hexmaster.leakout.scores.online.TopScoresState
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Top 10 button in the pause, level complete and level lost dialogs and the top 10 dialog it
 * opens (spec `level-top-scores`). The game clock moves with tick commands.
 */
@RunWith(AndroidJUnit4::class)
class GameScreenTopScoresTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val level = FieldTest.level(id = 7, stars = StarBenchmarks(20, 40, 200))
    private val loads = mutableListOf<Int>()
    private val scores = TopScores { level ->
        loads += level
        TopScoresState.Loaded(listOf(LeaderboardEntry(1, "Ada", 2140, isCurrentPlayer = true)))
    }
    private val access = TopScoresAccess(scores, MutableStateFlow(SignInState.SignedIn("Ada"))) {}
    private lateinit var session: GameSession

    @Test
    fun openingFromThePauseDialogKeepsTheGameFrozenAndCloseReturnsToIt() {
        show(access)
        compose.onNodeWithTag(HudTags.PAUSE).performClick()
        compose.waitForIdle()
        val clock = compose.runOnIdle { session.state.clockMs }

        compose.onNodeWithTag(PauseTags.TOP10).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(TopScoresTags.DIALOG).assertIsDisplayed()
        compose.onNodeWithTag(TopScoresTags.row(1)).assertIsDisplayed()
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(listOf(7), loads)
            assertTrue(session.state.paused)
            assertEquals(clock, session.state.clockMs)
        }

        compose.onNodeWithTag(TopScoresTags.CLOSE).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(TopScoresTags.DIALOG).assertDoesNotExist()
        compose.onNodeWithTag(PauseTags.DIALOG).assertIsDisplayed()
        compose.runOnIdle { assertTrue(session.state.paused) }
    }

    @Test
    fun backClosesTheTop10AndATapOnItsBackdropDoesNot() {
        show(access)
        compose.onNodeWithTag(HudTags.PAUSE).performClick()
        compose.onNodeWithTag(PauseTags.TOP10).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(TopScoresTags.BACKDROP).performTouchInput { click(topLeft + Offset(8f, 8f)) }
        compose.waitForIdle()
        compose.onNodeWithTag(TopScoresTags.DIALOG).assertIsDisplayed()

        Espresso.pressBack()
        compose.waitForIdle()
        compose.onNodeWithTag(TopScoresTags.DIALOG).assertDoesNotExist()
        compose.onNodeWithTag(PauseTags.DIALOG).assertIsDisplayed()
    }

    @Test
    fun theWinDialogOffersTop10OnceItsButtonsAreEnabled() {
        show(access)
        win()
        tickToWinAge(WinTiming.FINISH_EFFECT_MS)
        compose.onNodeWithTag(LevelCompleteTags.TOP10).assertIsNotEnabled()
        val end = compose.runOnIdle { WinTally.totalDurationMs(session.state.finalScore!!, session.state.finalStars) }
        tickToWinAge(WinTiming.FINISH_EFFECT_MS + end)

        // The Compact layout on this 800 × 360 dp screen keeps every button fully visible.
        compose.onNodeWithTag(LevelCompleteTags.LAYOUT_COMPACT).assertIsDisplayed()
        for (tag in listOf(LevelCompleteTags.NEXT, LevelCompleteTags.RESTART, LevelCompleteTags.TOP10, LevelCompleteTags.EXIT)) {
            compose.onNodeWithTag(tag).assertIsDisplayed()
        }
        compose.onNodeWithTag(LevelCompleteTags.TOP10).assertIsEnabled().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(TopScoresTags.DIALOG).assertIsDisplayed()

        compose.onNodeWithTag(TopScoresTags.CLOSE).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(LevelCompleteTags.DIALOG).assertIsDisplayed()
        compose.onNodeWithTag(LevelCompleteTags.NEXT).assertIsEnabled()
    }

    @Test
    fun theLostDialogOffersTop10AndCloseReturnsToIt() {
        show(access)
        lose()
        tick(LeakTiming.EFFECT_MS)
        compose.onNodeWithTag(LevelLostTags.TOP10).assertIsDisplayed().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(TopScoresTags.DIALOG).assertIsDisplayed()

        Espresso.pressBack()
        compose.waitForIdle()
        compose.onNodeWithTag(TopScoresTags.DIALOG).assertDoesNotExist()
        compose.onNodeWithTag(LevelLostTags.DIALOG).assertIsDisplayed()
        compose.onNodeWithTag(LevelLostTags.EXIT).assertIsDisplayed()
    }

    @Test
    fun withoutAccessNoDialogShowsATop10Button() {
        show(access = null)
        compose.onNodeWithTag(HudTags.PAUSE).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(PauseTags.RESTART).assertIsDisplayed()
        compose.onNodeWithTag(PauseTags.TOP10).assertDoesNotExist()
        compose.onNodeWithTag(PauseTags.RESUME).performClick()
        compose.waitForIdle()

        lose()
        tick(LeakTiming.EFFECT_MS)
        compose.onNodeWithTag(LevelLostTags.EXIT).assertIsDisplayed()
        compose.onNodeWithTag(LevelLostTags.TOP10).assertDoesNotExist()
    }

    @Test
    fun withoutAccessTheWinDialogHasNoTop10Button() {
        show(access = null)
        win()
        tickToWinAge(WinTiming.FINISH_EFFECT_MS)
        compose.onNodeWithTag(LevelCompleteTags.EXIT).assertIsDisplayed()
        compose.onNodeWithTag(LevelCompleteTags.TOP10).assertDoesNotExist()
    }

    private fun win() {
        compose.runOnIdle {
            for ((at, section) in FieldTest.ROUTE) assertTrue(session.placeSection(section, at))
            session.flowToEnd()
            assertTrue(session.state.outcome is Outcome.Won)
        }
        compose.waitForIdle()
    }

    private fun lose() {
        compose.runOnIdle {
            assertTrue(session.placeSection(PipeSection.H, Position(1, 0)))
            session.flowToEnd()
            assertTrue(session.state.outcome is Outcome.Lost)
        }
        compose.waitForIdle()
    }

    private fun tick(ms: Long) {
        compose.runOnIdle { session.sendTick(ms) }
        compose.waitForIdle()
    }

    private fun tickToWinAge(ageMs: Long) {
        val remaining = compose.runOnIdle { ageMs - session.winAgeMs }
        if (remaining > 0) tick(remaining)
    }

    private fun show(access: TopScoresAccess?) {
        session = GameSession(level, Random(7))
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(800.dp, 360.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
                    GameScreen(level = level, onExit = {}, onNextLevel = {}, session = session, topScores = access)
                }
            }
        }
        compose.waitForIdle()
    }
}
