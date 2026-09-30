package nl.hexmaster.leakout.core.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import nl.hexmaster.leakout.core.board.Board
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.engine.GameCommand.FastForward
import nl.hexmaster.leakout.core.engine.GameCommand.NormalSpeed
import nl.hexmaster.leakout.core.engine.GameCommand.Pause
import nl.hexmaster.leakout.core.engine.GameCommand.Place
import nl.hexmaster.leakout.core.engine.GameCommand.Restart
import nl.hexmaster.leakout.core.engine.GameCommand.Resume
import nl.hexmaster.leakout.core.engine.GameCommand.Start
import nl.hexmaster.leakout.core.engine.GameCommand.Tick
import nl.hexmaster.leakout.core.level.FlowSettings
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.PlacementRules
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.core.level.TestLevels
import nl.hexmaster.leakout.core.score.ScoreSheet

class GameEngineTest {
    private val engine = engine()
    private val level = engineLevel()

    // --- Start and restart ---

    @Test
    fun `starting a level builds a fresh state`() {
        val level = TestLevels.valid() // 12,000 ms start delay, visible stack size 5
        val state = engine.start(level)
        assertEquals(Board.from(level), state.board)
        assertTrue(state.board.pipes.isEmpty())
        assertEquals(5, state.stack.size)
        assertEquals(FlowState.Waiting(12_000), state.flow)
        assertEquals(0, state.score)
        assertEquals(ScoreSheet(), state.sheet)
        assertEquals(emptyList(), state.awards)
        assertEquals(0L, state.replaceLockUntilMs)
        assertEquals(0L, state.clockMs)
        assertFalse(state.paused)
        assertFalse(state.fastForward)
        assertFalse(state.routeComplete)
        assertEquals(Outcome.Playing, state.outcome)
        assertEquals(1, state.attempt)
    }

    @Test
    fun `restart starts attempt 2 of the same level`() {
        val state = engine.handle(engine.start(level), Restart)
        assertEquals(2, state.attempt)
        assertEquals(level, state.level)
    }

    @Test
    fun `start with another level replaces the level and counts the attempt`() {
        val other = engineLevel(flow = FlowSettings(initialDelayMs = 3_000, cellsPerSecond = 2.0))
        val state = engine.handle(engine.start(level), Start(other))
        assertEquals(other, state.level)
        assertEquals(FlowState.Waiting(3_000), state.flow)
        assertEquals(2, state.attempt)
    }

    @Test
    fun `restart after a loss clears everything`() {
        // Four H sections along row 1 lead off the right edge of the grid.
        var state = engine.start(level)
        state = engine.send(state, Place(p(1, 1)), Place(p(2, 1)), Place(p(3, 1)), Place(p(4, 1)))
        state = engine.runToEnd(state, dtMs = 100)
        val lost = assertIs<Outcome.Lost>(state.outcome)
        assertEquals(p(4, 1) to Side.RIGHT, lost.at to lost.side)
        assertEquals(40, state.score)
        assertEquals(4, state.board.pipes.size)

        val restarted = engine.handle(state, Restart)
        assertEquals(engine().start(level).copy(stack = restarted.stack, attempt = 2), restarted)
        assertEquals(FlowState.Waiting(10_000), restarted.flow)
        assertEquals(emptySet(), restarted.flow.used)
    }

    @Test
    fun `restart while paused gives an attempt that is not paused`() {
        val paused = engine.send(engine.start(level), Place(p(1, 1)), Tick(500), Pause)
        val restarted = engine.handle(paused, Restart)
        assertFalse(restarted.paused)
        assertTrue(restarted.board.pipes.isEmpty())
        assertEquals(0L, restarted.clockMs)
    }

    @Test
    fun `restart after a win clears the outcome, fast forward and the score`() {
        val won = engine.runToEnd(engine.handle(engine.start(level).withPipes(ROUTE), FastForward))
        assertIs<Outcome.Won>(won.outcome)
        assertTrue(won.fastForward)
        val restarted = engine.handle(won, Restart)
        assertEquals(Outcome.Playing, restarted.outcome)
        assertFalse(restarted.fastForward)
        assertFalse(restarted.routeComplete)
        assertEquals(0, restarted.score)
        assertEquals(emptyList(), restarted.awards)
    }

