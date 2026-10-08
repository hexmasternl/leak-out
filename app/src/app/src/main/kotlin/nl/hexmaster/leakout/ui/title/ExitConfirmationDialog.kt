package nl.hexmaster.leakout.ui.title

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
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

/** Stable handles for UI tests, independent of copy and layout. */
object ExitDialogTags {
    const val DIALOG = "exit_dialog"
    const val NEVER_SHOW = "exit_dialog_never_show"
    const val YES = "exit_dialog_yes"
    const val CANCEL = "exit_dialog_cancel"
}

/**
 * Asks before the welcome screen's Exit closes the app. Yes and Cancel pass whether "Never show
 * this again" is ticked; a tap outside and Back go to [onDismiss], which is not a choice and so
 * carries no checkbox state. The checkbox starts unticked every time the dialog is shown.
 */
@Composable
fun ExitConfirmationDialog(
    onConfirm: (neverShowAgain: Boolean) -> Unit,
    onCancel: (neverShowAgain: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
    ) {
        ExitConfirmationPanel(onConfirm = onConfirm, onCancel = onCancel)
    }
}

/** The panel inside the dialog window, in the style of the settings' reset confirmation. */
@Composable
private fun ExitConfirmationPanel(
    onConfirm: (neverShowAgain: Boolean) -> Unit,
    onCancel: (neverShowAgain: Boolean) -> Unit,
) {
    // Leaves the composition with the dialog, so every opening starts unticked.
    var neverShowAgain by rememberSaveable { mutableStateOf(false) }
    val title = stringResource(R.string.exit_dialog_title)
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .testTag(ExitDialogTags.DIALOG)
            .semantics { paneTitle = title }
            .widthIn(max = 360.dp)
            .background(LeakOutColors.SurfaceBase, shape)
            .border(1.dp, LeakOutColors.LineSubtle, shape)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = title, style = LeakOutTextStyles.DisplayLg, color = LeakOutColors.TextPrimary)
        Text(
            text = stringResource(R.string.exit_dialog_body),
            style = LeakOutTextStyles.BodyLg,
            color = LeakOutColors.TextSecondary,
        )
        // The whole row toggles, so the target is larger than the box itself.
        Row(
            modifier = Modifier
                .testTag(ExitDialogTags.NEVER_SHOW)
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(value = neverShowAgain, role = Role.Checkbox, onValueChange = { neverShowAgain = it }),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = neverShowAgain,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = LeakOutColors.Brass500,
                    uncheckedColor = LeakOutColors.Steel300,
                    checkmarkColor = LeakOutColors.TextOnBrass,
                ),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.exit_dialog_never_show),
                style = LeakOutTextStyles.BodyLg,
                color = LeakOutColors.TextPrimary,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            CartoonButton(
                text = stringResource(R.string.exit_dialog_cancel),
                onClick = { onCancel(neverShowAgain) },
                variant = CartoonButtonVariant.Secondary,
                faceHeight = 48.dp,
                modifier = Modifier.weight(1f).testTag(ExitDialogTags.CANCEL),
            )
            CartoonButton(
                text = stringResource(R.string.exit_dialog_yes),
                onClick = { onConfirm(neverShowAgain) },
                variant = CartoonButtonVariant.Danger,
                faceHeight = 48.dp,
                modifier = Modifier.weight(1f).testTag(ExitDialogTags.YES),
            )
        }
    }
}

@Preview(name = "Exit confirmation", widthDp = 640, heightDp = 360)
@Composable
private fun ExitConfirmationPanelPreview() {
    LeakOutTheme {
        ExitConfirmationPanel(onConfirm = {}, onCancel = {})
    }
}
