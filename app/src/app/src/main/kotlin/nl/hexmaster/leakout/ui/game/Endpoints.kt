package nl.hexmaster.leakout.ui.game

import kotlin.math.PI
import kotlin.math.cos
import nl.hexmaster.leakout.core.level.Side

/** A point in cell units relative to the cell centre, with y growing downwards. */
data class CellPoint(val x: Float, val y: Float)

/** A chevron on the start's stub: two legs meeting at [tip], pointing out of the open side. */
data class Chevron(val legA: CellPoint, val tip: CellPoint, val legB: CellPoint)

/** One square of the finish checker, in cell units relative to the cell centre. */
data class CheckerSquare(val left: Float, val top: Float, val size: Float, val light: Boolean)

/**
 * The start valve and the checkered finish of `docs/design-system.md` 6.4, in cell units relative
 * to the cell centre. Pure geometry, so the drawing code only scales it to the cell.
 */
object EndpointGeometry {
    const val WHEEL_RADIUS = 0.24f
    const val RIM_WIDTH = 0.03f
    const val SPOKE_REACH = 0.20f
    const val SPOKE_WIDTH = 0.045f
    const val HUB_RADIUS = 0.08f
    const val HUB_EDGE = 0.02f
    val HIGHLIGHT_CENTER = CellPoint(-0.08f, -0.10f)
    const val HIGHLIGHT_RADIUS = 0.05f
    const val HIGHLIGHT_ALPHA = 0.45f

    const val CHEVRON_WIDTH = 0.04f
    const val CHEVRON_TIP = 0.32f
    const val CHEVRON_HALF_SPAN = 0.08f
    const val CHEVRON_DEPTH = 0.08f

    const val RING_RADIUS = 0.24f
    const val RING_WIDTH = 0.05f
    const val RING_INNER_RADIUS = RING_RADIUS - RING_WIDTH / 2f
    const val CHECKER_COUNT = 6

    /** The glow reaches this far past the wheel or ring, in dp. */
    const val GLOW_REACH_DP = 12f

    /** The chevron on the stub, its tip [CHEVRON_TIP] from the centre towards [openSide]. */
    fun chevron(openSide: Side): Chevron {
        val out = CellPoint(openSide.dx.toFloat(), openSide.dy.toFloat())
        val across = CellPoint(-out.y, out.x)
        val back = CHEVRON_TIP - CHEVRON_DEPTH
        return Chevron(
            legA = CellPoint(out.x * back + across.x * CHEVRON_HALF_SPAN, out.y * back + across.y * CHEVRON_HALF_SPAN),
            tip = CellPoint(out.x * CHEVRON_TIP, out.y * CHEVRON_TIP),
            legB = CellPoint(out.x * back - across.x * CHEVRON_HALF_SPAN, out.y * back - across.y * CHEVRON_HALF_SPAN),
        )
    }

    /**
     * The checker filling the ring's inner disc: [CHECKER_COUNT] × [CHECKER_COUNT] squares spanning
     * the inner diameter, row by row from the top left, which is light. Drawn clipped to the disc.
     */
    val checker: List<CheckerSquare> = run {
        val side = 2f * RING_INNER_RADIUS / CHECKER_COUNT
        List(CHECKER_COUNT * CHECKER_COUNT) { i ->
            val row = i / CHECKER_COUNT
            val column = i % CHECKER_COUNT
            CheckerSquare(-RING_INNER_RADIUS + column * side, -RING_INNER_RADIUS + row * side, side, (row + column) % 2 == 0)
        }
    }
}

/**
 * The glows behind the start valve and the finish ring (design system 7.2), as an alpha of their
 * colour, from the game clock so they freeze on pause.
 */
object EndpointGlow {
    /** `motion.drift`: the valve pulse while the countdown runs. */
    const val VALVE_PERIOD_MS = 1_600L

    /** `motion.pulse`: the finish ring pulse until the win. */
    const val FINISH_PERIOD_MS = 2_000L

    /** The steady glow once a pulse has stopped, and always under reduced motion. */
    const val STEADY = 0.6f

    private const val LOW = 0.45f
    private const val SWING = 0.45f

    /** Pulses on [VALVE_PERIOD_MS] while waiting for the liquid; steady once it [flowing] or with [reducedMotion]. */
    fun valve(nowMs: Long, flowing: Boolean, reducedMotion: Boolean): Float =
        if (flowing || reducedMotion) STEADY else pulse(nowMs, VALVE_PERIOD_MS)

    /**
     * Pulses on [FINISH_PERIOD_MS] while playing; steady once [lost] (so the idle clock never freezes
     * it mid-pulse) and with [reducedMotion]; none once [won], when the finish flash takes over.
     */
    fun finish(nowMs: Long, won: Boolean, lost: Boolean, reducedMotion: Boolean): Float = when {
        won -> 0f
        lost || reducedMotion -> STEADY
        else -> pulse(nowMs, FINISH_PERIOD_MS)
    }

    /** Eases between [LOW] and [LOW] + [SWING]: low at the start of each period, high half way. */
    private fun pulse(nowMs: Long, periodMs: Long): Float {
        val t = (nowMs % periodMs).toDouble() / periodMs
        return LOW + SWING * ((1.0 - cos(2.0 * PI * t)) / 2.0).toFloat()
    }
}
