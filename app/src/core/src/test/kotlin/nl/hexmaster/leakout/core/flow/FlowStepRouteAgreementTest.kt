package nl.hexmaster.leakout.core.flow

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import nl.hexmaster.leakout.core.board.Board
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.engine.GameCommand
import nl.hexmaster.leakout.core.engine.GameEngine
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.level.Endpoint
import nl.hexmaster.leakout.core.level.GridSize
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.MandatorySection
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.PipeSection.H
import nl.hexmaster.leakout.core.level.PipeSection.LB
import nl.hexmaster.leakout.core.level.PipeSection.LT
import nl.hexmaster.leakout.core.level.PipeSection.RB
import nl.hexmaster.leakout.core.level.PipeSection.RES
import nl.hexmaster.leakout.core.level.PipeSection.RT
import nl.hexmaster.leakout.core.level.PipeSection.V
import nl.hexmaster.leakout.core.level.PipeSection.X
import nl.hexmaster.leakout.core.level.PortalPair
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.core.level.TestLevels
import nl.hexmaster.leakout.core.level.exitFor
import nl.hexmaster.leakout.core.route.Route
import nl.hexmaster.leakout.core.route.RouteTracer

/** Following [FlowStep] from the start reaches the finish exactly when [RouteTracer] says the route is complete. */
class FlowStepRouteAgreementTest {
    private fun p(x: Int, y: Int) = Position(x, y)

    private class Case(val name: String, val level: Level, val pipes: List<Pair<Position, PipeSection>>) {
        val board: Board = pipes.fold(Board.from(level)) { board, (at, section) -> board.with(at, Cell.Pipe(section)) }
    }

    private fun level(
        start: Endpoint = Endpoint(p(0, 1), Side.RIGHT),
        finish: Endpoint = Endpoint(p(2, 3), Side.TOP),
        blocked: List<Position> = emptyList(),
        mandatory: List<MandatorySection> = emptyList(),
        portals: List<PortalPair> = emptyList(),
        leaky: List<Position> = emptyList(),
    ): Level = TestLevels.valid(
        grid = GridSize(5, 5), start = start, finish = finish, blocked = blocked, mandatory = mandatory, portals = portals, leaky = leaky,
    )

    /** Portal a at (2, 1) open to the left, next to the start's first tile; portal b at (3, 2) open to the left. */
    private val pair = TestLevels.pair(2, 1, Side.LEFT, 3, 2, Side.LEFT)