    @Test
    fun `restart draws a new stack from the random source`() {
        val level = engineLevel(section = null)
        val first = engine.start(level)
        val restarted = engine.handle(first, Restart)
        // Two draws of five from the default weights are almost never the same; with this seed they differ.
        assertNotSame(first.stack, restarted.stack)
        assertTrue(first.stack != restarted.stack)
    }

    @Test
    fun `two engines with the same seed and commands give equal states`() {
        val level = engineLevel(section = null)
        val commands = listOf(Place(p(1, 1)), Tick(300), Place(p(2, 1)), Place(p(1, 1)), Tick(700), Pause, Tick(50), Resume, Tick(12_000))
        fun play(engine: GameEngine) = commands.fold(engine.start(level)) { s, c -> engine.handle(s, c) }
        assertEquals(play(engine(seed = 3)), play(engine(seed = 3)))
    }

    @Test
    fun `a placement makes a new state and leaves the previous one as it was`() {
        val before = engine.start(level)
        val after = engine.handle(before, Place(p(1, 1)))
        assertEquals(Cell.Pipe(PipeSection.H), after.board[p(1, 1)])
        assertEquals(Cell.Empty, before.board[p(1, 1)])
    }

    // --- Pause and resume ---

    @Test
    fun `a tick while paused returns the same instance`() {
        val paused = engine.send(engine.start(level), Tick(100), Pause)
        assertSame(paused, engine.handle(paused, Tick(5_000)))
    }

    @Test
    fun `a tick while paused keeps the head halfway through its tile`() {
        val state = engine.start(level).withPipes(ROUTE).headAt(p(1, 1), Side.LEFT, 0.5)
        val paused = engine.handle(state, Pause)
        val after = engine.send(paused, Tick(2_500), Tick(2_500))
        assertEquals(0.5, (after.flow as FlowState.Flowing).progress)
        assertEquals(state.clockMs, after.clockMs)
    }

    @Test
    fun `place and fast forward are refused while paused`() {
        val paused = engine.handle(engine.start(level).withPipes(ROUTE.take(2)), Pause)
        assertSame(paused, engine.handle(paused, Place(p(2, 2))))
        assertFalse(engine.canPlace(paused, p(2, 2)))
        val complete = engine.handle(engine.start(level).withPipes(ROUTE), Pause)
        assertSame(complete, engine.handle(complete, FastForward))
    }

    @Test
    fun `pause twice is the same as once and resume when not paused changes nothing`() {
        val paused = engine.handle(engine.start(level), Pause)
        assertTrue(paused.paused)
        assertSame(paused, engine.handle(paused, Pause))
        val playing = engine.start(level)
        assertSame(playing, engine.handle(playing, Resume))
    }

    @Test
    fun `pause is refused after a loss and after a win`() {
        val lost = engine.runToEnd(engine.start(level))
        assertIs<Outcome.Lost>(lost.outcome)
        assertSame(lost, engine.handle(lost, Pause))
        assertFalse(engine.handle(lost, Pause).paused)

        val won = engine.runToEnd(engine.start(level).withPipes(ROUTE))
        assertIs<Outcome.Won>(won.outcome)
        assertSame(won, engine.handle(won, Pause))
    }

    @Test
    fun `resume continues the countdown from where it stopped`() {
        val state = engine.send(engine.start(level), Tick(3_000), Pause, Tick(4_000), Resume, Tick(1_000))
        assertEquals(FlowState.Waiting(6_000), state.flow)
        assertEquals(4_000L, state.clockMs)
    }

    @Test
    fun `a negative tick is refused`() {
        val state = engine.start(level)
        assertSame(state, engine.handle(state, Tick(-16)))
    }

    // --- Placement ---

    @Test
    fun `place on an empty tile moves the stack down and draws a new section`() {
        val level = engineLevel(section = null)
        val state = engine.start(level)
        val next = state.stack.next
        val after = engine.handle(state, Place(p(3, 3)))
        assertEquals(Cell.Pipe(next), after.board[p(3, 3)])
        assertEquals(state.stack.sections.dropLast(1), after.stack.sections.drop(1))
        assertEquals(state.stack.size, after.stack.size)
    }

