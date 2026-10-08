package nl.hexmaster.leakout.ui.game

import nl.hexmaster.leakout.core.board.Board
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.engine.FlowState
import nl.hexmaster.leakout.core.flow.Channel
import nl.hexmaster.leakout.core.flow.ChannelUse
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.ui.pipes.ChannelFill

/**
 * Turns the flow state into the liquid of one placed section, by art channel id: every channel the
 * liquid has run through is full, and the channel the head is in is filled up to its progress from
 * its entry side. Progress comes straight from the engine, which moves it linearly in time, and a
 * channel is full exactly when the next one starts at 0, so the liquid runs on without a pause.
 */
object FieldLiquid {
    fun channelId(channel: Channel): String = when (channel) {
        // A portal's stub is the straight's art, whose one channel is the main one.
        Channel.SINGLE, Channel.PORTAL -> "channel-main"
        Channel.HORIZONTAL -> "channel-h"
        Channel.VERTICAL -> "channel-v"
    }

    fun fills(position: Position, section: PipeSection, used: Set<ChannelUse>, head: FlowState.Flowing?): Map<String, ChannelFill> =
        withHead(settledFills(position, section, used), position, head)

    /** The channels of the section on [position] the liquid has run through, all full; they only change with [used]. */
    fun settledFills(position: Position, section: PipeSection, used: Set<ChannelUse>): Map<String, ChannelFill> {
        var fills: Map<String, ChannelFill> = emptyMap()
        for (channel in Channel.entries) {
            if (ChannelUse(position, channel) in used) fills = fills + (channelId(channel) to ChannelFill.full(anyEntry(section, channel)))
        }
        return fills
    }

    /** [settled] with the channel the [head] is in filled up to its progress, when the head is on [position]. */
    fun withHead(settled: Map<String, ChannelFill>, position: Position, head: FlowState.Flowing?): Map<String, ChannelFill> =
        if (head != null && head.position == position) settled + (channelId(head.channel) to ChannelFill(head.progress.toFloat(), head.entry)) else settled

    /**
     * How far the liquid has come out of the start valve at [start], 0 to 1: dry before the flow,
     * the head's progress while it is in the start, and full once it has left (the start's channel
     * is then in [used]).
     */
    fun startProgress(start: Position, used: Set<ChannelUse>, head: FlowState.Flowing?): Float = when {
        ChannelUse(start, Channel.SINGLE) in used -> 1f
        head != null && head.position == start -> head.progress.toFloat().coerceIn(0f, 1f)
        else -> 0f
    }

    /**
     * The liquid of the start's stub, drawn on the straight's channel clipped to the half between
     * the centre and [openSide]. The liquid enters at the centre, so the whole channel is filled from
     * the side opposite the open side to half way plus half the start progress; the clip hides the
     * first half.
     */
    fun startFills(start: Position, openSide: Side, used: Set<ChannelUse>, head: FlowState.Flowing?): Map<String, ChannelFill> {
        val progress = startProgress(start, used, head)
        if (progress <= 0f) return emptyMap()
        return mapOf(channelId(Channel.SINGLE) to ChannelFill(0.5f + progress / 2f, openSide.opposite()))
    }

    /**
     * The liquid of a portal's stub, drawn like the start's on the straight's channel clipped to the
     * half towards the open side, [PortalStyle.stubProgress] full. In the entry portal the liquid
     * runs in from the open side towards the ring; in the exit portal it runs out from the ring.
     */
    fun portalFills(position: Position, portal: Cell.Portal, used: Set<ChannelUse>, head: FlowState.Flowing?): Map<String, ChannelFill> {
        val progress = PortalStyle.stubProgress(position, portal, used, head)
        if (progress <= 0f) return emptyMap()
        val entering = head != null && head.channel == Channel.PORTAL && head.position == position && progress < 1f
        val fill = if (entering) ChannelFill(progress / 2f, portal.openSide) else ChannelFill(0.5f + progress / 2f, portal.openSide.opposite())
        return mapOf(channelId(Channel.PORTAL) to fill)
    }

    /** A full channel shows the same from either side; this picks one it can be entered from. */
    private fun anyEntry(section: PipeSection, channel: Channel): Side = when (channel) {
        Channel.HORIZONTAL -> Side.LEFT
        Channel.VERTICAL -> Side.TOP
        Channel.SINGLE, Channel.PORTAL -> Side.entries.first { it in section.openSides }
    }
}

/**
 * The [FieldLiquid.settledFills] of the field's tiles, worked out once per board and set of used
 * channels instead of every frame. The engine keeps the same instances while the head moves inside
 * a tile, so they are compared by identity; a new board or used set starts the cache afresh.
 */
class SettledFillCache {
    private var board: Board? = null
    private var used: Set<ChannelUse>? = null
    private val fills = HashMap<Position, Map<String, ChannelFill>>()

    fun fills(board: Board, used: Set<ChannelUse>, position: Position, section: PipeSection): Map<String, ChannelFill> {
        if (board !== this.board || used !== this.used) {
            fills.clear()
            this.board = board
            this.used = used
        }
        return fills.getOrPut(position) { FieldLiquid.settledFills(position, section, used) }
    }
}
