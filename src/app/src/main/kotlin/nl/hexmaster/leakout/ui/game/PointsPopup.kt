package nl.hexmaster.leakout.ui.game

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import nl.hexmaster.leakout.core.engine.Award

/**
 * How long a points popup (one [Award]) fades in, holds and fades out, in game milliseconds. The
 * popup lasts exactly as long as the engine keeps the award: [TOTAL_MS] is [Award.VISIBLE_MS].
 */
object PopupTiming {
    const val FADE_IN_MS = 150L
    const val HOLD_MS = 500L
    const val FADE_OUT_MS = 350L
    const val TOTAL_MS = Award.VISIBLE_MS

    /** The largest rise, in tiles, reached at the end of the popup. */
    const val MAX_RISE = 0.25f

    /** The size at the start of the fade-in, relative to the full size. */
    const val START_SCALE = 0.8f
}

/** How a popup looks at one moment: [alpha] 0..1, [scale] 0.8..1 and [rise] in tiles, 0..0.25. */
data class PopupFrame(val alpha: Float, val scale: Float, val rise: Float)

/**
 * The look of a popup that has been shown for [ageMs] of game time, or null once it is over (at or
 * after [PopupTiming.TOTAL_MS]). A negative age is treated as 0. Pure, so pause and resume come
 * for free: the age is taken from the game clock, which stops while paused.
 */
fun popupFrame(ageMs: Long): PopupFrame? {
    if (ageMs >= PopupTiming.TOTAL_MS) return null
    val age = ageMs.coerceAtLeast(0L)
    val rise = PopupTiming.MAX_RISE * age / PopupTiming.TOTAL_MS
    val fadeOutAt = PopupTiming.FADE_IN_MS + PopupTiming.HOLD_MS
    return when {
        age < PopupTiming.FADE_IN_MS -> {
            val t = FastOutSlowInEasing.transform(age.toFloat() / PopupTiming.FADE_IN_MS)
            PopupFrame(alpha = t, scale = PopupTiming.START_SCALE + (1f - PopupTiming.START_SCALE) * t, rise = rise)
        }
        age < fadeOutAt -> PopupFrame(alpha = 1f, scale = 1f, rise = rise)
        else -> {
            val t = FastOutLinearInEasing.transform((age - fadeOutAt).toFloat() / PopupTiming.FADE_OUT_MS)
            PopupFrame(alpha = (1f - t).coerceIn(0f, 1f), scale = 1f, rise = rise)
        }
    }
}

/** The text of a points popup. */
object PointsFormat {
    fun text(points: Int): String = "+$points"
}
