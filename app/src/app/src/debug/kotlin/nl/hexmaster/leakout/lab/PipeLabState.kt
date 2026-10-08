package nl.hexmaster.leakout.lab

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.ui.pipes.ArtChannel
import nl.hexmaster.leakout.ui.pipes.ChannelFill
import nl.hexmaster.leakout.ui.pipes.LiquidTiming

/**
 * The Pipe Lab's animation state: an entry side and a progress for every channel of every
 * section, a running flag and the two speeds. Plain Compose snapshot state, no Android, so it is
 * unit tested on the JVM.
 *
 * Every section starts filling at once. The cross fills its horizontal channel first and passes
 * the rest of the frame on to its vertical channel, so both channels are seen flowing on their own.
 */
class PipeLabState(channels: Map<PipeSection, List<ArtChannel>>) {
    /** Channels per section in fill order: horizontal before vertical. */
    val channels: Map<PipeSection, List<ArtChannel>> =
        channels.mapValues { (_, list) -> list.sortedBy { if (it.isHorizontal()) 0 else 1 } }

    var running by mutableStateOf(false)
        private set

    private var flowSpeedState by mutableFloatStateOf(DEFAULT_FLOW_SPEED)
    private var reservoirFactorState by mutableFloatStateOf(DEFAULT_RESERVOIR_FACTOR)

    /** Cells per second, kept within the slider's range. Takes effect on the next tick. */
    var flowSpeed: Float
        get() = flowSpeedState
        set(value) {
            flowSpeedState = value.coerceIn(MIN_FLOW_SPEED, MAX_FLOW_SPEED)
        }

    /** The reservoir's speed multiplier, kept within the slider's range. */
    var reservoirSpeedFactor: Float
        get() = reservoirFactorState
        set(value) {
            reservoirFactorState = value.coerceIn(MIN_RESERVOIR_FACTOR, MAX_RESERVOIR_FACTOR)
        }

    private val entries = mutableStateMapOf<Pair<PipeSection, String>, Side>()
    private val progress = mutableStateMapOf<Pair<PipeSection, String>, Float>()

    fun entry(section: PipeSection, channelId: String): Side =
        entries[section to channelId] ?: channel(section, channelId).from

    fun progress(section: PipeSection, channelId: String): Float = progress[section to channelId] ?: 0f

    /** The fills to hand to the tile; channels the liquid has not reached are left out. */
    fun fills(section: PipeSection): Map<String, ChannelFill> =
        channels.getValue(section)
            .filter { progress(section, it.id) > 0f }
            .associate { it.id to ChannelFill(progress(section, it.id), entry(section, it.id)) }

    fun start() {
        running = true
    }

    fun pause() {
        running = false
    }

    fun reset() {
        running = false
        progress.clear()
    }

    /** Sets where the liquid enters [channelId] and empties that section. */
    fun setEntry(section: PipeSection, channelId: String, side: Side) {
        val channel = channel(section, channelId)
        require(side in channel.sides) { "$channelId of $section has no ${side.name.lowercase()} side" }
        entries[section to channelId] = side
        channels.getValue(section).forEach { progress.remove(section to it.id) }
    }

    /** Swaps the entry side of [channelId] to its other end. */
    fun reverse(section: PipeSection, channelId: String) {
        val channel = channel(section, channelId)
        setEntry(section, channelId, if (entry(section, channelId) == channel.from) channel.to else channel.from)
    }

    fun tick(dtMs: Float) {
        require(dtMs >= 0f) { "Time cannot run backwards, dt was $dtMs" }
        if (!running) return
        for ((section, list) in channels) {
            var remaining = dtMs
            for (channel in list) {
                if (remaining <= 0f) break
                val key = section to channel.id
                val current = progress[key] ?: 0f
                if (current >= 1f) continue
                val toFill = LiquidTiming.millisToFill(current, flowSpeed, section, reservoirSpeedFactor)
                if (remaining >= toFill) {
                    progress[key] = 1f
                    remaining -= toFill
                } else {
                    progress[key] = LiquidTiming.advance(current, remaining, flowSpeed, section, reservoirSpeedFactor)
                    remaining = 0f
                }
            }
        }
    }

    private fun channel(section: PipeSection, channelId: String): ArtChannel =
        channels.getValue(section).firstOrNull { it.id == channelId }
            ?: throw IllegalArgumentException("$section has no channel '$channelId'")

    companion object {
        const val MIN_FLOW_SPEED = 0.1f
        const val MAX_FLOW_SPEED = 4f
        const val DEFAULT_FLOW_SPEED = 1f
        const val MIN_RESERVOIR_FACTOR = 0.1f
        const val MAX_RESERVOIR_FACTOR = 1f
        const val DEFAULT_RESERVOIR_FACTOR = 0.3f
    }
}
