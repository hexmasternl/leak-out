package nl.hexmaster.leakout.ui.game

/**
 * One frame of the leak spurt, in tile fractions: the droplets fan out over [length] from the
 * middle of the leaking side, [spread] wide, drawn at [alpha].
 */
data class LeakFrame(val length: Float, val spread: Float, val alpha: Float)

/** The small puddle that stays on the leaking side once the effect is over, behind the dialog. */
val LeakPuddle = LeakFrame(length = 0.22f, spread = 0.5f, alpha = 0.75f)

private const val MAX_LENGTH = 0.7f
private const val MIN_SPREAD = 0.15f
private const val MAX_SPREAD = 0.6f

/**
 * The spurt [ageMs] after the leak: it shoots out with an ease-out and widens during
 * [LeakTiming.EFFECT_MS], then settles into [LeakPuddle].
 */
fun leakFrame(ageMs: Long): LeakFrame {
    if (ageMs >= LeakTiming.EFFECT_MS) return LeakPuddle
    val t = ageMs.coerceAtLeast(0L).toFloat() / LeakTiming.EFFECT_MS
    val eased = 1f - (1f - t) * (1f - t)
    return LeakFrame(
        length = MAX_LENGTH * eased,
        spread = MIN_SPREAD + (MAX_SPREAD - MIN_SPREAD) * eased,
        alpha = 1f - 0.25f * t,
    )
}
