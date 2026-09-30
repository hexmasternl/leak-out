package nl.hexmaster.leakout.ui.game

import kotlin.random.Random
import nl.hexmaster.leakout.core.board.Board
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.level.Endpoint
import nl.hexmaster.leakout.core.level.FlowSettings
import nl.hexmaster.leakout.core.level.GridSize
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.PlacementRules
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.core.level.StackSettings
import nl.hexmaster.leakout.core.level.StarBenchmarks
import nl.hexmaster.leakout.core.score.ScoreSheet
import nl.hexmaster.leakout.core.stack.PipeStack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSessionTest {
    private val level = Level(
        id = 3,
        name = "Test",
        grid = GridSize(4, 4),
        start = Endpoint(Position(0, 0), Side.RIGHT),
        finish = Endpoint(Position(3, 3), Side.LEFT),
        blocked = emptyList(),
        flow = FlowSettings(initialDelayMs = 10_000, cellsPerSecond = 0.8),
        stack = StackSettings(visibleSize = 5),
        rules = PlacementRules(),
        stars = StarBenchmarks(100, 200, 350),
    )

    private fun session() = GameSession(level, Random(42))

    @Test
    fun `a new session starts running at score 0 with the level's speed`() {
        val session = session()
        assertFalse(session.paused)
        assertFalse(session.pauseMenuOpen)
        assertEquals(0, session.score)
        assertEquals(0.8, session.flowSpeed, 0.0)
        assertEquals(0L, session.elapsedMs)
        assertEquals(5, session.stack.size)
        assertTrue(session.board.pipes.isEmpty())
    }

    @Test
    fun `tick does not move the clock while paused`() {
        val session = session()
        session.tick(100)
        session.pause()
        session.tick(5_000)
        assertEquals(100L, session.elapsedMs)
    }

    @Test
    fun `resume keeps stack, placed sections, score and clock`() {
        val session = session()
        session.tick(250)
        assertTrue(session.place(session.stack.next, Position(1, 1)))
        session.sheet = ScoreSheet(total = 40)
        val stack = session.stack
        val board = session.board

        session.pause()
        session.resume()

        assertFalse(session.paused)
        assertFalse(session.pauseMenuOpen)
        assertEquals(stack, session.stack)
        assertEquals(board, session.board)
        assertEquals(40, session.score)
        assertEquals(250L, session.elapsedMs)
        session.tick(50)
        assertEquals(300L, session.elapsedMs)
    }

    @Test
    fun `restart resets everything and unpauses`() {
        val session = session()
        session.tick(900)
        session.place(session.stack.next, Position(1, 1))
        session.sheet = ScoreSheet(total = 220)
        session.flowSpeed = 1.4
        session.pause()

        session.restart()

        assertFalse(session.paused)
        assertFalse(session.pauseMenuOpen)
        assertTrue(session.board.pipes.isEmpty())
        assertEquals(5, session.stack.size)
        assertEquals(0, session.score)
        assertEquals(0.8, session.flowSpeed, 0.0)
        assertEquals(0L, session.elapsedMs)
    }

    @Test
    fun `restart draws a new stack from the session's random`() {
        val session = GameSession(level, Random(1))
        session.restart()
        val random = Random(1)
        PipeStack.create(level.stack, random)
        assertEquals(PipeStack.create(level.stack, random), session.stack)
    }

    @Test
    fun `place is refused while paused`() {
        val session = session()
        val stack = session.stack
        session.pause()
        assertFalse(session.place(PipeSection.H, Position(1, 1)))
        assertTrue(session.board.pipes.isEmpty())
        assertEquals(stack, session.stack)
    }

    @Test
    fun `place advances the stack and refuses fixed cells`() {
        val session = session()
        val stack = session.stack
        assertTrue(session.place(stack.next, Position(1, 1)))
        assertEquals(stack.sections.dropLast(1), session.stack.sections.drop(1))
        assertEquals(Cell.Pipe(stack.next), session.board[Position(1, 1)])
        for (fixed in listOf(level.start.position, level.finish.position, Position(-1, 0))) {
            assertFalse(session.canPlace(fixed))
            assertFalse(session.place(PipeSection.V, fixed))
        }
    }

    @Test
    fun `a drop on an empty tile does not start the lock`() {
        val session = session()
        session.place(PipeSection.H, Position(1, 1))
        assertEquals(0L, session.replaceLockUntilMs)
        assertTrue(session.canPlace(Position(2, 1)))
    }

    @Test
    fun `a replacement swaps the section, advances the stack and starts the lock`() {
        val session = session()
        session.tick(1_000)
        session.place(PipeSection.H, Position(1, 1))
        val stack = session.stack

        assertTrue(session.place(PipeSection.V, Position(1, 1)))

        assertEquals(Cell.Pipe(PipeSection.V), session.board[Position(1, 1)])
        assertEquals(stack.sections.dropLast(1), session.stack.sections.drop(1))
        assertEquals(1_500L, session.replaceLockUntilMs)
        assertFalse(session.canPlace(Position(2, 1)))
        assertFalse(session.place(PipeSection.H, Position(2, 1)))
    }

    @Test
    fun `the lock does not run out while paused and ends after the penalty`() {
        val session = session()
        session.place(PipeSection.H, Position(1, 1))
        session.place(PipeSection.V, Position(1, 1))
        session.pause()
        session.tick(5_000)
        session.resume()
        assertFalse(session.canPlace(Position(2, 1)))

        session.tick(499)
        assertFalse(session.canPlace(Position(2, 1)))
        session.tick(1)
        assertTrue(session.canPlace(Position(2, 1)))
        assertTrue(session.place(PipeSection.H, Position(2, 1)))
    }

    @Test
    fun `nothing can be placed while paused`() {
        val session = session()
        session.pause()
        assertFalse(session.canPlace(Position(1, 1)))
    }

    @Test
    fun `a section with liquid cannot be replaced`() {
        val session = session()
        session.place(PipeSection.H, Position(1, 1))
        session.markLiquidEntered(Position(1, 1))
        assertFalse(session.canPlace(Position(1, 1)))
        assertFalse(session.place(PipeSection.V, Position(1, 1)))
        assertEquals(Cell.Pipe(PipeSection.H, liquidEntered = true), session.board[Position(1, 1)])
    }

    @Test
    fun `restart clears the board, the lock and counts the attempt`() {
        val session = session()
        session.place(PipeSection.H, Position(1, 1))
        session.markLiquidEntered(Position(1, 1))
        session.place(PipeSection.H, Position(2, 1))
        session.place(PipeSection.V, Position(2, 1))
        assertEquals(1, session.attempt)

        session.restart()

        assertEquals(Board.from(level), session.board)
        assertEquals(0L, session.replaceLockUntilMs)
        assertEquals(2, session.attempt)
        assertTrue(session.canPlace(Position(1, 1)))
    }

    @Test
    fun `pausing twice is the same as once`() {
        val session = session()
        session.pause()
        session.pause()
        assertTrue(session.paused)
        assertTrue(session.pauseMenuOpen)
        session.resume()
        assertFalse(session.paused)
        assertFalse(session.pauseMenuOpen)
    }

    @Test
    fun `stars follow the score thresholds`() {
        val session = session()
        val expected = mapOf(0 to 0, 99 to 0, 100 to 1, 199 to 1, 200 to 2, 349 to 2, 350 to 3, 1_000 to 3)
        expected.forEach { (score, stars) ->
            session.sheet = ScoreSheet(total = score)
            assertEquals("score $score", stars, session.stars)
        }
    }

    @Test
    fun `restart sets the stars back to 0`() {
        val session = session()
        session.sheet = ScoreSheet(total = 250)
        assertEquals(2, session.stars)
        session.restart()
        assertEquals(0, session.stars)
    }

    @Test
    fun `exiting a straight adds 10 and one popup on that tile`() {
        val session = session()
        session.tick(400)
        assertTrue(session.place(PipeSection.H, Position(1, 0)))

        session.liquidExited(Position(1, 0))

        assertEquals(10, session.score)
        assertEquals(1, session.popups.size)
        val popup = session.popups.single()
        assertEquals(Position(1, 0), popup.position)
        assertEquals(10, popup.points)
        assertEquals(400L, popup.startedAtMs)
    }

    @Test
    fun `exiting the start, the finish or an empty tile changes nothing`() {
        val session = session()
        session.liquidExited(level.start.position)
        session.liquidExited(level.finish.position)
        session.liquidExited(Position(2, 2))
        assertEquals(0, session.score)
        assertTrue(session.popups.isEmpty())
    }

    @Test
    fun `the second exit from a cross adds 50`() {
        val session = session()
        session.place(PipeSection.X, Position(1, 1))
        session.liquidExited(Position(1, 1))
        session.liquidExited(Position(1, 1))
        assertEquals(60, session.score)
        assertEquals(listOf(10, 50), session.popups.map { it.points })
    }

    @Test
    fun `nothing is awarded while paused`() {
        val session = session()
        session.place(PipeSection.LT, Position(1, 1))
        session.pause()
        session.liquidExited(Position(1, 1))
        assertEquals(0, session.score)
        assertTrue(session.popups.isEmpty())
    }

    @Test
    fun `ticks while paused do not age or drop popups`() {
        val session = session()
        session.place(PipeSection.V, Position(1, 1))
        session.liquidExited(Position(1, 1))
        session.tick(400)
        session.pause()
        session.tick(5_000)
        session.resume()
        assertEquals(1, session.popups.size)
        assertEquals(400L, session.popups.single().ageAt(session.elapsedMs))
        session.tick(599)
        assertEquals(1, session.popups.size)
        session.tick(1)
        assertTrue(session.popups.isEmpty())
    }

    @Test
    fun `a popup is dropped after 1000 ms of ticks and the score stays`() {
        val session = session()
        session.place(PipeSection.RES, Position(1, 1))
        session.liquidExited(Position(1, 1))
        repeat(9) { session.tick(100) }
        assertEquals(1, session.popups.size)
        session.tick(100)
        assertTrue(session.popups.isEmpty())
        assertEquals(25, session.score)
    }

    @Test
    fun `popups of close awards each keep their own timing`() {
        val session = session()
        session.place(PipeSection.H, Position(1, 0))
        session.place(PipeSection.LB, Position(2, 0))
        session.liquidExited(Position(1, 0))
        session.tick(300)
        session.liquidExited(Position(2, 0))
        session.tick(700)
        assertEquals(listOf(Position(2, 0)), session.popups.map { it.position })
        session.tick(300)
        assertTrue(session.popups.isEmpty())
    }

    @Test
    fun `restart clears the score and popups and resets the cross passes`() {
        val session = session()
        session.place(PipeSection.X, Position(1, 1))
        session.liquidExited(Position(1, 1))
        assertEquals(10, session.score)

        session.restart()

        assertEquals(0, session.score)
        assertTrue(session.popups.isEmpty())
        session.place(PipeSection.X, Position(1, 1))
        session.liquidExited(Position(1, 1))
        assertEquals(10, session.score)
    }

    /** Places the route start (0, 0) → (1, 0) → (2, 0) → down to (2, 3) → finish (3, 3), leaving out [skip]. */
    private fun GameSession.buildRoute(skip: Position? = null) {
        val route = listOf(
            Position(1, 0) to PipeSection.H,
            Position(2, 0) to PipeSection.LB,
            Position(2, 1) to PipeSection.V,
            Position(2, 2) to PipeSection.V,
            Position(2, 3) to PipeSection.RT,
        )
        for ((at, section) in route) if (at != skip) assertTrue(place(section, at))
    }

    @Test
    fun `a new session has no route and no fast forward`() {
        val session = session()
        assertFalse(session.routeComplete)
        assertFalse(session.fastForward)
        assertFalse(session.showFastForward)
        assertEquals(1, session.flowTimeScale)
    }

    @Test
    fun `placing the last missing section completes the route and shows the button`() {
        val session = session()
        session.buildRoute(skip = Position(2, 2))
        assertFalse(session.routeComplete)
        assertFalse(session.showFastForward)

        assertTrue(session.place(PipeSection.V, Position(2, 2)))

        assertTrue(session.routeComplete)
        assertTrue(session.showFastForward)
        assertFalse(session.fastForward)
    }

    @Test
    fun `a replacement that breaks the route clears it and hides the button`() {
        val session = session()
        session.buildRoute()
        assertTrue(session.showFastForward)

        assertTrue(session.place(PipeSection.H, Position(2, 1)))

        assertFalse(session.routeComplete)
        assertFalse(session.showFastForward)
    }

    @Test
    fun `fast forward does not start without a complete route`() {
        val session = session()
        session.buildRoute(skip = Position(2, 2))
        session.startFastForward()
        assertFalse(session.fastForward)
        assertEquals(1, session.flowTimeScale)
    }

    @Test
    fun `fast forward does not start while paused`() {
        val session = session()
        session.buildRoute()
        session.pause()
        session.startFastForward()
        assertFalse(session.fastForward)
    }

    @Test
    fun `fast forward with a complete route runs the flow 10 times as fast`() {
        val session = session()
        session.buildRoute()
        session.startFastForward()
        assertTrue(session.fastForward)
        assertTrue(session.showFastForward)
        assertEquals(10, session.flowTimeScale)
        assertEquals(GameSession.FAST_FORWARD_FACTOR, session.flowTimeScale)

        session.startFastForward()
        assertTrue(session.fastForward)
        assertEquals(10, session.flowTimeScale)
    }

    @Test
    fun `placing is rejected while fast forward is on`() {
        val session = session()
        session.buildRoute()
        session.startFastForward()
        val board = session.board
        val stack = session.stack

        assertFalse(session.canPlace(Position(0, 3)))
        assertFalse(session.place(session.stack.next, Position(0, 3)))
        assertFalse(session.canPlace(Position(2, 1)))
        assertFalse(session.place(PipeSection.H, Position(2, 1)))

        assertEquals(board, session.board)
        assertEquals(stack, session.stack)
    }

    @Test
    fun `pause and resume keep fast forward on`() {
        val session = session()
        session.buildRoute()
        session.startFastForward()
        session.pause()
        assertTrue(session.fastForward)
        session.resume()
        assertTrue(session.fastForward)
        assertEquals(10, session.flowTimeScale)
    }

    @Test
    fun `restart clears the route and fast forward`() {
        val session = session()
        session.buildRoute()
        session.startFastForward()

        session.restart()

        assertFalse(session.routeComplete)
        assertFalse(session.fastForward)
        assertFalse(session.showFastForward)
        assertEquals(1, session.flowTimeScale)
        assertTrue(session.canPlace(Position(1, 0)))
    }

    @Test
    fun `toggling switches between fast and normal speed and keeps the button`() {
        val session = session()
        session.buildRoute()

        session.toggleFastForward()
        assertTrue(session.fastForward)
        assertEquals(10, session.flowTimeScale)

        session.toggleFastForward()
        assertFalse(session.fastForward)
        assertEquals(1, session.flowTimeScale)
        assertTrue(session.showFastForward)

        session.toggleFastForward()
        assertEquals(10, session.flowTimeScale)
    }

    @Test
    fun `toggling does nothing while paused`() {
        val session = session()
        session.buildRoute()
        session.pause()
        session.toggleFastForward()
        assertFalse(session.fastForward)

        session.resume()
        session.toggleFastForward()
        session.pause()
        session.toggleFastForward()
        session.stopFastForward()
        assertTrue(session.fastForward)
    }

    @Test
    fun `toggling without a complete route does nothing`() {
        val session = session()
        session.toggleFastForward()
        assertFalse(session.fastForward)
    }

    @Test
    fun `placing works again after going back to normal speed`() {
        val session = session()
        session.buildRoute()
        session.toggleFastForward()
        assertFalse(session.canPlace(Position(0, 3)))

        session.toggleFastForward()

        assertTrue(session.canPlace(Position(0, 3)))
        assertTrue(session.place(PipeSection.H, Position(0, 3)))
        assertTrue(session.routeComplete)
        // Breaking the route now hides the button, as before fast forward.
        assertTrue(session.place(PipeSection.H, Position(2, 1)))
        assertFalse(session.showFastForward)
    }

    @Test
    fun `the game clock keeps real time with fast forward on`() {
        val session = session()
        session.buildRoute()
        session.tick(100)
        session.startFastForward()
        session.tick(16)
        assertEquals(116L, session.elapsedMs)
    }

    @Test
    fun `a new session is playing and not stopped`() {
        val session = session()
        assertEquals(Outcome.Playing, session.outcome)
        assertFalse(session.isStopped)
        assertFalse(session.showLostDialog)
        assertEquals(0L, session.lossAgeMs)
    }

    @Test
    fun `a leak loses the level at the given tile and side`() {
        val session = session()
        session.tick(300)
        session.liquidLeaked(Position(1, 0), Side.RIGHT)
        assertEquals(Outcome.Lost(Position(1, 0), Side.RIGHT, atMs = 300L), session.outcome)
        assertTrue(session.isStopped)
        assertFalse(session.paused)
    }

    @Test
    fun `a second leak changes nothing`() {
        val session = session()
        session.liquidLeaked(Position(1, 0), Side.RIGHT)
        session.tick(200)
        session.liquidLeaked(Position(2, 2), Side.TOP)
        assertEquals(Outcome.Lost(Position(1, 0), Side.RIGHT, atMs = 0L), session.outcome)
    }

    @Test
    fun `a leak while paused changes nothing`() {
        val session = session()
        session.pause()
        session.liquidLeaked(Position(1, 0), Side.RIGHT)
        assertEquals(Outcome.Playing, session.outcome)
    }

    @Test
    fun `after a loss placing, scoring and pausing are refused`() {
        val session = session()
        assertTrue(session.place(PipeSection.H, Position(1, 0)))
        session.liquidLeaked(Position(1, 0), Side.RIGHT)

        assertFalse(session.canPlace(Position(2, 0)))
        assertFalse(session.place(PipeSection.H, Position(2, 0)))
        assertEquals(Cell.Empty, session.board[Position(2, 0)])

        session.liquidExited(Position(1, 0))
        assertEquals(0, session.score)
        assertTrue(session.popups.isEmpty())

        session.pause()
        assertFalse(session.paused)
        assertFalse(session.pauseMenuOpen)
    }

    @Test
    fun `fast forward does not start or stop after a loss`() {
        val session = session()
        session.buildRoute()
        session.liquidLeaked(Position(2, 3), Side.BOTTOM)
        session.toggleFastForward()
        assertFalse(session.fastForward)

        val fast = session()
        fast.buildRoute()
        fast.startFastForward()
        fast.liquidLeaked(Position(2, 3), Side.BOTTOM)
        fast.toggleFastForward()
        assertTrue(fast.fastForward)
    }

    @Test
    fun `the lost dialog shows after 1000 ms of ticks`() {
        val session = session()
        session.tick(250)
        session.liquidLeaked(Position(1, 0), Side.RIGHT)
        session.tick(999)
        assertEquals(999L, session.lossAgeMs)
        assertFalse(session.showLostDialog)
        session.tick(1)
        assertTrue(session.showLostDialog)
    }

    @Test
    fun `skipping the leak effect shows the dialog at once`() {
        val session = session()
        session.tick(250)
        session.liquidLeaked(Position(1, 0), Side.RIGHT)
        session.tick(100)
        session.skipLeakEffect()
        assertTrue(session.showLostDialog)
        assertEquals(LeakTiming.EFFECT_MS, session.lossAgeMs)
        assertEquals(Position(1, 0), (session.outcome as Outcome.Lost).at)
    }

    @Test
    fun `skipping the leak effect while playing changes nothing`() {
        val session = session()
        session.skipLeakEffect()
        assertEquals(Outcome.Playing, session.outcome)
    }

    @Test
    fun `an exit from a corner then a leak scores 15 more and loses`() {
        val session = session()
        session.sheet = ScoreSheet(total = 40)
        assertTrue(session.place(PipeSection.LB, Position(1, 0)))

        session.liquidExited(Position(1, 0))
        session.liquidLeaked(Position(1, 0), Side.BOTTOM)

        assertEquals(55, session.score)
        assertTrue(session.outcome is Outcome.Lost)
    }

    @Test
    fun `restart after a loss plays again from scratch`() {
        val session = session()
        session.place(PipeSection.H, Position(1, 0))
        session.liquidExited(Position(1, 0))
        session.liquidLeaked(Position(1, 0), Side.RIGHT)
        session.tick(1_000)

        session.restart()

        assertEquals(Outcome.Playing, session.outcome)
        assertFalse(session.isStopped)
        assertFalse(session.showLostDialog)
        assertEquals(0, session.score)
        assertTrue(session.board.pipes.isEmpty())
        assertTrue(session.place(PipeSection.H, Position(1, 0)))
    }
}