    private val cases = listOf(
        Case("through a portal pair", level(portals = listOf(pair)), listOf(p(1, 1) to H, p(2, 2) to RB)),
        Case("through a portal pair from b to a", level(portals = listOf(PortalPair(pair.b, pair.a))), listOf(p(1, 1) to H, p(2, 2) to RB)),
        Case(
            "closed side of a portal",
            level(portals = listOf(PortalPair(Endpoint(p(2, 1), Side.TOP), pair.b))),
            listOf(p(1, 1) to H, p(2, 2) to RB),
        ),
        Case("nothing in front of the exit portal", level(portals = listOf(pair)), listOf(p(1, 1) to H)),
        Case(
            "through a portal pair to a mandatory reservoir",
            level(
                portals = listOf(TestLevels.pair(2, 1, Side.LEFT, 4, 0, Side.BOTTOM)),
                mandatory = listOf(MandatorySection(p(3, 2), RES)),
            ),
            listOf(p(1, 1) to H, p(4, 1) to V, p(4, 2) to LT, p(2, 2) to RB),
        ),
        Case(
            "portal pair skipping a mandatory reservoir",
            level(portals = listOf(pair), mandatory = listOf(MandatorySection(p(0, 4), RES))),
            listOf(p(1, 1) to H, p(2, 2) to RB),
        ),
        Case("straight and corner", level(), listOf(p(1, 1) to H, p(2, 1) to LB, p(2, 2) to V)),
        Case("over two leaky tiles", level(leaky = listOf(p(1, 1), p(2, 2))), listOf(p(1, 1) to H, p(2, 1) to LB, p(2, 2) to V)),
        Case("leaky tile into an empty tile", level(leaky = listOf(p(1, 1), p(2, 1))), listOf(p(1, 1) to H)),
        Case(
            "cross on a leaky tile",
            level(start = Endpoint(p(0, 2), Side.RIGHT), finish = Endpoint(p(2, 4), Side.TOP), leaky = listOf(p(2, 2))),
            listOf(p(1, 2) to H, p(2, 2) to X, p(3, 2) to LT, p(3, 1) to LB, p(2, 1) to RB, p(2, 3) to V),
        ),
        Case("empty tile", level(), listOf(p(1, 1) to H)),
        Case("nothing placed", level(), emptyList()),
        Case("blocked tile", level(blocked = listOf(p(2, 1))), listOf(p(1, 1) to H)),
        Case("back into the start", level(), listOf(p(1, 1) to LB, p(1, 2) to LT, p(0, 2) to RT)),
        Case("no matching opening", level(), listOf(p(1, 1) to V)),
        Case("finish from the wrong side", level(), listOf(p(1, 1) to LB, p(1, 2) to V, p(1, 3) to RT)),
        Case("off the grid", level(), listOf(p(1, 1) to LT, p(1, 0) to V)),
        Case("start opens off the grid", level(start = Endpoint(p(0, 1), Side.LEFT)), emptyList()),
        Case("reservoir from the top", level(), listOf(p(1, 1) to LB, p(1, 2) to RES)),
        Case("reservoir left to right", level(), listOf(p(1, 1) to RES, p(2, 1) to LB, p(2, 2) to V)),
        Case("cross straight through", level(), listOf(p(1, 1) to X, p(2, 1) to LB, p(2, 2) to V)),
        Case(
            "both channels of one cross",
            level(start = Endpoint(p(0, 2), Side.RIGHT), finish = Endpoint(p(2, 4), Side.TOP)),
            listOf(p(1, 2) to H, p(2, 2) to X, p(3, 2) to LT, p(3, 1) to LB, p(2, 1) to RB, p(2, 3) to V),
        ),
        Case(
            "back into a used channel",
            level(),
            listOf(p(1, 1) to X, p(2, 1) to LT, p(2, 0) to LB, p(1, 0) to RB, p(1, 2) to LT, p(0, 2) to RT),
        ),
        Case(
            "route skips a mandatory reservoir",
            level(mandatory = listOf(MandatorySection(p(4, 4), RES))),
            listOf(p(1, 1) to H, p(2, 1) to LB, p(2, 2) to V),
        ),
        Case(
            "route passes a mandatory cross on one channel",
            level(mandatory = listOf(MandatorySection(p(1, 1), X))),
            listOf(p(2, 1) to LB, p(2, 2) to V),
        ),
        Case(
            "route passes every mandatory channel",
            level(
                start = Endpoint(p(0, 2), Side.RIGHT),
                finish = Endpoint(p(2, 4), Side.TOP),
                mandatory = listOf(MandatorySection(p(1, 2), RES), MandatorySection(p(2, 2), X)),
            ),
            listOf(p(3, 2) to LT, p(3, 1) to LB, p(2, 1) to RB, p(2, 3) to V),
        ),
    )

    /**
     * Walks the flow one step at a time, keeping the used channels, until it finishes or leaks. The
     * finish only counts when every mandatory section is used, as in the flow simulation.
     */
    private fun flowReachesFinish(board: Board, level: Level): Boolean {
        val used = mutableSetOf<ChannelUse>()
        var from = level.start.position
        var exit = level.start.openSide
        while (true) {
            when (val result = FlowStep.after(board, from, exit, used)) {
                FlowStep.Result.Finish -> return MandatoryProgress.unused(level.mandatory, used).isEmpty()
                is FlowStep.Result.Leak -> return false
                is FlowStep.Result.Enter -> when (val cell = board[result.next]) {
                    is Cell.Portal -> {
                        used += portalPairUses(result.next, cell.partner)
                        exit = (board[cell.partner] as Cell.Portal).openSide
                        from = cell.partner
                    }
                    else -> {
                        val section = (cell as Cell.Pipe).section
                        used += ChannelUse(result.next, section.channelFor(result.entry))
                        exit = section.exitFor(result.entry)!!
                        from = result.next
                    }
                }
            }
        }
    }

    @Test
    fun `flow step and route tracer agree on every scripted board`() {
        for (case in cases) {
            val complete = RouteTracer.trace(case.board, case.level) is Route.Complete
            assertEquals(complete, flowReachesFinish(case.board, case.level), case.name)
        }
    }

    @Test
    fun `the flow simulation wins on every board the route check calls complete`() {
        val engine = GameEngine(Random(1))
        for (case in cases) {
            val complete = RouteTracer.trace(case.board, case.level) is Route.Complete
            var state = engine.start(case.level).copy(board = case.board)
            repeat(100_000) { if (state.outcome is Outcome.Playing) state = engine.handle(state, GameCommand.Tick(50)) }
            assertEquals(complete, state.outcome is Outcome.Won, "${case.name}: ${state.outcome}")
        }
    }

    @Test
    fun `the scripted boards include portal pairs`() {
        val withPortals = cases.filter { it.level.portals.isNotEmpty() }
        assertEquals(setOf(true, false), withPortals.map { RouteTracer.trace(it.board, it.level) is Route.Complete }.toSet())
    }

    @Test
    fun `the scripted boards include complete and open routes`() {
        val results = cases.map { RouteTracer.trace(it.board, it.level) is Route.Complete }.toSet()
        assertEquals(setOf(true, false), results)
    }
}
