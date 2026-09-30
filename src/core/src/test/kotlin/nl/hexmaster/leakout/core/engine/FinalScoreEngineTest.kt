package nl.hexmaster.leakout.core.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.engine.GameCommand.Pause
import nl.hexmaster.leakout.core.engine.GameCommand.RemoveUnused
import nl.hexmaster.leakout.core.engine.GameCommand.Restart
import nl.hexmaster.leakout.core.engine.GameCommand.Tick
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.StarBenchmarks

/** The final score the engine keeps for a won attempt, and the [RemoveUnused] command of the tally. */
class FinalScoreEngineTest {
    private val engine = engine()

    // 35 points on the route; three unused sections cost 9, so the total is 26: one star.
    private val level = engineLevel().copy(stars = StarBenchmarks(20, 30, 100))

    private val unused = listOf(p(4, 0) to PipeSection.V, p(0, 4) to PipeSection.X, p(4, 4) to PipeSection.RES)

    /** The route of [engineLevel] plus [unused] off the route, played to the finish. */
    private fun won(): GameState = engine.runToEnd(engine.start(level).withPipes(ROUTE + unused))

    @Test
    fun `a new attempt has no final score`() {
        val state = engine.start(level)
        assertNull(state.finalScore)
        assertEquals(0, state.sectionsRemoved)
        assertEquals(0, state.finalStars)
    }

    @Test
    fun `a win sets the final score from the earned points and the unused sections`() {
        val state = won()
        assertIs<Outcome.Won>(state.outcome)
        val score = state.finalScore!!
        assertEquals(35, score.earned)
        assertEquals(listOf(p(4, 0), p(0, 4), p(4, 4)), score.unusedSections)
        assertEquals(9, score.penalty)
        assertEquals(26, score.total)
        assertEquals(1, state.finalStars)
        // The HUD stars still follow the earned points.
        assertEquals(2, state.stars)
    }

    @Test
    fun `the route sections are not unused`() {
        val state = engine.runToEnd(engine.start(level).withPipes(ROUTE))
        assertEquals(emptyList(), state.finalScore!!.unusedSections)
        assertEquals(35, state.finalScore!!.total)
        assertEquals(2, state.finalStars) // 35 >= 30
    }

    @Test
    fun `a leak gives no final score`() {
        val state = engine.runToEnd(engine.start(level).withPipes(unused))
        assertIs<Outcome.Lost>(state.outcome)
        assertNull(state.finalScore)
    }

    @Test
    fun `the final score does not change after the win`() {
        val state = won()
        val later = engine.send(state, Tick(5_000), Pause)
        assertEquals(state.finalScore, later.finalScore)
    }

    @Test
    fun `remove unused takes the sections off one step at a time in order`() {
        val state = won()
        val one = engine.handle(state, RemoveUnused(1))
        assertEquals(1, one.sectionsRemoved)
        assertEquals(Cell.Empty, one.board[p(4, 0)])
        assertIs<Cell.Pipe>(one.board[p(0, 4)])
        val three = engine.handle(one, RemoveUnused(3))
        assertEquals(3, three.sectionsRemoved)
        for ((at, _) in unused) assertEquals(Cell.Empty, three.board[at])
        for ((at, section) in ROUTE) assertEquals(Cell.Pipe(section, liquidEntered = true), three.board[at])
        assertEquals(state.finalScore, three.finalScore)
    }

    @Test
    fun `remove unused is capped at the unused count`() {
        val state = engine.handle(won(), RemoveUnused(50))
        assertEquals(3, state.sectionsRemoved)
        assertSame(state, engine.handle(state, RemoveUnused(4)))
    }

    @Test
    fun `remove unused is refused for a count already reached`() {
        val state = engine.handle(won(), RemoveUnused(2))
        assertSame(state, engine.handle(state, RemoveUnused(2)))
        assertSame(state, engine.handle(state, RemoveUnused(1)))
        assertSame(state, engine.handle(state, RemoveUnused(0)))
    }

    @Test
    fun `remove unused is refused while playing and after a loss`() {
        val playing = engine.start(level).withPipes(unused)
        assertSame(playing, engine.handle(playing, RemoveUnused(1)))
        val lost = engine.runToEnd(playing)
        assertSame(lost, engine.handle(lost, RemoveUnused(1)))
    }

    @Test
    fun `restart clears the final score and the removed count`() {
        val state = engine.send(won(), RemoveUnused(2), Restart)
        assertEquals(Outcome.Playing, state.outcome)
        assertNull(state.finalScore)
        assertEquals(0, state.sectionsRemoved)
        assertTrue(state.board.pipes.isEmpty())
    }
}
