package nl.hexmaster.leakout.core.score

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import nl.hexmaster.leakout.core.board.Board
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.core.level.StarBenchmarks
import nl.hexmaster.leakout.core.level.TestLevels

class FinalScoreTest {
    // 7 by 9, start (0, 1), finish (6, 7), blocked (3, 4).
    private val empty = Board.from(TestLevels.valid())

    private fun Board.pipe(x: Int, y: Int, section: PipeSection, entered: Boolean = false) =
        with(Position(x, y), Cell.Pipe(section, liquidEntered = entered))

    @Test
    fun `four unused sections cost 12 points`() {
        val board = empty
            .pipe(1, 1, PipeSection.H, entered = true)
            .pipe(2, 1, PipeSection.LB, entered = true)
            .pipe(4, 0, PipeSection.V)
            .pipe(0, 5, PipeSection.X)
            .pipe(5, 5, PipeSection.RES)
            .pipe(2, 8, PipeSection.LT)
        val score = FinalScore.of(120, board)
        assertEquals(120, score.earned)
        assertEquals(4, score.unusedCount)
        assertEquals(12, score.penalty)
        assertEquals(108, score.total)
    }

    @Test
    fun `negative earned points give a total of 0 and 0 stars`() {
        val board = empty.pipe(1, 1, PipeSection.H, entered = true)
        val score = FinalScore.of(-15, board)
        assertEquals(-15, score.earned)
        assertEquals(0, score.penalty)
        assertEquals(0, score.total)
        assertEquals(0, StarBenchmarks(100, 200, 350).starsFor(score.total))
    }

    @Test
    fun `mandatory sections are never unused`() {
        val board = empty
            .pipe(1, 1, PipeSection.H, entered = true)
            .with(Position(4, 0), Cell.Pipe(PipeSection.X, mandatory = true))
            .with(Position(5, 5), Cell.Pipe(PipeSection.RES, liquidEntered = true, mandatory = true))
            .pipe(2, 8, PipeSection.LT)
        val score = FinalScore.of(50, board)
        assertEquals(listOf(Position(2, 8)), score.unusedSections)
        assertEquals(47, score.total)
    }

    @Test
    fun `no unused sections cost nothing`() {
        val board = empty.pipe(1, 1, PipeSection.H, entered = true).pipe(2, 1, PipeSection.LB, entered = true)
        val score = FinalScore.of(25, board)
        assertTrue(score.unusedSections.isEmpty())
        assertEquals(0, score.penalty)
        assertEquals(25, score.total)
    }

    @Test
    fun `the total never goes below 0`() {
        val board = (0 until 5).fold(empty) { board, x -> board.pipe(x, 3, PipeSection.H) }
        val score = FinalScore.of(10, board)
        assertEquals(15, score.penalty)
        assertEquals(0, score.total)
    }

    @Test
    fun `a cross the liquid entered once is not unused`() {
        val score = FinalScore.of(10, empty.pipe(3, 3, PipeSection.X, entered = true))
        assertTrue(score.unusedSections.isEmpty())
    }

    @Test
    fun `start, finish and blocked tiles are never unused`() {
        val board = Board(
            TestLevels.valid().grid,
            mapOf(
                Position(0, 1) to Cell.Start(Side.RIGHT),
                Position(6, 7) to Cell.Finish(Side.LEFT),
                Position(3, 4) to Cell.Blocked,
            ),
        )
        assertEquals(FinalScore(40, emptyList(), 40), FinalScore.of(40, board))
    }

    @Test
    fun `the unused sections come in row-major order`() {
        val board = empty
            .pipe(5, 2, PipeSection.H)
            .pipe(1, 2, PipeSection.V)
            .pipe(6, 0, PipeSection.LT)
            .pipe(0, 8, PipeSection.RB)
            .pipe(3, 2, PipeSection.X)
        assertEquals(
            listOf(Position(6, 0), Position(1, 2), Position(3, 2), Position(5, 2), Position(0, 8)),
            FinalScore.of(100, board).unusedSections,
        )
    }

    @Test
    fun `the stars come from the total`() {
        val stars = StarBenchmarks(70, 110, 160)
        val unused = (0 until 4).fold(empty) { board, x -> board.pipe(x, 0, PipeSection.H) }
        val five = unused.pipe(4, 0, PipeSection.H)
        assertEquals(1, stars.starsFor(FinalScore.of(120, unused).total)) // 108
        assertEquals(3, stars.starsFor(FinalScore.of(160, empty).total))
        assertEquals(0, stars.starsFor(FinalScore.of(10, five).total))
    }

    @Test
    fun `the penalty is 3 points per section`() {
        assertEquals(3, Points.UNUSED_SECTION_PENALTY)
    }
}
