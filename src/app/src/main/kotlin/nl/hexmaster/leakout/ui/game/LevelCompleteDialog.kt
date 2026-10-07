package nl.hexmaster.leakout.ui.game

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import kotlinx.coroutines.flow.StateFlow
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.core.achievements.Achievement
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.score.FinalScore
import nl.hexmaster.leakout.scores.online.ScorePostStatus
import nl.hexmaster.leakout.ui.components.FlowButton
import nl.hexmaster.leakout.ui.components.FlowButtonSize
import nl.hexmaster.leakout.ui.components.FlowButtonVariant
import nl.hexmaster.leakout.ui.components.displayUppercase
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles
import nl.hexmaster.leakout.ui.theme.LeakOutTheme

object LevelCompleteTags {
    const val DIALOG = "win_dialog"
    const val STARS = "win_stars"
    const val EARNED = "win_earned"
    const val PENALTY = "win_penalty"
    const val TOTAL = "win_total"
    const val NEXT = "win_next"
    const val RESTART = "win_restart"
    const val EXIT = "win_exit"
    const val NEW_BEST = "win_new_best"
    const val ACHIEVEMENTS = "win_achievements"
    const val LAYOUT_PREFIX = "win"
    const val LAYOUT_REGULAR = "win_layout_regular"
    const val LAYOUT_COMPACT = "win_layout_compact"
}

/** Adds a test tag, or nothing on the copy that is only measured, so every tag stays unique. */
private typealias Tagger = Modifier.(String) -> Modifier

/**
 * Modal level complete dialog (design-system.md 5.6): the [level] number, "Level Complete!", the
 * stars and the Points, Penalty and Score columns as the [frame] of the tally shows them, then Next
 * Level, Restart and Exit. The buttons are there from the start and enabled once the tally is done;
 * Next Level only when [hasNextLevel]. A tap outside does nothing; Back does nothing during the
 * tally and works as Exit after it. When the win beat the stored highscore ([newBest]), "New best!"
 * takes the place of the message once the tally is done. The achievements the win [unlocked] pop in
 * under the score row once the tally is done; the buttons never wait for them. The dialog uses the
 * Regular layout when it fits the window height and the Compact one otherwise. While the score is
 * sent to the score server ([scorePost]), the status dot shows in the top-left corner.
 */
@Composable
fun LevelCompleteDialog(
    level: Int,
    score: FinalScore,
    frame: WinFrame,
    hasNextLevel: Boolean,
    onNextLevel: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    newBest: Boolean = false,
    unlocked: List<Achievement> = emptyList(),
    scorePost: StateFlow<ScorePostStatus>? = null,
) {
    val paneName = stringResource(R.string.win_pane_title)
    ResultDialogWindow(
        onDismissRequest = onExit,
        dismissOnBackPress = frame.buttonsEnabled,
        panelModifier = Modifier.testTag(LevelCompleteTags.DIALOG).semantics { paneTitle = paneName },
        layoutTagPrefix = LevelCompleteTags.LAYOUT_PREFIX,
        measureRegular = { LevelCompleteMeasured(level, score, frame, newBest, unlocked) },
        // The tracker can answer after the dialog opened; the row's height then counts too.
        measureKey = unlocked.isNotEmpty(),
        cornerOverlay = { scorePost?.let { ScorePostIndicator(it) } },
    ) { layout ->
        LevelCompleteContent(layout, level, score, frame, hasNextLevel, onNextLevel, onRestart, onExit, newBest, unlocked)
    }
}

/** The Regular content with the final values, so the chosen layout does not change during the tally. */
@Composable
private fun LevelCompleteMeasured(level: Int, score: FinalScore, frame: WinFrame, newBest: Boolean, unlocked: List<Achievement>) {
    val finalFrame = frame.copy(penaltyShown = score.penalty, totalShown = score.total, buttonsEnabled = true)
    LevelCompleteContent(ResultDialogLayout.Regular, level, score, finalFrame, true, {}, {}, {}, newBest, unlocked, measuring = true)
}

