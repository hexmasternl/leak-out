package nl.hexmaster.leakout.core.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.engine.GameCommand.FastForward
import nl.hexmaster.leakout.core.engine.GameCommand.Place
import nl.hexmaster.leakout.core.engine.GameCommand.Tick
import nl.hexmaster.leakout.core.flow.Channel
import nl.hexmaster.leakout.core.flow.ChannelUse
import nl.hexmaster.leakout.core.level.Endpoint
import nl.hexmaster.leakout.core.level.FlowSettings
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
import nl.hexmaster.leakout.core.score.ScoreSheet

class FlowSimulationTest {
    private val engine = engine()
    private val tolerance = 1e-9

    private fun flowing(state: GameState): FlowState.Flowing = assertIs<FlowState.Flowing>(state.flow)

    private fun level(cellsPerSecond: Double = 1.0, ramp: Double = 0.0, reservoir: Double = 0.3, delay: Long = 10_000) =
        engineLevel(flow = FlowSettings(delay, cellsPerSecond, reservoirSpeedFactor = reservoir, speedRampPerCell = ramp))

    /** The countdown is over and the head is at the very start of the start tile. */
    private fun atStart(state: GameState): GameState = state.copy(flow = FlowSimulation.startFlow(state))

    // --- Countdown ---

    @Test
    fun `ticks run the countdown down without starting the liquid`() {
        val state = engine.send(engine.start(level()), Tick(1_000), Tick(3_000))
        assertEquals(FlowState.Waiting(6_000), state.flow)
    }

    @Test
    fun `the tick that ends the countdown moves the liquid with its leftover`() {
        val waiting = engine.start(level()).copy(flow = FlowState.Waiting(100))
        val state = engine.handle(waiting, Tick(300))
        val head = flowing(state)
        assertEquals(p(0, 1), head.position)
        assertEquals(Side.LEFT, head.entry)
        assertEquals(0.2, head.progress, tolerance)
    }

    @Test
    fun `a tick of exactly the countdown starts the liquid at progress 0`() {
        val state = engine.handle(engine.start(level()), Tick(10_000))
        assertEquals(0.0, flowing(state).progress)
    }

    @Test
    fun `fast forward runs the countdown ten times as fast but not the clock`() {
        val fast = engine.handle(engine.start(level()).withPipes(ROUTE).copy(flow = FlowState.Waiting(5_000)), FastForward)
        val state = engine.handle(fast, Tick(100))
        assertEquals(FlowState.Waiting(4_000), state.flow)
        assertEquals(100L, state.clockMs)
    }

    // --- The head ---

    @Test
    fun `progress within a tile follows the speed`() {
        val state = engine.start(level(cellsPerSecond = 0.5)).withPipes(ROUTE).headAt(p(1, 1), Side.LEFT, 0.0)
        val head = flowing(engine.handle(state, Tick(1_000)))
        assertEquals(p(1, 1), head.position)
        assertEquals(0.5, head.progress, tolerance)
    }

    @Test
    fun `overshoot carries over to the next tile`() {
        val state = engine.start(level()).withPipes(ROUTE).headAt(p(1, 1), Side.LEFT, 0.9)
        val head = flowing(engine.handle(state, Tick(300)))
        assertEquals(p(2, 1), head.position)
        assertEquals(Side.LEFT, head.entry)
        assertEquals(0.2, head.progress, tolerance)
    }

    @Test
    fun `the speed ramps up with every section left`() {
        val sheet = ScoreSheet(total = 40, passes = mapOf(p(0, 3) to 1, p(1, 3) to 1, p(2, 4) to 1, p(3, 4) to 1))
        val state = engine.start(level(ramp = 0.05)).withPipes(ROUTE).copy(sheet = sheet).headAt(p(1, 1), Side.LEFT, 0.0)
        assertEquals(4, state.sectionsLeft)
        assertEquals(1.2, state.flowSpeed, tolerance)
        assertEquals(0.6, flowing(engine.handle(state, Tick(500))).progress, tolerance)
    }

