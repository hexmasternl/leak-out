package nl.hexmaster.leakout.ui.game

import java.math.BigDecimal
import java.math.RoundingMode
import nl.hexmaster.leakout.core.level.StarBenchmarks

/** The HUD's maths, kept free of Android so the spec scenarios can be unit tested one-to-one. */
object HudModel {
    /** The countdown bar's colour level, by flow time left. */
    enum class Warning { Calm, Brass, Red }

    /** A star threshold on the score bar. */
    enum class Threshold { One, Two, Three }

    const val BRASS_MS = 5_000L
    const val RED_MS = 2_000L

    /**
     * The flow speed as shown: cells per second × 10, halves rounded up. It goes through the decimal
     * string so that 0.85 is 8.5 and rounds to 9, rather than 8.4999… rounding to 8.
     */
    fun speedNumber(cellsPerSecond: Double): Int =
        BigDecimal(cellsPerSecond.toString()).movePointRight(1).setScale(0, RoundingMode.HALF_UP).toInt()

    /** How full the countdown bar is: [remainingMs] over [initialDelayMs], 0..1, and 0 without a delay. */
    fun countdownFraction(remainingMs: Long, initialDelayMs: Long): Float =
        if (initialDelayMs <= 0L) 0f else (remainingMs.toFloat() / initialDelayMs).coerceIn(0f, 1f)

    fun countdownWarning(remainingMs: Long): Warning = when {
        remainingMs <= RED_MS -> Warning.Red
        remainingMs <= BRASS_MS -> Warning.Brass
        else -> Warning.Calm
    }

    /** Whole seconds left, rounded up, for the screen reader. */
    fun countdownSeconds(remainingMs: Long): Int = ((remainingMs.coerceAtLeast(0L) + 999L) / 1_000L).toInt()

    /** How full the score bar is: [score] over `stars.three`, 0..1. */
    fun scoreFraction(score: Int, stars: StarBenchmarks): Float =
        if (stars.three <= 0) 1f else (score.toFloat() / stars.three).coerceIn(0f, 1f)

    /** Where the one-star and two-star marker lines are along the bar. */
    fun markerFractions(stars: StarBenchmarks): List<Float> =
        listOf(position(Threshold.One, stars), position(Threshold.Two, stars))

    /** Where [threshold] is along the bar; three stars is the end. */
    fun position(threshold: Threshold, stars: StarBenchmarks): Float = when (threshold) {
        Threshold.One -> scoreFraction(stars.one, stars)
        Threshold.Two -> scoreFraction(stars.two, stars)
        Threshold.Three -> 1f
    }

    /** The thresholds a fill moving from [fromFraction] to [toFraction] passes, in `(from, to]`, in order. */
    fun thresholdsPassed(fromFraction: Float, toFraction: Float, stars: StarBenchmarks): List<Threshold> =
        Threshold.entries.filter { position(it, stars).let { p -> p > fromFraction && p <= toFraction } }
}
