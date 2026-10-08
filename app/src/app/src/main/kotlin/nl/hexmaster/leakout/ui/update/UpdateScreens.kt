package nl.hexmaster.leakout.ui.update

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
import nl.hexmaster.leakout.ui.title.WelcomeBackground

/** Stable handles for UI tests, independent of copy and layout. */
object UpdateTags {
    const val BLOCKING_SCREEN = "update_blocking_screen"
    const val BLOCKING_UPDATE = "update_blocking_update"
    const val BLOCKING_CLOSE = "update_blocking_close"
    const val OPTIONAL_DIALOG = "update_optional_dialog"
    const val OPTIONAL_UPDATE = "update_optional_update"
    const val OPTIONAL_LATER = "update_optional_later"
    const val INSTALL_DIALOG = "update_install_dialog"
    const val INSTALL_RESTART = "update_install_restart"
    const val INSTALL_LATER = "update_install_later"
}

/**
 * Replaces every screen outside a level while a required update is known (openspec/specs/app-updates).
 * It offers only to update, through Google Play, and to close the app; Back closes the app too.
 */
@Composable
fun RequiredUpdateScreen(onUpdate: () -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize().background(LeakOutColors.SurfaceDeep).testTag(UpdateTags.BLOCKING_SCREEN)) {
        WelcomeBackground(Modifier.fillMaxSize())
        Box(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            UpdatePanel(
                title = stringResource(R.string.update_required_title),
                body = stringResource(R.string.update_required_body),
                tag = null,
            ) {
                CartoonButton(
                    text = stringResource(R.string.update_required_close),
                    onClick = onClose,
                    variant = CartoonButtonVariant.Secondary,
                    faceHeight = 48.dp,
                    modifier = Modifier.weight(1f).testTag(UpdateTags.BLOCKING_CLOSE),
                )
                CartoonButton(
                    text = stringResource(R.string.update_required_update),
                    onClick = onUpdate,
                    variant = CartoonButtonVariant.Primary,
                    faceHeight = 48.dp,
                    modifier = Modifier.weight(1f).testTag(UpdateTags.BLOCKING_UPDATE),
                )
            }
        }
    }
}

/** Offers an optional update at start; Back, a tap outside and Not now all dismiss it. */
@Composable
fun OptionalUpdateDialog(onUpdate: () -> Unit, onDismiss: () -> Unit) {
    UpdateDialog(
        title = stringResource(R.string.update_optional_title),
        body = stringResource(R.string.update_optional_body),
        tag = UpdateTags.OPTIONAL_DIALOG,
        later = stringResource(R.string.update_optional_later) to UpdateTags.OPTIONAL_LATER,
        accept = stringResource(R.string.update_optional_update) to UpdateTags.OPTIONAL_UPDATE,
        onAccept = onUpdate,
        onDismiss = onDismiss,
    )
}

/** Offers to restart and install a downloaded optional update; never shown during a level. */
@Composable
fun InstallUpdateDialog(onRestart: () -> Unit, onDismiss: () -> Unit) {
    UpdateDialog(
        title = stringResource(R.string.update_install_title),
        body = stringResource(R.string.update_install_body),
        tag = UpdateTags.INSTALL_DIALOG,
        later = stringResource(R.string.update_install_later) to UpdateTags.INSTALL_LATER,
        accept = stringResource(R.string.update_install_restart) to UpdateTags.INSTALL_RESTART,
        onAccept = onRestart,
        onDismiss = onDismiss,
    )
}

@Composable
private fun UpdateDialog(
    title: String,
    body: String,
    tag: String,
    later: Pair<String, String>,
    accept: Pair<String, String>,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
    ) {
        UpdatePanel(title = title, body = body, tag = tag) {
            CartoonButton(
                text = later.first,
                onClick = onDismiss,
                variant = CartoonButtonVariant.Secondary,
                faceHeight = 48.dp,
                modifier = Modifier.weight(1f).testTag(later.second),
            )
            CartoonButton(
                text = accept.first,
                onClick = onAccept,
                variant = CartoonButtonVariant.Primary,
                faceHeight = 48.dp,
                modifier = Modifier.weight(1f).testTag(accept.second),
            )
        }
    }
}

/** The panel of every update screen, in the style of the exit confirmation. */
@Composable
private fun UpdatePanel(
    title: String,
    body: String,
    tag: String?,
    buttons: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .semantics { paneTitle = title }
            .widthIn(max = 400.dp)
            .background(LeakOutColors.SurfaceBase, shape)
            .border(1.dp, LeakOutColors.LineSubtle, shape)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = title, style = LeakOutTextStyles.DisplayLg, color = LeakOutColors.TextPrimary)
        Text(text = body, style = LeakOutTextStyles.BodyLg, color = LeakOutColors.TextSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth(), content = buttons)
    }
}

@Preview(name = "Required update", widthDp = 640, heightDp = 360)
@Composable
private fun RequiredUpdateScreenPreview() {
    LeakOutTheme { RequiredUpdateScreen(onUpdate = {}, onClose = {}) }
}

@Preview(name = "Optional update", widthDp = 640, heightDp = 360)
@Composable
private fun OptionalUpdatePanelPreview() {
    LeakOutTheme {
        OptionalUpdateDialog(onUpdate = {}, onDismiss = {})
    }
}
