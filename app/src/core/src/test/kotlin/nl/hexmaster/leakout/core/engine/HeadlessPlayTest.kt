package nl.hexmaster.leakout.core.engine

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.engine.GameCommand.Place
import nl.hexmaster.leakout.core.engine.GameCommand.Tick
import nl.hexmaster.leakout.core.level.Endpoint
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.LevelParser
import nl.hexmaster.leakout.core.level.LevelSolver
import nl.hexmaster.leakout.core.level.MandatorySection
import nl.hexmaster.leakout.core.level.ParseResult
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.core.level.SolverResult

/**
 * Plays bundled levels from start to end with commands only, the way a player would: the section at
 * the bottom of the stack goes on the route when it fits, and on a spare tile when it does not.
 */
class HeadlessPlayTest {
    private val engine = engine(seed = 11)

    private fun bundled(id: Int): Level {
        val dir = File(System.getProperty("leakout.levelsDir") ?: fail("System property leakout.levelsDir is not set"))
        val file = File(dir, "level_%03d.json".format(id))
        return assertIs<ParseResult.Success>(LevelParser.parse(file.readText())).level
    }

    /**
     * The straight or corner section for each cell of the solver's path, so the path carries the
     * liquid. The level's mandatory sections are already on the board and are left out.
     */
    private fun routeFor(level: Level): List<Pair<Position, PipeSection>> {
        val path = assertIs<SolverResult.Solvable>(LevelSolver.solve(level)).path
        val cells = listOf(level.start.position) + path + level.finish.position
        return path.mapIndexed { i, cell ->
            val sides = setOf(sideTowards(cell, cells[i]), sideTowards(cell, cells[i + 2]))
            cell to PipeSection.entries.first { it != PipeSection.X && it != PipeSection.RES && it.openSides == sides }
        }.filter { (cell, _) -> level.mandatoryAt(cell) == null }
    }

    private fun sideTowards(from: Position, to: Position): Side = Side.entries.first { from.neighbour(it) == to }

    /**
     * Places the sections of [route] in order through [Place] commands. A section that is not needed
     * yet goes on a spare tile, replacing one there once all spare tiles are taken. Placing costs no
     * time; the clock only moves to wait for a replace lock. Spare tiles are never on [reserved].
     */
    private fun build(start: GameState, route: List<Pair<Position, PipeSection>>, reserved: List<Position> = route.map { it.first }): GameState {
        val onRoute = reserved.toSet()
        val spares = start.level.grid.let { grid -> (0 until grid.columns).flatMap { x -> (0 until grid.rows).map { y -> Position(x, y) } } }
            .filter { it !in onRoute && start.board[it] == Cell.Empty }
        var state = start
        var spareIndex = 0
        for ((at, section) in route) {
            var guard = 0
            while (state.board[at] != Cell.Pipe(section)) {
                check(++guard < 10_000) { "the stack never gave $section" }
                val target = if (state.stack.next == section) at else spares[spareIndex++ % spares.size]
                if (!engine.canPlace(state, target)) state = engine.handle(state, Tick(state.level.rules.replacePenaltyMs))
                val placed = engine.handle(state, Place(target))
                assertTrue(placed !== state, "placing on $target was refused")
                state = placed
            }
        }
        return state
    }

    private fun play(state: GameState): GameState = engine.runToEnd(state, dtMs = 16)

    @Test
    fun `level 1 is played from start to a win`() {
        val level = bundled(1)
        val route = routeFor(level)
        val built = build(engine.start(level), route)
        assertTrue(built.routeComplete)
        assertIs<FlowState.Waiting>(built.flow)

        val end = play(built)
        assertIs<Outcome.Won>(end.outcome)
        assertEquals(route.sumOf { (_, section) -> if (section == PipeSection.H || section == PipeSection.V) 10 else 15 }, end.score)
        assertTrue(route.all { (at, _) -> (end.board[at] as Cell.Pipe).liquidEntered })
    }