    @Test
    fun `the ramp counts sections as the head leaves them`() {
        val start = engine.start(level(ramp = 0.5)).withPipes(ROUTE)
        assertEquals(1.0, start.flowSpeed)
        // Countdown, then the start tile (no points) and the H at 1 cell per second.
        val state = engine.handle(start, Tick(12_000))
        assertEquals(1, state.sectionsLeft)
        assertEquals(1.5, state.flowSpeed, tolerance)
    }

    @Test
    fun `a reservoir slows the head but not the HUD speed`() {
        val state = engine.start(level(reservoir = 0.3)).withPipes(p(1, 1) to RES).headAt(p(1, 1), Side.LEFT, 0.0)
        val after = engine.handle(state, Tick(1_000))
        assertEquals(0.3, flowing(after).progress, tolerance)
        assertEquals(1.0, after.flowSpeed)
    }

    @Test
    fun `a section counts as entered as soon as the head enters it`() {
        val state = engine.start(level()).withPipes(ROUTE).headAt(p(1, 1), Side.LEFT, 0.95)
        val after = engine.handle(state, Tick(100)) // into the LB at progress 0.05
        assertEquals(p(2, 1), flowing(after).position)
        assertEquals(Cell.Pipe(LB, liquidEntered = true), after.board[p(2, 1)])
        assertEquals(Cell.Pipe(V, liquidEntered = false), after.board[p(2, 2)])
        assertSame(after, engine.handle(after, Place(p(2, 1))))
    }

    @Test
    fun `a section the head is in cannot be replaced`() {
        val state = engine.start(level()).withPipes(ROUTE).headAt(p(1, 1), Side.LEFT, 0.1)
        assertSame(state, engine.handle(state, Place(p(1, 1))))
    }

    // --- Leaving a tile ---

    @Test
    fun `the last section scores before the leak`() {
        val state = engine.start(level()).withPipes(ROUTE.take(2)).copy(sheet = ScoreSheet(total = 40)).headAt(p(2, 1), Side.LEFT, 0.5)
        val after = engine.handle(state, Tick(1_000))
        assertEquals(55, after.score)
        assertEquals(Outcome.Lost(p(2, 1), Side.BOTTOM, atMs = 1_000), after.outcome)
        assertEquals(listOf(Award(p(2, 1), 15, 1_000)), after.awards)
    }

    @Test
    fun `a leak straight from the start scores nothing`() {
        val after = engine.handle(atStart(engine.start(level())), Tick(1_000))
        assertEquals(Outcome.Lost(p(0, 1), Side.RIGHT, atMs = 1_000), after.outcome)
        assertEquals(0, after.score)
        assertEquals(emptyList(), after.awards)
    }

    @Test
    fun `the finish is won with the last section's points`() {
        val state = engine.start(level()).withPipes(ROUTE).copy(sheet = ScoreSheet(total = 25)).headAt(p(2, 2), Side.TOP, 0.5)
        val after = engine.handle(state, Tick(600))
        assertEquals(35, after.score)
        assertEquals(Outcome.Won(atMs = 600), after.outcome)
        assertEquals(FlowState.Ended(setOf(ChannelUse(p(2, 2), Channel.SINGLE))), after.flow)
    }

    @Test
    fun `the second pass through a cross gives 50`() {
        val level = engineLevel(start = Endpoint(p(0, 2), Side.RIGHT), finish = Endpoint(p(2, 4), Side.TOP))
        val route = listOf(p(1, 2) to H, p(2, 2) to X, p(3, 2) to LT, p(3, 1) to LB, p(2, 1) to RB, p(2, 3) to V)
        val won = engine.runToEnd(engine.start(level).withPipes(route), dtMs = 50)
        assertIs<Outcome.Won>(won.outcome)
        assertEquals(10 + 10 + 15 + 15 + 15 + 50 + 10, won.score)
        assertEquals(2, won.sheet.passes[p(2, 2)])
        assertTrue(ChannelUse(p(2, 2), Channel.HORIZONTAL) in won.flow.used)
        assertTrue(ChannelUse(p(2, 2), Channel.VERTICAL) in won.flow.used)
    }

