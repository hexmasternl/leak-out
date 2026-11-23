package nl.hexmaster.leakout.ui.daily

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.scores.online.DAILY_LEADERBOARD_ROWS
import nl.hexmaster.leakout.scores.online.DailyLeaderboardEntry
import nl.hexmaster.leakout.scores.online.DailyLeaderboardRow
import nl.hexmaster.leakout.scores.online.DailyPlayerRank
import nl.hexmaster.leakout.scores.online.DailyTopResult
import nl.hexmaster.leakout.scores.online.DailyTopScores
import nl.hexmaster.leakout.scores.online.dailyLeaderboardRows
import nl.hexmaster.leakout.ui.components.FlowButton
import nl.hexmaster.leakout.ui.components.FlowButtonSize
import nl.hexmaster.leakout.ui.components.FlowButtonVariant
import nl.hexmaster.leakout.ui.components.displayUppercase
import nl.hexmaster.leakout.ui.game.CompactColumns
import nl.hexmaster.leakout.ui.game.HudFormat
import nl.hexmaster.leakout.ui.game.RegularColumn
import nl.hexmaster.leakout.ui.game.ResultDialogFrame
import nl.hexmaster.leakout.ui.game.ResultDialogLayout
import nl.hexmaster.leakout.ui.game.ResultDialogMetrics
import nl.hexmaster.leakout.ui.game.ResultDialogPanel
import nl.hexmaster.leakout.ui.game.ResultDialogWindow
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles
import nl.hexmaster.leakout.ui.theme.LeakOutTheme

object DailyLeaderboardTags {
    const val DIALOG = "daily_leaderboard"
    const val TITLE = "daily_leaderboard_title"
    const val TABLE = "daily_leaderboard_table"
    const val LOADING = "daily_leaderboard_loading"
    const val EMPTY = "daily_leaderboard_empty"
    const val FAILED = "daily_leaderboard_failed"
    const val START = "daily_leaderboard_start"
    const val RESTART = "daily_leaderboard_restart"
    const val EXIT = "daily_leaderboard_exit"
    const val LAYOUT_PREFIX = "daily_leaderboard"

    /** The row of [position]. */
    fun row(position: Int) = "daily_leaderboard_row_$position"
}

/** Sizes of the daily leaderboard table (design-system.md 5.6). */
object DailyLeaderboardMetrics {
    val RowHeight: Dp = 30.dp
    val HeaderHeight: Dp = 22.dp
    val PositionWidth: Dp = 76.dp
    val PointsWidth: Dp = 72.dp
    val RowPadding: Dp = 8.dp

    /** The table's space: the header and the top rows plus the player's own row, the same in every state. */
    val TableHeight: Dp = HeaderHeight + RowHeight * (DAILY_LEADERBOARD_ROWS + 1)
}

/** What the daily leaderboard shows: still loading, the day's top scores, or that they could not be loaded. */
sealed interface DailyLeaderboardState {
    data object Loading : DailyLeaderboardState

    data class Loaded(val top: DailyTopScores) : DailyLeaderboardState

    data object Failed : DailyLeaderboardState

    companion object {
        fun of(result: DailyTopResult): DailyLeaderboardState = when (result) {
            is DailyTopResult.Loaded -> Loaded(result.top)
            DailyTopResult.Failed -> Failed
        }
    }
}

/** The buttons under the daily leaderboard: Start before the pipe is played, Restart and Exit after a win. */
sealed interface DailyLeaderboardActions {
    data class Start(val onStart: () -> Unit) : DailyLeaderboardActions

    data class AfterWin(val onRestart: () -> Unit, val onExit: () -> Unit) : DailyLeaderboardActions
}

/**
 * The daily leaderboard (spec `daily-pipe`) as a result dialog over the paused or won board: the
 * eyebrow, "Today's top 5", the Position, Player and Points table and the [actions]. Back calls
 * [onBack]; a tap outside does nothing. [playerName] names the player's own row when the server sent
 * no name for it.
 */
@Composable
fun DailyLeaderboardDialog(
    state: DailyLeaderboardState,
    actions: DailyLeaderboardActions,
    onBack: () -> Unit,
    eyebrow: String,
    playerName: String?,
) {
    val paneName = stringResource(R.string.daily_leaderboard_title)
    ResultDialogWindow(
        onDismissRequest = onBack,
        dismissOnBackPress = true,
        panelModifier = Modifier.testTag(DailyLeaderboardTags.DIALOG).semantics { paneTitle = paneName },
        layoutTagPrefix = DailyLeaderboardTags.LAYOUT_PREFIX,
        measureRegular = { DailyLeaderboardContent(ResultDialogLayout.Regular, state, actions, eyebrow, playerName, measuring = true) },
    ) { layout ->
        DailyLeaderboardContent(layout, state, actions, eyebrow, playerName)
    }
}

