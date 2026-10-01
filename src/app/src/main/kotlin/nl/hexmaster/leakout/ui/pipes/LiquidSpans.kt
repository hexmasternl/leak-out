package nl.hexmaster.leakout.ui.pipes

import nl.hexmaster.leakout.core.level.Side

/**
 * The liquid in one channel: how far it got ([progress], 0–1, clamped) and the side it came in
 * through. Liquid can enter a channel from either of its two sides.
 */
data class ChannelFill(val progress: Float, val entry: Side) {
    val clamped: Float get() = progress.coerceIn(0f, 1f)

    companion object {
        fun full(entry: Side) = ChannelFill(1f, entry)
    }
}

/**
 * A part of a channel path, as fractions of its length measured the way the path is written
 * (from the channel's `data-from` side). [head] is the leading edge of the liquid, or `null` when
 * there is no moving edge to show (empty or full).
 */
data class PathSpan(val start: Float, val end: Float, val head: Float? = null) {
    val isEmpty: Boolean get() = end <= start

    companion object {
        val EMPTY = PathSpan(0f, 0f)
    }
}

/** What is filled in a reservoir: the inlet stub, the tank (0–1 of its height) and the outlet stub. */
data class ReservoirFill(val inlet: PathSpan, val tankLevel: Float, val outlet: PathSpan)

/**
 * Maps a [ChannelFill] onto the channel path. Pure, so every direction and phase is unit tested;
 * the renderer only cuts the returned span out of the measured path.
 */
object LiquidSpans {
    /** Reservoir phases (design system 7.3): inlet stub, tank, outlet stub. */
    const val RESERVOIR_INLET_END = 0.1f
    const val RESERVOIR_OUTLET_START = 0.9f

    fun forChannel(channel: ArtChannel, fill: ChannelFill): PathSpan {
        val p = fill.clamped
        return when (fill.entry) {
            channel.from -> PathSpan(0f, p, head = p.takeIf { it > 0f && it < 1f })
            channel.to -> PathSpan(1f - p, 1f, head = (1f - p).takeIf { p > 0f && p < 1f })
            else -> throw IllegalArgumentException(
                "Liquid cannot enter channel '${channel.id}' (${channel.from}–${channel.to}) from ${fill.entry}",
            )
        }
    }

    /**
     * The reservoir channel must be the straight centre line from the left edge to the right edge
     * (or the other way round), so a path fraction is `x / 100` measured from its `data-from` side.
     */
    fun forReservoir(channel: ArtChannel, tank: TankCircle, fill: ChannelFill): ReservoirFill {
        require(channel.isHorizontal()) { "The reservoir channel must run left–right" }
        forChannel(channel, fill) // validates the entry side
        val size = PipeArt.VIEW_BOX_SIZE
        // Tank edges as path fractions from the entry side.
        val leftEdge = (tank.cx - tank.r) / size
        val rightEdge = (tank.cx + tank.r) / size
        val tankNear = if (fill.entry == Side.LEFT) leftEdge else 1f - rightEdge
        val tankFar = if (fill.entry == Side.LEFT) rightEdge else 1f - leftEdge

        val p = fill.clamped
        val inletPart = (p / RESERVOIR_INLET_END).coerceIn(0f, 1f)
        val tankLevel = ((p - RESERVOIR_INLET_END) / (RESERVOIR_OUTLET_START - RESERVOIR_INLET_END)).coerceIn(0f, 1f)
        val outletPart = ((p - RESERVOIR_OUTLET_START) / (1f - RESERVOIR_OUTLET_START)).coerceIn(0f, 1f)

        // Spans in entry-relative fractions, turned into path fractions at the end.
        val inletEnd = tankNear * inletPart
        val outletEnd = tankFar + (1f - tankFar) * outletPart
        val inlet = entrySpan(channel, fill.entry, 0f, inletEnd, head = inletPart > 0f && inletPart < 1f)
        val outlet = if (outletPart > 0f) {
            entrySpan(channel, fill.entry, tankFar, outletEnd, head = outletPart < 1f)
        } else {
            PathSpan.EMPTY
        }
        return ReservoirFill(inlet, tankLevel, outlet)
    }

    /** A span given as fractions from the entry side, converted to fractions of the written path. */
    private fun entrySpan(channel: ArtChannel, entry: Side, from: Float, to: Float, head: Boolean): PathSpan =
        if (entry == channel.from) {
            PathSpan(from, to, head = to.takeIf { head })
        } else {
            PathSpan(1f - to, 1f - from, head = (1f - to).takeIf { head })
        }
}

/**
 * A span drawn with a rounded head: [body] is the part drawn as a band and [capCenter] the centre
 * of the round cap, both as fractions of the path. The cap sits behind the leading edge, so its
 * front is exactly on the head; near a border its centre may lie before 0 or past 1, and the
 * renderer extends the path there along its end tangent. [capCenter] is `null` without a head.
 */
data class HeadedSpan(val body: PathSpan, val capCenter: Float?)

/**
 * Where the band stops and the cap goes for [span] with a cap of radius [capFraction] (a fraction
 * of the path length). The liquid front then moves at exactly the speed of the head and leaves a
 * channel at the same moment the next one starts, so the flow runs on across sections.
 */
fun LiquidSpans.withRoundHead(span: PathSpan, capFraction: Float): HeadedSpan {
    val head = span.head ?: return HeadedSpan(span, null)
    require(capFraction >= 0f) { "The cap cannot be negative, was $capFraction" }
    // The liquid runs towards the end of the path when its head is the end of the span.
    val forwards = kotlin.math.abs(head - span.end) <= kotlin.math.abs(head - span.start)
    return if (forwards) {
        val center = head - capFraction
        HeadedSpan(PathSpan(span.start, center.coerceIn(span.start, span.end)), center)
    } else {
        val center = head + capFraction
        HeadedSpan(PathSpan(center.coerceIn(span.start, span.end), span.end), center)
    }
}
