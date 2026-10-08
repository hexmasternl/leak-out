package nl.hexmaster.leakout.ui.game

import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.flow.Channel
import nl.hexmaster.leakout.core.flow.ChannelUse
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import org.junit.Assert.assertEquals
import org.junit.Test

class CellDescriptionTest {
    private val at = Position(2, 1)

    /** The English strings the descriptions use, so the test reads like what a screen reader says. */
    private val english = mapOf(
        R.string.cell_empty to "Empty",
        R.string.cell_blocked to "Blocked",
        R.string.cell_start to "Start",
        R.string.cell_finish to "Finish",
        R.string.cell_placed to "Placed %1\$s",
        R.string.cell_placed_filled to "Placed %1\$s, filled",
        R.string.cell_mandatory_used to "Mandatory %1\$s, used",
        R.string.cell_mandatory_unused to "Mandatory %1\$s, not used yet",
        R.string.game_section_h to "Horizontal straight",
        R.string.game_section_x to "Cross section",
        R.string.game_section_res to "Reservoir",
    )

    private fun spoken(cell: Cell, filled: Set<ChannelUse> = emptySet()): String =
        CellDescription.text(CellDescription.of(cell, at, filled)) { id, args -> english.getValue(id).format(*args) }

    @Test
    fun `cells without a section`() {
        assertEquals("Empty", spoken(Cell.Empty))
        assertEquals("Blocked", spoken(Cell.Blocked))
        assertEquals("Start", spoken(Cell.Start(Side.RIGHT)))
        assertEquals("Finish", spoken(Cell.Finish(Side.LEFT)))
    }

    @Test
    fun `placed sections say their spoken name`() {
        assertEquals("Placed Horizontal straight", spoken(Cell.Pipe(PipeSection.H)))
        assertEquals("Placed Cross section, filled", spoken(Cell.Pipe(PipeSection.X, liquidEntered = true)))
    }

    @Test
    fun `a mandatory reservoir is used once its channel is filled`() {
        val reservoir = Cell.Pipe(PipeSection.RES, mandatory = true)
        assertEquals("Mandatory Reservoir, not used yet", spoken(reservoir))
        assertEquals("Mandatory Reservoir, used", spoken(reservoir, setOf(ChannelUse(at, Channel.SINGLE))))
    }

    @Test
    fun `a mandatory cross needs both channels`() {
        val cross = Cell.Pipe(PipeSection.X, liquidEntered = true, mandatory = true)
        assertEquals("Mandatory Cross section, not used yet", spoken(cross, setOf(ChannelUse(at, Channel.HORIZONTAL))))
        assertEquals(
            "Mandatory Cross section, used",
            spoken(cross, setOf(ChannelUse(at, Channel.HORIZONTAL), ChannelUse(at, Channel.VERTICAL))),
        )
    }

    @Test
    fun `every section has its own spoken name`() {
        val names = PipeSection.entries.map { CellDescription.sectionName(it) }
        assertEquals(PipeSection.entries.size, names.toSet().size)
    }
}
