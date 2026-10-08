package nl.hexmaster.leakout.core.engine

import nl.hexmaster.leakout.core.flow.Channel
import nl.hexmaster.leakout.core.flow.ChannelUse
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side

/** Where the liquid is. [used] are the channels it has run through, which a later liquid drawing fills. */
sealed interface FlowState {
    val used: Set<ChannelUse>

    /** The countdown before the liquid starts, in flow milliseconds. */
    data class Waiting(val remainingMs: Long) : FlowState {
        override val used: Set<ChannelUse> get() = emptySet()
    }

    /**
     * The liquid head is in the tile [position], entered through [entry] and running through
     * [channel], [progress] of the way from the entry side (0) to the exit side (1).
     */
    data class Flowing(
        val position: Position,
        val entry: Side,
        val channel: Channel,
        val progress: Double,
        override val used: Set<ChannelUse>,
    ) : FlowState

    /** The liquid entered the finish or leaked; it does not move again. */
    data class Ended(override val used: Set<ChannelUse>) : FlowState
}
