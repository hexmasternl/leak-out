package nl.hexmaster.leakout.ui.daily

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.scores.online.DailyScoreResult
import nl.hexmaster.leakout.scores.online.DailySubmission
import nl.hexmaster.leakout.scores.online.DailyTopScores
import nl.hexmaster.leakout.ui.components.FlowButton
import nl.hexmaster.leakout.ui.components.FlowButtonSize
import nl.hexmaster.leakout.ui.components.FlowButtonVariant
import nl.hexmaster.leakout.ui.game.HudFormat
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles

/** Sends a won daily pipe's score and stars to the server; the game screen's view of [nl.hexmaster.leakout.scores.online.DailyPipes]. */
fun interface DailyResults {
    suspend fun submit(score: Int, stars: Int): DailySubmission
}

/** How sending a daily pipe win goes, as the level complete dialog shows it. */
sealed interface DailyResultState {
    data object Sending : DailyResultState

    /** The server took the result: the player's best, rank and streak, and the day's [top] scores when they loaded. */
    data class Sent(val result: DailyScoreResult, val top: DailyTopScores?) : DailyResultState

    /** UTC midnight passed during the attempt: the daily pipe has ended and the streak did not change. */
    data object Ended : DailyResultState

    /** The result could not be sent; Try again sends it once more. */
    data object Failed : DailyResultState

    companion object {
        fun of(submission: DailySubmission): DailyResultState = when (submission) {
            is DailySubmission.Accepted -> Sent(submission.result, submission.top)
            DailySubmission.Ended -> Ended
            DailySubmission.Failed -> Failed
        }
    }
}

object DailyResultTags {
    const val PANEL = "daily_result"
    const val STREAK = "daily_result_streak"
    const val RANK = "daily_result_rank"
    const val TOP = "daily_result_top"
    const val ENDED = "daily_result_ended"
    const val FAILED = "daily_result_failed"
    const val RETRY = "daily_result_retry"
}

/** The rows of the day's top scores shown in the dialog; the panel must fit the Compact layout too. */
const val DAILY_TOP_ROWS = 3

/**
 * The daily pipe part of the level complete dialog (spec `daily-pipe`): the streak and the player's
 * rank with the day's top scores once the server took the result, or why it did not.
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
    val entries = state.top?.entries.orEmpty().take(DAILY_TOP_ROWS)
    if (entries.isNotEmpty()) {
        val locale = LocalConfiguration.current.locales[0]
        Column(Modifier.tag(DailyResultTags.TOP), horizontalAlignment = Alignment.CenterHorizontally) {
            Line(stringResource(R.string.daily_result_top_title), LeakOutColors.TextSecondary)
            entries.forEach { entry ->
                Line(
                    stringResource(R.string.daily_result_top_row, entry.position, entry.displayName, HudFormat.score(entry.score, locale)),
                    if (entry.isCurrentPlayer) LeakOutColors.Brass300 else LeakOutColors.TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun Line(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(text, style = LeakOutTextStyles.BodyMd, color = color, textAlign = TextAlign.Center, modifier = modifier)
}
