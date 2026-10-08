package nl.hexmaster.leakout.core.achievements

import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.engine.GameState
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.engine.PathStep
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.LevelSolver
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.SolverResult

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
 * @param freeCells grid cells that are not blocked, not the start, not the finish and not a portal.
 * @param piecesOnBoard sections on the board when the outcome was reached, mandatory ones included.
 * @param record what recording the win did to the history; null on a loss.
 * @param longestCrossChain the most crosses the liquid left one directly after the other.
 * @param longestStraightRun the most straights of the same direction the liquid left one directly after the other.
 * @param longestCornerRun the most corners the liquid left one directly after the other.
 * @param reservoirsLeft exits of the liquid from reservoirs.
 * @param mostReplacementTaps the most taps on the tile of one running replacement.
 * @param minimumSections the cells of the level's shortest route as [LevelSolver] finds it; null
 *   when the solver cannot solve the level.
 *
 * The route shape facts come from [GameState.flowPath], so mandatory sections count like placed ones.
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
    val longestCrossChain: Int = 0,
    val longestStraightRun: Int = 0,
    val longestCornerRun: Int = 0,
    val reservoirsLeft: Int = 0,
    val mostReplacementTaps: Int = 0,
    val minimumSections: Int? = null,
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
                longestCrossChain = longestRun(state.flowPath) { _, step -> step.section == PipeSection.X },
                longestStraightRun = longestRun(state.flowPath) { previous, step -> step.section in STRAIGHTS && (previous == null || previous.section == step.section) },
                longestCornerRun = longestRun(state.flowPath) { _, step -> step.section in CORNERS },
                reservoirsLeft = state.flowPath.count { it.section == PipeSection.RES },
                mostReplacementTaps = state.mostReplacementTaps,
                minimumSections = minimumSections(state.level),
            )
        }

        /**
         * The cells of [level]'s shortest route as [LevelSolver] finds it, or null when it finds none.
         * A mandatory cross is on the solver's path twice but is one cell.
         */
        fun minimumSections(level: Level): Int? = (LevelSolver.solve(level) as? SolverResult.Solvable)?.path?.distinct()?.size

        private val STRAIGHTS = setOf(PipeSection.H, PipeSection.V)
        private val CORNERS = setOf(PipeSection.LT, PipeSection.LB, PipeSection.RT, PipeSection.RB)

        /**
         * The longest run of [path] in which every step [continues] the run: it is given the step
         * before it in the run (null for the first one) and the step itself.
         */
        private fun longestRun(path: List<PathStep>, continues: (previous: PathStep?, step: PathStep) -> Boolean): Int {
            var longest = 0
            var run = 0
            var previous: PathStep? = null
            for (step in path) {
                run = when {
                    continues(previous.takeIf { run > 0 }, step) -> run + 1
                    continues(null, step) -> 1
                    else -> 0
                }
                previous = step
                longest = maxOf(longest, run)
            }
            return longest
        }

        /** The cells of [level] that can hold a section: not blocked, not the start, not the finish and not a portal. */
        fun freeCells(level: Level): Int {
            val fixed = (level.blocked + level.start.position + level.finish.position + level.portalCells).toSet()
            return (0 until level.grid.columns).sumOf { x ->
                (0 until level.grid.rows).count { y -> Position(x, y) !in fixed }
            }
        }

        private fun Cell.isCross(): Boolean = this is Cell.Pipe && section == PipeSection.X
    }
}
