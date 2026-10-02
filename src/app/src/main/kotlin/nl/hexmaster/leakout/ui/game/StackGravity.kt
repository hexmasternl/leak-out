package nl.hexmaster.leakout.ui.game

import kotlin.math.PI
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * `motion.fall` (design system 7.2): how far a stack section is above its slot while it falls into it.
 * Distances and offsets are in dp, times in ms. An offset is the distance left above the target slot; 0 is at rest.
 */
object StackGravity {
    const val BOUNCE_MS: Long = 120L
    const val STAGGER_MS: Long = 24L
    const val MAX_STAGGER_STEPS: Int = 8
    const val REDUCED_MS: Long = 160L

    /** A one-slot fall rests within this time after the placement, stagger included. */
    const val MAX_REST_MS: Long = 400L

    private const val MIN_FALL_MS = 120f
    private const val MAX_FALL_MS = 260f
    private const val FALL_MS_PER_STEP = 90f
    private const val FALL_STEP_DP = 8f
    private const val BOUNCE_FRACTION = 0.12f
    private const val MAX_BOUNCE_DP = 4f

    /** Time to fall [distance]: `sqrt(d / 8 dp) · 90 ms`, between 120 and 260 ms. */
    fun fallDuration(distance: Float): Long =
        (sqrt(distance.coerceAtLeast(0f) / FALL_STEP_DP) * FALL_MS_PER_STEP).coerceIn(MIN_FALL_MS, MAX_FALL_MS).roundToLong()

    /** Height of the single rebound after landing: 12 % of the fall, at most 4 dp. */
    fun bounceHeight(distance: Float): Float = min(BOUNCE_FRACTION * distance, MAX_BOUNCE_DP).coerceAtLeast(0f)

    /** Time from the start of the fall until the section rests: the fall plus the bounce. */
    fun settleDuration(distance: Float): Long = fallDuration(distance) + BOUNCE_MS

    /**
     * Offset above the slot [elapsedMs] after the fall started: `d · (1 − (t / T)²)` while falling (constant
     * acceleration), then `h · sin(π u)` for the bounce, then 0.
     */
    fun offsetAt(distance: Float, elapsedMs: Long): Float {
        if (distance <= 0f) return 0f
        if (elapsedMs <= 0L) return distance
        val fall = fallDuration(distance)
        if (elapsedMs < fall) {
            val progress = elapsedMs.toFloat() / fall
            return distance * (1f - progress * progress)
        }
        val bounce = elapsedMs - fall
        if (bounce < BOUNCE_MS) return bounceHeight(distance) * sin(PI.toFloat() * bounce / BOUNCE_MS)
        return 0f
    }

    /**
     * Delay before the section [indexFromBottom] slots above the bottom starts to fall: 24 ms per slot, at most
     * 8 steps, and shortened where needed so that a fall of [distance] still rests within [MAX_REST_MS].
     */
    fun startDelay(indexFromBottom: Int, distance: Float): Long {
        val stagger = STAGGER_MS * indexFromBottom.coerceIn(0, MAX_STAGGER_STEPS)
        val room = (MAX_REST_MS - settleDuration(distance)).coerceAtLeast(0L)
        return min(stagger, room)
    }

    /** The fall that continues from [currentOffset] when the section's slot moves [pitch] further down. */
    fun retarget(currentOffset: Float, pitch: Float): Float = currentOffset.coerceAtLeast(0f) + pitch

    /** Reduced motion: [distance] slides to 0 over 160 ms with a cubic ease-out, without bounce. */
    fun reducedOffsetAt(distance: Float, elapsedMs: Long): Float {
        if (distance <= 0f) return 0f
        val progress = (elapsedMs.toFloat() / REDUCED_MS).coerceIn(0f, 1f)
        val remaining = 1f - progress
        return distance * remaining * remaining * remaining
    }

