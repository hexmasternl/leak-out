package nl.hexmaster.leakout.ui.game.onboarding

import java.math.BigDecimal
import java.math.RoundingMode
import nl.hexmaster.leakout.core.board.Board
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.stack.PipeStack
import nl.hexmaster.leakout.settings.TipId
import kotlin.math.abs

/**
 * Which one-time tips are due (spec `game-play-onboarding`), as a pure function of the game, so the
 * game screen only has to ask at the right moments: at level start once the stack is laid out,
 * after every accepted placement and whenever [fullyVisibleSlots] changes.
 */
object TipScheduler {
    /**
     * The tips due now, in the order they are shown: intro, mandatory, portal, cross, reservoir.
     * Nothing is due once the attempt has ended. The intro tour, the mandatory tip and the portal tip
     * only come [atLevelStart]; the cross and reservoir tips come whenever an `X` / `RES` sits in a
     * stack slot whose index is in [fullyVisibleSlots], so a section on a cut-off top of the stack
     * waits until it moves into view. [mandatory] are the level's mandatory section cells, and
     * [portalPairs] the number of portal pairs the level has.
     */
    fun dueTips(
        stack: PipeStack,
        outcome: Outcome,
        seen: Set<TipId>,
        atLevelStart: Boolean,
        fullyVisibleSlots: Set<Int>,
        mandatory: Collection<Position> = emptyList(),
        portalPairs: Int = 0,
    ): List<TipId> {
        if (outcome != Outcome.Playing) return emptyList()
        return buildList {
            if (atLevelStart && TipId.Intro !in seen) add(TipId.Intro)
            if (atLevelStart && mandatory.isNotEmpty() && TipId.Mandatory !in seen) add(TipId.Mandatory)
            if (atLevelStart && portalPairs > 0 && TipId.Portal !in seen) add(TipId.Portal)
            if (TipId.Cross !in seen && tipSlot(stack, PipeSection.X, fullyVisibleSlots) != null) add(TipId.Cross)
            if (TipId.Reservoir !in seen && tipSlot(stack, PipeSection.RES, fullyVisibleSlots) != null) add(TipId.Reservoir)
        }
    }

    /**
     * The cells the portal tip highlights and links: both portals of pair 1, in the level's order.
     * The other pairs are not highlighted, so the player sees which two cells belong together.
     */
    fun portalTipCells(level: Level): List<Position> =
        level.portals.firstOrNull()?.let { listOf(it.a.position, it.b.position) }.orEmpty()

    /**
     * The stack slot to highlight for a [section] tip: the lowest fully visible slot holding it
     * (the highest index, as the next section is at the bottom), or null when none is visible.
     */
    fun tipSlot(stack: PipeStack, section: PipeSection, fullyVisibleSlots: Set<Int>): Int? =
        stack.sections.indices.lastOrNull { stack.sections[it] == section && it in fullyVisibleSlots }

    /**
     * The cell the drag hint of the placing step ends on: the empty cell nearest to the cell the
     * start's exit opens onto, by grid distance, ties going to the upper and then the left one.
     * Blocked, start, finish and filled cells are skipped. Null when the board has no empty cell.
     */
    fun hintCell(level: Level, board: Board = Board.from(level)): Position? {
        val exit = level.start.position.neighbour(level.start.openSide)
        val cells = (0 until level.grid.rows).flatMap { y -> (0 until level.grid.columns).map { x -> Position(x, y) } }
        return cells
            .filter { board[it] == Cell.Empty }
            .minWithOrNull(compareBy<Position>({ abs(it.x - exit.x) + abs(it.y - exit.y) }, { it.y }, { it.x }))
    }
}

/** Numbers shown in tip texts. */
object TipText {
    /** The reservoir speed [factor] as a whole percentage, rounded half up: 0.3 → 30, 0.333 → 33. */
    fun percent(factor: Double): Int = BigDecimal.valueOf(factor).movePointRight(2).setScale(0, RoundingMode.HALF_UP).toInt()
}
