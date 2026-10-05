package nl.hexmaster.leakout.ui.game

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.random.Random
import nl.hexmaster.leakout.core.engine.FlowState
import nl.hexmaster.leakout.core.engine.GameCommand
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.flow.Channel
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.StackSettings
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** The game screen rendered from the engine's state: a played route, a full reset on restart, and pausing. */
@RunWith(AndroidJUnit4::class)
class GameEngineScreenTest {
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(LandscapeRule()).around(compose)

    private lateinit var session: GameSession

    // 6.1
    @Test
    fun aPlayedRouteScoresInTheHudShowsPopupsAndIsWon() {
        show(FieldTest.level(cellsPerSecond = 1.0))
        compose.runOnIdle {
            for ((at, section) in FieldTest.ROUTE) assertTrue(session.placeSection(section, at))
            session.endCountdown()
        }
        compose.waitForIdle()
        compose.onNodeWithTag(FastForwardTags.BUTTON).assertExists()

        tick(1_000) // through the start
        compose.cellNode(Position(1, 0)).assert(SemanticsMatcher.expectValue(LiquidHead, true))
        compose.cellNode(Position(1, 0)).assertContentDescriptionEquals("Placed Horizontal straight, filled")
        tick(1_000) // leaves the H
        compose.onNodeWithTag(HudTags.SCORE).assertScore("10")
        compose.cellNode(Position(1, 0)).assert(SemanticsMatcher.expectValue(PointsPopupText, listOf("+10")))
        compose.cellNode(Position(1, 0)).assert(SemanticsMatcher.expectValue(FilledChannels, setOf(Channel.SINGLE)))
        compose.cellNode(Position(2, 0)).assert(SemanticsMatcher.expectValue(LiquidHead, true))
        compose.cellNode(Position(1, 0)).assert(SemanticsMatcher.expectValue(LiquidHead, false))

        tick(1_000) // leaves the LB
        compose.onNodeWithTag(HudTags.SCORE).assertScore("25")
        compose.cellNode(Position(2, 0)).assert(SemanticsMatcher.expectValue(PointsPopupText, listOf("+15")))

        compose.runOnIdle { session.flowToEnd(stepMs = 16) }
        compose.waitForIdle()
        compose.onNodeWithTag(HudTags.SCORE).assertScore("60")
        compose.runOnIdle {
            assertTrue(session.state.outcome is Outcome.Won)
            assertEquals(60, session.state.score)
        }
        compose.onNodeWithTag(FastForwardTags.BUTTON).assertDoesNotExist()
        compose.onNodeWithTag(PauseTags.DIALOG).assertDoesNotExist()
        compose.onNodeWithTag(LevelLostTags.DIALOG).assertDoesNotExist()
        onNoCell(LiquidHead)
    }

