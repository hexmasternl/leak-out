package nl.hexmaster.leakout.core.engine

import nl.hexmaster.leakout.core.level.Position

/** [points] awarded for the liquid leaving the tile [position] at game time [atMs]. Shown as a popup. */
data class Award(val position: Position, val points: Int, val atMs: Long) {
    /** How long the award has been shown at game time [nowMs]. */
    fun ageAt(nowMs: Long): Long = nowMs - atMs

    companion object {
        /** How long an award stays in the state, and so how long its popup is shown, in game milliseconds. */
        const val VISIBLE_MS = 1_000L
    }
}
