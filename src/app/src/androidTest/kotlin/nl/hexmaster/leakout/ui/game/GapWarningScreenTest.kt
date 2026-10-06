package nl.hexmaster.leakout.ui.game

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.random.Random
import nl.hexmaster.leakout.core.engine.GameCommand
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.level.Endpoint
import nl.hexmaster.leakout.core.level.GridSize
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.core.level.StarBenchmarks
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The score bar's gap warning (spec `game-play-gap-warning`, tasks 2.3 and 2.4). The frame loop
 * does not run in these tests, so the clock is moved with tick commands.
 */
@RunWith(AndroidJUnit4::class)
class GapWarningScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val stars = StarBenchmarks(100, 200, 350)

    // ScoreBar on its own (task 2.3).

    @Test
    fun scoreBarShowsTheScoreWithoutAWarning() {
        compose.setContent { LeakOutTheme { ScoreBar(150, stars, attempt = 1, reducedMotion = true) } }
        compose.onNodeWithText("150").assertIsDisplayed()
        score().assertContentDescriptionEquals("Score 150, 1 of 3 stars")
        score().assert(SemanticsMatcher.expectValue(ScoreWarning, 0f))
    }

    @Test
    fun scoreBarSwapsTheTextForWarningInCapitals() {
        compose.setContent {
            LeakOutTheme { ScoreBar(150, stars, attempt = 1, reducedMotion = true, warning = 1f, warningText = true, leakInSections = 3) }
        }
        compose.onNodeWithText("WARNING", useUnmergedTree = true).assertExists()
        score().assertContentDescriptionEquals("Score 150, 1 of 3 stars. Warning: leak in 3 sections")
    }

    @Test
    fun scoreBarDescriptionKeepsTheWarningWhileTheScoreShows() {
        compose.setContent {
            LeakOutTheme { ScoreBar(150, stars, attempt = 1, reducedMotion = true, warning = 1f, warningText = false, leakInSections = 1) }
        }
        score().assertContentDescriptionEquals("Score 150, 1 of 3 stars. Warning: leak in 1 section")
    }

    // On the game screen (task 2.4).

    @Test
    fun farFromTheGapTheBarIsBrass() {
        val session = show()
        startFlow(session)
        assertEquals(0f, warning(), 0.001f)
        assertWarningText(false)
    }

    @Test
    fun theBarTurnsRedFromFiveSections() {
        val session = show()
        startFlow(session)
        tick(session, 3_000) // 4 sections away
        assertEquals(0.5f, warning(), 0.01f)
        assertWarningText(false)
        tick(session, 1_000) // 3 sections away
        assertEquals(1f, warning(), 0.001f)
    }

    @Test
    fun atThreeSectionsTheBarBlinksStartingWithWarning() {
        val session = show()
        startFlow(session)
        tick(session, 4_000)
        assertWarningText(true)
        assertTrue(description().endsWith(". Warning: leak in 3 sections"))
        tick(session, 500)
        assertWarningText(false)
        tick(session, 500)
        assertWarningText(true)
    }

    @Test
    fun pausedTheBlinkStandsStill() {
        val session = show()
        startFlow(session)
        tick(session, 4_000)
        assertWarningText(true)
        compose.runOnIdle { session.send(GameCommand.Pause) }
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
        assertWarningText(true)
    }

    @Test
    fun buildingStopsTheBlinkAndLeavesTheBarPartlyRed() {
        val session = show()
        startFlow(session)
        tick(session, 4_000)
        assertWarningText(true)
        compose.runOnIdle { assertTrue(session.placeSection(PipeSection.H, Position(7, 0))) }
        compose.waitForIdle()
        assertWarningText(false)
        assertEquals(0.5f, warning(), 0.01f)
        assertTrue(!description().contains("Warning"))
    }

    @Test
    fun aLeakEndsTheWarning() {
        val session = show()
        startFlow(session)
        tick(session, 4_000)
        compose.runOnIdle { session.flowToEnd() }
        compose.waitForIdle()
        compose.runOnIdle { assertTrue(session.state.outcome is Outcome.Lost) }
        assertWarningText(false)
        assertEquals(0f, warning(), 0.001f)
    }

    private fun score() = compose.onNodeWithTag(HudTags.SCORE)

    private fun warning(): Float = score().fetchSemanticsNode().config[ScoreWarning]

    private fun description(): String =
        score().fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull().orEmpty()

    private fun assertWarningText(shown: Boolean) {
        score().assert(SemanticsMatcher.expectValue(ScoreWarningText, shown))
    }

    private fun tick(session: GameSession, ms: Long) {
        compose.runOnIdle { session.sendTick(ms) }
        compose.waitForIdle()
    }

    /** Ends the countdown: the head enters the start tile, seven sections from the gap at (7, 0). */
    private fun startFlow(session: GameSession) {
        compose.runOnIdle { session.endCountdown() }
        compose.waitForIdle()
    }

    private fun show(): GameSession {
        val session = GameSession(GapLevels.row(), Random(7))
        for (x in 1..6) assertTrue(session.placeSection(PipeSection.H, Position(x, 0)))
        compose.mainClock.autoAdvance = false
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(800.dp, 360.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
                    GameScreen(level = session.state.level, onExit = {}, session = session)
                }
            }
        }
        compose.waitForIdle()
        return session
    }
}

/** Levels for the gap warning tests. */
internal object GapLevels {
    /**
     * A row along the top: start at (0, 0) open to the right, the finish out of the way at the bottom
     * right. With straights on (1, 0) to (6, 0) the gap is at (7, 0), seven sections from the start's entry.
     */
    fun row(columns: Int = 9) = FieldTest.level(
        grid = GridSize(columns, 2),
        start = Endpoint(Position(0, 0), Side.RIGHT),
        finish = Endpoint(Position(columns - 1, 1), Side.TOP),
        cellsPerSecond = 1.0,
    )
}