    @Test
    fun `replacing an unentered section starts the replace lock`() {
        val level = engineLevel(rules = PlacementRules(allowReplace = true, replacePenaltyMs = 500))
        val placed = engine.send(engine.start(level), Tick(200), Place(p(1, 1)))
        assertEquals(0L, placed.replaceLockUntilMs)
        val replaced = engine.handle(placed, Place(p(1, 1)))
        assertNotSame(placed, replaced)
        assertEquals(700L, replaced.replaceLockUntilMs)
        assertSame(replaced, engine.handle(replaced, Place(p(3, 3))))
        assertFalse(engine.canPlace(replaced, p(3, 3)))
    }

    @Test
    fun `the replace lock runs out with the game clock`() {
        val level = engineLevel(rules = PlacementRules(allowReplace = true, replacePenaltyMs = 500))
        val replaced = engine.send(engine.start(level), Place(p(1, 1)), Place(p(1, 1)))
        val almost = engine.handle(replaced, Tick(499))
        assertFalse(engine.canPlace(almost, p(3, 3)))
        val after = engine.handle(almost, Tick(1))
        assertTrue(engine.canPlace(after, p(3, 3)))
        assertNotSame(after, engine.handle(after, Place(p(3, 3))))
    }

    @Test
    fun `refused placements leave the state unchanged`() {
        val level = engineLevel(blocked = listOf(p(4, 4)))
        val state = engine.start(level).withPipes(ROUTE).headAt(p(1, 1), Side.LEFT, 0.1)
        // A filled section (the head is in it), the start, the finish, a blocked tile, outside the grid.
        for (at in listOf(p(1, 1), p(0, 1), p(2, 3), p(4, 4), p(5, 0), p(-1, 2))) {
            assertSame(state, engine.handle(state, Place(at)), "place on $at")
        }
    }

    @Test
    fun `replacing is refused when the level does not allow it`() {
        val level = engineLevel(rules = PlacementRules(allowReplace = false))
        val placed = engine.handle(engine.start(level), Place(p(1, 1)))
        assertSame(placed, engine.handle(placed, Place(p(1, 1))))
    }

    @Test
    fun `placing is refused while fast forward is on and allowed again at normal speed`() {
        val fast = engine.handle(engine.start(level).withPipes(ROUTE), FastForward)
        assertTrue(fast.fastForward)
        assertSame(fast, engine.handle(fast, Place(p(3, 3))))
        val normal = engine.handle(fast, NormalSpeed)
        assertFalse(normal.fastForward)
        assertNotSame(normal, engine.handle(normal, Place(p(3, 3))))
    }

    @Test
    fun `placing is refused after an outcome`() {
        val lost = engine.runToEnd(engine.start(level))
        assertSame(lost, engine.handle(lost, Place(p(3, 3))))
        val won = engine.runToEnd(engine.start(level).withPipes(ROUTE))
        assertSame(won, engine.handle(won, Place(p(3, 3))))
    }

    @Test
    fun `canPlace equals the result of Place for every tile of a scripted board`() {
        val level = engineLevel(blocked = listOf(p(4, 0)), rules = PlacementRules(allowReplace = true, replacePenaltyMs = 500))
        val scripted = engine.start(level).withPipes(ROUTE).withPipes(p(3, 3) to PipeSection.X).headAt(p(1, 1), Side.LEFT, 0.3)
        val states = listOf(
            scripted,
            engine.handle(scripted, Place(p(3, 3))), // replaced: locked for 500 ms
            engine.handle(scripted.withPipes(p(1, 1) to PipeSection.H), Pause),
        )
        for (state in states) {
            for (x in -1..5) for (y in -1..5) {
                val at = p(x, y)
                assertEquals(engine.canPlace(state, at), engine.handle(state, Place(at)) !== state, "tile $at")
            }
        }
    }

    @Test
    fun `a closing placement completes the route`() {
        var state = engine.start(level).withPipes(ROUTE.take(2))
        assertFalse(state.routeComplete)
        state = engine.handle(state.copy(stack = state.stack.copy(sections = List(5) { PipeSection.V })), Place(p(2, 2)))
        assertTrue(state.routeComplete)
    }

    @Test
    fun `fast forward is refused on an open route`() {
        val state = engine.start(level).withPipes(ROUTE.take(2))
        assertSame(state, engine.handle(state, FastForward))
        assertFalse(engine.handle(state, FastForward).fastForward)
    }

    @Test
    fun `normal speed is refused when fast forward is off`() {
        val state = engine.start(level).withPipes(ROUTE)
        assertSame(state, engine.handle(state, NormalSpeed))
    }
}
