package nl.hexmaster.leakout.core.engine

import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position

/**
 * A replacement in progress on the tile [at]: [removed] was destroyed at game time [startedAtMs],
 * the tile is empty, and [incoming], taken from the stack at the drop, lands at [landsAtMs].
 * [taps] counts the [GameCommand.ReplacementTap]s on [at] while it runs.
 */
data class Replacement(
    val at: Position,
    val removed: PipeSection,
    val incoming: PipeSection,
    val startedAtMs: Long,
    val landsAtMs: Long,
    val taps: Int = 0,
) {
    val durationMs: Long get() = landsAtMs - startedAtMs

    /** How far the sequence is at game time [nowMs], from 0 at the drop to 1 at the landing. */
    fun progressAt(nowMs: Long): Float =
        if (durationMs <= 0) 1f else ((nowMs - startedAtMs).toFloat() / durationMs).coerceIn(0f, 1f)
}
