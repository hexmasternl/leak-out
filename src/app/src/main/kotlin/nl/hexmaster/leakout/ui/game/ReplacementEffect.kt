package nl.hexmaster.leakout.ui.game

import kotlin.random.Random
import nl.hexmaster.leakout.core.engine.Replacement
import nl.hexmaster.leakout.core.level.Position

object ReplacementTiming {
    /** The longest the old section takes to break apart. */
    const val MAX_BREAK_MS = 600L

    /** The share of a short replacement the break may take at most. */
    const val MAX_BREAK_SHARE = 0.4f

    /** The share of the break during which the section shakes and cracks before it falls apart. */
    const val CRACK_SHARE = 0.3f

    /** How long the landed section pops in. */
    const val LANDING_POP_MS = 180L

    /** How long the break of a [durationMs] long replacement takes. */
    fun breakMs(durationMs: Long): Long = minOf(MAX_BREAK_MS, (durationMs * MAX_BREAK_SHARE).toLong())
}

/**
 * One frame of a replacement. While the old section breaks, [breakProgress] runs from 0 to 1 and
 * the section is drawn cracking and then flying apart; it is null once the section is gone. From
 * then on the tile shows the busy ring, filled to [progress], the share of the whole replacement.
 */
data class ReplacementFrame(val breakProgress: Float?, val progress: Float) {
    val showsBusyRing: Boolean get() = breakProgress == null

    /** How far the section has cracked, 0 to 1, before it falls apart; 1 once it is falling apart. */
    val crack: Float get() = breakProgress?.let { (it / ReplacementTiming.CRACK_SHARE).coerceAtMost(1f) } ?: 1f

    /** How far the fragments have flown, eased out, 0 while cracking and 1 at the end of the break. */
    val scatter: Float
        get() {
            val t = breakProgress ?: return 1f
            if (t <= ReplacementTiming.CRACK_SHARE) return 0f
            val s = (t - ReplacementTiming.CRACK_SHARE) / (1f - ReplacementTiming.CRACK_SHARE)
            return 1f - (1f - s) * (1f - s)
        }
}

/** The frame of [replacement] at game time [nowMs]. */
fun replacementFrame(replacement: Replacement, nowMs: Long): ReplacementFrame {
    val ageMs = (nowMs - replacement.startedAtMs).coerceAtLeast(0L)
    val breakMs = ReplacementTiming.breakMs(replacement.durationMs)
    val breakProgress = if (breakMs > 0 && ageMs < breakMs) ageMs.toFloat() / breakMs else null
    return ReplacementFrame(breakProgress, replacement.progressAt(nowMs))
}

/**
 * The scale of a section [ageMs] after it landed: it grows from 60 % past full size and settles
 * back, over [ReplacementTiming.LANDING_POP_MS]. Null outside the pop, when it is drawn as usual.
 */
fun landingPopScale(ageMs: Long): Float? {
    if (ageMs < 0 || ageMs >= ReplacementTiming.LANDING_POP_MS) return null
    val t = ageMs.toFloat() / ReplacementTiming.LANDING_POP_MS
    return if (t < 0.6f) 0.6f + 0.48f * (t / 0.6f) else 1.08f - 0.08f * ((t - 0.6f) / 0.4f)
}

/**
 * One piece of a broken section: the quarter of the tile at ([column], [row]) flies off along
 * ([dx], [dy]) in tile fractions and turns [spinDegrees], while falling and fading.
 */
data class Fragment(val column: Int, val row: Int, val dx: Float, val dy: Float, val spinDegrees: Float)

/**
 * The four quarters of the section on [at], each thrown outward from the tile centre with a small
 * jitter. Seeded by the tile, so a tile always breaks the same way, frame after frame.
 */
fun fragmentsFor(at: Position): List<Fragment> {
    val random = Random(at.x * 31 + at.y * 17 + 7)
    return listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1).map { (column, row) ->
        val outX = if (column == 0) -1f else 1f
        val outY = if (row == 0) -1f else 1f
        Fragment(
            column = column,
            row = row,
            dx = outX * (0.25f + random.nextFloat() * 0.15f),
            dy = outY * (0.15f + random.nextFloat() * 0.15f),
            spinDegrees = outX * (25f + random.nextFloat() * 35f),
        )
    }
}

/** Where a replacement section landed, and when: drives the landing pop. */
data class Landing(val attempt: Int, val at: Position, val atMs: Long)
