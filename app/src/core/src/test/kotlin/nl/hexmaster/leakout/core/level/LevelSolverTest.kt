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

    // --- Mandatory sections ---

    private fun mandatory(x: Int, y: Int, section: PipeSection) = MandatorySection(Position(x, y), section)

    /**
     * Asserts that [path] is a route for [level]: adjacent steps from the start through the path into
     * the finish, every ordinary cell free and used once, and every mandatory channel passed exactly
     * once, straight through and in a direction the section allows.
     */
    private fun assertRoute(level: Level, path: List<Position>) {
        val chain = listOf(level.start.position) + path + level.finish.position
        chain.zipWithNext().forEach { (from, to) ->
            assertTrue(Side.entries.any { from.neighbour(it) == to }, "$from and $to are not adjacent in $path")
        }
        val ordinary = path.filter { level.mandatoryAt(it) == null }
        assertEquals(ordinary.size, ordinary.toSet().size, "an ordinary cell is used twice in $path")
        ordinary.forEach { cell ->
            assertTrue(cell in level.grid && !level.isBlocked(cell), "$cell is not a free cell")
            assertTrue(cell != level.start.position && cell != level.finish.position, "$cell is an endpoint")
        }
        val passed = mutableListOf<Pair<Position, Boolean>>()
        for (i in 1 until chain.size - 1) {
            val section = level.mandatoryAt(chain[i])?.section ?: continue
            val (before, cell, after) = Triple(chain[i - 1], chain[i], chain[i + 1])
            val horizontal = before.y == cell.y
            assertEquals(Position(2 * cell.x - before.x, 2 * cell.y - before.y), after, "the route turns inside the mandatory $section at $cell")
            assertTrue(horizontal || section == PipeSection.X, "the mandatory $section at $cell is passed from top to bottom")
            passed += cell to horizontal
        }
        val required = level.mandatory.flatMap { m ->
            if (m.section == PipeSection.X) listOf(m.position to true, m.position to false) else listOf(m.position to true)
        }
        assertEquals(required.toSet(), passed.toSet(), "mandatory channels passed by $path")
        assertEquals(passed.size, passed.toSet().size, "a mandatory channel is passed twice in $path")
    }

    @Test
    fun `route through a mandatory reservoir`() {
        val level = valid(mandatory = listOf(mandatory(3, 2, PipeSection.RES)))
        val path = solvable(level).path
        assertTrue(Position(3, 2) in path)
        assertRoute(level, path)
    }

    @Test
    fun `a mandatory reservoir that cannot be entered from the left is unsolvable`() {
        val level = valid(blocked = listOf(Position(2, 2)), mandatory = listOf(mandatory(3, 2, PipeSection.RES)))
        val reason = unsolvable(level).reason
        assertTrue("mandatory RES at (3, 2)" in reason, reason)
        assertTrue("(2, 2)" in reason, reason)
    }

    @Test
    fun `a mandatory cross needs both channels`() {
        val level = valid(blocked = listOf(Position(3, 1), Position(3, 3)), mandatory = listOf(mandatory(3, 2, PipeSection.X)))
        val reason = unsolvable(level).reason
        assertTrue("mandatory X at (3, 2)" in reason, reason)
        assertTrue("top to bottom" in reason, reason)
    }

    @Test
    fun `a mandatory cross with zero stack weight is solvable through both channels`() {
        val level = valid(
            stack = StackSettings(weights = weights(PipeSection.X to 0)),
            mandatory = listOf(mandatory(3, 2, PipeSection.X)),
        )
        val path = solvable(level).path
        assertEquals(2, path.count { it == Position(3, 2) })
        assertRoute(level, path)
    }

    @Test
    fun `a mandatory section on a facing cell is entered from the endpoint`() {
        val atStart = valid(mandatory = listOf(mandatory(1, 1, PipeSection.RES)))
        assertEquals(Position(1, 1), solvable(atStart).path.first())
        assertRoute(atStart, solvable(atStart).path)

        val atFinish = valid(mandatory = listOf(mandatory(5, 7, PipeSection.X)))
        assertEquals(Position(5, 7), solvable(atFinish).path.last())
        assertRoute(atFinish, solvable(atFinish).path)
    }

    @Test
    fun `adjacent mandatory sections are passed one into the other`() {
        val level = valid(mandatory = listOf(mandatory(2, 2, PipeSection.RES), mandatory(3, 2, PipeSection.X), mandatory(4, 2, PipeSection.RES)))
        assertRoute(level, solvable(level).path)
    }

    @Test
    fun `a mandatory section sealed off from the route is named`() {
        // The reservoir at (3, 2) and both its neighbours are walled in, so no route can reach them.
        val wall = listOf(Position(1, 2), Position(5, 2)) + (2..4).flatMap { listOf(Position(it, 1), Position(it, 3)) }
        val level = valid(blocked = wall, mandatory = listOf(mandatory(3, 2, PipeSection.RES)))
        val reason = unsolvable(level).reason
        assertTrue("mandatory RES at (3, 2)" in reason, reason)
    }

    @Test
    fun `a level without mandatory sections keeps its shortest route`() {
        val level = valid(blocked = emptyList())
        assertEquals(1 + 4 + 6, solvable(level).path.size)
    }

    // Portal pairs: a 7 by 4 grid split by a wall in column 3, start on the left at (0, 1), finish
    // on the right at (6, 3) open at the top, so the finish's facing cell is (6, 2).

    private val splitGrid = GridSize(7, 4)
    private val splitStart = at(0, 1, Side.RIGHT)
    private val splitFinish = at(6, 3, Side.TOP)
    private val splitWall = (0 until 4).map { Position(3, it) }

    private fun split(portals: List<PortalPair>, mandatory: List<MandatorySection> = emptyList()) = valid(
        grid = splitGrid, start = splitStart, finish = splitFinish, blocked = splitWall, mandatory = mandatory, portals = portals,
    )

    /** Asserts that consecutive cells of [path] are neighbours, except a portal followed by its partner. */
    private fun assertWalkable(level: Level, path: List<Position>) {
        path.zipWithNext().forEach { (from, to) ->
            val jump = level.partnerOf(from)?.position == to
            assertTrue(jump || Side.entries.any { from.neighbour(it) == to }, "$from and $to are not adjacent")
        }
        assertEquals(path.size, path.distinct().size, "path $path uses a cell twice")
    }

    @Test
    fun `a level that is only solvable through a portal pair runs into one portal and out of the other`() {
        val level = split(listOf(TestLevels.pair(2, 3, Side.TOP, 4, 0, Side.BOTTOM)))
        val path = solvable(level).path
        assertEquals(Position(1, 1), path.first())
        assertEquals(Position(6, 2), path.last())
        val into = path.indexOf(Position(2, 3))
        assertEquals(listOf(Position(2, 2), Position(2, 3), Position(4, 0), Position(4, 1)), path.subList(into - 1, into + 3))
        assertWalkable(level, path)
        assertTrue(LevelSolver.solve(split(emptyList())) is SolverResult.Unsolvable)
    }

    @Test
    fun `a portal pair that does not cross the wall leaves the level unsolvable`() {
        val reason = unsolvable(split(listOf(TestLevels.pair(2, 3, Side.TOP, 0, 3, Side.TOP)))).reason
        assertTrue("(6, 2)" in reason, reason)
    }

    @Test
    fun `a mandatory reservoir that can only be reached through a pair is passed after the pair`() {
        val level = split(listOf(TestLevels.pair(2, 3, Side.TOP, 4, 0, Side.BOTTOM)), listOf(mandatory(5, 1, PipeSection.RES)))
        val path = solvable(level).path
        assertTrue(path.indexOf(Position(4, 0)) < path.indexOf(Position(5, 1)), "path $path")
        assertWalkable(level, path)
    }

    @Test
    fun `a portal facing a mandatory reservoir feeds it`() {
        val level = split(listOf(TestLevels.pair(2, 3, Side.TOP, 4, 1, Side.RIGHT)), listOf(mandatory(5, 1, PipeSection.RES)))
        val path = solvable(level).path
        val into = path.indexOf(Position(2, 3))
        assertEquals(listOf(Position(2, 3), Position(4, 1), Position(5, 1), Position(6, 1)), path.subList(into, into + 4))
        assertWalkable(level, path)
    }

    @Test
    fun `a mandatory reservoir may be left through a portal that faces it`() {
        val level = split(listOf(TestLevels.pair(2, 1, Side.LEFT, 4, 0, Side.BOTTOM)), listOf(mandatory(1, 1, PipeSection.RES)))
        val path = solvable(level).path
        assertEquals(listOf(Position(1, 1), Position(2, 1), Position(4, 0), Position(4, 1)), path.take(4))
        assertWalkable(level, path)
    }

    @Test
    fun `a portal pair that is no shortcut leaves the shortest route as it was`() {
        val level = valid(blocked = emptyList())
        val withPair = level.copy(portals = listOf(TestLevels.pair(0, 8, Side.TOP, 6, 0, Side.BOTTOM)))
        val path = solvable(withPair).path
        assertEquals(solvable(level).path.size, path.size)
        assertTrue(path.none { withPair.portalAt(it) != null }, "path $path")
    }

    @Test
    fun `every route the solver returns is self-avoiding and passes each mandatory channel`() {
        val random = kotlin.random.Random(42)
        val grid = GridSize(10, 10)
        val start = at(0, 4, Side.RIGHT)
        val finish = at(9, 5, Side.LEFT)
        var solved = 0
        repeat(300) {
            val cells = (0 until 10).flatMap { x -> (0 until 10).map { y -> Position(x, y) } }
                .filter { it != start.position && it != finish.position }
                .shuffled(random)
            // Mandatory sections away from the edge, where every channel has two neighbours.
            val inside = cells.filter { it.x in 1..8 && it.y in 1..8 }
            val sections = inside.take(random.nextInt(1, 7)).map { MandatorySection(it, if (random.nextBoolean()) PipeSection.X else PipeSection.RES) }
            val protected = setOf(start.facingCell, finish.facingCell) + sections.map { it.position }
            val blocked = cells.filter { it !in protected && random.nextInt(100) < 15 }
            val level = valid(grid = grid, start = start, finish = finish, blocked = blocked, mandatory = sections)
            val result = LevelSolver.solve(level)
            if (result is SolverResult.Solvable) {
                assertRoute(level, result.path)
                solved++
            }
        }
        assertTrue(solved >= 50, "only $solved of 300 random levels were solvable")
    }

    @Test
    fun `a 25 by 15 level with 6 mandatory sections solves within the time budget`() {
        val grid = GridSize(25, 15)
        val start = at(0, 7, Side.RIGHT)
        val finish = at(24, 6, Side.LEFT)
        val sections = listOf(
            mandatory(5, 3, PipeSection.X), mandatory(20, 3, PipeSection.RES), mandatory(12, 7, PipeSection.X),
            mandatory(5, 11, PipeSection.RES), mandatory(20, 11, PipeSection.X), mandatory(12, 2, PipeSection.RES),
        )
        val protected = sections.flatMap { m -> listOf(m.position) + Side.entries.map(m.position::neighbour) }.toSet() +
            setOf(start.position, finish.position, start.facingCell, finish.facingCell)
        // A scattered pattern of blocked cells, about one in eleven.
        val blocked = (0 until 25).flatMap { x -> (0 until 15).map { y -> Position(x, y) } }
            .filter { (it.x * 7 + it.y * 13) % 11 == 0 && it !in protected }
        val level = valid(grid = grid, start = start, finish = finish, blocked = blocked, mandatory = sections)
        assertEquals(emptyList(), LevelValidator.validate(level).errors)

        val started = System.nanoTime()
        val path = solvable(level).path
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        assertRoute(level, path)
        assertTrue(elapsedMs < 2_000, "solving took $elapsedMs ms")
    }

    @Test
    fun `an unsolvable 25 by 15 level with 6 mandatory sections gives up within the time budget`() {
        // As above, but the finish is walled in behind its facing cell's neighbours, so the search runs out.
        val grid = GridSize(25, 15)
        val start = at(0, 7, Side.RIGHT)
        val finish = at(24, 6, Side.LEFT)
        val sections = listOf(
            mandatory(5, 3, PipeSection.X), mandatory(20, 3, PipeSection.RES), mandatory(12, 7, PipeSection.X),
            mandatory(5, 11, PipeSection.RES), mandatory(20, 11, PipeSection.X), mandatory(12, 2, PipeSection.RES),
        )
        val wall = listOf(Position(23, 5), Position(22, 6), Position(23, 7), Position(24, 5), Position(24, 7))
        val level = valid(grid = grid, start = start, finish = finish, blocked = wall, mandatory = sections)

        val started = System.nanoTime()
        unsolvable(level)
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        assertTrue(elapsedMs < 10_000, "giving up took $elapsedMs ms")
    }
}
