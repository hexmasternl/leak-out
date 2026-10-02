package nl.hexmaster.leakout.core.achievements

import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.engine.GameState
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position

enum class AttemptOutcome { WON, LOST }

/**
 * What recording a win did to the level history. The app's history repository reports it; core
 * keeps its own copy so the rules do not depend on the app.
 */
enum class RecordOutcome { FIRST_COMPLETION, NEW_HIGHSCORE, NO_IMPROVEMENT }

/**
 * The facts of one finished attempt that the skill, hidden and lifetime achievements are decided
 * from. Plain values, so the rules can be tested without playing.
 *
 * @param finalTotal the final total after the unused-section penalty; null on a loss.
 * @param finalStars the stars of [finalTotal]; 0 on a loss.
 * @param unusedSections placed sections the liquid never entered, as the final score lists them; 0 on a loss.
 * @param distinctTilesLeft placed tiles the liquid left at least once; a cross counts once.
 * @param sectionsLeft exits of the liquid from placed sections; a cross passed twice counts twice.
 * @param crossesLeftTwice crosses the liquid left through both channels.
 * @param fastForwardAtWin whether fast forward was on when the liquid entered the finish; false on a loss.
 * @param freeCells grid cells that are not blocked, not the start and not the finish.
 * @param piecesOnBoard sections on the board when the outcome was reached, mandatory ones included.
 * @param record what recording the win did to the history; null on a loss.
 */
data class AttemptSummary(
    val levelId: Int,
    val outcome: AttemptOutcome,
    val finalTotal: Int?,
    val finalStars: Int,
    val unusedSections: Int,
    val distinctTilesLeft: Int,
    val sectionsLeft: Int,
    val crossesLeftTwice: Int,
    val placements: Int,
    val replacements: Int,
    val everPaused: Boolean,
    val fastForwardAtWin: Boolean,
    val routeCompletedBeforeFlow: Boolean,
    val photoFinish: Boolean,
    val freeCells: Int,
    val piecesOnBoard: Int,
    val record: RecordOutcome?,
) {
    val won: Boolean get() = outcome == AttemptOutcome.WON

    val lost: Boolean get() = outcome == AttemptOutcome.LOST

    companion object {
        /**
         * The summary of [state], which must be won or lost. Sections that [GameState.sectionsRemoved]
         * took off a won board are counted back, so the summary describes the board at the moment of
         * the win whenever it is built.
         *
         * @param record what recording the win did to the history; ignored on a loss.
         */
        fun from(state: GameState, record: RecordOutcome?): AttemptSummary {
            val outcome = when (state.outcome) {
                is Outcome.Won -> AttemptOutcome.WON
                is Outcome.Lost -> AttemptOutcome.LOST
                Outcome.Playing -> throw IllegalArgumentException("An attempt that is still playing has no summary")
            }
            val won = outcome == AttemptOutcome.WON
            val passes = state.sheet.passes
            val finalScore = state.finalScore.takeIf { won }
            return AttemptSummary(
                levelId = state.level.id,
                outcome = outcome,
                finalTotal = finalScore?.total,
                finalStars = if (won) state.finalStars else 0,
                unusedSections = finalScore?.unusedCount ?: 0,
                distinctTilesLeft = passes.size,
                sectionsLeft = passes.values.sum(),
                // RemoveUnused only takes unentered sections, which have no passes, so every tile
                // with passes still holds its section.
                crossesLeftTwice = passes.count { (at, count) -> count >= 2 && state.board[at].isCross() },
                placements = state.placements,
                replacements = state.replacements,
                everPaused = state.everPaused,
                fastForwardAtWin = won && state.fastForward,
                routeCompletedBeforeFlow = state.routeCompletedBeforeFlow,
                photoFinish = state.photoFinish,
                freeCells = freeCells(state.level),
                piecesOnBoard = state.board.pipes.size + if (won) state.sectionsRemoved else 0,
                record = record.takeIf { won },
            )
        }

        /** The cells of [level] that can hold a section: not blocked, not the start and not the finish. */
        fun freeCells(level: Level): Int {
            val fixed = (level.blocked + level.start.position + level.finish.position).toSet()
            return (0 until level.grid.columns).sumOf { x ->
                (0 until level.grid.rows).count { y -> Position(x, y) !in fixed }
            }
        }

        private fun Cell.isCross(): Boolean = this is Cell.Pipe && section == PipeSection.X
    }
}
