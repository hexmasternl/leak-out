package nl.hexmaster.leakout.ui.game

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.random.Random
import nl.hexmaster.leakout.BundledLevels
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.engine.LossReason
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.LevelSolver
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.core.level.SolverResult
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Plays the bundled showcase level 36, with its mandatory cross at (4, 3) and mandatory reservoir
 * at (3, 5), on the real game screen: the red tiles show, drops on them are refused, skipping the
 * reservoir loses with the missed-section message, and a route through every channel wins.
 */
@RunWith(AndroidJUnit4::class)
class MandatoryShowcaseTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var session: GameSession
    private val level: Level = checkNotNull(BundledLevels(InstrumentationRegistry.getInstrumentation().targetContext.assets).load(36))
    private val cross = Position(4, 3)
    private val reservoir = Position(3, 5)

    @Test
    fun theShowcaseLevelHasItsMandatorySectionsOnRedTiles() {
        show()
        assertEquals(listOf(cross, reservoir), level.mandatory.map { it.position })
        compose.cellNode(cross).assert(SemanticsMatcher.expectValue(MandatoryTile, true)).assertContentDescriptionEquals("Mandatory Cross section, not used yet")
        compose.cellNode(reservoir).assert(SemanticsMatcher.expectValue(MandatoryTile, true)).assertContentDescriptionEquals("Mandatory Reservoir, not used yet")
        val field = compose.fieldRect()
        val pixels = compose.onNodeWithTag(GameScreenTags.FIELD).captureToImage().toPixelMap()
        for (cell in listOf(cross, reservoir)) {
            val rect = compose.cellRect(cell).translate(-field.left, -field.top)
            val corner = pixels[(rect.left + rect.width * 0.12f).toInt(), (rect.top + rect.height * 0.12f).toInt()]
            assertTrue("tile of $cell is $corner", corner.red > 0.15f && corner.red > corner.green + 0.04f)
        }
    }

    @Test
    fun dropsAndTapsOnMandatorySectionsAreRefused() {
        show()
        // The drag helper looks for a horizontal straight as the next section.
        compose.runOnIdle { session.state = session.state.copy(stack = session.state.stack.copy(sections = session.state.stack.sections.dropLast(1) + PipeSection.H)) }
        compose.waitForIdle()
        val stack = session.state.stack
        compose.dropOnCell(cross)
        compose.cellNode(reservoir).performClick()
        compose.waitForIdle()
        assertEquals(stack, session.state.stack)
        assertEquals(Cell.Pipe(PipeSection.X, mandatory = true), session.state.board[cross])
        assertEquals(Cell.Pipe(PipeSection.RES, mandatory = true), session.state.board[reservoir])
    }

    @Test
    fun skippingTheReservoirLosesWithTheMissedSectionMessage() {
        show()
        // Through the cross to the right, up and round, down through it, then round below to the finish.
        val route = listOf(
            Position(1, 3) to PipeSection.H, Position(2, 3) to PipeSection.H, Position(3, 3) to PipeSection.H,
            Position(5, 3) to PipeSection.LT, Position(5, 2) to PipeSection.V, Position(5, 1) to PipeSection.LB,
            Position(4, 1) to PipeSection.RB, Position(4, 2) to PipeSection.V, Position(4, 4) to PipeSection.V,
            Position(4, 5) to PipeSection.RT, Position(5, 5) to PipeSection.H, Position(6, 5) to PipeSection.H,
            Position(7, 5) to PipeSection.LT, Position(7, 4) to PipeSection.V, Position(7, 3) to PipeSection.RB,
        )
        compose.runOnIdle {
            for ((at, section) in route) assertTrue("place $section on $at", session.placeSection(section, at))
            assertTrue("the route misses the reservoir", !session.state.routeComplete)
            session.flowToEnd()
            val lost = session.state.outcome as Outcome.Lost
            assertEquals(LossReason.MANDATORY_MISSED, lost.reason)
            // The finish may lie outside the starting view, so its leak is checked on the state.
            assertEquals(level.finish.position to Side.LEFT, lost.at to lost.side)
        }
        compose.waitForIdle()
        compose.cellNode(cross).assertContentDescriptionEquals("Mandatory Cross section, used")
        compose.cellNode(reservoir).assert(SemanticsMatcher.expectValue(MissedMandatory, true))
        tick(1_000)
        compose.onNodeWithText("The liquid missed a mandatory section. Level lost.").assertIsDisplayed()
    }

    @Test
    fun aRouteThroughBothChannelsOfTheCrossAndTheReservoirWins() {
        show()
        compose.runOnIdle {
            for ((at, section) in solverRoute()) assertTrue("place $section on $at", session.placeSection(section, at))
            assertTrue(session.state.routeComplete)
            session.flowToEnd()
            assertTrue(session.state.outcome is Outcome.Won)
        }
        compose.waitForIdle()
        compose.cellNode(cross).assertContentDescriptionEquals("Mandatory Cross section, used")
        compose.cellNode(reservoir).assertContentDescriptionEquals("Mandatory Reservoir, used")
    }

    /** A straight or corner for every ordinary cell of the solver's route; the mandatory cells are already there. */
    private fun solverRoute(): List<Pair<Position, PipeSection>> {
        val path = (LevelSolver.solve(level) as SolverResult.Solvable).path
        val cells = listOf(level.start.position) + path + level.finish.position
        fun side(from: Position, to: Position) = Side.entries.first { from.neighbour(it) == to }
        return path.mapIndexedNotNull { i, cell ->
            if (level.mandatoryAt(cell) != null) return@mapIndexedNotNull null
            val sides = setOf(side(cell, cells[i]), side(cell, cells[i + 2]))
            cell to PipeSection.entries.first { it.alwaysOnStack && it.openSides == sides }
        }
    }

    private fun tick(ms: Long) {
        compose.runOnIdle { session.sendTick(ms) }
        compose.waitForIdle()
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
}
