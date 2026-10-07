package nl.hexmaster.leakout.ui.game

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.StarBenchmarks
import nl.hexmaster.leakout.scores.online.ScorePostStatus
import nl.hexmaster.leakout.scores.online.ScoreSubmissions
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The won score goes to the score server while the level complete dialog is shown, and the dialog
 * shows the status dot (spec `online-score-submission`). The game clock moves with tick commands.
 */
@RunWith(AndroidJUnit4::class)
class GameScreenScorePostTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val stars = StarBenchmarks(20, 40, 200)
    private val level = FieldTest.level(id = 99, stars = stars)
    private val nextLevel = FieldTest.level(id = 100, stars = stars)

    private val submitted = mutableListOf<Pair<Int, Int>>()
    private val status = MutableStateFlow(ScorePostStatus.InProgress)
    private val scores = ScoreSubmissions { level, score ->
        submitted += level to score
        status
    }

    private lateinit var session: GameSession
    private val dot get() = compose.onNodeWithTag(ScorePostIndicatorTags.DOT, useUnmergedTree = true)

    @Test
    fun theScoreIsSentOnceTheDialogShowsAndTheDotAppears() {
        show(scores)
        win()
        tickToWinAge(WinTiming.FINISH_EFFECT_MS - 1)
        compose.runOnIdle { assertEquals("not before the dialog", emptyList<Pair<Int, Int>>(), submitted) }
        dot.assertDoesNotExist()

        tickToWinAge(WinTiming.FINISH_EFFECT_MS)
        compose.onNodeWithTag(LevelCompleteTags.DIALOG).assertIsDisplayed()
        compose.runOnIdle { assertEquals(listOf(99 to 60), submitted) }
        dot.assertIsDisplayed()
    }

    @Test
    fun theButtonsDoNotWaitForTheSendingAndNextLevelRemovesTheDot() {
        show(scores)
        win()
        val end = compose.runOnIdle { WinTally.totalDurationMs(session.state.finalScore!!, session.state.finalStars) }
        tickToWinAge(WinTiming.FINISH_EFFECT_MS + end)
        compose.onNodeWithTag(LevelCompleteTags.NEXT).assertIsEnabled()
        dot.assertIsDisplayed()

        compose.onNodeWithTag(LevelCompleteTags.NEXT).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(LevelCompleteTags.DIALOG).assertDoesNotExist()
        dot.assertDoesNotExist()
        compose.runOnIdle { assertEquals("sent once", 1, submitted.size) }
    }

    @Test
    fun withoutScoresNothingIsSentAndThereIsNoDot() {
        show(scores = null)
        win()
        tickToWinAge(WinTiming.FINISH_EFFECT_MS)
        compose.onNodeWithTag(LevelCompleteTags.DIALOG).assertIsDisplayed()
        dot.assertDoesNotExist()
        compose.runOnIdle { assertTrue(submitted.isEmpty()) }
    }

    private fun win() {
        compose.runOnIdle {
            for ((at, section) in FieldTest.ROUTE) assertTrue(session.placeSection(section, at))
            session.flowToEnd()
            assertTrue(session.state.outcome is Outcome.Won)
        }
        compose.waitForIdle()
    }

    private fun tickToWinAge(ageMs: Long) {
        val remaining = compose.runOnIdle { ageMs - session.winAgeMs }
        if (remaining > 0) {
            compose.runOnIdle { session.sendTick(remaining) }
            compose.waitForIdle()
        }
    }

    private fun show(scores: ScoreSubmissions?) {
        session = GameSession(level, Random(7))
        compose.setContent {
            LeakOutTheme {
                var current: Level by remember { mutableStateOf(level) }
                Box(Modifier.size(800.dp, 360.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
                    key(current) {
                        GameScreen(
                            level = current,
                            onExit = {},
                            onNextLevel = if (current == level) ({ current = nextLevel }) else null,
                            random = Random(7),
                            session = remember { if (current == level) session else GameSession(current, Random(7)) },
                            scores = scores,
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
    }
}
