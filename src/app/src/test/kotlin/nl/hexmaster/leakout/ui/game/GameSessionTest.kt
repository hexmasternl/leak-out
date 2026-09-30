package nl.hexmaster.leakout.ui.game

import kotlin.random.Random
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
        assertTrue(session.placed.isEmpty())
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
        assertTrue(session.place(Position(1, 1), session.stack.next))
        session.score = 40
        val stack = session.stack
        val placed = session.placed.toMap()

        session.pause()
        session.resume()

        assertFalse(session.paused)
        assertFalse(session.pauseMenuOpen)
        assertEquals(stack, session.stack)
        assertEquals(placed, session.placed)
        assertEquals(40, session.score)
        assertEquals(250L, session.elapsedMs)
        session.tick(50)
        assertEquals(300L, session.elapsedMs)
    }

    @Test
    fun `restart resets everything and unpauses`() {
        val session = session()
        session.tick(900)
        session.place(Position(1, 1), session.stack.next)
        session.score = 220
        session.flowSpeed = 1.4
        session.pause()

        session.restart()

        assertFalse(session.paused)
        assertFalse(session.pauseMenuOpen)
        assertTrue(session.placed.isEmpty())
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
        assertFalse(session.place(Position(1, 1), PipeSection.H))
        assertTrue(session.placed.isEmpty())
        assertEquals(stack, session.stack)
    }

    @Test
    fun `place advances the stack and refuses an occupied cell`() {
        val session = session()
        val stack = session.stack
        assertTrue(session.place(Position(1, 1), stack.next))
        assertEquals(stack.sections.dropLast(1), session.stack.sections.drop(1))
        assertFalse(session.place(Position(1, 1), PipeSection.V))
        assertEquals(stack.next, session.placed[Position(1, 1)])
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
            session.score = score
            assertEquals("score $score", stars, session.stars)
        }
    }

    @Test
    fun `restart sets the stars back to 0`() {
        val session = session()
        session.score = 250
        assertEquals(2, session.stars)
        session.restart()
        assertEquals(0, session.stars)
    }
}
