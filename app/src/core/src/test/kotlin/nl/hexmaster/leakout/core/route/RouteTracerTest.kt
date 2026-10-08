package nl.hexmaster.leakout.core.route

import kotlin.test.Test
import kotlin.test.assertEquals
import nl.hexmaster.leakout.core.board.Board
import nl.hexmaster.leakout.core.board.Cell
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
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.core.level.TestLevels

class RouteTracerTest {
    /** By default: start at (0, 1) open to the right, finish at (2, 3) open at the top, on a 5 by 5 grid. */
    private fun level(
        start: Endpoint = Endpoint(p(0, 1), Side.RIGHT),
        finish: Endpoint = Endpoint(p(2, 3), Side.TOP),
        blocked: List<Position> = emptyList(),
        mandatory: List<MandatorySection> = emptyList(),
    ): Level = TestLevels.valid(grid = GridSize(5, 5), start = start, finish = finish, blocked = blocked, mandatory = mandatory)

    private fun board(level: Level, vararg pipes: Pair<Position, PipeSection>): Board =
        pipes.fold(Board.from(level)) { board, (at, section) -> board.with(at, Cell.Pipe(section)) }

    private fun p(x: Int, y: Int) = Position(x, y)

    private fun trace(level: Level, vararg pipes: Pair<Position, PipeSection>): Route =
        RouteTracer.trace(board(level, *pipes), level)

    @Test
    fun `a straight and corner route is complete and lists its cells in order`() {
        val route = trace(level(), p(1, 1) to H, p(2, 1) to LB, p(2, 2) to V)
        assertEquals(Route.Complete(listOf(p(1, 1), p(2, 1), p(2, 2))), route)
    }

    @Test
    fun `an empty tile leaves the route open`() {
        assertEquals(Route.Open(p(2, 1), Route.Reason.EMPTY), trace(level(), p(1, 1) to H))
    }

    @Test
    fun `nothing placed leaves the route open at the tile next to the start`() {
        assertEquals(Route.Open(p(1, 1), Route.Reason.EMPTY), trace(level()))
    }

    @Test
    fun `a blocked tile leaves the route open`() {
        val level = level(blocked = listOf(p(2, 1)))
        assertEquals(Route.Open(p(2, 1), Route.Reason.BLOCKED), trace(level, p(1, 1) to H))
    }

    @Test
    fun `running back into the start leaves the route open`() {
        // (1, 1) turns down, (1, 2) turns left, (0, 2) turns up into the start tile.
        val route = trace(level(), p(1, 1) to LB, p(1, 2) to LT, p(0, 2) to RT)
        assertEquals(Route.Open(p(0, 1), Route.Reason.BLOCKED), route)
    }

    @Test
    fun `a section without a matching opening leaves the route open`() {
        assertEquals(Route.Open(p(1, 1), Route.Reason.NO_OPENING), trace(level(), p(1, 1) to V))
    }

    @Test
    fun `reaching the finish from the wrong side leaves the route open`() {
        // Down from (1, 1) to (1, 3), then right into the finish's left side; the finish is open at the top.
        val route = trace(level(), p(1, 1) to LB, p(1, 2) to V, p(1, 3) to RT)
        assertEquals(Route.Open(p(2, 3), Route.Reason.WRONG_SIDE), route)
    }

    @Test
    fun `leaving the grid leaves the route open`() {
        val route = trace(level(), p(1, 1) to LT, p(1, 0) to V)
        assertEquals(Route.Open(p(1, -1), Route.Reason.OFF_GRID), route)
    }

    @Test
    fun `a start that opens off the grid leaves the route open`() {
        val level = level(start = Endpoint(p(0, 1), Side.LEFT))
        assertEquals(Route.Open(p(-1, 1), Route.Reason.OFF_GRID), trace(level))
    }

    @Test
    fun `a reservoir entered from the top leaves the route open`() {
        val route = trace(level(), p(1, 1) to LB, p(1, 2) to RES)
        assertEquals(Route.Open(p(1, 2), Route.Reason.NO_OPENING), route)
    }

