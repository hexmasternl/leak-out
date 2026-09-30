package nl.hexmaster.leakout.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.score.FinalScore
import nl.hexmaster.leakout.ui.components.CartoonButton
import nl.hexmaster.leakout.ui.components.CartoonButtonVariant
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
}

/**
 * Modal level complete dialog: the celebration, the stars and the three score rows as the [frame]
 * of the tally shows them, then Next level, Restart and Exit. The buttons are there from the start
 * and enabled once the tally is done; Next level only when [hasNextLevel]. A tap outside does
 * nothing; Back does nothing during the tally and works as Exit after it. When the win beat the
 * stored highscore ([newBest]), "New best!" takes the place of the message once the tally is done.
 */
@Composable
fun LevelCompleteDialog(
    score: FinalScore,
    frame: WinFrame,
    hasNextLevel: Boolean,
    onNextLevel: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    newBest: Boolean = false,
) {
    Dialog(
        onDismissRequest = onExit,
        properties = DialogProperties(dismissOnBackPress = frame.buttonsEnabled, dismissOnClickOutside = false),
    ) {
        LevelCompleteCard(score, frame, hasNextLevel, onNextLevel, onRestart, onExit, newBest)
    }
}

@Composable
private fun LevelCompleteCard(
    score: FinalScore,
    frame: WinFrame,
    hasNextLevel: Boolean,
    onNextLevel: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    newBest: Boolean = false,
) {
    val paneName = stringResource(R.string.win_pane_title)
    val locale = LocalConfiguration.current.locales[0]
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .testTag(LevelCompleteTags.DIALOG)
            .semantics { paneTitle = paneName }
            .widthIn(max = 560.dp)
            .background(LeakOutColors.SurfaceCard, shape)
            .border(3.dp, LeakOutColors.InkDark, shape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // The game runs in landscape, down to 360 dp high, so the card stays short: the celebration
        // and the stars on the left, the score on the right, the buttons across the bottom.
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.win_eyebrow).uppercase(),
                    style = LeakOutTextStyles.ButtonText,
                    color = LeakOutColors.SuccessGreen,
                )
                Text(
                    text = stringResource(R.string.win_title),
                    style = LeakOutTextStyles.Header1,
                    color = LeakOutColors.InkDark,
                )
                // In the message's place, so the card does not grow.
                if (newBest && frame.buttonsEnabled) {
                    Text(
                        text = stringResource(R.string.win_new_best),
                        style = LeakOutTextStyles.Header2,
                        color = LeakOutColors.SuccessGreen,
                        modifier = Modifier.testTag(LevelCompleteTags.NEW_BEST),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.win_message),
                        style = LeakOutTextStyles.Header2,
                        color = LeakOutColors.InkLight,
                    )
                }
                WinStars(
                    lit = frame.starsLit,
                    scales = frame.starScales,
                    modifier = Modifier.padding(top = 6.dp).testTag(LevelCompleteTags.STARS),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ScoreRow(
                    label = stringResource(R.string.win_earned),
                    value = HudFormat.score(score.earned, locale),
                    description = stringResource(R.string.win_earned_description, HudFormat.score(score.earned, locale)),
                    tag = LevelCompleteTags.EARNED,
                )
                val penalty = HudFormat.score(frame.penaltyShown, locale)
                ScoreRow(
                    label = stringResource(R.string.win_penalty),
                    value = stringResource(R.string.win_penalty_value, penalty),
                    description = stringResource(R.string.win_penalty_description, penalty),
                    tag = LevelCompleteTags.PENALTY,
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(LeakOutColors.InkDark, RoundedCornerShape(2.dp)),
                )
                ScoreRow(
                    label = stringResource(R.string.win_total),
                    value = HudFormat.score(frame.totalShown, locale),
                    description = stringResource(R.string.win_total_description, HudFormat.score(frame.totalShown, locale)),
                    tag = LevelCompleteTags.TOTAL,
                    valueStyle = LeakOutTextStyles.Header1,
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CartoonButton(
                text = stringResource(R.string.win_next),
                onClick = onNextLevel,
                variant = CartoonButtonVariant.Success,
                enabled = frame.buttonsEnabled && hasNextLevel,
                modifier = Modifier.weight(1.3f).heightIn(min = 48.dp).testTag(LevelCompleteTags.NEXT),
            )
            CartoonButton(
                text = stringResource(R.string.win_restart),
                onClick = onRestart,
                enabled = frame.buttonsEnabled,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag(LevelCompleteTags.RESTART),
            )
            CartoonButton(
                text = stringResource(R.string.win_exit),
                onClick = onExit,
                variant = CartoonButtonVariant.Danger,
                enabled = frame.buttonsEnabled,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag(LevelCompleteTags.EXIT),
            )
        }
    }
}

/** A label on the left and its right-aligned value, read as one: "Total 108". */
@Composable
private fun ScoreRow(
    label: String,
    value: String,
    description: String,
    tag: String,
    valueStyle: TextStyle = LeakOutTextStyles.Header2,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = LeakOutTextStyles.Header2, color = LeakOutColors.InkLight, modifier = Modifier.weight(1f))
        Text(value, style = valueStyle, color = LeakOutColors.InkDark)
    }
}

private val PreviewScore = FinalScore(earned = 120, unusedSections = List(4) { Position(it, 0) }, total = 108)

@Preview(name = "Level complete, start", widthDp = 640, heightDp = 360)
@Composable
private fun LevelCompleteStartPreview() {
    LeakOutTheme {
        LevelCompleteCard(PreviewScore, winFrame(0, PreviewScore, stars = 2), true, {}, {}, {})
    }
}

@Preview(name = "Level complete, done with 2 stars", widthDp = 640, heightDp = 360)
@Composable
private fun LevelCompleteDonePreview() {
    LeakOutTheme {
        val frame = winFrame(WinTally.totalDurationMs(PreviewScore, 2), PreviewScore, stars = 2)
        LevelCompleteCard(PreviewScore, frame, true, {}, {}, {})
    }
}
