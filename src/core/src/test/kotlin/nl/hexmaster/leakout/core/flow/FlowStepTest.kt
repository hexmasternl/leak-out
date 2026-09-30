package nl.hexmaster.leakout.core.flow

import kotlin.test.Test
import kotlin.test.assertEquals
import nl.hexmaster.leakout.core.board.Board
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.flow.FlowStep.Result
import nl.hexmaster.leakout.core.level.Endpoint
import nl.hexmaster.leakout.core.level.GridSize
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.PipeSection.H
import nl.hexmaster.leakout.core.level.PipeSection.LB
import nl.hexmaster.leakout.core.level.PipeSection.RES
import nl.hexmaster.leakout.core.level.PipeSection.V
import nl.hexmaster.leakout.core.level.PipeSection.X
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.core.level.TestLevels

class FlowStepTest {
    /** Start at (0, 1) open to the right, finish at (2, 3) open at the top, (3, 3) blocked, on a 5 by 5 grid. */
    private val level: Level = TestLevels.valid(
        grid = GridSize(5, 5),
        start = Endpoint(p(0, 1), Side.RIGHT),
        finish = Endpoint(p(2, 3), Side.TOP),
        blocked = listOf(p(3, 3)),
    )

    private fun board(vararg pipes: Pair<Position, PipeSection>): Board =
        pipes.fold(Board.from(level)) { board, (at, section) -> board.with(at, Cell.Pipe(section)) }

    private fun p(x: Int, y: Int) = Position(x, y)

    private fun after(board: Board, from: Position, exit: Side, used: Set<ChannelUse> = emptySet()): Result =
        FlowStep.after(board, from, exit, used)

    @Test
    fun `liquid enters a straight from its left side`() {
        assertEquals(Result.Enter(p(2, 2), Side.LEFT), after(board(p(2, 2) to H), p(1, 2), Side.RIGHT))
    }

    @Test
    fun `liquid reaches the finish through its open side`() {
        assertEquals(Result.Finish, after(board(), p(2, 2), Side.BOTTOM))
    }

    @Test
    fun `liquid leaks into an empty tile`() {
        assertEquals(Result.Leak(p(1, 2), Side.RIGHT), after(board(), p(1, 2), Side.RIGHT))
    }

    @Test
    fun `liquid leaks into a blocked tile`() {
        assertEquals(Result.Leak(p(3, 4), Side.TOP), after(board(), p(3, 4), Side.TOP))
    }

    @Test
    fun `liquid leaks into the start tile`() {
        assertEquals(Result.Leak(p(0, 2), Side.TOP), after(board(), p(0, 2), Side.TOP))
    }

    @Test
    fun `liquid leaks over each edge of the grid`() {
        val board = board()
        assertEquals(Result.Leak(p(0, 3), Side.LEFT), after(board, p(0, 3), Side.LEFT))
        assertEquals(Result.Leak(p(4, 3), Side.RIGHT), after(board, p(4, 3), Side.RIGHT))
        assertEquals(Result.Leak(p(2, 0), Side.TOP), after(board, p(2, 0), Side.TOP))
        assertEquals(Result.Leak(p(1, 4), Side.BOTTOM), after(board, p(1, 4), Side.BOTTOM))
    }

    @Test
    fun `liquid leaks into a vertical straight from the left`() {
        assertEquals(Result.Leak(p(1, 2), Side.RIGHT), after(board(p(2, 2) to V), p(1, 2), Side.RIGHT))
    }

    @Test
    fun `liquid leaks into a reservoir from above`() {
        assertEquals(Result.Leak(p(1, 1), Side.BOTTOM), after(board(p(1, 2) to RES), p(1, 1), Side.BOTTOM))
    }

    @Test
    fun `liquid leaks into the finish from the wrong side`() {
        assertEquals(Result.Leak(p(1, 3), Side.RIGHT), after(board(), p(1, 3), Side.RIGHT))
    }

    @Test
    fun `liquid leaks into a used cross channel from the opposite side`() {
        val used = setOf(ChannelUse(p(2, 2), Channel.HORIZONTAL))
        assertEquals(Result.Leak(p(3, 2), Side.LEFT), after(board(p(2, 2) to X), p(3, 2), Side.LEFT, used))
    }

    @Test
    fun `liquid enters a cross on its free channel`() {
        val used = setOf(ChannelUse(p(2, 2), Channel.HORIZONTAL))
        assertEquals(Result.Enter(p(2, 2), Side.TOP), after(board(p(2, 2) to X), p(2, 1), Side.BOTTOM, used))
    }

    @Test
    fun `a leak is reported at the tile left and the side left through`() {
        assertEquals(Result.Leak(p(3, 2), Side.RIGHT), after(board(p(3, 2) to H), p(3, 2), Side.RIGHT))
    }

    @Test
    fun `the start leaks when the tile next to it is empty`() {
        assertEquals(
            Result.Leak(level.start.position, level.start.openSide),
            after(board(), level.start.position, level.start.openSide),
        )
    }

    @Test
    fun `the start enters a matching section next to it`() {
        assertEquals(
            Result.Enter(p(1, 1), Side.LEFT),
            after(board(p(1, 1) to LB), level.start.position, level.start.openSide),
        )
    }

    @Test
    fun `a cross channel is the axis of the entry side`() {
        assertEquals(Channel.HORIZONTAL, X.channelFor(Side.LEFT))
        assertEquals(Channel.HORIZONTAL, X.channelFor(Side.RIGHT))
        assertEquals(Channel.VERTICAL, X.channelFor(Side.TOP))
        assertEquals(Channel.VERTICAL, X.channelFor(Side.BOTTOM))
        assertEquals(Channel.SINGLE, H.channelFor(Side.LEFT))
        assertEquals(Channel.SINGLE, LB.channelFor(Side.BOTTOM))
    }
}
