package nl.hexmaster.leakout.core.level

import nl.hexmaster.leakout.core.level.TestLevels.valid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LevelValidatorTest {
    private fun errors(level: Level): List<ValidationError> = LevelValidator.validate(level).errors

    private fun assertValid(level: Level) = assertEquals(emptyList(), errors(level))

    /** Asserts that validation fails with exactly one error, for [field], and returns it. */
    private fun assertOnlyError(field: String, level: Level): ValidationError {
        val errors = errors(level)
        assertEquals(listOf(field), errors.map { it.field }, "errors: $errors")
        return errors.single()
    }

    private fun flow(
        initialDelayMs: Long = 12_000,
        cellsPerSecond: Double = 0.6,
        reservoirSpeedFactor: Double = LevelDefaults.RESERVOIR_SPEED_FACTOR,
        speedRampPerCell: Double = LevelDefaults.SPEED_RAMP_PER_CELL,
    ) = FlowSettings(initialDelayMs, cellsPerSecond, reservoirSpeedFactor, speedRampPerCell)

    private fun endpoints(start: Endpoint, finish: Endpoint) =
        valid(start = start, finish = finish, blocked = emptyList())

    private fun at(x: Int, y: Int, side: Side) = Endpoint(Position(x, y), side)

    // Id and name

    @Test
    fun `valid level has no errors`() = assertValid(valid())

    @Test
    fun `positive id is accepted`() = assertValid(valid(id = 3))

    @Test
    fun `zero or negative id is rejected`() {
        assertOnlyError("id", valid(id = 0))
        assertOnlyError("id", valid(id = -4))
    }

    @Test
    fun `duplicate ids in a set are rejected`() {
        val errors = LevelValidator.validateSet(listOf(valid(id = 1), valid(id = 2), valid(id = 2, name = "Other"))).errors
        assertEquals(1, errors.size)
        assertEquals("id", errors.single().field)
        assertTrue("2" in errors.single().message)
    }

    @Test
    fun `unique ids in a set are accepted`() {
        assertTrue(LevelValidator.validateSet(listOf(valid(id = 1), valid(id = 2))).isValid)
    }

    @Test
    fun `blank name is rejected`() {
        assertOnlyError("name", valid(name = ""))
        assertOnlyError("name", valid(name = "   "))
    }

    // Grid

    @Test
    fun `smallest and largest grids are accepted`() {
        assertValid(valid(grid = GridSize(4, 4), start = at(0, 1, Side.RIGHT), finish = at(3, 2, Side.LEFT), blocked = emptyList()))
        assertValid(valid(grid = GridSize(50, 50)))
    }

    @Test
    fun `grid below the minimum is rejected per dimension`() {
        val small = valid(grid = GridSize(3, 9), start = at(0, 1, Side.RIGHT), finish = at(2, 7, Side.LEFT), blocked = emptyList())
        assertOnlyError("grid.columns", small)
        assertOnlyError("grid.rows", valid(grid = GridSize(7, 3), finish = at(6, 2, Side.LEFT), blocked = emptyList()))
    }

    @Test
    fun `grid above the maximum is rejected per dimension`() {
        assertOnlyError("grid.columns", valid(grid = GridSize(51, 9)))
        assertOnlyError("grid.rows", valid(grid = GridSize(7, 51)))
    }

    // Start and finish

    @Test
    fun `start outside the grid names the coordinate`() {
        assertOnlyError("start.x", valid(start = at(-1, 1, Side.RIGHT)))
        assertOnlyError("start.y", valid(start = at(0, 9, Side.RIGHT)))
    }

    @Test
    fun `finish outside the grid names the coordinate`() {
        assertOnlyError("finish.x", valid(finish = at(7, 7, Side.LEFT)))
        assertOnlyError("finish.y", valid(finish = at(6, -1, Side.LEFT)))
    }

    @Test
    fun `start and finish on the same cell are rejected`() {
        val error = assertOnlyError("finish", valid(start = at(2, 2, Side.RIGHT), finish = at(2, 2, Side.LEFT)))
        assertTrue("different cells" in error.message)
    }

    @Test
    fun `start opening onto the grid edge is rejected`() {
        val error = assertOnlyError("start.openSide", valid(start = at(0, 1, Side.LEFT)))
        assertTrue("edge of the grid" in error.message)
    }

    @Test
    fun `finish opening onto the grid edge is rejected`() {
        val error = assertOnlyError("finish.openSide", valid(finish = at(6, 8, Side.BOTTOM)))
        assertTrue("edge of the grid" in error.message)
    }

    @Test
    fun `start with a blocked facing cell is walled off`() {
        val error = assertOnlyError("start.openSide", valid(blocked = listOf(Position(1, 1))))
        assertTrue("walled off" in error.message)
    }

    @Test
    fun `finish with a blocked facing cell is walled off`() {
        val error = assertOnlyError("finish.openSide", valid(blocked = listOf(Position(5, 7))))
        assertTrue("walled off" in error.message)
    }

    @Test
    fun `start and finish facing each other are rejected`() {
        val error = assertOnlyError("start", endpoints(at(1, 1, Side.RIGHT), at(2, 1, Side.LEFT)))
        assertTrue("face each other" in error.message)
    }

    @Test
    fun `start facing the closed side of the finish is rejected`() {
        val error = assertOnlyError("start.openSide", endpoints(at(1, 1, Side.RIGHT), at(2, 1, Side.TOP)))
        assertTrue("must not be the finish" in error.message)
    }

    @Test
    fun `finish facing the closed side of the start is rejected`() {
        val error = assertOnlyError("finish.openSide", endpoints(at(2, 1, Side.TOP), at(1, 1, Side.RIGHT)))
        assertTrue("must not be the start" in error.message)
    }

    @Test
    fun `start and finish may share a facing cell`() {
        assertValid(endpoints(at(1, 1, Side.RIGHT), at(2, 2, Side.TOP)))
    }

    // Flow

    @Test
    fun `valid flow settings are accepted`() {
        assertValid(valid(flow = flow(12_000, 0.6, 0.3, 0.02)))
    }

    @Test
    fun `zero delay is accepted`() = assertValid(valid(flow = flow(initialDelayMs = 0)))

    @Test
    fun `negative delay is rejected`() {
        assertOnlyError("flow.initialDelayMs", valid(flow = flow(initialDelayMs = -1)))
    }

    @Test
    fun `speed must be positive`() {
        assertOnlyError("flow.cellsPerSecond", valid(flow = flow(cellsPerSecond = 0.0)))
        assertOnlyError("flow.cellsPerSecond", valid(flow = flow(cellsPerSecond = -0.5)))
    }

    @Test
    fun `speed above the upper bound is rejected`() {
        assertOnlyError("flow.cellsPerSecond", valid(flow = flow(cellsPerSecond = 60.0)))
    }

    @Test
    fun `speed on the upper bound is accepted`() = assertValid(valid(flow = flow(cellsPerSecond = 10.0)))

    @Test
    fun `reservoir speed factor out of range is rejected`() {
        for (factor in listOf(0.0, -0.1, 1.1)) {
            assertOnlyError("flow.reservoirSpeedFactor", valid(flow = flow(reservoirSpeedFactor = factor)))
        }
    }

    @Test
    fun `reservoir speed factor of one is accepted`() = assertValid(valid(flow = flow(reservoirSpeedFactor = 1.0)))

    @Test
    fun `negative speed ramp is rejected`() {
        assertOnlyError("flow.speedRampPerCell", valid(flow = flow(speedRampPerCell = -0.01)))
    }

    // Stack and rules

    @Test
    fun `cross and reservoir weights are accepted`() {
        assertValid(valid(stack = StackSettings(weights = TestLevels.weights(PipeSection.X to 1, PipeSection.RES to 2))))
    }

    @Test
    fun `always-on-stack section with zero weight is rejected`() {
        assertOnlyError("stack.weights.LT", valid(stack = StackSettings(weights = TestLevels.weights(PipeSection.LT to 0))))
    }

    @Test
    fun `negative weight is rejected`() {
        assertOnlyError("stack.weights.X", valid(stack = StackSettings(weights = TestLevels.weights(PipeSection.X to -1))))
    }

    @Test
    fun `visible size out of range is rejected`() {
        assertOnlyError("stack.visibleSize", valid(stack = StackSettings(visibleSize = 0)))
        assertOnlyError("stack.visibleSize", valid(stack = StackSettings(visibleSize = 11)))
    }

    @Test
    fun `visible size bounds are accepted`() {
        assertValid(valid(stack = StackSettings(visibleSize = 1)))
        assertValid(valid(stack = StackSettings(visibleSize = 10)))
    }

    @Test
    fun `replacing can be disabled`() = assertValid(valid(rules = PlacementRules(allowReplace = false)))

    @Test
    fun `negative replace penalty is rejected`() {
        assertOnlyError("rules.replacePenaltyMs", valid(rules = PlacementRules(replacePenaltyMs = -1)))
    }

    // Blocked cells

    @Test
    fun `level without blocked cells is accepted`() = assertValid(valid(blocked = emptyList()))

    @Test
    fun `blocked cell inside the grid is accepted and reported`() {
        val level = valid(blocked = listOf(Position(3, 4)))
        assertValid(level)
        assertTrue(level.isBlocked(Position(3, 4)))
    }

    @Test
    fun `blocked cell outside the grid names the cell`() {
        val error = assertOnlyError("blocked[1]", valid(blocked = listOf(Position(3, 4), Position(7, 0))))
        assertTrue("(7, 0)" in error.message)
    }

    @Test
    fun `blocked cell on the start or finish is rejected`() {
        val onStart = errors(valid(blocked = listOf(Position(0, 1))))
        assertEquals(listOf("blocked[0]"), onStart.map { it.field })
        assertTrue("cannot be blocked" in onStart.single().message)
        assertOnlyError("blocked[0]", valid(blocked = listOf(Position(6, 7))))
    }

    @Test
    fun `duplicate blocked cell is rejected`() {
        val error = assertOnlyError("blocked[1]", valid(blocked = listOf(Position(3, 4), Position(3, 4))))
        assertTrue("(3, 4)" in error.message)
    }

    // Stars

    @Test
    fun `valid star benchmarks are accepted`() = assertValid(valid(stars = StarBenchmarks(100, 200, 350)))

    @Test
    fun `star thresholds that do not increase are rejected`() {
        for (stars in listOf(StarBenchmarks(200, 200, 350), StarBenchmarks(200, 100, 350), StarBenchmarks(100, 350, 350))) {
            val error = assertOnlyError("stars", valid(stars = stars))
            assertTrue("strictly increasing" in error.message)
        }
    }

    @Test
    fun `non-positive star threshold is named`() {
        assertOnlyError("stars.one", valid(stars = StarBenchmarks(0, 200, 350)))
        val errors = errors(valid(stars = StarBenchmarks(-10, -5, 350)))
        assertEquals(setOf("stars.one", "stars.two"), errors.map { it.field }.toSet())
    }

    // Reporting

    @Test
    fun `every problem is reported together`() {
        val level = valid(flow = flow(cellsPerSecond = 60.0), blocked = listOf(Position(20, 20)))
        val fields = errors(level).map { it.field }
        assertEquals(listOf("blocked[0]", "flow.cellsPerSecond"), fields.sorted())
    }
}