    @Test
    fun `reaching a cross on a channel it already used leaks`() {
        // The liquid passed the cross at (1, 1) from left to right and now leaves the H at (2, 1) to the left.
        val scripted = engine.start(level()).withPipes(p(1, 1) to X, p(2, 1) to H).headAt(p(2, 1), Side.RIGHT, 0.5)
        val state = scripted.copy(flow = (scripted.flow as FlowState.Flowing).copy(used = setOf(ChannelUse(p(1, 1), Channel.HORIZONTAL))))
        val after = engine.handle(state, Tick(1_000))
        assertEquals(Outcome.Lost(p(2, 1), Side.LEFT, atMs = 1_000), after.outcome)
    }

    @Test
    fun `the leak reports the tile and side the liquid left`() {
        // An LT at (1, 1) takes the liquid from the left and sends it up into (1, 0), which is empty.
        val after = engine.runToEnd(engine.start(level()).withPipes(p(1, 1) to LT))
        assertEquals(p(1, 1) to Side.TOP, (after.outcome as Outcome.Lost).let { it.at to it.side })
    }

    // --- Large ticks ---

    @Test
    fun `one long tick walks every section in route order and wins`() {
        val state = atStart(engine.start(level(cellsPerSecond = 2.0)).withPipes(ROUTE))
        val after = engine.handle(state, Tick(5_000))
        assertEquals(ROUTE.map { it.first }, after.awards.map { it.position })
        assertEquals(listOf(10, 15, 10), after.awards.map { it.points })
        assertEquals(Outcome.Won(atMs = 5_000), after.outcome)
        assertIs<FlowState.Ended>(after.flow)
        assertEquals(after.flow, engine.handle(after, Tick(5_000)).flow)
    }

    @Test
    fun `a long tick into a leak stops at the right tile`() {
        val state = atStart(engine.start(level()).withPipes(ROUTE.take(2)))
        val after = engine.handle(state, Tick(60_000))
        assertEquals(listOf(p(1, 1), p(2, 1)), after.awards.map { it.position })
        assertEquals(Outcome.Lost(p(2, 1), Side.BOTTOM, atMs = 60_000), after.outcome)
    }

    @Test
    fun `1 ms ticks and one long tick give the same outcome and score`() {
        val level = level(cellsPerSecond = 0.7, ramp = 0.13, reservoir = 0.4, delay = 1_000)
        val route = listOf(p(1, 1) to RES, p(2, 1) to LB, p(2, 2) to V)
        val start = engine.start(level).withPipes(route)
        val long = engine.handle(start, Tick(60_000))
        val short = engine.runToEnd(start, dtMs = 1)
        assertIs<Outcome.Won>(long.outcome)
        assertIs<Outcome.Won>(short.outcome)
        assertEquals(long.score, short.score)
        assertEquals(long.sheet, short.sheet)
        assertEquals(long.board, short.board)
        assertEquals(long.flow, short.flow)
    }

    @Test
    fun `1 ms ticks reach a leak at the same tile as one long tick`() {
        val start = engine.start(level(cellsPerSecond = 0.9, ramp = 0.2, delay = 500)).withPipes(ROUTE.take(2))
        val long = engine.handle(start, Tick(60_000)).outcome as Outcome.Lost
        val short = engine.runToEnd(start, dtMs = 1).outcome as Outcome.Lost
        assertEquals(long.at to long.side, short.at to short.side)
    }

    // --- Flow and route check agree ---

    private class Case(val name: String, val start: Endpoint, val finish: Endpoint, val pipes: List<Pair<Position, PipeSection>>)

    private val defaultStart = Endpoint(p(0, 1), Side.RIGHT)
    private val defaultFinish = Endpoint(p(2, 3), Side.TOP)