    @Test
    fun `level 1 is played from start to a leak`() {
        val level = bundled(1)
        val route = routeFor(level)
        val built = build(engine.start(level), route.take(3), reserved = route.map { it.first })
        assertTrue(!built.routeComplete)

        val end = play(built)
        val lost = assertIs<Outcome.Lost>(end.outcome)
        val last = route[2].first
        assertEquals(last, lost.at)
        assertEquals(30, end.score)
        assertTrue(engine.handle(end, Place(route[3].first)) === end, "no placing after the loss")
    }

    /**
     * Start (0, 2) open right into a mandatory reservoir at (1, 2), then a mandatory cross at (2, 2);
     * the finish at (2, 4) is open at the top. The stack deals standard sections only.
     */
    private val mandatoryLevel = engineLevel(
        section = null,
        start = Endpoint(Position(0, 2), Side.RIGHT),
        finish = Endpoint(Position(2, 4), Side.TOP),
        mandatory = listOf(MandatorySection(Position(1, 2), PipeSection.RES), MandatorySection(Position(2, 2), PipeSection.X)),
    )

    @Test
    fun `a level with a mandatory cross and reservoir is won through both channels of the cross`() {
        // Up and round after the cross, then down through it into the finish.
        val route = listOf(
            Position(3, 2) to PipeSection.LT,
            Position(3, 1) to PipeSection.LB,
            Position(2, 1) to PipeSection.RB,
            Position(2, 3) to PipeSection.V,
        )
        val built = build(engine.start(mandatoryLevel), route)
        assertTrue(built.routeComplete)

        val end = play(built)
        assertIs<Outcome.Won>(end.outcome)
        assertEquals(25 + 10 + 15 + 15 + 15 + 50 + 10, end.score)
        for (at in listOf(Position(1, 2), Position(2, 2))) {
            val pipe = assertIs<Cell.Pipe>(end.board[at])
            assertTrue(pipe.mandatory && pipe.liquidEntered, "mandatory section at $at")
            assertTrue(at !in end.finalScore!!.unusedSections)
        }
    }

    @Test
    fun `a level with a mandatory cross is lost when the liquid passes it on one channel only`() {
        // Down and round below the cross into the finish: the cross's vertical channel is never used.
        val route = listOf(
            Position(3, 2) to PipeSection.LB,
            Position(3, 3) to PipeSection.LT,
            Position(2, 3) to PipeSection.RB,
        )
        val built = build(engine.start(mandatoryLevel), route)
        assertTrue(!built.routeComplete, "a route that misses a mandatory channel is not complete")

        val end = play(built)
        val lost = assertIs<Outcome.Lost>(end.outcome)
        assertEquals(LossReason.MANDATORY_MISSED, lost.reason)
        assertEquals(Position(2, 4) to Side.TOP, lost.at to lost.side)
        assertEquals(25 + 10 + 15 + 15 + 15, end.score)
        assertTrue(engine.handle(end, Place(Position(4, 4))) === end, "no placing after the loss")
    }

    @Test
    fun `every bundled level can be won by following the solver's path`() {
        val dir = File(System.getProperty("leakout.levelsDir")!!)
        val ids = dir.listFiles { f -> f.name.matches(Regex("""level_\d{3}\.json""")) }!!.map { it.name.substring(6, 9).toInt() }.sorted()
        assertTrue(ids.isNotEmpty())
        for (id in ids) {
            val level = bundled(id)
            // A fixed, known stack: this checks the flow over the route, not the luck of the draw.
            val route = routeFor(level)
            val state = route.fold(engine.start(level)) { s, (at, section) ->
                s.copy(stack = s.stack.copy(sections = List(s.stack.size) { section })).let { engine.handle(it, Place(at)) }
            }
            assertTrue(state.routeComplete, "level $id: the solver's path is a complete route")
            assertIs<Outcome.Won>(play(state).outcome, "level $id")
        }
    }
}
