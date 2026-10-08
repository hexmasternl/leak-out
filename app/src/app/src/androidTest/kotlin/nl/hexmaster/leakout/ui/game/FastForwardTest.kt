package nl.hexmaster.leakout.ui.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.random.Random
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.level.Endpoint
import nl.hexmaster.leakout.core.level.GridSize
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * The fast forward button on a bare 800 × 360 dp game screen. The level is 16 × 12 with a straight
 * route along row 5, and the stack only draws `H`, so dropping on the last gap completes the route.
 */
@RunWith(AndroidJUnit4::class)
class FastForwardTest {
    val compose = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(LandscapeRule()).around(compose)

    private lateinit var session: GameSession

    /** The tile next to the start, left for the drop that completes the route. */
    private val lastGap = Position(1, 5)

    private val level: Level = FieldTest.level(
        grid = GridSize(16, 12),
        start = Endpoint(Position(0, 5), Side.RIGHT),
        finish = Endpoint(Position(15, 5), Side.LEFT),
    )

    private fun button() = compose.onNodeWithTag(FastForwardTags.BUTTON)

    // 6.1
    @Test
    fun completingTheRouteShowsTheButtonInTheBottomRightCorner() {
        show()
        buildRouteExceptLastGap()
        button().assertDoesNotExist()

        compose.dropOnCell(lastGap)

        compose.runOnIdle { assertTrue(session.state.routeComplete) }
        button().assertIsDisplayed().assertContentDescriptionEquals("Fast forward")
        val bounds = button().fetchSemanticsNode().boundsInRoot
        val screen = compose.onNodeWithTag(GameScreenTags.SCREEN).fetchSemanticsNode().boundsInRoot
        assertEquals(screen.right - compose.px(16.dp), bounds.right, 1f)
        assertEquals(screen.bottom - compose.px(16.dp), bounds.bottom, 1f)
        assertTrue("at least 56 dp wide", bounds.width >= compose.px(56.dp) - 1)
        assertTrue("at least 56 dp tall", bounds.height >= compose.px(56.dp) - 1)
    }

    // 6.2
    @Test
    fun noButtonWithoutACompleteRouteAndBreakingTheRouteHidesIt() {
        show()
        buildRouteExceptLastGap()
        compose.waitForIdle()
        button().assertDoesNotExist()

        compose.dropOnCell(lastGap)
        button().assertIsDisplayed()

        compose.runOnIdle { assertTrue(session.placeSection(PipeSection.V, Position(7, 5))) }
        compose.waitForIdle()
        button().assertDoesNotExist()
    }

