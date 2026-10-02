package nl.hexmaster.leakout.ui.game

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
        Channel.SINGLE -> "channel-main"
        Channel.HORIZONTAL -> "channel-h"
        Channel.VERTICAL -> "channel-v"
    }

    fun fills(position: Position, section: PipeSection, used: Set<ChannelUse>, head: FlowState.Flowing?): Map<String, ChannelFill> {
        var fills: Map<String, ChannelFill> = emptyMap()
        for (channel in Channel.entries) {
            if (ChannelUse(position, channel) in used) fills = fills + (channelId(channel) to ChannelFill.full(anyEntry(section, channel)))
        }
        if (head != null && head.position == position) {
            fills = fills + (channelId(head.channel) to ChannelFill(head.progress.toFloat(), head.entry))
        }
        return fills
    }

    /**
     * How far the liquid has run out of the start at [position], 0..1: 0 while the countdown runs,
     * the head's progress while it crosses the start, and 1 once it has left. The start has no pipe
     * art, so without this the liquid stays invisible for a whole cell after the countdown ends.
     */
    fun startFill(position: Position, used: Set<ChannelUse>, head: FlowState.Flowing?): Float = when {
        ChannelUse(position, Channel.SINGLE) in used -> 1f
        head != null && head.position == position -> head.progress.toFloat().coerceIn(0f, 1f)
        else -> 0f
    }

    /** A full channel shows the same from either side; this picks one it can be entered from. */
    private fun anyEntry(section: PipeSection, channel: Channel): Side = when (channel) {
        Channel.HORIZONTAL -> Side.LEFT
        Channel.VERTICAL -> Side.TOP
        Channel.SINGLE -> Side.entries.first { it in section.openSides }
    }
}
