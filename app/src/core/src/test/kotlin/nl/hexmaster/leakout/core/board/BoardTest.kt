package nl.hexmaster.leakout.core.board

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import nl.hexmaster.leakout.core.level.MandatorySection
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.core.level.TestLevels

class BoardTest {
    private val level = TestLevels.valid()

    @Test
    fun `from level puts start, finish and blocked cells in place`() {
        val board = Board.from(level)
        assertEquals(level.grid, board.grid)
        assertEquals(Cell.Start(Side.RIGHT), board[Position(0, 1)])
        assertEquals(Cell.Finish(Side.LEFT), board[Position(6, 7)])
        assertEquals(Cell.Blocked, board[Position(3, 4)])
        assertTrue(board.pipes.isEmpty())
    }

    @Test
    fun `every other cell is empty`() {
        val board = Board.from(level)
        val fixed = setOf(Position(0, 1), Position(6, 7), Position(3, 4))
        for (x in 0 until level.grid.columns) {
            for (y in 0 until level.grid.rows) {
                val position = Position(x, y)
                if (position !in fixed) assertEquals(Cell.Empty, board[position], "cell $position")
            }
        }
        assertEquals(Cell.Empty, board[Position(-1, 0)])
        assertEquals(Cell.Empty, board[Position(99, 99)])
    }

    @Test
    fun `with returns a copy and leaves the original unchanged`() {
        val board = Board.from(level)
        val changed = board.with(Position(2, 2), Cell.Pipe(PipeSection.H))
        assertEquals(Cell.Pipe(PipeSection.H), changed[Position(2, 2)])
        assertEquals(Cell.Empty, board[Position(2, 2)])
        assertEquals(board, changed.with(Position(2, 2), Cell.Empty))
    }

    @Test
    fun `markLiquidEntered sets the flag on a pipe`() {
        val board = Board.from(level).with(Position(2, 2), Cell.Pipe(PipeSection.V))
        val entered = board.markLiquidEntered(Position(2, 2))
        assertEquals(Cell.Pipe(PipeSection.V, liquidEntered = true), entered[Position(2, 2)])
        assertEquals(Cell.Pipe(PipeSection.V), board[Position(2, 2)])
    }

    @Test
    fun `from level holds its mandatory sections without liquid`() {
        val withMandatory = TestLevels.valid(
            mandatory = listOf(MandatorySection(Position(3, 2), PipeSection.X), MandatorySection(Position(5, 4), PipeSection.RES)),
        )
        val board = Board.from(withMandatory)
        assertEquals(Cell.Pipe(PipeSection.X, liquidEntered = false, mandatory = true), board[Position(3, 2)])
        assertEquals(Cell.Pipe(PipeSection.RES, liquidEntered = false, mandatory = true), board[Position(5, 4)])
        assertEquals(setOf(Position(3, 2), Position(5, 4)), board.pipes.keys)
    }

    @Test
    fun `markLiquidEntered keeps the mandatory flag`() {
        val board = Board.from(TestLevels.valid(mandatory = listOf(MandatorySection(Position(3, 2), PipeSection.X))))
        val entered = board.markLiquidEntered(Position(3, 2))
        assertEquals(Cell.Pipe(PipeSection.X, liquidEntered = true, mandatory = true), entered[Position(3, 2)])
    }

    @Test
    fun `a placed section is not mandatory`() {
        assertEquals(false, Cell.Pipe(PipeSection.X).mandatory)
    }

    @Test
    fun `markLiquidEntered leaves every other cell alone`() {
        val board = Board.from(level)
        for (position in listOf(Position(0, 1), Position(6, 7), Position(3, 4), Position(2, 2))) {
            assertSame(board, board.markLiquidEntered(position), "cell $position")
        }
    }
}