    // 6.3
    @Test
    fun tappingTogglesBetweenFastForwardAndTheSlowButton() {
        show()
        completeRoute()

        button().performClick()
        compose.runOnIdle { assertEquals(10, session.state.flowTimeScale) }
        button()
            .assertIsDisplayed()
            .assertContentDescriptionEquals("Normal speed")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Fast forward on"))

        button().performClick()
        compose.runOnIdle { assertEquals(1, session.state.flowTimeScale) }
        button()
            .assertIsDisplayed()
            .assertContentDescriptionEquals("Fast forward")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))

        // Placing works again at normal speed.
        val target = Position(3, 3)
        compose.dropOnCell(target)
        compose.runOnIdle { assertEquals(Cell.Pipe(PipeSection.H), session.state.board[target]) }
    }

    // 6.4
    @Test
    fun fastForwardLocksPlacingButPanAndPinchStillWork() {
        show()
        completeRoute()
        button().performClick()
        val board = compose.runOnIdle { session.state.board }
        val stack = compose.runOnIdle { session.state.stack }

        compose.beginDragTo(compose.cellRect(Position(3, 3)).center)
        compose.onNodeWithTag(GameScreenTags.DRAGGED).assertDoesNotExist()
        compose.assertNothingHighlighted()
        compose.releaseDrag()
        compose.runOnIdle {
            assertEquals(board, session.state.board)
            assertEquals(stack, session.state.stack)
        }

        val before = compose.cellRect(Position(0, 5))
        swipeField(Offset(0f, -compose.px(60.dp)))
        val panned = compose.cellRect(Position(0, 5))
        assertTrue("panned up: ${before.top} -> ${panned.top}", panned.top < before.top - compose.px(20.dp))

        val centre = compose.fieldRect().center
        val focus = (0 until 16).flatMap { x -> (0 until 12).map { y -> Position(x, y) } }.first { cell ->
            runCatching { compose.cellRect(cell).contains(centre) }.getOrDefault(false)
        }
        pinchFieldOut()
        val zoomed = compose.cellRect(focus)
        assertTrue("zoomed in: ${panned.width} -> ${zoomed.width}", zoomed.width > panned.width + 1)
    }

    // 6.5
    @Test
    fun pauseAndResumeKeepFastForwardAndRestartHidesTheButton() {
        show()
        completeRoute()
        button().performClick()

        compose.onNodeWithTag(HudTags.PAUSE).performClick()
        compose.runOnIdle { assertTrue(session.state.fastForward) }
        compose.onNodeWithTag(PauseTags.RESUME).performClick()
        compose.runOnIdle {
            assertTrue(session.state.fastForward)
            assertEquals(10, session.state.flowTimeScale)
        }
        button().assertIsDisplayed().assertContentDescriptionEquals("Normal speed")

        compose.onNodeWithTag(HudTags.PAUSE).performClick()
        compose.onNodeWithTag(PauseTags.RESTART).performClick()
        compose.waitForIdle()

        compose.runOnIdle {
            assertFalse(session.state.fastForward)
            assertEquals(1, session.state.flowTimeScale)
        }
        button().assertDoesNotExist()
    }

    @Test
    fun aReplacementDisablesTheButtonButNotPause() {
        show()
        compose.runOnIdle { assertTrue(session.placeSection(PipeSection.V, Position(3, 3))) }
        completeRoute()

        // Replacing a section off the route keeps the route complete, but the button is off until it lands.
        compose.runOnIdle { assertEquals(PlaceResult.Replaced, session.place(Position(3, 3))) }
        button().assertIsDisplayed().assertIsNotEnabled()
        button().performClick()
        compose.runOnIdle { assertFalse(session.state.fastForward) }

        compose.onNodeWithTag(HudTags.PAUSE).performClick()
        compose.runOnIdle { assertTrue(session.state.paused) }
        compose.onNodeWithTag(PauseTags.RESUME).performClick()

        compose.runOnIdle { session.sendTick(session.state.level.rules.replacePenaltyMs) }
        button().assertIsEnabled().performClick()
        compose.runOnIdle { assertTrue(session.state.fastForward) }
    }

    private fun show() {
        session = GameSession(level, Random(7))
        compose.setContent {
            LeakOutTheme {
                // The test activity shows the system bars; the game hides them, so measure a bare 360 dp screen.
                Box(Modifier.size(800.dp, 360.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
                    GameScreen(level = level, onExit = {}, session = session)
                }
            }
        }
        compose.waitForIdle()
    }

    /** Places `H` on row 5 from column 2 to 14, most of it out of view, leaving [lastGap] open. */
    private fun buildRouteExceptLastGap() {
        compose.runOnIdle {
            for (x in 2..14) assertTrue(session.placeSection(PipeSection.H, Position(x, 5)))
            assertFalse(session.state.routeComplete)
        }
    }

    private fun completeRoute() {
        buildRouteExceptLastGap()
        compose.dropOnCell(lastGap)
        button().assertIsDisplayed()
    }

    private fun swipeField(delta: Offset) {
        compose.onNodeWithTag(GameScreenTags.FIELD).performTouchInput {
            down(center)
            for (step in 1..10) moveTo(center + delta * (step / 10f))
            up()
        }
        compose.waitForIdle()
    }

    private fun pinchFieldOut() {
        compose.onNodeWithTag(GameScreenTags.FIELD).performTouchInput {
            val near = Offset(40.dp.toPx(), 0f)
            val far = Offset(160.dp.toPx(), 0f)
            pinch(center - near, center - far, center + near, center + far)
        }
        compose.waitForIdle()
    }
}
