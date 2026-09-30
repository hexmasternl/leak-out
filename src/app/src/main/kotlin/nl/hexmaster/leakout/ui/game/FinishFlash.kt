package nl.hexmaster.leakout.ui.game

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** One frame of the success ring on the finish tile: drawn at [ringAlpha], [ringScale] times the tile. */
data class FinishFlashFrame(val ringAlpha: Float, val ringScale: Float)

/** The steady solid ring that keeps the finish marked once the flash is over, behind the dialog. */
val FinishRing = FinishFlashFrame(ringAlpha = 1f, ringScale = 1f)

private const val FADE_IN = 0.15f
private const val PEAK_SCALE = 1.3f
private const val PULSES = 2

/**
 * The flash [ageMs] after the win: the ring fades in fast, pulses twice and swells to [PEAK_SCALE]
 * halfway through [WinTiming.FINISH_EFFECT_MS], then lands on [FinishRing].
 */
fun finishFlashFrame(ageMs: Long): FinishFlashFrame {
    if (ageMs >= WinTiming.FINISH_EFFECT_MS) return FinishRing
    val t = ageMs.coerceAtLeast(0L).toFloat() / WinTiming.FINISH_EFFECT_MS
    val alpha = if (t < FADE_IN) {
        t / FADE_IN
    } else {
        0.75f + 0.25f * cos(2f * PI.toFloat() * PULSES * (t - FADE_IN) / (1f - FADE_IN))
    }
    return FinishFlashFrame(ringAlpha = alpha, ringScale = 1f + (PEAK_SCALE - 1f) * sin(PI.toFloat() * t))
}
