package nl.hexmaster.leakout.core.level

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LevelDraftTest {
    private val blank = LevelDraft.new(id = 5)

    private fun row(y: Int, xs: IntRange) = xs.map { Position(it, y) }

    // New level

    @Test
    fun `new draft is the blank level from the spec`() {
        val level = blank.level
        assertEquals(5, level.id)
        assertEquals("New level", level.name)
        assertEquals(GridSize(7, 5), level.grid)
        assertEquals(Endpoint(Position(0, 2), Side.RIGHT), level.start)
        assertEquals(Endpoint(Position(6, 2), Side.LEFT), level.finish)
        assertEquals(emptyList(), level.blocked)
        assertEquals(FlowSettings(20_000, 0.4), level.flow)
        assertEquals(StackSettings(), level.stack)
        assertEquals(PlacementRules(), level.rules)
        assertEquals(StarBenchmarks(200, 250, 330), level.stars)
        assertEquals(emptyList(), level.mandatory)
    }

    @Test
    fun `blank level is valid and solvable`() {
        assertIs<DesignStatus.Solvable>(blank.status)
        assertTrue(blank.status.isPlayable)
    }

    // Grid size

    @Test
    fun `growing the grid keeps start, finish and blocked cells`() {
        val draft = blank.toggleBlocked(Position(3, 1)).resize(10, 5)
        assertEquals(GridSize(10, 5), draft.level.grid)
        assertEquals(blank.level.start, draft.level.start)
        assertEquals(blank.level.finish, draft.level.finish)
        assertEquals(listOf(Position(3, 1)), draft.level.blocked)
    }

    @Test
    fun `shrinking drops blocked cells outside the grid and keeps the rest`() {
        val draft = blank.resize(10, 5)
            .paintBlocked(listOf(Position(8, 1), Position(2, 1), Position(7, 3)), blocked = true)
            .resize(8, 5)
        assertEquals(listOf(Position(2, 1), Position(7, 3)), draft.level.blocked)
    }

    @Test
    fun `shrinking moves the finish to the nearest cell inside, keeping its open side`() {
        val draft = blank.resize(10, 5).placeFinish(Position(9, 2)).resize(6, 5)
        assertEquals(Endpoint(Position(5, 2), Side.LEFT), draft.level.finish)
    }

    @Test
    fun `shrinking rows moves the start up`() {
        val draft = blank.resize(7, 10).placeStart(Position(0, 9)).resize(7, 6)
        assertEquals(Endpoint(Position(0, 5), Side.RIGHT), draft.level.start)
    }

    @Test
    fun `endpoints clamped onto one cell are separated`() {
        val draft = blank.resize(10, 5).placeStart(Position(8, 0)).placeFinish(Position(9, 0)).resize(4, 5)
        assertEquals(Position(3, 0), draft.level.start.position)
        assertTrue(draft.level.finish.position != draft.level.start.position)
        assertTrue(draft.level.finish.position in draft.level.grid)
    }

    @Test
    fun `an endpoint moved onto a blocked cell unblocks it`() {
        val draft = blank.resize(10, 5).placeFinish(Position(9, 2)).toggleBlocked(Position(5, 2)).resize(6, 5)
        assertEquals(Position(5, 2), draft.level.finish.position)
        assertEquals(emptyList(), draft.level.blocked)
    }

    @Test
    fun `grid size is clamped to 4 to 50`() {
        assertEquals(GridSize(4, 4), blank.resize(1, -3).level.grid)
        assertEquals(GridSize(50, 50), blank.resize(51, 400).level.grid)
    }

    @Test
    fun `resize drops mandatory sections outside the grid`() {
        val draft = LevelDraft(blank.resize(10, 5).level.copy(mandatory = listOf(MandatorySection(Position(8, 1), PipeSection.X))))
        assertEquals(emptyList(), draft.resize(7, 5).level.mandatory)
    }

    // Start and finish

    @Test
    fun `placing the start keeps its open side`() {
        assertEquals(Endpoint(Position(2, 3), Side.RIGHT), blank.placeStart(Position(2, 3)).level.start)
    }

    @Test
    fun `placing the start on a blocked cell unblocks it`() {
        val draft = blank.toggleBlocked(Position(2, 3)).toggleBlocked(Position(4, 4)).placeStart(Position(2, 3))
        assertEquals(Position(2, 3), draft.level.start.position)
        assertEquals(listOf(Position(4, 4)), draft.level.blocked)
    }

    @Test
    fun `endpoints cannot share a cell`() {
        assertSame(blank, blank.placeFinish(blank.level.start.position))
        assertSame(blank, blank.placeStart(blank.level.finish.position))
    }

    @Test
    fun `cells outside the grid are ignored`() {
        assertSame(blank, blank.placeStart(Position(7, 2)))
        assertSame(blank, blank.placeFinish(Position(-1, 0)))
    }

    @Test
    fun `endpoints are not placed on a mandatory section`() {
        val draft = LevelDraft(blank.level.copy(mandatory = listOf(MandatorySection(Position(3, 2), PipeSection.X))))
        assertSame(draft, draft.placeStart(Position(3, 2)))
    }

    @Test
    fun `open side can be changed`() {
        val draft = blank.setOpenSide(EndpointKind.FINISH, Side.TOP)
        assertEquals(Side.TOP, draft.level.finish.openSide)
        assertEquals(blank.level.finish.position, draft.level.finish.position)
        assertEquals(Side.BOTTOM, blank.setOpenSide(EndpointKind.START, Side.BOTTOM).level.start.openSide)
    }

    // Blocked cells

    @Test
    fun `toggle blocks then frees a cell`() {
        val once = blank.toggleBlocked(Position(3, 1))
        assertEquals(listOf(Position(3, 1)), once.level.blocked)
        assertEquals(emptyList(), once.toggleBlocked(Position(3, 1)).level.blocked)
    }

    @Test
    fun `toggle never blocks an endpoint or a cell outside the grid`() {
        assertSame(blank, blank.toggleBlocked(blank.level.start.position))
        assertSame(blank, blank.toggleBlocked(blank.level.finish.position))
        assertSame(blank, blank.toggleBlocked(Position(9, 9)))
    }

    @Test
    fun `drag paint blocks every cell passed`() {
        val draft = blank.paintBlocked(row(1, 1..4), blocked = true)
        assertEquals(row(1, 1..4), draft.level.blocked)
    }

    @Test
    fun `drag paint skips the start and blocks the cells around it`() {
        val draft = blank.paintBlocked(listOf(Position(0, 1), Position(0, 2), Position(0, 3)), blocked = true)
        assertEquals(listOf(Position(0, 1), Position(0, 3)), draft.level.blocked)
    }

    @Test
    fun `drag paint never adds a duplicate`() {
        val draft = blank.toggleBlocked(Position(2, 1)).paintBlocked(row(1, 1..3) + row(1, 1..3), blocked = true)
        assertEquals(listOf(Position(2, 1), Position(1, 1), Position(3, 1)), draft.level.blocked)
    }

    @Test
    fun `drag paint frees cells`() {
        val draft = blank.paintBlocked(row(1, 1..4), blocked = true).paintBlocked(row(1, 2..3), blocked = false)
        assertEquals(listOf(Position(1, 1), Position(4, 1)), draft.level.blocked)
    }

    @Test
    fun `clear blocked removes every blocked cell`() {
        assertEquals(emptyList(), blank.paintBlocked(row(1, 1..4), blocked = true).clearBlocked().level.blocked)
    }

    // Other fields

    @Test
    fun `field setters change only their field`() {
        val draft = blank
            .withId(9)
            .withName("Pipes")
            .withFlow(FlowSettings(1_000, 0.8, 0.5, 0.1))
            .withVisibleStackSize(3)
            .withWeight(PipeSection.X, 1)
            .withRules(PlacementRules(allowReplace = false, replacePenaltyMs = 10))
        val level = draft.level
        assertEquals(9, level.id)
        assertEquals("Pipes", level.name)
        assertEquals(FlowSettings(1_000, 0.8, 0.5, 0.1), level.flow)
        assertEquals(3, level.stack.visibleSize)
        assertEquals(1, level.stack.weightOf(PipeSection.X))
        assertEquals(7, level.stack.weightOf(PipeSection.H))
        assertEquals(PlacementRules(false, 10), level.rules)
        assertEquals(blank.level.stars, level.stars)
        assertEquals(blank.level.grid, level.grid)
    }

    // Mandatory sections

    private val cross = PipeSection.X
    private val reservoir = PipeSection.RES

    @Test
    fun `a mandatory cross is placed on a free cell`() {
        val draft = blank.toggleMandatory(Position(3, 2), cross)
        assertEquals(listOf(MandatorySection(Position(3, 2), cross)), draft.level.mandatory)
    }

    @Test
    fun `tapping a mandatory section of the same kind removes it`() {
        val draft = blank.toggleMandatory(Position(3, 2), reservoir).toggleMandatory(Position(3, 2), reservoir)
        assertEquals(emptyList(), draft.level.mandatory)
    }

    @Test
    fun `tapping a mandatory section of the other kind changes it in place`() {
        val draft = blank
            .toggleMandatory(Position(2, 1), cross)
            .toggleMandatory(Position(3, 2), cross)
            .toggleMandatory(Position(2, 1), reservoir)
        assertEquals(
            listOf(MandatorySection(Position(2, 1), reservoir), MandatorySection(Position(3, 2), cross)),
            draft.level.mandatory,
        )
    }

    @Test
    fun `mandatory sections are refused on blocked cells, endpoints and outside the grid`() {
        val blocked = blank.toggleBlocked(Position(3, 1))
        assertSame(blocked, blocked.toggleMandatory(Position(3, 1), cross))
        assertSame(blank, blank.toggleMandatory(blank.level.start.position, reservoir))
        assertSame(blank, blank.toggleMandatory(blank.level.finish.position, cross))
        assertSame(blank, blank.toggleMandatory(Position(7, 2), cross))
    }

    @Test
    fun `at most six mandatory sections, which can still be removed or changed`() {
        val six = row(1, 1..5).plus(Position(1, 3)).fold(blank) { draft, cell -> draft.toggleMandatory(cell, cross) }
        assertEquals(6, six.level.mandatory.size)
        assertSame(six, six.toggleMandatory(Position(3, 3), cross))
        assertEquals(5, six.toggleMandatory(Position(1, 1), cross).level.mandatory.size)
        assertEquals(reservoir, six.toggleMandatory(Position(1, 1), reservoir).level.mandatoryAt(Position(1, 1))?.section)
    }

    @Test
    fun `only a cross or a reservoir can be mandatory`() {
        assertFailsWith<IllegalArgumentException> { blank.toggleMandatory(Position(3, 2), PipeSection.H) }
    }

    @Test
    fun `the block tool skips a mandatory section`() {
        val draft = blank.toggleMandatory(Position(3, 2), cross)
        assertSame(draft, draft.toggleBlocked(Position(3, 2)))
    }

    @Test
    fun `a design with mandatory sections stays valid`() {
        val draft = blank.toggleMandatory(Position(3, 2), cross).toggleMandatory(Position(2, 0), reservoir)
        assertTrue(draft.status !is DesignStatus.Invalid)
    }

    // Star thresholds

    @Test
    fun `stars follow the free cells`() {
        assertEquals(StarBenchmarks(360, 450, 600), StarBenchmarks.forFreeCells(60))
        assertEquals(StarBenchmarks(350, 440, 590), StarBenchmarks.forFreeCells(59))
        assertEquals(StarBenchmarks(200, 250, 330), StarBenchmarks.forFreeCells(33))
        assertEquals(StarBenchmarks(20, 20, 30), StarBenchmarks.forFreeCells(3))
        assertEquals(StarBenchmarks(0, 0, 0), StarBenchmarks.forFreeCells(0))
    }

    @Test
    fun `halves round up`() {
        // 75% of 100 is 75, and 75% of 20 is 15: both round up to the next 10.
        assertEquals(StarBenchmarks(60, 80, 100), StarBenchmarks.forFreeCells(10))
        assertEquals(StarBenchmarks(10, 20, 20), StarBenchmarks.forFreeCells(2))
    }

    @Test
    fun `free cells leave out blocked cells, the start and the finish`() {
        val draft = blank.resize(7, 9).toggleBlocked(Position(3, 4))
        assertEquals(60, draft.level.freeCells)
        assertEquals(StarBenchmarks(360, 450, 600), draft.level.stars)
    }

    @Test
    fun `stars are recalculated after every grid edit`() {
        val sixty = blank.resize(7, 9).toggleBlocked(Position(3, 4))
        assertEquals(StarBenchmarks(350, 440, 590), sixty.toggleBlocked(Position(3, 5)).level.stars)
        assertEquals(StarBenchmarks(370, 460, 610), sixty.clearBlocked().level.stars)
        assertEquals(sixty.level.stars, sixty.placeStart(Position(0, 3)).level.stars)
        assertEquals(StarBenchmarks(370, 460, 610), sixty.placeStart(Position(3, 4)).level.stars)
    }

    @Test
    fun `mandatory sections do not change the stars`() {
        assertEquals(blank.level.stars, blank.toggleMandatory(Position(3, 2), cross).level.stars)
    }

    @Test
    fun `a draft built from any level calculates its stars`() {
        val level = blank.level.copy(stars = StarBenchmarks(1, 2, 3))
        assertEquals(StarBenchmarks(200, 250, 330), LevelDraft(level).level.stars)
    }

    @Test
    fun `fewer than four free cells is invalid`() {
        val crowded = blank.resize(4, 4).paintBlocked(allCells(4).filter { it.y != 2 } + Position(1, 2), blocked = true)
        assertEquals(1, crowded.level.freeCells)
        val status = assertIs<DesignStatus.Invalid>(crowded.status)
        assertTrue("stars" in status.errors.map { it.field })
    }

    private fun allCells(size: Int) = (0 until size).flatMap { y -> (0 until size).map { x -> Position(x, y) } }

    // Status

    @Test
    fun `several errors are listed and the start is highlighted`() {
        // 4 by 4 with 11 cells blocked: 3 free cells give the non-increasing stars 20, 20 and 30.
        val draft = blank.resize(4, 4)
            .paintBlocked(allCells(4).filter { it.y != 2 }.drop(1), blocked = true)
            .setOpenSide(EndpointKind.START, Side.LEFT)
        assertEquals(3, draft.level.freeCells)
        val status = assertIs<DesignStatus.Invalid>(draft.status)
        assertEquals(listOf("start.openSide", "stars"), status.errors.map { it.field })
        assertEquals(setOf(Position(0, 2)), status.highlightedCells)
        assertTrue(!status.isPlayable)
    }

    @Test
    fun `walled-off finish is unsolvable with the solver's reason`() {
        val draft = blank.paintBlocked(listOf(Position(4, 1), Position(4, 2), Position(4, 3), Position(4, 0), Position(4, 4)), blocked = true)
        val status = assertIs<DesignStatus.Unsolvable>(draft.status)
        assertTrue(status.reason.isNotBlank())
        assertTrue(!status.isPlayable)
    }

    @Test
    fun `error fields map to cells`() {
        val level = blank.paintBlocked(listOf(Position(2, 1), Position(3, 3)), blocked = true).level
            .copy(mandatory = listOf(MandatorySection(Position(4, 4), PipeSection.RES)))
        assertEquals(level.start.position, DesignStatus.cellOf("start.x", level))
        assertEquals(level.finish.position, DesignStatus.cellOf("finish", level))
        assertEquals(Position(3, 3), DesignStatus.cellOf("blocked[1]", level))
        assertEquals(Position(4, 4), DesignStatus.cellOf("mandatory[0].section", level))
        assertEquals(null, DesignStatus.cellOf("blocked[7]", level))
        assertEquals(null, DesignStatus.cellOf("stars.one", level))
    }
}
