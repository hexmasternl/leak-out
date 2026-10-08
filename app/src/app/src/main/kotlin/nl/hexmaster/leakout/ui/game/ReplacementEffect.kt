package nl.hexmaster.leakout.ui.game

import kotlin.random.Random
import nl.hexmaster.leakout.core.engine.Replacement
import nl.hexmaster.leakout.core.level.Position

object ReplacementTiming {
    /** How long the old section takes to break apart, in a replacement of [SEQUENCE_MS]. */
    const val BREAK_MS = 600L

    /** How long the broken pieces then lie still. */
    const val HOLD_MS = 400L

    /** How long the pieces then take to fade out, after which the new section lands. */
    const val FADE_MS = 500L

    /** The whole sequence, and so the default replacement penalty. */
    const val SEQUENCE_MS = BREAK_MS + HOLD_MS + FADE_MS

    /** The share of the break during which the section shakes and cracks before it falls apart. */
    const val CRACK_SHARE = 0.3f

    /** How long the landed section pops in. */
    const val LANDING_POP_MS = 180L
}

/**
 * One frame of a replacement. [breakProgress] runs from 0 to 1 while the old section cracks and
 * flies apart and stays 1 while the pieces lie still; [fadeProgress] then runs from 0 to 1 while
 * they fade out. A replacement of another length than [ReplacementTiming.SEQUENCE_MS] scales the
 * three phases along.
 */
data class ReplacementFrame(val breakProgress: Float, val fadeProgress: Float) {
    /** How visible the pieces are: fully until the fade, gone at its end. */
    val alpha: Float get() = 1f - fadeProgress

    /** How far the section has cracked, 0 to 1, before it falls apart; 1 once it is falling apart. */
    val crack: Float get() = (breakProgress / ReplacementTiming.CRACK_SHARE).coerceAtMost(1f)

    /** How far the fragments have flown, eased out, 0 while cracking and 1 from the end of the break. */
    val scatter: Float
        get() {
            val t = breakProgress
            if (t <= ReplacementTiming.CRACK_SHARE) return 0f
            val s = (t - ReplacementTiming.CRACK_SHARE) / (1f - ReplacementTiming.CRACK_SHARE)
            return 1f - (1f - s) * (1f - s)
        }
}

/** The frame of [replacement] at game time [nowMs]. */
fun replacementFrame(replacement: Replacement, nowMs: Long): ReplacementFrame {
    val progress = replacement.progressAt(nowMs)
    val breakShare = ReplacementTiming.BREAK_MS.toFloat() / ReplacementTiming.SEQUENCE_MS
    val fadeShare = ReplacementTiming.FADE_MS.toFloat() / ReplacementTiming.SEQUENCE_MS
    val breakProgress = (progress / breakShare).coerceIn(0f, 1f)
    val fadeProgress = ((progress - (1f - fadeShare)) / fadeShare).coerceIn(0f, 1f)
    return ReplacementFrame(breakProgress, fadeProgress)
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
