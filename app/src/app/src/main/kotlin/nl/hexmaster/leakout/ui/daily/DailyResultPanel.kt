package nl.hexmaster.leakout.ui.daily

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.scores.online.DailyPipes
import nl.hexmaster.leakout.scores.online.DailyScoreResult
import nl.hexmaster.leakout.scores.online.DailySubmission
import nl.hexmaster.leakout.scores.online.DailyTopResult
import nl.hexmaster.leakout.ui.components.FlowButton
import nl.hexmaster.leakout.ui.components.FlowButtonSize
import nl.hexmaster.leakout.ui.components.FlowButtonVariant
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles

/**
 * The game screen's view of one day's [DailyPipes]: sends a won daily pipe's score and stars, and
 * loads that day's top scores for the daily leaderboard. [playerName] is the name the leaderboard
 * shows on the player's own row when the server sent no entry for it.
 */
interface DailyResults {
    suspend fun submit(score: Int, stars: Int): DailySubmission

    suspend fun top(): DailyTopResult

    val playerName: String? get() = null

    companion object {
        /** The results of the pipe of [date] on [pipes]; [playerName] is read when the leaderboard shows. */
        fun of(pipes: DailyPipes, date: LocalDate, playerName: () -> String? = { null }): DailyResults = object : DailyResults {
            override suspend fun submit(score: Int, stars: Int): DailySubmission = pipes.submit(date, score, stars)

            override suspend fun top(): DailyTopResult = pipes.top(date)

            override val playerName: String? get() = playerName()
        }
    }
}

/** How sending a daily pipe win goes, as the level complete dialog shows it. */
sealed interface DailyResultState {
    data object Sending : DailyResultState

    /** The server took the result: the player's best, rank and streak. */
    data class Sent(val result: DailyScoreResult) : DailyResultState

    /** UTC midnight passed during the attempt: the daily pipe has ended and the streak did not change. */
    data object Ended : DailyResultState

    /** The result could not be sent; Try again sends it once more. */
    data object Failed : DailyResultState

    companion object {
        fun of(submission: DailySubmission): DailyResultState = when (submission) {
            is DailySubmission.Accepted -> Sent(submission.result)
            DailySubmission.Ended -> Ended
            DailySubmission.Failed -> Failed
        }
    }
}

object DailyResultTags {
    const val PANEL = "daily_result"
    const val STREAK = "daily_result_streak"
    const val RANK = "daily_result_rank"
    const val ENDED = "daily_result_ended"
    const val FAILED = "daily_result_failed"
    const val RETRY = "daily_result_retry"
}

/**
 * The daily pipe part of the level complete dialog (spec `daily-pipe`): the streak and the player's
 * rank once the server took the result, or why it did not. The day's top scores follow after Next,
 * on the daily leaderboard.
 */
@Composable
fun DailyResultPanel(state: DailyResultState, onRetry: () -> Unit, modifier: Modifier = Modifier, measuring: Boolean = false) {
    val tag: Modifier.(String) -> Modifier = { if (measuring) this else testTag(it) }
    Column(
        modifier = modifier.tag(DailyResultTags.PANEL).semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        when (state) {
            DailyResultState.Sending -> Line(stringResource(R.string.daily_result_sending), LeakOutColors.TextSecondary)
            DailyResultState.Ended -> Line(stringResource(R.string.daily_result_ended), LeakOutColors.Leak500, Modifier.tag(DailyResultTags.ENDED))
            DailyResultState.Failed -> {
                Line(stringResource(R.string.daily_result_failed), LeakOutColors.Leak500, Modifier.tag(DailyResultTags.FAILED))
                FlowButton(
                    text = stringResource(R.string.daily_result_retry),
                    onClick = onRetry,
                    variant = FlowButtonVariant.Tertiary,
                    size = FlowButtonSize.Small,
                    modifier = Modifier.tag(DailyResultTags.RETRY),
                )
            }
            is DailyResultState.Sent -> Sent(state, tag)
        }
    }
}

@Composable
private fun Sent(state: DailyResultState.Sent, tag: Modifier.(String) -> Modifier) {
    val streak = state.result.streak
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            pluralStringResource(R.plurals.daily_result_streak, streak.current, streak.current),
            style = LeakOutTextStyles.DisplaySm,
            color = LeakOutColors.Brass300,
            modifier = Modifier.tag(DailyResultTags.STREAK),
        )
        Text(stringResource(R.string.daily_result_best_streak, streak.best), style = LeakOutTextStyles.BodyMd, color = LeakOutColors.TextSecondary)
    }
    Line(stringResource(R.string.daily_result_rank, state.result.rank), LeakOutColors.TextPrimary, Modifier.tag(DailyResultTags.RANK))
}

@Composable
private fun Line(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(text, style = LeakOutTextStyles.BodyMd, color = color, textAlign = TextAlign.Center, modifier = modifier)
}