@Composable
private fun DailyLeaderboardContent(
    layout: ResultDialogLayout,
    state: DailyLeaderboardState,
    actions: DailyLeaderboardActions,
    eyebrow: String,
    playerName: String?,
    measuring: Boolean = false,
) {
    val tag: Modifier.(String) -> Modifier = { if (measuring) this else testTag(it) }
    val info: @Composable ColumnScope.() -> Unit = {
        Text(eyebrow.displayUppercase(), style = LeakOutTextStyles.DisplayXs, color = LeakOutColors.Success500)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.daily_leaderboard_title),
            style = LeakOutTextStyles.DisplayLg,
            color = LeakOutColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.tag(DailyLeaderboardTags.TITLE).semantics { heading() },
        )
        Spacer(Modifier.height(12.dp))
        DailyLeaderboardTable(state, playerName, tag)
    }
    when (layout) {
        ResultDialogLayout.Regular -> RegularColumn {
            info()
            Spacer(Modifier.height(20.dp))
            Buttons(actions, FlowButtonSize.Large, tag)
        }
        ResultDialogLayout.Compact -> CompactColumns(info = info, buttons = { Buttons(actions, FlowButtonSize.Medium, tag) })
    }
}

@Composable
private fun ColumnScope.Buttons(actions: DailyLeaderboardActions, size: FlowButtonSize, tag: Modifier.(String) -> Modifier) {
    // CompactColumns already spaces its buttons; the Regular column does not.
    val gap = @Composable { if (size == FlowButtonSize.Large) Spacer(Modifier.height(ResultDialogMetrics.ButtonSpacing)) }
    when (actions) {
        is DailyLeaderboardActions.Start -> FlowButton(
            text = stringResource(R.string.daily_leaderboard_start),
            onClick = actions.onStart,
            variant = FlowButtonVariant.Primary,
            size = size,
            modifier = Modifier.fillMaxWidth().tag(DailyLeaderboardTags.START),
        )
        is DailyLeaderboardActions.AfterWin -> {
            FlowButton(
                text = stringResource(R.string.win_restart),
                onClick = actions.onRestart,
                variant = FlowButtonVariant.Secondary,
                size = size,
                modifier = Modifier.fillMaxWidth().tag(DailyLeaderboardTags.RESTART),
            )
            gap()
            FlowButton(
                text = stringResource(R.string.win_exit),
                onClick = actions.onExit,
                variant = FlowButtonVariant.Tertiary,
                size = size,
                modifier = Modifier.fillMaxWidth().tag(DailyLeaderboardTags.EXIT),
            )
        }
    }
}

/** The table, or the loading, empty or failed message in its place; the same height in every state. */
@Composable
private fun DailyLeaderboardTable(state: DailyLeaderboardState, playerName: String?, tag: Modifier.(String) -> Modifier) {
    Box(Modifier.fillMaxWidth().height(DailyLeaderboardMetrics.TableHeight), contentAlignment = Alignment.Center) {
        val rows = (state as? DailyLeaderboardState.Loaded)?.let { dailyLeaderboardRows(it.top) }
        when {
            state == DailyLeaderboardState.Loading -> Column(Modifier.tag(DailyLeaderboardTags.LOADING), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(Modifier.height(28.dp).width(28.dp), color = LeakOutColors.Liquid500, strokeWidth = 3.dp, trackColor = LeakOutColors.LineSubtle)
                Spacer(Modifier.height(8.dp))
                Message(stringResource(R.string.daily_leaderboard_loading))
            }
            state == DailyLeaderboardState.Failed -> Message(stringResource(R.string.daily_leaderboard_failed), Modifier.tag(DailyLeaderboardTags.FAILED))
            rows.isNullOrEmpty() -> Message(stringResource(R.string.daily_leaderboard_empty), Modifier.tag(DailyLeaderboardTags.EMPTY))
            else -> Column(Modifier.fillMaxWidth().height(DailyLeaderboardMetrics.TableHeight).tag(DailyLeaderboardTags.TABLE)) {
                HeaderRow()
                rows.forEach { row -> TableRow(row, playerName, Modifier.tag(DailyLeaderboardTags.row(row.position))) }
            }
        }
    }
}

@Composable
private fun Message(text: String, modifier: Modifier = Modifier) {
    Text(text, style = LeakOutTextStyles.BodyMd, color = LeakOutColors.TextSecondary, textAlign = TextAlign.Center, modifier = modifier)
}

