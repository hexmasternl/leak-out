package nl.hexmaster.leakout.core.engine

import nl.hexmaster.leakout.core.board.Board
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.score.FinalScore
import nl.hexmaster.leakout.core.score.ScoreSheet
import nl.hexmaster.leakout.core.stack.PipeStack

/**
 * Everything about one attempt at a level. Immutable: the [GameEngine] makes a new state for every
 * accepted command. Values that follow from others (score, stars, speed) are computed, so they
 * cannot drift apart.
 */
data class GameState(
    /** Counts attempts, starting at 1. Screen state that must reset on a new attempt is keyed on it. */
    val attempt: Int,
    val level: Level,
    /** The start, finish, blocked tiles and every placed section. */
    val board: Board,
    val stack: PipeStack,
    val flow: FlowState,
    /** The points of this attempt and how often the liquid has left each tile. */
    val sheet: ScoreSheet,
    /** Recent awards, oldest first, dropped after [Award.VISIBLE_MS]. */
    val awards: List<Award>,
    /** Game time until which no placement is accepted, set by a replacement. */
    val replaceLockUntilMs: Long,
    /** The replacement in progress, if any. Its tile is empty until the new section lands. */
    val replacement: Replacement?,
    /** Game time in milliseconds. It does not move while [paused]. */
    val clockMs: Long,
    val paused: Boolean,
    /** While on, the flow runs [FAST_FORWARD_FACTOR] times as fast and placing is locked. */
    val fastForward: Boolean,
    /** Whether the placed sections connect the start to the finish. Checked after every placement. */
    val routeComplete: Boolean,
    val outcome: Outcome,
    /** The final score, set when the level is won and fixed from then on; null otherwise. */
    val finalScore: FinalScore?,
    /** How many of [FinalScore.unusedSections] have been taken off the board, in order, by [GameCommand.RemoveUnused]. */
    val sectionsRemoved: Int,
) {
    val score: Int get() = sheet.total

    val stars: Int get() = level.stars.starsFor(score)

    /** The stars of a won attempt: the thresholds applied to the final total. 0 without a final score. */
    val finalStars: Int get() = finalScore?.let { level.stars.starsFor(it.total) } ?: 0

    /** How many times the liquid has left a placed section. A cross passed twice counts twice. */
    val sectionsLeft: Int get() = sheet.passes.values.sum()

    /** The flow speed with the ramp, in cells per second, as the HUD shows it: without the reservoir factor. */
    val flowSpeed: Double get() = level.flow.cellsPerSecond * (1 + level.flow.speedRampPerCell * sectionsLeft)

    /** How much faster than real time the flow runs. */
    val flowTimeScale: Int get() = if (fastForward) FAST_FORWARD_FACTOR else 1

    /** Whether the game stands still for the player: paused, won or lost. */
    val isStopped: Boolean get() = paused || outcome !is Outcome.Playing

    companion object {
        const val FAST_FORWARD_FACTOR = 10
    }
}