@Composable
private fun LevelCompleteContent(
    layout: ResultDialogLayout,
    level: Int,
    score: FinalScore,
    frame: WinFrame,
    hasNextLevel: Boolean,
    onNextLevel: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    newBest: Boolean,
    unlocked: List<Achievement>,
    measuring: Boolean = false,
) {
    val tag: Tagger = { if (measuring) this else testTag(it) }
    val showNewBest = newBest && frame.buttonsEnabled
    // Once the tally is done, so it never competes with the counting.
    val showUnlocked = unlocked.isNotEmpty() && frame.buttonsEnabled
    when (layout) {
        ResultDialogLayout.Regular -> RegularColumn {
            WinEyebrow(level)
            Spacer(Modifier.height(8.dp))
            WinHeadline(LeakOutTextStyles.DisplayXl)
            Spacer(Modifier.height(8.dp))
            // In the message's place, so the panel does not grow.
            if (showNewBest) NewBest(Modifier.tag(LevelCompleteTags.NEW_BEST)) else WinMessage()
            Spacer(Modifier.height(16.dp))
            WinStars(lit = frame.starsLit, scales = frame.starScales, modifier = Modifier.tag(LevelCompleteTags.STARS), tagStars = !measuring)
            Spacer(Modifier.height(16.dp))
            WinScoreRow(score, frame, tag)
            if (showUnlocked) {
                Spacer(Modifier.height(12.dp))
                AchievementUnlockedRow(unlocked, Modifier.tag(LevelCompleteTags.ACHIEVEMENTS), animate = !measuring)
            }
            Spacer(Modifier.height(24.dp))
            WinButtons(FlowButtonSize.Large, frame, hasNextLevel, onNextLevel, onRestart, onExit, tag)
        }
        ResultDialogLayout.Compact -> CompactColumns(
            info = {
                WinEyebrow(level)
                Spacer(Modifier.height(4.dp))
                WinHeadline(LeakOutTextStyles.DisplayLg)
                Spacer(Modifier.height(8.dp))
                // As in Regular: "New best!" takes the message's place, so the column does not grow.
                if (showNewBest) NewBest(Modifier.tag(LevelCompleteTags.NEW_BEST)) else WinMessage()
                Spacer(Modifier.height(8.dp))
                WinStars(lit = frame.starsLit, scales = frame.starScales, modifier = Modifier.tag(LevelCompleteTags.STARS), size = 32.dp, tagStars = !measuring)
                Spacer(Modifier.height(12.dp))
                WinScoreRow(score, frame, tag)
                if (showUnlocked) {
                    Spacer(Modifier.height(8.dp))
                    AchievementUnlockedRow(unlocked, Modifier.tag(LevelCompleteTags.ACHIEVEMENTS), animate = !measuring)
                }
            },
            buttons = { WinButtons(FlowButtonSize.Medium, frame, hasNextLevel, onNextLevel, onRestart, onExit, tag) },
        )
    }
}

@Composable
private fun WinEyebrow(level: Int) {
    Text(
        text = stringResource(R.string.result_eyebrow, level).displayUppercase(),
        style = LeakOutTextStyles.DisplayXs,
        color = LeakOutColors.Success500,
    )
}

@Composable
private fun WinHeadline(style: TextStyle) {
    Text(stringResource(R.string.win_title), style = style, color = LeakOutColors.TextPrimary, textAlign = TextAlign.Center)
}

