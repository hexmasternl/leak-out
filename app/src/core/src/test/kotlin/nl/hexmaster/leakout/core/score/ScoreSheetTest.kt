package nl.hexmaster.leakout.core.score

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position

class ScoreSheetTest {
    @Test
    fun `a new sheet is 0`() {
        val sheet = ScoreSheet()
        assertEquals(0, sheet.total)
        assertTrue(sheet.passes.isEmpty())
    }

    @Test
    fun `a route of two straights, three corners and a reservoir totals 90`() {
        val route = listOf(
            Position(1, 0) to PipeSection.H,
            Position(2, 0) to PipeSection.LB,
            Position(2, 1) to PipeSection.V,
            Position(2, 2) to PipeSection.RT,
            Position(3, 2) to PipeSection.RES,
            Position(4, 2) to PipeSection.LT,
        )
        val sheet = route.fold(ScoreSheet()) { sheet, (at, section) -> sheet.award(at, section).sheet }
        assertEquals(90, sheet.total)
    }

    @Test
    fun `two exits from the same cross give 10 then 50`() {
        val at = Position(3, 3)
        val first = ScoreSheet().award(at, PipeSection.X)
        val second = first.sheet.award(at, PipeSection.X)
        assertEquals(10, first.points)
        assertEquals(50, second.points)
        assertEquals(60, second.sheet.total)
        assertEquals(2, second.sheet.passes[at])
    }

    @Test
    fun `exits from two different crosses give 10 each`() {
        val first = ScoreSheet().award(Position(1, 1), PipeSection.X)
        val second = first.sheet.award(Position(2, 1), PipeSection.X)
        assertEquals(10, first.points)
        assertEquals(10, second.points)
        assertEquals(20, second.sheet.total)
    }

    @Test
    fun `award does not change the original sheet`() {
        val original = ScoreSheet()
        original.award(Position(0, 0), PipeSection.RES)
        assertEquals(ScoreSheet(), original)
    }
}
