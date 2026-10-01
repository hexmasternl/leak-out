package nl.hexmaster.leakout.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/**
 * Confirms wiping the level history. Cancel, a tap outside and Back all go to [onDismiss], so the
 * safe choice is always the easy one; only Reset calls [onConfirm].
 */
@Composable
fun ResetProgressDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val title = stringResource(R.string.settings_reset_title)
    val shape = RoundedCornerShape(SettingsMetrics.PanelRadius)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
    ) {
        Column(
            modifier = Modifier
                .testTag(SettingsTags.RESET_DIALOG)
                .semantics { paneTitle = title }
                .widthIn(max = 360.dp)
                .background(LeakOutColors.SurfaceBase, shape)
                .border(1.dp, LeakOutColors.LineSubtle, shape)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = title, style = LeakOutTextStyles.DisplayLg, color = LeakOutColors.TextPrimary)
            Text(
                text = stringResource(R.string.settings_reset_body),
                style = LeakOutTextStyles.BodyLg,
                color = LeakOutColors.TextSecondary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                CartoonButton(
                    text = stringResource(R.string.settings_reset_cancel),
                    onClick = onDismiss,
                    variant = CartoonButtonVariant.Secondary,
                    faceHeight = 48.dp,
                    modifier = Modifier.weight(1f).testTag(SettingsTags.RESET_CANCEL),
                )
                CartoonButton(
                    text = stringResource(R.string.settings_reset_confirm),
                    onClick = onConfirm,
                    variant = CartoonButtonVariant.Danger,
                    faceHeight = 48.dp,
                    modifier = Modifier.weight(1f).testTag(SettingsTags.RESET_CONFIRM),
                )
            }
        }
    }
}
