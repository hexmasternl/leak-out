package nl.hexmaster.leakout.ui.game

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Timing and size of the score bar's threshold sparkle (design system 7.2). Sizes are in dp. */
object HudSparkle {
    const val DURATION_MS = 600L
    const val FLASH_MS = 300L
    const val PARTICLES = 8
    const val FLASH_RADIUS_DP = 16f
    const val PARTICLE_MIN_DP = 6f
    const val PARTICLE_MAX_DP = 14f
    const val PARTICLE_RADIUS_DP = 1.5f
}

/** One sparkle particle, as an offset in dp from the sparkle's centre. [glow] picks `liquid.glow` over `brass.300`. */
data class SparkleParticle(val dx: Float, val dy: Float, val alpha: Float, val glow: Boolean)

/** One frame of a threshold sparkle: the glow flash behind it and the particles flying out. */
data class SparkleFrame(val flashAlpha: Float, val particles: List<SparkleParticle>)

/**
 * The sparkle [ageMs] after it started, or null once it is over. The [HudSparkle.PARTICLES] particles
 * fly out evenly spaced, alternating between the two particle lengths and colours, with an ease-out
 * and fading as they go. The flash fades out over the first [HudSparkle.FLASH_MS].
 */
fun sparkleFrame(ageMs: Long): SparkleFrame? {
    if (ageMs < 0L || ageMs >= HudSparkle.DURATION_MS) return null
    val t = ageMs.toFloat() / HudSparkle.DURATION_MS
    val eased = 1f - (1f - t) * (1f - t)
    val flash = (1f - ageMs.toFloat() / HudSparkle.FLASH_MS).coerceAtLeast(0f)
    val particles = List(HudSparkle.PARTICLES) { i ->
        val angle = 2.0 * PI * (i + 0.5) / HudSparkle.PARTICLES
        val reach = if (i % 2 == 0) HudSparkle.PARTICLE_MAX_DP else HudSparkle.PARTICLE_MIN_DP
        val distance = reach * eased
        SparkleParticle(
            dx = (cos(angle) * distance).toFloat(),
            dy = (sin(angle) * distance).toFloat(),
            alpha = 1f - t,
            glow = i % 2 == 0,
        )
    }
    return SparkleFrame(flashAlpha = flash, particles = particles)
}
