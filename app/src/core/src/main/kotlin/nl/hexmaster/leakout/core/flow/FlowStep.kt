package nl.hexmaster.leakout.core.flow

import nl.hexmaster.leakout.core.board.Board
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.core.level.exitFor

/**
 * A channel of a section that liquid runs through. A cross has two, one per axis, so entering it
 * from the left and later from the right is the same channel. Every other section has one. A
 * portal pair is one [PORTAL] channel, recorded on both of its portals.
 */
enum class Channel { HORIZONTAL, VERTICAL, SINGLE, PORTAL }

/** The channel at [position] the liquid has already run through. */
data class ChannelUse(val position: Position, val channel: Channel)

/** The channel [this] section uses for liquid entering through [entry]. */
fun PipeSection.channelFor(entry: Side): Channel = when {
    this != PipeSection.X -> Channel.SINGLE
    entry == Side.LEFT || entry == Side.RIGHT -> Channel.HORIZONTAL
    else -> Channel.VERTICAL
}

/** The uses that mark the portal pair of the portals at [portal] and [partner] as passed. */
fun portalPairUses(portal: Position, partner: Position): Set<ChannelUse> =
    setOf(ChannelUse(portal, Channel.PORTAL), ChannelUse(partner, Channel.PORTAL))

/**
 * One step of the flow: what happens when the liquid leaves the tile [from] through [exit]. The
 * start is checked with `after(board, start.position, start.openSide, emptySet())`.
 */
object FlowStep {
    sealed interface Result {
        /** The liquid enters the section or portal pair at [next] through its [entry] side. */
        data class Enter(val next: Position, val entry: Side) : Result

        /** The liquid enters the finish through its open side. */
        data object Finish : Result

        /** The pipe leaks at the tile [at] the liquid left, through [side]. */
        data class Leak(val at: Position, val side: Side) : Result
    }

    /** Why the tile the liquid moves into does not take it. */
    enum class Gap { OFF_GRID, EMPTY, BLOCKED, NO_OPENING, WRONG_SIDE, USED }

    /** The tile the liquid moves into and what it does with the liquid. */
    sealed interface Next {
        /** The section at [at], entered through [entry], running through [channel] to [exit]. */
        data class Section(val at: Position, val entry: Side, val channel: Channel, val exit: Side) : Next

        /**
         * The portal at [at], entered through its open side [entry], of a pair not passed yet. The
         * liquid leaves the pair from the other portal, [exitPortal], through its open side [exit].
         */
        data class Portal(val at: Position, val entry: Side, val exitPortal: Position, val exit: Side) : Next

        /** The finish at [at], entered through its open side. Whether it wins depends on the mandatory sections. */
        data class Finish(val at: Position) : Next

        /** The tile at [at] does not take the liquid, for [gap]. */
        data class Refused(val at: Position, val gap: Gap) : Next
    }

    /**
     * The one rule for the tile after [from], left through [exit], with the channels in [used]
     * already run through. The flow ([after]) and the route walk both take it, so they cannot
     * disagree about sections, crosses, portals or the finish.
     */
    fun next(board: Board, from: Position, exit: Side, used: Set<ChannelUse>): Next {
        val at = from.neighbour(exit)
        val entry = exit.opposite()
        if (at !in board.grid) return Next.Refused(at, Gap.OFF_GRID)
        return when (val cell = board[at]) {
            Cell.Empty -> Next.Refused(at, Gap.EMPTY)
            Cell.Blocked, is Cell.Start -> Next.Refused(at, Gap.BLOCKED)
            is Cell.Finish -> if (entry == cell.openSide) Next.Finish(at) else Next.Refused(at, Gap.WRONG_SIDE)
            is Cell.Pipe -> {
                val out = cell.section.exitFor(entry)
                val channel = cell.section.channelFor(entry)
                when {
                    out == null -> Next.Refused(at, Gap.NO_OPENING)
                    ChannelUse(at, channel) in used -> Next.Refused(at, Gap.USED)
                    else -> Next.Section(at, entry, channel, out)
                }
            }
            is Cell.Portal -> {
                val exitPortal = board[cell.partner] as? Cell.Portal
                when {
                    entry != cell.openSide || exitPortal == null -> Next.Refused(at, Gap.NO_OPENING)
                    ChannelUse(at, Channel.PORTAL) in used -> Next.Refused(at, Gap.USED)
                    else -> Next.Portal(at, entry, cell.partner, exitPortal.openSide)
                }
            }
        }
    }

    fun after(board: Board, from: Position, exit: Side, usedChannels: Set<ChannelUse>): Result =
        when (val next = next(board, from, exit, usedChannels)) {
            is Next.Section -> Result.Enter(next.at, next.entry)
            is Next.Portal -> Result.Enter(next.at, next.entry)
            is Next.Finish -> Result.Finish
            is Next.Refused -> Result.Leak(from, exit)
        }
}
