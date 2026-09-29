package nl.hexmaster.leakout.ui.splash

/** Pure splash rules, kept out of Compose so they can be unit tested on the JVM. */
object SplashTiming {
    /** The wordmark stays up at least this long from the moment it first appears. */
    const val MIN_DURATION_MS = 1_200L

    /** Cross-fade into the welcome screen (`motion.pop` duration). */
    const val CROSSFADE_MS = 320

    /**
     * How much longer the splash must stay, given how long it has already been visible. Never
     * negative, and never more than the minimum (a clock that went backwards does not extend it).
     */
    fun remainingDelayMs(elapsedMs: Long): Long = (MIN_DURATION_MS - elapsedMs).coerceIn(0L, MIN_DURATION_MS)
}

/** Wordmark sizing for the splash screen. */
object SplashLayout {
    /** `display.hero`: the wordmark is never smaller than this. */
    const val MIN_SIZE_DP = 56f

    private const val HEIGHT_FRACTION = 0.30f
    private const val WIDTH_FRACTION = 0.22f

    /** Glyph size per line in dp: `min(h * 0.30, w * 0.22)`, at least [MIN_SIZE_DP]. */
    fun wordmarkSizeDp(widthDp: Float, heightDp: Float): Float =
        maxOf(minOf(heightDp * HEIGHT_FRACTION, widthDp * WIDTH_FRACTION), MIN_SIZE_DP)
}
