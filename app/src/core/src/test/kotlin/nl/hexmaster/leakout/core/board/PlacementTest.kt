package nl.hexmaster.leakout.core.board

import kotlin.test.Test
import kotlin.test.assertEquals
import nl.hexmaster.leakout.core.board.PlacementCheck.Reason
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.PlacementRules
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.TestLevels

class PlacementTest {
    private val level = TestLevels.valid()
    private val unfilled = Position(2, 2)
    private val filled = Position(4, 4)
    private val dryMandatory = Position(5, 2)
    private val filledMandatory = Position(1, 6)
    private val board = Board.from(level)
        .with(unfilled, Cell.Pipe(PipeSection.H))
        .with(filled, Cell.Pipe(PipeSection.V))
        .markLiquidEntered(filled)
        .with(dryMandatory, Cell.Pipe(PipeSection.X, mandatory = true))
        .with(filledMandatory, Cell.Pipe(PipeSection.RES, mandatory = true))
        .markLiquidEntered(filledMandatory)
    private val rules = PlacementRules(allowReplace = true, replacePenaltyMs = 500)

    private fun check(at: Position, rules: PlacementRules = this.rules, nowMs: Long = 0, lockUntilMs: Long = 0) =
        Placement.check(board, rules, at, nowMs, lockUntilMs)

    @Test
    fun `an empty tile takes a section`() {
        assertEquals(PlacementCheck.Place, check(Position(1, 1)))
    }

    @Test
    fun `blocked, start and finish tiles refuse`() {
        for (position in listOf(Position(3, 4), level.start.position, level.finish.position)) {
            assertEquals(PlacementCheck.Rejected(Reason.FIXED), check(position), "cell $position")
        }
    }

    @Test
    fun `a pipe without liquid is replaced`() {
        assertEquals(PlacementCheck.Replace, check(unfilled))
    }

    @Test
    fun `a pipe with liquid refuses`() {
        assertEquals(PlacementCheck.Rejected(Reason.FILLED), check(filled))
    }

    @Test
    fun `a pipe refuses when the level does not allow replacing`() {
        assertEquals(PlacementCheck.Rejected(Reason.REPLACE_NOT_ALLOWED), check(unfilled, rules.copy(allowReplace = false)))
        assertEquals(PlacementCheck.Place, check(Position(1, 1), rules.copy(allowReplace = false)))
    }

    @Test
    fun `a dry mandatory section refuses even when replacing is allowed`() {
        assertEquals(PlacementCheck.Rejected(Reason.FIXED), check(dryMandatory, rules.copy(allowReplace = true)))
    }

    @Test
    fun `a mandatory section the liquid has entered refuses as fixed`() {
        assertEquals(PlacementCheck.Rejected(Reason.FIXED), check(filledMandatory))
        assertEquals(PlacementCheck.Rejected(Reason.FIXED), check(filledMandatory, rules.copy(allowReplace = false)))
    }

    @Test
    fun `tiles outside the grid refuse`() {
        for (position in listOf(Position(-1, 0), Position(0, -1), Position(7, 0), Position(0, 9))) {
            assertEquals(PlacementCheck.Rejected(Reason.OUTSIDE), check(position), "cell $position")
        }
    }

    @Test
    fun `the replace lock refuses every tile until it ends`() {
        for (position in listOf(Position(1, 1), unfilled)) {
            assertEquals(PlacementCheck.Rejected(Reason.LOCKED), check(position, nowMs = 999, lockUntilMs = 1_000))
        }
        assertEquals(PlacementCheck.Place, check(Position(1, 1), nowMs = 1_000, lockUntilMs = 1_000))
        assertEquals(PlacementCheck.Replace, check(unfilled, nowMs = 1_000, lockUntilMs = 1_000))
    }
}
