package nl.hexmaster.leakout.ui.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.ui.daily.DailyLeaderboardState
import nl.hexmaster.leakout.ui.daily.DailyResultState
import nl.hexmaster.leakout.ui.daily.DailyResults

/** The daily pipe result of one attempt, for the level complete dialog, and its Try again. */
class DailyResultHolder {
    var state: DailyResultState? by mutableStateOf(null)
        internal set

    internal var tries: Int by mutableIntStateOf(0)

    /** Sends the result again after it failed; does nothing otherwise. */
    fun retry() {
        if (state == DailyResultState.Failed) tries++
    }
}

/**
 * Sends a won daily pipe attempt to [daily] once the level complete dialog shows: the final total
 * and the stars the level's thresholds give it. Every attempt is sent on its own; a loss sends
 * nothing, and a result the server took (or refused because the day ended) is not sent again.
 */
@Composable
internal fun rememberDailyResult(session: GameSession, daily: DailyResults): DailyResultHolder {
    val attempt = session.select { state.attempt }
    val showWinDialog = session.select { showWinDialog }
    val holder = remember(session, daily, attempt) { DailyResultHolder() }
    LaunchedEffect(holder, showWinDialog, holder.tries) {
        val won = session.state
        val score = won.finalScore ?: return@LaunchedEffect
        if (!showWinDialog || won.outcome !is Outcome.Won) return@LaunchedEffect
        if (holder.state is DailyResultState.Sent || holder.state == DailyResultState.Ended) return@LaunchedEffect
        holder.state = DailyResultState.Sending
        holder.state = DailyResultState.of(daily.submit(score.total, won.finalStars))
    }
    return holder
}

/**
 * The daily leaderboard of one attempt (spec `daily-pipe`): shown before the attempt starts, while
 * [waitingForStart], and again after a win once [afterWin] is set by the level complete dialog's Next.
 */
class DailyLeaderboardHolder {
    var waitingForStart: Boolean by mutableStateOf(true)
        internal set

    var afterWin: Boolean by mutableStateOf(false)
        internal set

    var state: DailyLeaderboardState by mutableStateOf(DailyLeaderboardState.Loading)
        internal set

    /** Closes the starting leaderboard; the attempt starts. */
    fun start() {
        waitingForStart = false
    }

    /** Shows the leaderboard after a win, loaded again so that it holds the new result. */
    fun showAfterWin() {
        afterWin = true
    }
}

/**
 * Loads the day's top scores through [daily] for the leaderboard of each attempt: when the attempt
 * opens, and after a win once Next was tapped and the [result] is no longer being sent, so the new
 * result is in the list. A new attempt (Restart) opens on the starting leaderboard again.
 */
@Composable
internal fun rememberDailyLeaderboard(session: GameSession, daily: DailyResults, result: DailyResultHolder?): DailyLeaderboardHolder {
    val attempt = session.select { state.attempt }
    val holder = remember(session, daily, attempt) { DailyLeaderboardHolder() }
    LaunchedEffect(holder) {
        holder.state = DailyLeaderboardState.of(daily.top())
    }
    val sending = result?.state == DailyResultState.Sending
    LaunchedEffect(holder, holder.afterWin, sending) {
        if (!holder.afterWin || sending) return@LaunchedEffect
        holder.state = DailyLeaderboardState.Loading
        holder.state = DailyLeaderboardState.of(daily.top())
    }
    return holder
}
