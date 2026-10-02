package nl.hexmaster.leakout.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.core.engine.LossReason
import nl.hexmaster.leakout.ui.components.FlowButton
import nl.hexmaster.leakout.ui.components.FlowButtonSize
import nl.hexmaster.leakout.ui.components.FlowButtonVariant
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles
import nl.hexmaster.leakout.ui.theme.LeakOutTheme

object LevelLostTags {
    const val DIALOG = "lost_dialog"
    const val SCORE = "lost_score"
    const val RESTART = "lost_restart"
    const val EXIT = "lost_exit"
    const val SPLASH = "lost_splash"
    const val LAYOUT_PREFIX = "lost"
    const val LAYOUT_REGULAR = "lost_layout_regular"
    const val LAYOUT_COMPACT = "lost_layout_compact"
}

/**
 * Modal level lost dialog (design-system.md 5.6): the [level] number, the splash, "Leaked!", the
 * message for the [reason], the [score] of the attempt, then Restart and Exit. Back works as Exit; a
 * tap outside does nothing. There is no Resume. The dialog uses the Regular layout when it fits the
 * window height and the Compact one otherwise.
 */
@Composable
fun LevelLostDialog(
    level: Int,
    score: Int,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    reason: LossReason = LossReason.LEAK,
) {
    val paneName = stringResource(R.string.lost_pane_title)
    ResultDialogWindow(
        onDismissRequest = onExit,
        dismissOnBackPress = true,
        panelModifier = Modifier.testTag(LevelLostTags.DIALOG).semantics { paneTitle = paneName },
        layoutTagPrefix = LevelLostTags.LAYOUT_PREFIX,
        measureRegular = { LevelLostContent(ResultDialogLayout.Regular, level, score, reason, {}, {}, measuring = true) },
    ) { layout ->
        LevelLostContent(layout, level, score, reason, onRestart, onExit)
    }
}

@Composable
private fun LevelLostContent(
    layout: ResultDialogLayout,
    level: Int,
    score: Int,
    reason: LossReason,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    measuring: Boolean = false,
) {
    // Tags only on the shown content, never on the copy that is only measured.
    val tag: Modifier.(String) -> Modifier = { if (measuring) this else testTag(it) }
    when (layout) {
        ResultDialogLayout.Regular -> RegularColumn {
            LostEyebrow(level)
            Spacer(Modifier.height(8.dp))
            LeakSplash(Modifier.size(72.dp).tag(LevelLostTags.SPLASH))
            Spacer(Modifier.height(12.dp))
            LostHeadline(LeakOutTextStyles.DisplayXl)
            Spacer(Modifier.height(8.dp))
            LostMessage(reason, LeakOutTextStyles.BodyMd)
            Spacer(Modifier.height(16.dp))
            LostScore(score, Modifier.tag(LevelLostTags.SCORE))
            Spacer(Modifier.height(24.dp))
            LostButtons(FlowButtonSize.Large, onRestart, onExit, tag)
        }
        ResultDialogLayout.Compact -> CompactColumns(
            info = {
                LostEyebrow(level)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LeakSplash(Modifier.size(48.dp).tag(LevelLostTags.SPLASH))
                    LostHeadline(LeakOutTextStyles.DisplayLg)
                }
                Spacer(Modifier.height(8.dp))
                LostMessage(reason, LeakOutTextStyles.BodySm)
                Spacer(Modifier.height(12.dp))
                LostScore(score, Modifier.tag(LevelLostTags.SCORE))
            },
            buttons = { LostButtons(FlowButtonSize.Medium, onRestart, onExit, tag) },
        )
    }
}

@Composable
private fun LostEyebrow(level: Int) {
    Text(
        text = stringResource(R.string.result_eyebrow, level).uppercase(),
        style = LeakOutTextStyles.DisplayXs,
        color = LeakOutColors.Leak500,
    )
}

@Composable
private fun LostHeadline(style: TextStyle) {
    Text(stringResource(R.string.lost_title), style = style, color = LeakOutColors.TextPrimary, textAlign = TextAlign.Center)
}

@Composable
private fun LostMessage(reason: LossReason, style: TextStyle) {
    val message = when (reason) {
        LossReason.LEAK -> R.string.lost_message
        LossReason.MANDATORY_MISSED -> R.string.lost_message_mandatory
    }
    Text(stringResource(message), style = style, color = LeakOutColors.TextSecondary, textAlign = TextAlign.Center)
}

/** One centred Score column, read as "Score 85". */
@Composable
private fun LostScore(score: Int, modifier: Modifier) {
    val value = HudFormat.score(score, LocalConfiguration.current.locales[0])
    ResultScoreColumn(
        caption = stringResource(R.string.lost_score_label),
        value = value,
        valueColor = LeakOutColors.Brass300,
        description = stringResource(R.string.lost_score, value),
        modifier = modifier.fillMaxWidth(),
        glow = true,
    )
}

/** Brass primary Restart and tertiary Exit, stacked 12 dp apart. */
@Composable
private fun ColumnScope.LostButtons(size: FlowButtonSize, onRestart: () -> Unit, onExit: () -> Unit, tag: Modifier.(String) -> Modifier) {
    FlowButton(
        text = stringResource(R.string.lost_restart),
        onClick = onRestart,
        variant = FlowButtonVariant.Primary,
        size = size,
        modifier = Modifier.fillMaxWidth().tag(LevelLostTags.RESTART),
    )
    // CompactColumns already spaces its buttons; the Regular column does not.
    if (size == FlowButtonSize.Large) Spacer(Modifier.height(ResultDialogMetrics.ButtonSpacing))
    FlowButton(
        text = stringResource(R.string.lost_exit),
        onClick = onExit,
        variant = FlowButtonVariant.Tertiary,
        modifier = Modifier.fillMaxWidth().tag(LevelLostTags.EXIT),
    )
}

/** The dialog's frame and panel without the dialog window, so previews can render them. */
@Composable
private fun LevelLostPreview() {
    LeakOutTheme {
        ResultDialogFrame(measureRegular = { LevelLostContent(ResultDialogLayout.Regular, 7, 85, LossReason.LEAK, {}, {}, measuring = true) }) { layout ->
            ResultDialogPanel(layout) { LevelLostContent(layout, 7, 85, LossReason.LEAK, {}, {}) }
        }
    }
}

@Preview(name = "Level lost, 640 × 360 (Compact)", widthDp = 640, heightDp = 360, showBackground = true, backgroundColor = 0xFF0C1A2A)
@Composable
private fun LevelLostCompactPreview() = LevelLostPreview()

@Preview(name = "Level lost, 640 × 360, 130 % font", widthDp = 640, heightDp = 360, fontScale = 1.3f, showBackground = true, backgroundColor = 0xFF0C1A2A)
@Composable
private fun LevelLostLargeFontPreview() = LevelLostPreview()

@Preview(name = "Level lost, 1280 × 800 (Regular)", widthDp = 1280, heightDp = 800, showBackground = true, backgroundColor = 0xFF0C1A2A)
@Composable
private fun LevelLostRegularPreview() = LevelLostPreview()
