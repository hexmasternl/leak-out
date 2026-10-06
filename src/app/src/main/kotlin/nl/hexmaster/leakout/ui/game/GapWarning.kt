package nl.hexmaster.leakout.ui.game

import kotlin.math.ceil

/**
 * How the HUD and the heartbeat warn of the gap (spec `game-play-gap-warning`), as pure functions of
 * the gap distance in sections (`GapDistance.of`, null without a gap). The distance only shrinks
 * while the liquid flows and only grows on a placement, so nothing flickers around a threshold.
 */
object GapWarning {
    /** From this distance on, the score bar turns red and the heartbeat plays. */
    const val COLOUR_FROM = 5.0

    /** From this distance on, the score bar is fully red and blinks. */
    const val BLINK_AT = 3.0

    /** How long the score bar shows the score or the word "Warning" before switching. */
    const val BLINK_PHASE_MS = 500L

    /** The heartbeat's volume factor at [COLOUR_FROM]; it grows linearly to 1 at distance 0. */
    const val HEARTBEAT_QUIETEST = 0.15f

    /** How far the score bar's fill is from brass (0) to `leak.500` (1). */
    fun redness(distance: Double?): Float = when {
        distance == null || distance > COLOUR_FROM -> 0f
        distance <= BLINK_AT -> 1f
        else -> ((COLOUR_FROM - distance) / (COLOUR_FROM - BLINK_AT)).toFloat()
    }

    fun blinking(distance: Double?): Boolean = distance != null && distance <= BLINK_AT

    /**
     * Whether the blinking bar shows "Warning" at game time [nowMs], with blinking since [sinceMs].
     * The first phase shows "Warning"; null [sinceMs] means blinking starts now.
     */
    fun showWarningText(distance: Double?, nowMs: Long, sinceMs: Long?): Boolean {
        if (!blinking(distance)) return false
        val elapsed = (nowMs - (sinceMs ?: nowMs)).coerceAtLeast(0L)
        return (elapsed / BLINK_PHASE_MS) % 2 == 0L
    }

    /** The heartbeat's volume factor, before the sound volume: null when it should not play. */
    fun heartbeatFactor(distance: Double?): Float? {
        if (distance == null || distance > COLOUR_FROM) return null
        val closeness = (COLOUR_FROM - distance.coerceAtLeast(0.0)) / COLOUR_FROM
        return (HEARTBEAT_QUIETEST + (1f - HEARTBEAT_QUIETEST) * closeness).toFloat()
    }

    /** The gap distance in whole sections, rounded up, for the screen reader. */
    fun sectionsToLeak(distance: Double): Int = ceil(distance).toInt().coerceAtLeast(1)
}
