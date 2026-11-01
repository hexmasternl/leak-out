package nl.hexmaster.leakout.core.engine

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.PipeSection.H
import nl.hexmaster.leakout.core.level.PipeSection.LB
import nl.hexmaster.leakout.core.level.PipeSection.LT
import nl.hexmaster.leakout.core.level.PipeSection.RB
import nl.hexmaster.leakout.core.level.PipeSection.RT
import nl.hexmaster.leakout.core.level.PipeSection.V
import nl.hexmaster.leakout.core.level.StackSettings

/**
 * The daily pipe deals the stack from the server's seed, so every player gets the same sections in
 * the same order, and a restart deals them again.
 */
class SeededDealTest {
    private val level = engineLevel().copy(stack = StackSettings())

    @Test
    fun `a known seed deals a pinned sequence`() {
        // Pinned: a change here means every player's daily pipe stack changes with the app version.
        assertEquals(PINNED, dealt(GameEngine.seeded(SEED), placements = 10))
    }

    @Test
    fun `two engines with the same seed deal the same sequence, placements included`() {
        assertEquals(dealt(GameEngine.seeded(SEED), placements = 40), dealt(GameEngine.seeded(SEED), placements = 40))
    }

    @Test
    fun `another seed deals another sequence`() {
        assertNotEquals(dealt(GameEngine.seeded(SEED), placements = 10), dealt(GameEngine.seeded(SEED + 1), placements = 10))
    }

    @Test
    fun `a restart deals the same sequence again`() {
        val engine = GameEngine.seeded(SEED)
        val first = engine.start(level)
        val firstDeals = stackSequence(engine, first, placements = 10)
        val afterPlacing = place(engine, first, 5)

        val restarted = engine.handle(afterPlacing, GameCommand.Restart)

        assertEquals(2, restarted.attempt)
        assertEquals(first.stack, restarted.stack)
        assertEquals(firstDeals, stackSequence(engine, restarted, placements = 10))
    }

    @Test
    fun `an engine with a random source keeps drawing from it across restarts`() {
        val engine = GameEngine(Random(SEED))
        val first = engine.start(level)

        assertNotEquals(first.stack.sections, engine.handle(first, GameCommand.Restart).stack.sections)
    }

    /** The visible and hidden sections of a fresh attempt, then every section dealt during [placements] placements. */
    private fun dealt(engine: GameEngine, placements: Int): List<PipeSection> = stackSequence(engine, engine.start(level), placements)

    private fun stackSequence(engine: GameEngine, start: GameState, placements: Int): List<PipeSection> {
        val sections = start.stack.sections.reversed().toMutableList()
        var state = start
        repeat(placements) {
            state = place(engine, state, 1)
            sections += state.stack.sections.first()
        }
        return sections
    }

    /** Places [count] sections on empty cells, row by row; each placement deals one new section. */
    private fun place(engine: GameEngine, start: GameState, count: Int): GameState {
        var state = start
        val free = (0 until level.grid.rows).flatMap { y -> (0 until level.grid.columns).map { x -> p(x, y) } }
            .filter { state.board[it] == Cell.Empty && engine.canPlace(state, it) }
        free.take(count).forEach { at -> state = engine.handle(state, GameCommand.Place(at)) }
        return state
    }

    private companion object {
        const val SEED = 20_261_102L

        // The 15 sections of the fresh stack, from the next one up, then the 10 dealt by placing.
        val PINNED: List<PipeSection> = listOf(
            LB, RT, H, LB, LT, RB, LB, LT, V, H, RT, V, RB, RT, V,
            RB, LT, RT, H, V, LT, H, LT, RB, LB,
        )
    }
}
