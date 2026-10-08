package nl.hexmaster.leakout.core.board

import nl.hexmaster.leakout.core.level.PlacementRules
import nl.hexmaster.leakout.core.level.Position

/** What dropping the next section on a tile would do. */
sealed interface PlacementCheck {
    data object Place : PlacementCheck
    data object Replace : PlacementCheck
    data class Rejected(val reason: Reason) : PlacementCheck

    enum class Reason { OUTSIDE, LOCKED, FIXED, FILLED, REPLACE_NOT_ALLOWED }

    val accepted: Boolean get() = this !is Rejected
}

/**
 * The one placement rule, shared by the hover highlight and the drop so they cannot disagree.
 * The section being dropped does not matter, and pausing is checked by the caller.
 */
object Placement {
    fun check(board: Board, rules: PlacementRules, at: Position, nowMs: Long, lockUntilMs: Long): PlacementCheck {
        if (at !in board.grid) return PlacementCheck.Rejected(PlacementCheck.Reason.OUTSIDE)
        if (nowMs < lockUntilMs) return PlacementCheck.Rejected(PlacementCheck.Reason.LOCKED)
        return when (val cell = board[at]) {
            Cell.Empty -> PlacementCheck.Place
            Cell.Blocked, is Cell.Start, is Cell.Finish, is Cell.Portal -> PlacementCheck.Rejected(PlacementCheck.Reason.FIXED)
            is Cell.Pipe -> when {
                cell.mandatory -> PlacementCheck.Rejected(PlacementCheck.Reason.FIXED)
                cell.liquidEntered -> PlacementCheck.Rejected(PlacementCheck.Reason.FILLED)
                !rules.allowReplace -> PlacementCheck.Rejected(PlacementCheck.Reason.REPLACE_NOT_ALLOWED)
                else -> PlacementCheck.Replace
            }
        }
    }
}
