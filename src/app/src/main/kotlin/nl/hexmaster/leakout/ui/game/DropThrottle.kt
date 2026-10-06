package nl.hexmaster.leakout.ui.game

/**
 * Lets the points awarded sound play at most once per [DROP_MIN_INTERVAL_MS] of [nowMs]. The clock
 * is real time, not game time: in fast forward the game clock runs 10× and awards would pile up
 * into a buzz.
 */
class DropThrottle(private val nowMs: () -> Long) {
    private var lastDropMs: Long? = null

    /** Whether a drop may play now. A drop that may play is counted as played. */
    fun tryDrop(): Boolean {
        val now = nowMs()
        val last = lastDropMs
        if (last != null && now - last < DROP_MIN_INTERVAL_MS) return false
        lastDropMs = now
        return true
    }

    companion object {
        const val DROP_MIN_INTERVAL_MS = 100L
    }
}