/** The column captions; screen readers skip them, every row says what its values are. */
@Composable
private fun HeaderRow() {
    Row(
        Modifier
            .fillMaxWidth()
            .height(DailyLeaderboardMetrics.HeaderHeight)
            .clearAndSetSemantics { }
            .padding(horizontal = DailyLeaderboardMetrics.RowPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Caption(stringResource(R.string.daily_leaderboard_position), Modifier.width(DailyLeaderboardMetrics.PositionWidth))
        Caption(stringResource(R.string.daily_leaderboard_player), Modifier.weight(1f))
        Caption(stringResource(R.string.daily_leaderboard_points), Modifier.width(DailyLeaderboardMetrics.PointsWidth), TextAlign.End)
    }
}

@Composable
private fun Caption(text: String, modifier: Modifier, align: TextAlign = TextAlign.Start) {
    Text(text.displayUppercase(), style = LeakOutTextStyles.LabelMd, color = LeakOutColors.TextSecondary, maxLines = 1, textAlign = align, modifier = modifier)
}

/**
 * One row, read as one item. The player's own row has "(you)" after the name, a `liquid.tint` wash
 * and a `liquid.500` edge; positions 1 to 3 are `brass.300`.
 */
@Composable
private fun TableRow(row: DailyLeaderboardRow, playerName: String?, modifier: Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val points = HudFormat.score(row.score, locale)
    val own = row.isCurrentPlayer
    val name = row.displayName ?: playerName ?: stringResource(R.string.daily_leaderboard_you_unnamed)
    val shown = if (own) stringResource(R.string.daily_leaderboard_you, name) else name
    val description = stringResource(
        if (own) R.string.daily_leaderboard_row_own_description else R.string.daily_leaderboard_row_description,
        row.position,
        name,
        points,
    )
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(DailyLeaderboardMetrics.RowHeight)
            .then(if (own) Modifier.background(LeakOutColors.LiquidTint, shape).border(1.dp, LeakOutColors.Liquid500, shape) else Modifier)
            .clearAndSetSemantics { contentDescription = description }
            .padding(horizontal = DailyLeaderboardMetrics.RowPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = HudFormat.score(row.position, locale),
            style = LeakOutTextStyles.NumericMd,
            color = if (row.position <= 3) LeakOutColors.Brass300 else LeakOutColors.TextSecondary,
            maxLines = 1,
            modifier = Modifier.width(DailyLeaderboardMetrics.PositionWidth),
        )
        Text(
            text = shown,
            style = LeakOutTextStyles.BodyMd,
            color = if (own) LeakOutColors.Liquid300 else LeakOutColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = points,
            style = LeakOutTextStyles.NumericMd,
            color = LeakOutColors.TextPrimary,
            maxLines = 1,
            textAlign = TextAlign.End,
            modifier = Modifier.width(DailyLeaderboardMetrics.PointsWidth),
        )
    }
}

private val PreviewTop = DailyTopScores(
    LocalDate.of(2026, 11, 23),
    listOf("Player v" to 512, "Player w" to 511, "Player x" to 501, "Player y" to 498, "Player z" to 495)
        .mapIndexed { index, (name, score) -> DailyLeaderboardEntry(index + 1, name, score, 3, isCurrentPlayer = false) },
    DailyPlayerRank(position = 76, score = 125, stars = 1),
)

@Composable
private fun DailyLeaderboardPreview(actions: DailyLeaderboardActions) {
    LeakOutTheme {
        val state = DailyLeaderboardState.Loaded(PreviewTop)
        ResultDialogFrame(measureRegular = { DailyLeaderboardContent(ResultDialogLayout.Regular, state, actions, "Daily Pipe", "Player a", measuring = true) }) { layout ->
            ResultDialogPanel(layout) { DailyLeaderboardContent(layout, state, actions, "Daily Pipe", "Player a") }
        }
    }
}

@Preview(name = "Daily leaderboard, start, 640 × 360", widthDp = 640, heightDp = 360, showBackground = true, backgroundColor = 0xFF0C1A2A)
@Composable
private fun DailyLeaderboardStartCompactPreview() = DailyLeaderboardPreview(DailyLeaderboardActions.Start {})

@Preview(name = "Daily leaderboard, after a win, 1280 × 800", widthDp = 1280, heightDp = 800, showBackground = true, backgroundColor = 0xFF0C1A2A)
@Composable
private fun DailyLeaderboardAfterWinRegularPreview() = DailyLeaderboardPreview(DailyLeaderboardActions.AfterWin({}, {}))
