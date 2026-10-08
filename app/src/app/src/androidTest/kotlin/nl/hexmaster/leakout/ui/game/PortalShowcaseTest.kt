package nl.hexmaster.leakout.ui.game

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.random.Random
import nl.hexmaster.leakout.BundledLevels
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.level.Endpoint
import nl.hexmaster.leakout.core.level.GridSize
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.LevelSolver
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.PortalPair
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.core.level.SolverResult
import nl.hexmaster.leakout.settings.InMemorySettingsRepository
import nl.hexmaster.leakout.settings.TipId
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Portal pairs on the real game screen: a small level whose wall can only be crossed through its
 * pair, and the bundled level 49, the first level with portals.
 */
@RunWith(AndroidJUnit4::class)
class PortalShowcaseTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var session: GameSession

    // A wall down column 3; the pair leads from (2, 1), open at the top, to (4, 2), open at the bottom.
    private val entry = Position(2, 1)
    private val exit = Position(4, 2)
    private val walled: Level = FieldTest.level(
        grid = GridSize(6, 4),
        finish = Endpoint(Position(5, 3), Side.LEFT),
        blocked = (0 until 4).map { Position(3, it) },
    ).copy(portals = listOf(PortalPair(Endpoint(entry, Side.TOP), Endpoint(exit, Side.BOTTOM))))

    @Test
    fun bothPortalsShowTheirPairAndWhereTheyLead() {
        show(walled)
        assertEquals(Cell.Portal(1, Side.TOP, exit), session.state.board[entry])
        assertEquals(Cell.Portal(1, Side.BOTTOM, entry), session.state.board[exit])
        compose.cellNode(entry).assertContentDescriptionEquals("Portal pair 1, circle, opens upwards, leads to column 5, row 3, not passed yet")
        compose.cellNode(exit).assertContentDescriptionEquals("Portal pair 1, circle, opens downwards, leads to column 3, row 2, not passed yet")
    }

    @Test
    fun theLiquidPassesThePairAndTheLevelIsWon() {
        show(walled)
        compose.runOnIdle {
            for ((at, section) in solverRoute(walled)) assertTrue("place $section on $at", session.placeSection(section, at))
            session.flowToEnd()
            assertTrue(session.state.outcome is Outcome.Won)
        }
        compose.waitForIdle()
        compose.cellNode(entry).assertContentDescriptionEquals("Portal pair 1, circle, opens upwards, leads to column 5, row 3, passed")
        compose.cellNode(exit).assertContentDescriptionEquals("Portal pair 1, circle, opens downwards, leads to column 3, row 2, passed")
    }

    @Test
    fun level49IsWonThroughItsPair() {
        val level = checkNotNull(BundledLevels(InstrumentationRegistry.getInstrumentation().targetContext.assets).load(49))
        assertEquals(1, level.portals.size)
        show(level)
        compose.runOnIdle {
            for ((at, section) in solverRoute(level)) assertTrue("place $section on $at", session.placeSection(section, at))
            session.flowToEnd()
            assertTrue(session.state.outcome is Outcome.Won)
        }
    }

    /**
     * A straight or corner for every ordinary cell of the solver's route. The portals are already on
     * the board; a cell next to one on the route connects towards it, so towards its open side.
     */
    private fun solverRoute(level: Level): List<Pair<Position, PipeSection>> {
        val path = (LevelSolver.solve(level) as SolverResult.Solvable).path
        val cells = listOf(level.start.position) + path + level.finish.position
        fun side(from: Position, to: Position) = Side.entries.first { from.neighbour(it) == to }
        return path.mapIndexedNotNull { i, cell ->
            if (level.portalAt(cell) != null || level.mandatoryAt(cell) != null) return@mapIndexedNotNull null
            val sides = setOf(side(cell, cells[i]), side(cell, cells[i + 2]))
            cell to PipeSection.entries.first { it.alwaysOnStack && it.openSides == sides }
        }
    }

    private fun show(level: Level) {
        session = GameSession(level, Random(7))
        // The portal tip is not under test here.
        val tips = InMemorySettingsRepository(initialSeenTips = TipId.entries.toSet())
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(800.dp, 360.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
                    GameScreen(level = level, onExit = {}, session = session, tips = tips)
                }
            }
        }
        compose.waitForIdle()
    }
}