    // The complete and open boards of the route agreement test, run through the engine.
    private val cases = listOf(
        Case("straight and corner", defaultStart, defaultFinish, ROUTE),
        Case("reservoir left to right", defaultStart, defaultFinish, listOf(p(1, 1) to RES, p(2, 1) to LB, p(2, 2) to V)),
        Case("cross straight through", defaultStart, defaultFinish, listOf(p(1, 1) to X, p(2, 1) to LB, p(2, 2) to V)),
        Case(
            "both channels of one cross",
            Endpoint(p(0, 2), Side.RIGHT),
            Endpoint(p(2, 4), Side.TOP),
            listOf(p(1, 2) to H, p(2, 2) to X, p(3, 2) to LT, p(3, 1) to LB, p(2, 1) to RB, p(2, 3) to V),
        ),
        Case("long way round", defaultStart, defaultFinish, listOf(p(1, 1) to H, p(2, 1) to H, p(3, 1) to LB, p(3, 2) to LT, p(2, 2) to RB)),
        Case("empty tile", defaultStart, defaultFinish, listOf(p(1, 1) to H)),
        Case("no matching opening", defaultStart, defaultFinish, listOf(p(1, 1) to V)),
        Case("finish from the wrong side", defaultStart, defaultFinish, listOf(p(1, 1) to LB, p(1, 2) to V, p(1, 3) to RT)),
        Case("back into the start", defaultStart, defaultFinish, listOf(p(1, 1) to LB, p(1, 2) to LT, p(0, 2) to RT)),
    )

    @Test
    fun `every complete route is won and every open route is lost`() {
        var complete = 0
        for (case in cases) {
            val state = engine.start(engineLevel(start = case.start, finish = case.finish)).withPipes(case.pipes)
            val end = engine.runToEnd(state, dtMs = 33)
            if (state.routeComplete) {
                complete++
                assertIs<Outcome.Won>(end.outcome, case.name)
            } else {
                assertIs<Outcome.Lost>(end.outcome, case.name)
            }
        }
        assertTrue(complete in 1 until cases.size, "the cases include complete and open routes")
    }

    @Test
    fun `the clock keeps running after an outcome while the score and flow stay`() {
        val lost = engine.runToEnd(engine.start(level()).withPipes(ROUTE.take(1)))
        val later = engine.handle(lost, Tick(300))
        assertEquals(lost.clockMs + 300, later.clockMs)
        assertEquals(lost.score, later.score)
        assertEquals(lost.flow, later.flow)
        assertEquals(lost.outcome, later.outcome)

        val won = engine.runToEnd(engine.start(level()).withPipes(ROUTE))
        val wonLater = engine.send(won, Tick(300), Tick(300))
        assertEquals(won.outcome, wonLater.outcome)
        assertEquals(won.score, wonLater.score)
        assertEquals(won.clockMs + 600, wonLater.clockMs)
    }

    @Test
    fun `awards are dropped once they have been shown`() {
        // At 0.5 cells per second the head leaves the H after 600 ms and needs 2 seconds for the LB.
        val state = engine.start(level(cellsPerSecond = 0.5)).withPipes(ROUTE).headAt(p(1, 1), Side.LEFT, 0.7)
        val awarded = engine.handle(state, Tick(600))
        assertEquals(listOf(Award(p(1, 1), 10, 600)), awarded.awards)
        assertEquals(1, engine.handle(awarded, Tick(999)).awards.size)
        assertEquals(emptyList(), engine.handle(awarded, Tick(1_000)).awards)
    }

    @Test
    fun `fast forward runs the head ten times as fast and keeps real time for the awards`() {
        val state = engine.handle(engine.start(level()).withPipes(ROUTE).headAt(p(1, 1), Side.LEFT, 0.0), FastForward)
        val after = engine.handle(state, Tick(150)) // 1.5 cells of flow time
        assertEquals(p(2, 1), flowing(after).position)
        assertEquals(0.5, flowing(after).progress, tolerance)
        assertEquals(listOf(Award(p(1, 1), 10, 150)), after.awards)
    }
}
