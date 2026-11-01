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

    @Test
    fun `a straight, a corner and a reservoir on a leaky tile each cost 20`() {
        for (section in listOf(PipeSection.H, PipeSection.RB, PipeSection.RES)) {
            val award = ScoreSheet(total = 40).award(Position(2, 2), section, leaky = true)
            assertEquals(-Points.LEAKY_TILE_PENALTY, award.points, "$section")
            assertEquals(20, award.sheet.total, "$section")
        }
    }

    @Test
    fun `a cross on a leaky tile passed twice costs 20 twice`() {
        val at = Position(3, 3)
        val first = ScoreSheet(total = 100).award(at, PipeSection.X, leaky = true)
        val second = first.sheet.award(at, PipeSection.X, leaky = true)
        assertEquals(listOf(-20, -20), listOf(first.points, second.points))
        assertEquals(80, first.sheet.total)
        assertEquals(60, second.sheet.total)
        assertEquals(2, second.sheet.passes[at])
    }

    @Test
    fun `a route over a leaky corner scores 30`() {
        val route = listOf(
            Triple(Position(1, 0), PipeSection.H, false),
            Triple(Position(2, 0), PipeSection.LB, false),
            Triple(Position(2, 1), PipeSection.V, false),
            Triple(Position(2, 2), PipeSection.RT, true),
            Triple(Position(3, 2), PipeSection.LT, false),
        )
        val sheet = route.fold(ScoreSheet()) { sheet, (at, section, leaky) -> sheet.award(at, section, leaky).sheet }
        assertEquals(30, sheet.total)
    }

    @Test
    fun `leak penalties take the total below 0`() {
        val first = ScoreSheet().award(Position(1, 0), PipeSection.H, leaky = true)
        val second = first.sheet.award(Position(2, 0), PipeSection.H, leaky = true)
        assertEquals(-20, first.sheet.total)
        assertEquals(-40, second.sheet.total)
    }
}