    /** Reduced motion: the new top section fades in over 160 ms. */
    fun reducedAlphaAt(elapsedMs: Long): Float = (elapsedMs.toFloat() / REDUCED_MS).coerceIn(0f, 1f)
}

/**
 * One section's fall: it started [from] dp above its slot at frame time [startMs] (null until the next frame stamps
 * it) and waits [delayMs] before moving.
 */
data class SectionFall(
    val from: Float,
    val startMs: Long?,
    val delayMs: Long = 0L,
    val reduced: Boolean = false,
    val fadeIn: Boolean = false,
) {
    private fun elapsed(nowMs: Long): Long = if (startMs == null) 0L else nowMs - startMs - delayMs

    fun offsetAt(nowMs: Long): Float =
        if (reduced) StackGravity.reducedOffsetAt(from, elapsed(nowMs)) else StackGravity.offsetAt(from, elapsed(nowMs))

    fun alphaAt(nowMs: Long): Float =
        if (!fadeIn) 1f else if (startMs == null) 0f else StackGravity.reducedAlphaAt(nowMs - startMs)

    fun restsAt(nowMs: Long): Boolean {
        if (from <= 0f && !fadeIn) return true
        if (startMs == null) return false
        val duration = if (reduced) StackGravity.REDUCED_MS else StackGravity.settleDuration(from)
        return elapsed(nowMs) >= duration
    }

    companion object {
        val Resting = SectionFall(from = 0f, startMs = 0L)
    }
}

/**
 * The falls of every section in the stack, keyed by `PipeStack.idAt`. [placements] is the stack's placement count the
 * falls were last brought up to date with. Pure and immutable, so the panel's motion is testable without a device.
 */
data class StackFalls(val placements: Int, val falls: Map<Int, SectionFall>) {

    fun offsetOf(id: Int, nowMs: Long): Float = falls[id]?.offsetAt(nowMs) ?: 0f

    fun alphaOf(id: Int, nowMs: Long): Float = falls[id]?.alphaAt(nowMs) ?: 1f

    fun isActive(nowMs: Long): Boolean = falls.values.any { !it.restsAt(nowMs) }

    /** Starts every fall that is still waiting for a frame at [frameMs]. */
    fun stamp(frameMs: Long): StackFalls =
        if (falls.values.none { it.startMs == null }) this
        else copy(falls = falls.mapValues { (_, fall) -> if (fall.startMs == null) fall.copy(startMs = frameMs) else fall })

    /**
     * Brings the falls up to date with a stack of [ids] (top first) after [placements] placements, at frame time
     * [nowMs]. Each placement moves every section one [pitch] down: a section keeps its current position and falls
     * on from there, and a new section enters one pitch above its slot. A lower placement count is a new attempt,
     * which starts at rest.
     */
    fun sync(ids: List<Int>, placements: Int, pitch: Float, nowMs: Long, reduced: Boolean): StackFalls {
        if (placements == this.placements && ids.all { it in falls }) return this
        if (placements <= this.placements) return resting(ids, placements)
        val steps = placements - this.placements
        val updated = ids.withIndex().associate { (index, id) ->
            val indexFromBottom = ids.size - 1 - index
            val existing = falls[id]
            id to if (existing != null) {
                val from = StackGravity.retarget(existing.offsetAt(nowMs), pitch * steps)
                val moving = !existing.restsAt(nowMs)
                val delay = if (reduced || moving) 0L else StackGravity.startDelay(indexFromBottom, from)
                SectionFall(from = from, startMs = null, delayMs = delay, reduced = reduced)
            } else if (reduced) {
                SectionFall(from = 0f, startMs = null, reduced = true, fadeIn = true)
            } else {
                val from = pitch * steps
                SectionFall(from = from, startMs = null, delayMs = StackGravity.startDelay(indexFromBottom, from))
            }
        }
        return StackFalls(placements, updated)
    }

    companion object {
        fun resting(ids: List<Int>, placements: Int): StackFalls =
            StackFalls(placements, ids.associateWith { SectionFall.Resting })
    }
}
