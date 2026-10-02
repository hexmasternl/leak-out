package nl.hexmaster.leakout.core.level

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LevelModelTest {
    @Test
    fun `every side has an opposite that leads back`() {
        for (side in Side.entries) {
            assertEquals(side, side.opposite().opposite())
            assertEquals(0, side.dx + side.opposite().dx)
            assertEquals(0, side.dy + side.opposite().dy)
        }
    }

    @Test
    fun `side offsets use y growing downward`() {
        assertEquals(Position(2, 2), Position(2, 3).neighbour(Side.TOP))
        assertEquals(Position(2, 4), Position(2, 3).neighbour(Side.BOTTOM))
        assertEquals(Position(1, 3), Position(2, 3).neighbour(Side.LEFT))
        assertEquals(Position(3, 3), Position(2, 3).neighbour(Side.RIGHT))
    }

    @Test
    fun `facing cell is the neighbour on the open side`() {
        assertEquals(Position(1, 1), Endpoint(Position(0, 1), Side.RIGHT).facingCell)
    }

    @Test
    fun `grid contains only zero-based in-range cells`() {
        val grid = GridSize(columns = 7, rows = 9)
        assertTrue(Position(0, 0) in grid)
        assertTrue(Position(6, 8) in grid)
        assertFalse(Position(7, 0) in grid)
        assertFalse(Position(0, 9) in grid)
        assertFalse(Position(-1, 0) in grid)
    }

    @Test
    fun `stack weights must cover every section`() {
        assertFailsWith<IllegalArgumentException> {
            StackSettings(weights = mapOf(PipeSection.H to 1))
        }
    }

    @Test
    fun `a section is allowed when its weight is positive`() {
        val level = TestLevels.valid(stack = StackSettings(weights = LevelDefaults.STACK_WEIGHTS + (PipeSection.X to 1)))
        assertTrue(level.isAllowed(PipeSection.X))
        assertFalse(level.isAllowed(PipeSection.RES))
        assertTrue(level.isAllowed(PipeSection.LT))
    }

    @Test
    fun `default stack weights deal the straights and corners evenly with specials off`() {
        val settings = StackSettings()
        for (section in PipeSection.entries) {
            assertEquals(if (section.mandatory) 7 else 0, settings.weightOf(section), "$section")
        }
        assertEquals(1.0, LevelDefaults.STEERING_STRENGTH)
        assertEquals(2.5, LevelDefaults.DROUGHT_FACTOR)
    }

    @Test
    fun `cross weight 3 next to the defaults allows the cross`() {
        val level = TestLevels.valid(stack = StackSettings(weights = TestLevels.weights(PipeSection.X to 3)))
        assertTrue(level.isAllowed(PipeSection.X))
        assertEquals(3, level.stack.weightOf(PipeSection.X))
        assertEquals(7, level.stack.weightOf(PipeSection.RB))
    }

    @Test
    fun `stars below the first threshold are zero`() {
        assertEquals(0, StarBenchmarks(100, 200, 350).starsFor(99))
    }

    @Test
    fun `stars on each threshold`() {
        val stars = StarBenchmarks(100, 200, 350)
        assertEquals(1, stars.starsFor(100))
        assertEquals(2, stars.starsFor(200))
        assertEquals(3, stars.starsFor(350))
    }

    @Test
    fun `stars just below each threshold`() {
        val stars = StarBenchmarks(100, 200, 350)
        assertEquals(1, stars.starsFor(199))
        assertEquals(2, stars.starsFor(349))
    }

    @Test
    fun `stars above the last threshold are three`() {
        assertEquals(3, StarBenchmarks(100, 200, 350).starsFor(1000))
    }

    @Test
    fun `zero and negative points earn zero stars`() {
        assertEquals(0, StarBenchmarks(100, 200, 350).starsFor(0))
        assertEquals(0, StarBenchmarks(100, 200, 350).starsFor(-5))
    }
}
