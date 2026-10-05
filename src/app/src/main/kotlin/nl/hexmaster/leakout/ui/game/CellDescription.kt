package nl.hexmaster.leakout.ui.game

import androidx.annotation.StringRes
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.flow.ChannelUse
import nl.hexmaster.leakout.core.flow.MandatoryProgress
import nl.hexmaster.leakout.core.level.MandatorySection
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position

/**
 * What a screen reader says for a playing field cell, as string resources so it is translated
 * (openspec/specs/app-localization). Kept free of Compose so the mapping is JVM-testable.
 */
object CellDescription {
    /** A cell's description: the string [text], filled in with the spoken name of [section] when there is one. */
    data class Spoken(@StringRes val text: Int, val section: PipeSection? = null)

    /** The description of [cell] at [position]; a mandatory section says whether [filled] uses it. */
    fun of(cell: Cell, position: Position, filled: Set<ChannelUse>): Spoken = when (cell) {
        Cell.Empty -> Spoken(R.string.cell_empty)
        Cell.Blocked -> Spoken(R.string.cell_blocked)
        is Cell.Start -> Spoken(R.string.cell_start)
        is Cell.Finish -> Spoken(R.string.cell_finish)
        is Cell.Pipe -> when {
            cell.mandatory -> {
                val used = MandatoryProgress.isUsed(MandatorySection(position, cell.section), filled)
                Spoken(if (used) R.string.cell_mandatory_used else R.string.cell_mandatory_unused, cell.section)
            }
            cell.liquidEntered -> Spoken(R.string.cell_placed_filled, cell.section)
            else -> Spoken(R.string.cell_placed, cell.section)
        }
    }

    /** The spoken name of [section], for example "Cross section". */
    @StringRes
    fun sectionName(section: PipeSection): Int = when (section) {
        PipeSection.H -> R.string.game_section_h
        PipeSection.V -> R.string.game_section_v
        PipeSection.LT -> R.string.game_section_lt
        PipeSection.LB -> R.string.game_section_lb
        PipeSection.RT -> R.string.game_section_rt
        PipeSection.RB -> R.string.game_section_rb
        PipeSection.X -> R.string.game_section_x
        PipeSection.RES -> R.string.game_section_res
    }

    /** [spoken] as text, with [string] resolving a resource and its format arguments. */
    fun text(spoken: Spoken, string: (Int, Array<out Any>) -> String): String =
        if (spoken.section == null) string(spoken.text, emptyArray()) else string(spoken.text, arrayOf(string(sectionName(spoken.section), emptyArray())))
}
