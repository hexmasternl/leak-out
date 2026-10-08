package nl.hexmaster.leakout.ui.game

/**
 * Decides on which display frames the game clock ticks (spec `game-play-frame-budget`): only once
 * at least [minTickMs] whole milliseconds have built up since the last tick, so a fast panel ticks
 * at most about 60 times a second. Time moves on in whole milliseconds only and the rest is kept,
 * so the deltas add up to exactly the time since the first frame: nothing is lost or gained.
 *
 * One pacer serves one run of the clock; a pause or an idle clock starts a new one, so the time in
 * between never reaches the game.
 */
class TickPacer(private val minTickMs: Long = MIN_TICK_MS) {
    private var lastNanos = -1L

    /** The delta to tick for the frame at [frameNanos], or null to skip it; 0 on the first frame. */
    fun onFrame(frameNanos: Long): Long? {
        if (lastNanos < 0) {
            lastNanos = frameNanos
            return 0L
        }
        val dtMs = (frameNanos - lastNanos) / 1_000_000
        if (dtMs < minTickMs) return null
        lastNanos += dtMs * 1_000_000
        return dtMs
    }

    companion object {
        /**
         * Below the 16 ms of a 60 Hz frame (and of the Compose test clock), so those tick on every
         * frame, while 120 Hz frames of 8.3 ms tick on every second one.
         */
        const val MIN_TICK_MS = 12L
    }
}