    @Test
    fun `a reservoir is passed from left to right`() {
        val route = trace(level(), p(1, 1) to RES, p(2, 1) to LB, p(2, 2) to V)
        assertEquals(Route.Complete(listOf(p(1, 1), p(2, 1), p(2, 2))), route)
    }

    @Test
    fun `a cross is passed straight through`() {
        val route = trace(level(), p(1, 1) to X, p(2, 1) to LB, p(2, 2) to V)
        assertEquals(Route.Complete(listOf(p(1, 1), p(2, 1), p(2, 2))), route)
    }

    @Test
    fun `a route that uses both channels of one cross is complete`() {
        // Across the cross at (2, 2) to the right, up and round, then down through the same cross.
        val level = level(start = Endpoint(p(0, 2), Side.RIGHT), finish = Endpoint(p(2, 4), Side.TOP))
        val route = trace(
            level,
            p(1, 2) to H, p(2, 2) to X, p(3, 2) to LT, p(3, 1) to LB, p(2, 1) to RB, p(2, 3) to V,
        )
        assertEquals(
            Route.Complete(listOf(p(1, 2), p(2, 2), p(3, 2), p(3, 1), p(2, 1), p(2, 2), p(2, 3))),
            route,
        )
    }

    @Test
    fun `a route cannot come back into a channel it already used`() {
        // Round the cross at (1, 1) and down through it on the other axis: the only way back to its
        // horizontal channel would be to run the route backwards, which ends in the start tile.
        val route = trace(
            level(),
            p(1, 1) to X, p(2, 1) to LT, p(2, 0) to LB, p(1, 0) to RB, p(1, 2) to LT, p(0, 2) to RT,
        )
        assertEquals(Route.Open(p(0, 1), Route.Reason.BLOCKED), route)
    }

    // --- Mandatory sections ---

    /** Start (0, 2) open right, finish (2, 4) open at the top; the cross at (2, 2) can be passed both ways. */
    private fun crossLevel(mandatory: List<MandatorySection>) =
        level(start = Endpoint(p(0, 2), Side.RIGHT), finish = Endpoint(p(2, 4), Side.TOP), mandatory = mandatory)

    @Test
    fun `a route that skips a mandatory reservoir is open at the finish`() {
        val level = level(mandatory = listOf(MandatorySection(p(4, 4), RES)))
        val route = trace(level, p(1, 1) to H, p(2, 1) to LB, p(2, 2) to V)
        assertEquals(Route.Open(p(2, 3), Route.Reason.MANDATORY_MISSED), route)
    }

    @Test
    fun `a route through one channel of a mandatory cross is open at the finish`() {
        val level = level(mandatory = listOf(MandatorySection(p(1, 1), X)))
        val route = trace(level, p(2, 1) to LB, p(2, 2) to V)
        assertEquals(Route.Open(p(2, 3), Route.Reason.MANDATORY_MISSED), route)
    }

    @Test
    fun `a route through every mandatory channel is complete`() {
        val level = crossLevel(listOf(MandatorySection(p(1, 2), RES), MandatorySection(p(2, 2), X)))
        val route = trace(level, p(3, 2) to LT, p(3, 1) to LB, p(2, 1) to RB, p(2, 3) to V)
        assertEquals(
            Route.Complete(listOf(p(1, 2), p(2, 2), p(3, 2), p(3, 1), p(2, 1), p(2, 2), p(2, 3))),
            route,
        )
    }

    @Test
    fun `a mandatory section on an unfinished route does not change why it is open`() {
        val level = level(mandatory = listOf(MandatorySection(p(4, 4), RES)))
        assertEquals(Route.Open(p(2, 1), Route.Reason.EMPTY), trace(level, p(1, 1) to H))
    }

    @Test
    fun `liquid already in the first sections does not change a complete route`() {
        val level = level()
        val board = board(level, p(1, 1) to H, p(2, 1) to LB, p(2, 2) to V)
            .markLiquidEntered(p(1, 1))
            .markLiquidEntered(p(2, 1))
        assertEquals(Route.Complete(listOf(p(1, 1), p(2, 1), p(2, 2))), RouteTracer.trace(board, level))
    }
}
