package nl.hexmaster.leakout.ui.game

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin
import nl.hexmaster.leakout.core.score.FinalScore
import nl.hexmaster.leakout.core.score.Points

/** The timing of the win, in game milliseconds. */
object WinTiming {
    /** The finish flash; the level complete dialog opens after it. */
    const val FINISH_EFFECT_MS = 1_000L

    /** The earned row is shown alone this long before the penalty starts. */
    const val EARNED_HOLD_MS = 500L

    /** One unused section per step; the whole penalty phase is capped at [PENALTY_MAX_MS]. */
    const val PENALTY_STEP_MS = 120L
    const val PENALTY_MAX_MS = 2_000L

    /** The total runs from 0 to its final value in this time. */
    const val TOTAL_COUNT_MS = 800L

    /** Between two earned stars lighting up. */
    const val STAR_STAGGER_MS = 300L

    /** One star's pop: up to [STAR_POP_SCALE] and back to 1 (`motion.pop`). */
    const val STAR_POP_MS = 320L
    const val STAR_POP_SCALE = 1.35f
}

/**
 * What the level complete dialog shows at one moment: the penalty so far and how many unused
 * sections are gone from the field, the running total, the lit stars with their pop scales, and
 * whether the buttons are enabled.
 */
data class WinFrame(
    val sectionsRemoved: Int,
    val penaltyShown: Int,
    val totalShown: Int,
    val starsLit: Int,
    val starScales: List<Float>,
    val buttonsEnabled: Boolean,
)

/**
 * The tally as a pure function of the dialog's age, so it runs on the game clock, freezes when the
 * clock stops and can be skipped. The phases follow each other with no gap: earned points alone,
 * the penalty one section at a time, the total counting up, then the earned stars one by one.
 */
object WinTally {
    /** How long one penalty step takes for [unusedCount] sections: [WinTiming.PENALTY_STEP_MS], shorter when capped. */
    fun penaltyStepMs(unusedCount: Int): Long =
        if (unusedCount <= 0) 0L else minOf(WinTiming.PENALTY_STEP_MS, WinTiming.PENALTY_MAX_MS / unusedCount)

    fun penaltyStartMs(): Long = WinTiming.EARNED_HOLD_MS

    fun totalStartMs(score: FinalScore): Long = penaltyStartMs() + penaltyStepMs(score.unusedCount) * score.unusedCount

    fun starsStartMs(score: FinalScore): Long = totalStartMs(score) + WinTiming.TOTAL_COUNT_MS

    /** The dialog age at which the tally is done and the buttons are enabled. */
    fun totalDurationMs(score: FinalScore, stars: Int): Long {
        val earned = stars.coerceIn(0, STAR_COUNT)
        return if (earned == 0) {
            starsStartMs(score)
        } else {
            starsStartMs(score) + (earned - 1) * WinTiming.STAR_STAGGER_MS + WinTiming.STAR_POP_MS
        }
    }
}

/** The frame of the tally [dialogAgeMs] after the level complete dialog opened, for [score] and its [stars]. */
fun winFrame(dialogAgeMs: Long, score: FinalScore, stars: Int): WinFrame {
    val age = dialogAgeMs.coerceAtLeast(0L)
    val earnedStars = stars.coerceIn(0, STAR_COUNT)

    val step = WinTally.penaltyStepMs(score.unusedCount)
    val removed = if (step == 0L) 0 else ((age - WinTally.penaltyStartMs()) / step).coerceIn(0L, score.unusedCount.toLong()).toInt()

    val totalProgress = ((age - WinTally.totalStartMs(score)).toDouble() / WinTiming.TOTAL_COUNT_MS).coerceIn(0.0, 1.0)
    val eased = 1.0 - (1.0 - totalProgress) * (1.0 - totalProgress)
    val totalShown = if (totalProgress >= 1.0) score.total else floor(score.total * eased).toInt()

    val starsStart = WinTally.starsStartMs(score)
    val scales = List(STAR_COUNT) { index ->
        if (index >= earnedStars) 1f else starPopScale(age - starsStart - index * WinTiming.STAR_STAGGER_MS)
    }
    val lit = (0 until earnedStars).count { age >= starsStart + it * WinTiming.STAR_STAGGER_MS }

    return WinFrame(
        sectionsRemoved = removed,
        penaltyShown = removed * Points.UNUSED_SECTION_PENALTY,
        totalShown = totalShown,
        starsLit = lit,
        starScales = scales,
        buttonsEnabled = age >= WinTally.totalDurationMs(score, earnedStars),
    )
}

/** A star [ageMs] after it lit up: a half sine from 1 up to [WinTiming.STAR_POP_SCALE] and back. */
private fun starPopScale(ageMs: Long): Float {
    if (ageMs < 0 || ageMs >= WinTiming.STAR_POP_MS) return 1f
    val t = ageMs.toFloat() / WinTiming.STAR_POP_MS
    return 1f + (WinTiming.STAR_POP_SCALE - 1f) * sin(PI.toFloat() * t)
}
