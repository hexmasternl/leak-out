package nl.hexmaster.leakout.core.level

import nl.hexmaster.leakout.core.level.TestLevels.valid
import nl.hexmaster.leakout.core.level.TestLevels.weights
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LevelSolverTest {
    private fun at(x: Int, y: Int, side: Side) = Endpoint(Position(x, y), side)

    private fun solvable(level: Level): SolverResult.Solvable =
        assertIs<SolverResult.Solvable>(LevelSolver.solve(level), "expected a solvable level")

    private fun unsolvable(level: Level): SolverResult.Unsolvable =
        assertIs<SolverResult.Unsolvable>(LevelSolver.solve(level), "expected an unsolvable level")

    /** Every cell of the grid except the endpoints and the cells left open by [open]. */
    private fun wallExcept(grid: GridSize, start: Endpoint, finish: Endpoint, open: Set<Position>): List<Position> =
        (0 until grid.columns).flatMap { x -> (0 until grid.rows).map { y -> Position(x, y) } }
            .filter { it !in open && it != start.position && it != finish.position }

    @Test
    fun `open grid is solvable`() {
        val result = solvable(valid(blocked = emptyList()))
        assertEquals(Position(1, 1), result.path.first())
        assertEquals(Position(5, 7), result.path.last())
    }

    @Test
    fun `path is a chain of adjacent free cells`() {
        val level = valid(blocked = emptyList())
        val path = solvable(level).path
        path.zipWithNext().forEach { (from, to) ->
            assertTrue(Side.entries.any { from.neighbour(it) == to }, "$from and $to are not adjacent")
        }
        path.forEach { cell ->
            assertTrue(cell in level.grid && !level.isBlocked(cell), "$cell is not a free cell")
            assertTrue(cell != level.start.position && cell != level.finish.position, "$cell is an endpoint")
        }
    }

    @Test
    fun `blocked cells sealing off the finish make a level unsolvable`() {
        val grid = GridSize(columns = 6, rows = 4)
        val start = at(0, 1, Side.RIGHT)
        val finish = at(5, 2, Side.LEFT)
        // A full wall in column 3 leaves the finish's facing cell (4, 2) cut off from the start.
        val wall = (0 until grid.rows).map { Position(3, it) }
        val result = unsolvable(valid(grid = grid, start = start, finish = finish, blocked = wall))
        assertTrue("(4, 2)" in result.reason, result.reason)
        assertTrue("finish" in result.reason, result.reason)
    }

    @Test
    fun `shared facing cell is solvable`() {
        val start = at(2, 1, Side.BOTTOM)
        val finish = at(2, 3, Side.TOP)
        val level = valid(grid = GridSize(6, 6), start = start, finish = finish, blocked = emptyList())
        assertEquals(listOf(Position(2, 2)), solvable(level).path)
    }

    @Test
    fun `a walled in start reports the start as unconnectable`() {
        val grid = GridSize(columns = 6, rows = 4)
        val start = at(0, 1, Side.RIGHT)
        val finish = at(5, 2, Side.LEFT)
        val blocked = listOf(Position(1, 1))
        val result = unsolvable(valid(grid = grid, start = start, finish = finish, blocked = blocked))
        assertTrue("start" in result.reason, result.reason)
        assertTrue("(1, 1)" in result.reason, result.reason)
    }

    @Test
    fun `a facing cell outside the grid does not throw`() {
        val grid = GridSize(columns = 6, rows = 4)
        val level = valid(grid = grid, start = at(0, 1, Side.LEFT), finish = at(5, 2, Side.LEFT), blocked = emptyList())
        assertTrue("outside" in unsolvable(level).reason)
    }

    @Test
    fun `a route needing only straights and corners ignores cross and reservoir weights`() {
        val grid = GridSize(columns = 6, rows = 5)
        val start = at(0, 0, Side.BOTTOM)
        val finish = at(5, 4, Side.TOP)
        // A single corridor: down column 1, across row 3, up column 5. No cell is used twice.
        val corridor = (1..3).map { Position(1, it) } +
            (1..5).map { Position(it, 3) } +
            listOf(Position(5, 2), Position(5, 1), Position(0, 1))
        val level = valid(
            grid = grid,
            start = start,
            finish = finish,
            blocked = wallExcept(grid, start, finish, corridor.toSet()),
            stack = StackSettings(weights = weights(PipeSection.X to 0, PipeSection.RES to 0)),
        )
        assertTrue(solvable(level).path.isNotEmpty())
    }

    @Test
    fun `a route through an endpoint cell is not a route`() {
        val grid = GridSize(columns = 4, rows = 4)
        val start = at(0, 0, Side.BOTTOM)
        val finish = at(2, 0, Side.BOTTOM)
        // The only way from (0, 1) to (2, 1) along row 1 is barred; row 0 would have to cross the finish.
        val blocked = listOf(Position(1, 1)) +
            (0 until grid.columns).map { Position(it, 2) } +
            listOf(Position(1, 0), Position(3, 0), Position(3, 1))
        assertTrue("finish" in unsolvable(valid(grid = grid, start = start, finish = finish, blocked = blocked)).reason)
    }
}
