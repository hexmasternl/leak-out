package nl.hexmaster.leakout.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.ui.components.CartoonButton
import nl.hexmaster.leakout.ui.components.CartoonButtonVariant
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles
import nl.hexmaster.leakout.ui.theme.LeakOutTheme

object LevelLostTags {
    const val DIALOG = "lost_dialog"
    const val SCORE = "lost_score"
    const val RESTART = "lost_restart"
    const val EXIT = "lost_exit"
}

/**
 * Modal level lost dialog: "Leaked!", a leaking pipe, the message, the [score] of the attempt, then
 * Restart and Exit. Back works as Exit; a tap outside does nothing. There is no Resume.
 */
@Composable
fun LevelLostDialog(score: Int, onRestart: () -> Unit, onExit: () -> Unit) {
    Dialog(
        onDismissRequest = onExit,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false),
    ) {
        LevelLostCard(score, onRestart, onExit)
    }
}

@Composable
private fun LevelLostCard(score: Int, onRestart: () -> Unit, onExit: () -> Unit) {
    val paneName = stringResource(R.string.lost_pane_title)
    val locale = LocalConfiguration.current.locales[0]
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .testTag(LevelLostTags.DIALOG)
            .semantics { paneTitle = paneName }
            .widthIn(max = 360.dp)
            .background(LeakOutColors.SurfaceCard, shape)
            .border(3.dp, LeakOutColors.InkDark, shape)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.lost_title).uppercase(),
            style = LeakOutTextStyles.Header1,
            color = LeakOutColors.ErrorRed,
        )
        LeakingPipe(Modifier.size(width = 96.dp, height = 56.dp))
        Text(
            text = stringResource(R.string.lost_message),
            style = LeakOutTextStyles.Header2,
            color = LeakOutColors.InkDark,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.lost_score, HudFormat.score(score, locale)),
            style = LeakOutTextStyles.Header2,
            color = LeakOutColors.InkLight,
            modifier = Modifier.testTag(LevelLostTags.SCORE),
        )
        CartoonButton(
            text = stringResource(R.string.lost_restart),
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(LevelLostTags.RESTART),
        )
        CartoonButton(
            text = stringResource(R.string.lost_exit),
            onClick = onExit,
            variant = CartoonButtonVariant.Danger,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(LevelLostTags.EXIT),
        )
    }
}

/** A steel pipe stub from the left with liquid dripping out of its open right end. Decoration only. */
@Composable
private fun LeakingPipe(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val pipeY = size.height * 0.35f
        val pipeEnd = size.width * 0.55f
        val thickness = size.height * 0.34f
        drawLine(LeakOutColors.Steel700, Offset(0f, pipeY), Offset(pipeEnd, pipeY), thickness + 6.dp.toPx(), StrokeCap.Butt)
        drawLine(LeakOutColors.Steel300, Offset(0f, pipeY), Offset(pipeEnd, pipeY), thickness, StrokeCap.Butt)
        drawLine(LeakOutColors.Liquid500, Offset(pipeEnd, pipeY), Offset(pipeEnd + size.width * 0.12f, pipeY + thickness * 0.2f), thickness * 0.5f, StrokeCap.Round)
        val drops = listOf(0.72f to 0.55f, 0.82f to 0.75f, 0.66f to 0.9f)
        for ((x, y) in drops) {
            val center = Offset(size.width * x, size.height * y)
            drawCircle(LeakOutColors.Liquid500, size.height * 0.08f, center)
            drawCircle(LeakOutColors.LiquidGlow, size.height * 0.03f, center - Offset(2.dp.toPx(), 2.dp.toPx()))
        }
    }
}

@Preview(name = "Level lost", widthDp = 400, heightDp = 480)
@Composable
private fun LevelLostDialogPreview() {
    LeakOutTheme {
        LevelLostCard(score = 85, onRestart = {}, onExit = {})
    }
}