    // 6.2
    @Test
    fun restartFromThePauseDialogResetsTheFieldBoardHudAndPopups() {
        show(FieldTest.large(cellsPerSecond = 1.0))
        val start = Position(0, 5)
        val startRect = compose.cellRect(start)

        // Zoom in on the field, then pan away.
        compose.onNodeWithTag(GameScreenTags.FIELD).performTouchInput {
            pinch(center - Offset(40f, 0f), center - Offset(200f, 0f), center + Offset(40f, 0f), center + Offset(200f, 0f))
        }
        compose.onNodeWithTag(GameScreenTags.FIELD).performTouchInput {
            down(center)
            for (step in 1..10) moveTo(center + Offset(-compose.px(80.dp), -compose.px(40.dp)) * (step / 10f))
            up()
        }
        compose.waitForIdle()
        // The start has moved, or left the visible field altogether.
        val startNodes = compose.onAllNodesWithTag(GameScreenTags.cell(start.x, start.y), useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue(startNodes.isEmpty() || compose.cellRect(start) != startRect)

        // Sections, points and a popup on the field.
        compose.runOnIdle {
            session.placeSection(PipeSection.H, Position(1, 5))
            session.placeSection(PipeSection.H, Position(2, 5))
            session.endCountdown()
            session.sendTick(2_000) // the start, then the H on (1, 5)
        }
        compose.waitForIdle()
        compose.onNodeWithTag(HudTags.SCORE).assertScore("10")
        compose.runOnIdle { session.setScore(120) } // one star earned
        compose.waitForIdle()
        compose.onNodeWithTag(HudTags.SCORE).assertContentDescriptionEquals("Score 120, 1 of 3 stars")

        compose.onNodeWithTag(HudTags.PAUSE).performClick()
        compose.onNodeWithTag(PauseTags.RESTART).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(PauseTags.DIALOG).assertDoesNotExist()
        assertSameRect(startRect, compose.cellRect(start))
        compose.onAllNodes(hasContentDescription("Placed Horizontal straight"), useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodes(hasContentDescription("Placed Horizontal straight, filled"), useUnmergedTree = true).assertCountEquals(0)
        compose.onNodeWithTag(HudTags.SCORE).assertContentDescriptionEquals("Score 0, 0 of 3 stars")
        compose.onNodeWithTag(HudTags.SCORE).assert(SemanticsMatcher.expectValue(ScoreFill, 0f))
        compose.onAllNodes(SemanticsMatcher("has a popup") { node ->
            node.config.getOrElseNullable(PointsPopupText) { null }?.isNotEmpty() == true
        }, useUnmergedTree = true).assertCountEquals(0)
        compose.runOnIdle {
            assertEquals(2, session.state.attempt)
            assertFalse(session.state.paused)
            assertEquals(FlowState.Waiting(10_000), session.state.flow)
        }
    }

    // 6.3
    @Test
    fun restartFromCodeCancelsADragAndShowsTheNewStack() {
        show(FieldTest.level().copy(stack = StackSettings(visibleSize = 5)))
        val next = compose.onNodeWithContentDescription(nextDescription(), useUnmergedTree = true)
        val before = compose.runOnIdle { session.state.stack }

        next.performTouchInput {
            down(center)
            moveBy(Offset(80f, 0f))
            moveBy(Offset(40f, 0f))
        }
        compose.waitForIdle()
        compose.onNodeWithTag(GameScreenTags.DRAGGED).assertExists()

        compose.runOnIdle { assertTrue(session.send(GameCommand.Restart)) }
        compose.mainClock.advanceTimeBy(400)
        compose.waitForIdle()
        // The old next slot is gone with the old attempt, so the finger is lifted on the screen.
        compose.onNodeWithTag(GameScreenTags.SCREEN).performTouchInput { up() }
        compose.mainClock.advanceTimeBy(400)
        compose.waitForIdle()

        compose.onNodeWithTag(GameScreenTags.DRAGGED).assertDoesNotExist()
        compose.onAllNodes(hasContentDescription("Placed", substring = true), useUnmergedTree = true).assertCountEquals(0)
        val after = compose.runOnIdle { session.state.stack }
        compose.onNodeWithContentDescription(nextDescription(after.next), useUnmergedTree = true).assertExists()
        // On a 360 dp tall screen the stack rail fully shows the bottom 5 of the 15 sections; only those are described.
        val previews = after.sections.dropLast(1).takeLast(4)
        for (section in previews.distinct()) {
            compose.onAllNodes(hasContentDescription(label(section)), useUnmergedTree = true).assertCountEquals(previews.count { it == section })
        }
        compose.runOnIdle { assertNotEquals(before, after) }
    }

    // 6.4
    @Test
    fun theCogWheelAndGoingToTheBackgroundFreezeTheGameUntilResume() {
        compose.mainClock.autoAdvance = false
        show(FieldTest.level(cellsPerSecond = 1.0))
        compose.runOnIdle {
            for ((at, section) in FieldTest.ROUTE) session.placeSection(section, at)
        }
        compose.mainClock.advanceTimeBy(500)
        val running = compose.runOnIdle { session.state }
        assertTrue("the frame loop moves the clock", running.clockMs > 0)

        // The cog wheel: nothing moves while the dialog is open.
        compose.onNodeWithTag(HudTags.PAUSE).performClick()
        compose.mainClock.advanceTimeBy(100)
        val paused = compose.runOnIdle { session.state }
        assertTrue(paused.paused)
        compose.mainClock.advanceTimeBy(3_000)
        compose.runOnIdle {
            assertEquals(paused.clockMs, session.state.clockMs)
            assertEquals(paused.flow, session.state.flow)
        }
        compose.onNodeWithTag(PauseTags.RESUME).performClick()
        compose.mainClock.advanceTimeBy(1_000)
        val resumed = compose.runOnIdle { session.state }
        assertTrue(resumed.clockMs > paused.clockMs)
        assertTrue("no pause time leaked in", resumed.clockMs - paused.clockMs <= 1_100)
        assertTrue((resumed.flow as FlowState.Waiting).remainingMs < (paused.flow as FlowState.Waiting).remainingMs)

        // Going to the background: the same freeze, with the dialog shown on return.
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.mainClock.advanceTimeBy(100)
        val stopped = compose.runOnIdle { session.state }
        assertTrue(stopped.paused)
        compose.mainClock.advanceTimeBy(3_000)
        compose.onNodeWithTag(PauseTags.DIALOG).assertExists()
        compose.runOnIdle {
            assertEquals(stopped.clockMs, session.state.clockMs)
            assertEquals(stopped.flow, session.state.flow)
        }
        compose.onNodeWithTag(PauseTags.RESUME).performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.runOnIdle { assertTrue(session.state.clockMs > stopped.clockMs) }
    }

    private fun tick(ms: Long) {
        compose.runOnIdle { session.sendTick(ms) }
        compose.waitForIdle()
    }

    private fun onNoCell(key: androidx.compose.ui.semantics.SemanticsPropertyKey<Boolean>) {
        compose.onAllNodes(SemanticsMatcher.expectValue(key, true), useUnmergedTree = true).assertCountEquals(0)
    }

    private fun assertSameRect(expected: Rect, actual: Rect) {
        assertEquals(expected.left, actual.left, 1f)
        assertEquals(expected.top, actual.top, 1f)
        assertEquals(expected.width, actual.width, 1f)
    }

    private fun label(section: PipeSection): String = when (section) {
        PipeSection.H -> "Horizontal straight"
        PipeSection.V -> "Vertical straight"
        PipeSection.LT -> "Left-top corner"
        PipeSection.LB -> "Left-bottom corner"
        PipeSection.RT -> "Right-top corner"
        PipeSection.RB -> "Right-bottom corner"
        PipeSection.X -> "Cross section"
        PipeSection.RES -> "Reservoir"
    }

    private fun nextDescription(section: PipeSection = session.state.stack.next): String = "Next: ${label(section)}"

    private fun show(level: Level) {
        session = GameSession(level, Random(7))
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(800.dp, 360.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
                    GameScreen(level = level, onExit = {}, session = session)
                }
            }
        }
        if (compose.mainClock.autoAdvance) compose.waitForIdle() else compose.mainClock.advanceTimeByFrame()
    }
}