@Composable
private fun WinMessage() {
    Text(
        stringResource(R.string.win_message),
        style = LeakOutTextStyles.BodyMd,
        color = LeakOutColors.TextSecondary,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun NewBest(modifier: Modifier) {
    Text(stringResource(R.string.win_new_best), style = LeakOutTextStyles.DisplaySm, color = LeakOutColors.Brass300, modifier = modifier)
}

/** Points · Penalty · Score in three equal columns; Penalty is "0" in `text.muted` until there is one. */
@Composable
private fun WinScoreRow(score: FinalScore, frame: WinFrame, tag: Tagger) {
    val locale = LocalConfiguration.current.locales[0]
    val earned = HudFormat.score(score.earned, locale)
    val penalty = HudFormat.score(frame.penaltyShown, locale)
    val total = HudFormat.score(frame.totalShown, locale)
    Row(Modifier.fillMaxWidth()) {
        ResultScoreColumn(
            caption = stringResource(R.string.win_earned),
            value = earned,
            valueColor = LeakOutColors.TextPrimary,
            description = stringResource(R.string.win_earned_description, earned),
            modifier = Modifier.weight(1f).tag(LevelCompleteTags.EARNED),
        )
        ResultScoreColumn(
            caption = stringResource(R.string.win_penalty),
            value = if (frame.penaltyShown == 0) penalty else stringResource(R.string.win_penalty_value, penalty),
            valueColor = if (frame.penaltyShown == 0) LeakOutColors.TextMuted else LeakOutColors.Leak500,
            description = stringResource(R.string.win_penalty_description, penalty),
            modifier = Modifier.weight(1f).tag(LevelCompleteTags.PENALTY),
        )
        ResultScoreColumn(
            caption = stringResource(R.string.win_total),
            value = total,
            valueColor = LeakOutColors.Brass300,
            description = stringResource(R.string.win_total_description, total),
            modifier = Modifier.weight(1f).tag(LevelCompleteTags.TOTAL),
            glow = frame.buttonsEnabled,
        )
    }
}

/** Success Next Level, glass Restart and tertiary Exit, stacked 12 dp apart. */
@Composable
private fun ColumnScope.WinButtons(
    size: FlowButtonSize,
    frame: WinFrame,
    hasNextLevel: Boolean,
    onNextLevel: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    tag: Tagger,
) {
    // CompactColumns already spaces its buttons; the Regular column does not.
    val gap = @Composable { if (size == FlowButtonSize.Large) Spacer(Modifier.height(ResultDialogMetrics.ButtonSpacing)) }
    FlowButton(
        text = stringResource(R.string.win_next),
        onClick = onNextLevel,
        variant = FlowButtonVariant.Success,
        size = size,
        enabled = frame.buttonsEnabled && hasNextLevel,
        modifier = Modifier.fillMaxWidth().tag(LevelCompleteTags.NEXT),
    )
    gap()
    FlowButton(
        text = stringResource(R.string.win_restart),
        onClick = onRestart,
        variant = FlowButtonVariant.Secondary,
        size = size,
        enabled = frame.buttonsEnabled,
        modifier = Modifier.fillMaxWidth().tag(LevelCompleteTags.RESTART),
    )
    gap()
    FlowButton(
        text = stringResource(R.string.win_exit),
        onClick = onExit,
        variant = FlowButtonVariant.Tertiary,
        enabled = frame.buttonsEnabled,
        modifier = Modifier.fillMaxWidth().tag(LevelCompleteTags.EXIT),
    )
}

private val PreviewScore = FinalScore(earned = 120, unusedSections = List(4) { Position(it, 0) }, total = 108)

/** The dialog's frame and panel without the dialog window, so previews can render them. */
@Composable
private fun LevelCompletePreview(frame: WinFrame, newBest: Boolean = false, unlocked: List<Achievement> = emptyList()) {
    LeakOutTheme {
        ResultDialogFrame(measureRegular = { LevelCompleteMeasured(7, PreviewScore, frame, newBest, unlocked) }, measureKey = unlocked.isNotEmpty()) { layout ->
            ResultDialogPanel(layout) { LevelCompleteContent(layout, 7, PreviewScore, frame, true, {}, {}, {}, newBest, unlocked) }
        }
    }
}

private val PreviewUnlocked = listOf(Achievement.ZERO_WASTE, Achievement.MEASURE_TWICE, Achievement.STEADY_HANDS, Achievement.PERSONAL_BEST)

@Preview(name = "Level complete, achievements, 640 × 360 (Compact)", widthDp = 640, heightDp = 360, showBackground = true, backgroundColor = 0xFF0C1A2A)
@Composable
private fun LevelCompleteAchievementsCompactPreview() = LevelCompletePreview(doneFrame(), unlocked = PreviewUnlocked)

@Preview(name = "Level complete, achievements, 1280 × 800 (Regular)", widthDp = 1280, heightDp = 800, showBackground = true, backgroundColor = 0xFF0C1A2A)
@Composable
private fun LevelCompleteAchievementsRegularPreview() = LevelCompletePreview(doneFrame(), unlocked = PreviewUnlocked)

private fun doneFrame() = winFrame(WinTally.totalDurationMs(PreviewScore, 2), PreviewScore, stars = 2)

@Preview(name = "Level complete, 640 × 360 (Compact)", widthDp = 640, heightDp = 360, showBackground = true, backgroundColor = 0xFF0C1A2A)
@Composable
private fun LevelCompleteCompactPreview() = LevelCompletePreview(doneFrame())

@Preview(
    name = "Level complete, 640 × 360, 130 % font",
    widthDp = 640,
    heightDp = 360,
    fontScale = 1.3f,
    showBackground = true,
    backgroundColor = 0xFF0C1A2A,
)
@Composable
private fun LevelCompleteLargeFontPreview() = LevelCompletePreview(doneFrame(), newBest = true)

@Preview(name = "Level complete, 1280 × 800 (Regular)", widthDp = 1280, heightDp = 800, showBackground = true, backgroundColor = 0xFF0C1A2A)
@Composable
private fun LevelCompleteRegularPreview() = LevelCompletePreview(doneFrame())

@Preview(name = "Level complete, tally start", widthDp = 640, heightDp = 360, showBackground = true, backgroundColor = 0xFF0C1A2A)
@Composable
private fun LevelCompleteStartPreview() = LevelCompletePreview(winFrame(0, PreviewScore, stars = 2))
