package nl.hexmaster.leakout.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.ui.components.CartoonButton
import nl.hexmaster.leakout.ui.components.CartoonButtonVariant
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles

object PauseTags {
    const val DIALOG = "pause_dialog"
    const val RESUME = "pause_resume"
    const val RESTART = "pause_restart"
    const val DIVIDER = "pause_divider"
    const val EXIT = "pause_exit"
}

/**
 * Modal pause menu: Resume, Restart, a separator and Exit. Back works as Resume; a tap outside
 * does nothing so the player cannot close it by accident.
 */
@Composable
fun PauseDialog(onResume: () -> Unit, onRestart: () -> Unit, onExit: () -> Unit) {
    val paneName = stringResource(R.string.hud_pause_menu)
    val shape = RoundedCornerShape(16.dp)
    Dialog(
        onDismissRequest = onResume,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false),
    ) {
        Column(
            modifier = Modifier
                .testTag(PauseTags.DIALOG)
                .semantics { paneTitle = paneName }
                .widthIn(max = 360.dp)
                .background(LeakOutColors.SurfaceCard, shape)
                .border(3.dp, LeakOutColors.InkDark, shape)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.pause_title).uppercase(),
                style = LeakOutTextStyles.Header1,
                color = LeakOutColors.InkDark,
            )
            CartoonButton(
                text = stringResource(R.string.pause_resume),
                onClick = onResume,
                modifier = Modifier.fillMaxWidth().testTag(PauseTags.RESUME),
            )
            CartoonButton(
                text = stringResource(R.string.pause_restart),
                onClick = onRestart,
                variant = CartoonButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth().testTag(PauseTags.RESTART),
            )
            HorizontalDivider(
                thickness = 2.dp,
                color = LeakOutColors.SteelLight,
                modifier = Modifier.testTag(PauseTags.DIVIDER),
            )
            CartoonButton(
                text = stringResource(R.string.pause_exit),
                onClick = onExit,
                variant = CartoonButtonVariant.Danger,
                modifier = Modifier.fillMaxWidth().testTag(PauseTags.EXIT),
            )
        }
    }
}
