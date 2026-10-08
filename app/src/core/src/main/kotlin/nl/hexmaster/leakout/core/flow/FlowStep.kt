package nl.hexmaster.leakout.core.flow

import nl.hexmaster.leakout.core.board.Board
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.core.level.exitFor

/**
 * A channel of a section that liquid runs through. A cross has two, one per axis, so entering it
 * from the left and later from the right is the same channel. Every other section has one.
 */
enum class Channel { HORIZONTAL, VERTICAL, SINGLE }

/** The channel at [position] the liquid has already run through. */
data class ChannelUse(val position: Position, val channel: Channel)

/** The channel [this] section uses for liquid entering through [entry]. */
fun PipeSection.channelFor(entry: Side): Channel = when {
    this != PipeSection.X -> Channel.SINGLE
    entry == Side.LEFT || entry == Side.RIGHT -> Channel.HORIZONTAL
    else -> Channel.VERTICAL
}

/**
 * One step of the flow: what happens when the liquid leaves the tile [from] through [exit]. The
 * start is checked with `after(board, start.position, start.openSide, emptySet())`.
 */
object FlowStep {
    sealed interface Result {
        /** The liquid enters the section at [next] through its [entry] side. */
        data class Enter(val next: Position, val entry: Side) : Result

        /** The liquid enters the finish through its open side. */
        data object Finish : Result

        /** The pipe leaks at the tile [at] the liquid left, through [side]. */
        data class Leak(val at: Position, val side: Side) : Result
    }

    fun after(board: Board, from: Position, exit: Side, usedChannels: Set<ChannelUse>): Result {
        val next = from.neighbour(exit)
        val entry = exit.opposite()
        val leak = Result.Leak(from, exit)
        if (next !in board.grid) return leak
        return when (val cell = board[next]) {
            Cell.Empty, Cell.Blocked, is Cell.Start -> leak
            is Cell.Finish -> if (entry == cell.openSide) Result.Finish else leak
            is Cell.Pipe -> when {
                cell.section.exitFor(entry) == null -> leak
                ChannelUse(next, cell.section.channelFor(entry)) in usedChannels -> leak
                else -> Result.Enter(next, entry)
            }
        }
    }
}
