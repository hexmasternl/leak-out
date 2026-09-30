package nl.hexmaster.leakout.ui.title

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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

object PlayMenuTags {
    const val DIALOG = "play_menu_dialog"
    const val CONTINUE = "play_menu_continue"
    const val SELECT_LEVEL = "play_menu_select_level"
}

/**
 * Modal choice after Play: Continue or Select level. Back and a tap outside close it through
 * [onDismiss] without navigating.
 */
@Composable
fun PlayMenuDialog(
    onContinue: () -> Unit,
    onSelectLevel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val paneName = stringResource(R.string.play_menu_title)
    val shape = RoundedCornerShape(16.dp)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
    ) {
        Column(
            modifier = Modifier
                .testTag(PlayMenuTags.DIALOG)
                .semantics { paneTitle = paneName }
                .widthIn(max = 360.dp)
                .background(LeakOutColors.SurfaceCard, shape)
                .border(3.dp, LeakOutColors.InkDark, shape)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = paneName.uppercase(),
                style = LeakOutTextStyles.Header1,
                color = LeakOutColors.InkDark,
            )
            CartoonButton(
                text = stringResource(R.string.play_menu_continue),
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().testTag(PlayMenuTags.CONTINUE),
            )
            CartoonButton(
                text = stringResource(R.string.play_menu_select_level),
                onClick = onSelectLevel,
                variant = CartoonButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth().testTag(PlayMenuTags.SELECT_LEVEL),
            )
        }
    }
}
