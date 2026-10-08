package nl.hexmaster.leakout.ui.game

import androidx.annotation.StringRes
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.flow.Channel
import nl.hexmaster.leakout.core.flow.ChannelUse
import nl.hexmaster.leakout.core.flow.MandatoryProgress
import nl.hexmaster.leakout.core.level.MandatorySection
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side

/**
 * What a screen reader says for a playing field cell, as string resources so it is translated
 * (openspec/specs/app-localization). Kept free of Compose so the mapping is JVM-testable.
 */
object CellDescription {
    /** A format argument of a description: a number, or a string resource that is resolved first. */
    sealed interface Arg {
        data class Number(val value: Int) : Arg
        data class Text(@StringRes val id: Int) : Arg
    }

    /**
     * A cell's description: the string [text], filled in with the spoken name of [section] when
     * there is one, otherwise with [args].
     */
    data class Spoken(@StringRes val text: Int, val section: PipeSection? = null, val args: List<Arg> = emptyList())

    /**
     * The description of [cell] at [position]; a mandatory section says whether [filled] uses it,
     * and a portal whether the liquid has passed its pair.
     */
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
        is Cell.Portal -> Spoken(
            if (ChannelUse(position, Channel.PORTAL) in filled) R.string.cell_portal_passed else R.string.cell_portal,
            args = listOf(
                Arg.Number(cell.pair),
                Arg.Text(PortalStyle.symbol(cell.pair).spokenName),
                Arg.Text(opensTo(cell.openSide)),
                // Columns and rows are counted from 1 for people.
                Arg.Number(cell.partner.x + 1),
                Arg.Number(cell.partner.y + 1),
            ),
        )
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

    /** Where a portal open to [side] opens, for example "to the right". */
    @StringRes
    fun opensTo(side: Side): Int = when (side) {
        Side.LEFT -> R.string.portal_opens_left
        Side.RIGHT -> R.string.portal_opens_right
        Side.TOP -> R.string.portal_opens_top
        Side.BOTTOM -> R.string.portal_opens_bottom
    }

    /** [spoken] as text, with [string] resolving a resource and its format arguments. */
    fun text(spoken: Spoken, string: (Int, Array<out Any>) -> String): String = when {
        spoken.section != null -> string(spoken.text, arrayOf(string(sectionName(spoken.section), emptyArray())))
        spoken.args.isEmpty() -> string(spoken.text, emptyArray())
        else -> string(
            spoken.text,
            spoken.args.map<Arg, Any> { arg ->
                when (arg) {
                    is Arg.Number -> arg.value
                    is Arg.Text -> string(arg.id, emptyArray())
                }
            }.toTypedArray(),
        )
    }
}
