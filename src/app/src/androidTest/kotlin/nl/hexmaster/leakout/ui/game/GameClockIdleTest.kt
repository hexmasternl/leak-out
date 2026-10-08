package nl.hexmaster.leakout.ui.game

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.random.Random
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** The frame loop goes idle behind a result dialog once the end effects are over, and runs again on restart. */
@RunWith(AndroidJUnit4::class)
class GameClockIdleTest {
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(LandscapeRule()).around(compose)

    private lateinit var session: GameSession

    @Test
    fun theClockIdlesBehindTheLostDialogUntilRestart() {
        compose.mainClock.autoAdvance = false
        show(FieldTest.level())
        compose.runOnIdle { session.sendTick(20_000) } // nothing placed: the start leaks
        compose.runOnIdle { assertTrue(session.state.outcome is Outcome.Lost) }

        // The frame loop plays the leak effect, then stops.
        compose.mainClock.advanceTimeBy(LeakTiming.EFFECT_MS + 500)
        val idle = compose.runOnIdle {
            assertTrue("the frame loop ran the leak effect", session.lossAgeMs >= LeakTiming.EFFECT_MS)
            assertFalse(session.needsClock)
            session.state.clockMs
        }
        compose.mainClock.advanceTimeBy(3_000)
        compose.runOnIdle { assertEquals(idle, session.state.clockMs) }
        compose.onNodeWithTag(LevelLostTags.DIALOG).assertExists()

        compose.onNodeWithTag(LevelLostTags.RESTART).performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.runOnIdle {
            assertEquals(Outcome.Playing, session.state.outcome)
            assertTrue("the countdown of the new attempt runs", session.state.clockMs > 0)
        }
    }

    @Test
    fun theClockIdlesBehindTheLevelCompleteDialogAfterTheTally() {
        compose.mainClock.autoAdvance = false
        show(FieldTest.level(cellsPerSecond = 1.0))
        compose.runOnIdle {
            for ((at, section) in FieldTest.ROUTE) assertTrue(session.placeSection(section, at))
            session.endCountdown()
            session.flowToEnd(stepMs = 16)
            assertTrue(session.state.outcome is Outcome.Won)
        }
        val end = compose.runOnIdle { WinTiming.FINISH_EFFECT_MS + WinTally.totalDurationMs(session.state.finalScore!!, session.state.finalStars) }

        // The frame loop plays the finish flash and the whole tally, then stops.
        compose.mainClock.advanceTimeBy(end + 1_500)
        val idle = compose.runOnIdle {
            assertTrue("the frame loop ran the tally", session.winAgeMs >= end)
            assertTrue(session.winFrame!!.buttonsEnabled)
            assertTrue(session.state.awards.isEmpty())
            assertFalse(session.needsClock)
            session.state.clockMs
        }
        compose.mainClock.advanceTimeBy(3_000)
        compose.runOnIdle { assertEquals(idle, session.state.clockMs) }
        compose.onNodeWithTag(LevelCompleteTags.DIALOG).assertExists()

        compose.onNodeWithTag(LevelCompleteTags.RESTART).performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.runOnIdle {
            assertEquals(Outcome.Playing, session.state.outcome)
            assertTrue("the countdown of the new attempt runs", session.state.clockMs > 0)
        }
    }

    private fun show(level: Level) {
        session = GameSession(level, Random(7))
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(800.dp, 360.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
                    GameScreen(level = level, onExit = {}, session = session)
                }
            }
        }
        compose.mainClock.advanceTimeByFrame()
    }
}
