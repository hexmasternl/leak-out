package nl.hexmaster.leakout.ui.title

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.ui.components.DialogEntry
import nl.hexmaster.leakout.ui.components.FloatingPanel
import nl.hexmaster.leakout.ui.components.FloatingPanelMetrics
import nl.hexmaster.leakout.ui.components.FlowButton
import nl.hexmaster.leakout.ui.components.FlowButtonSize
import nl.hexmaster.leakout.ui.components.FlowButtonVariant
import nl.hexmaster.leakout.ui.components.panelScale
import nl.hexmaster.leakout.ui.splash.rememberReducedMotion
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles
import nl.hexmaster.leakout.ui.theme.LeakOutTheme

object PlayMenuTags {
    const val DIALOG = "play_menu_dialog"
    const val SCRIM = "play_menu_scrim"
    const val TITLE = "play_menu_title"
    const val CONTINUE = "play_menu_continue"
    const val SELECT_LEVEL = "play_menu_select_level"
}

/**
 * Modal choice after Play: Continue or Select Level (design-system.md 5.6, "Play menu"). Back and a
 * tap on the scrim close it through [onDismiss] without navigating.
 */
@Composable
fun PlayMenuDialog(
    onContinue: () -> Unit,
    onSelectLevel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val reducedMotion = rememberReducedMotion()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            // The window fills the screen, so the scrim takes over the tap outside.
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        // The scrim is drawn below; the platform dim would darken it off its token.
        SideEffect { window?.setDimAmount(0f) }
        PlayMenuDialogContent(
            onContinue = onContinue,
            onSelectLevel = onSelectLevel,
            onDismiss = onDismiss,
            entry = remember(reducedMotion) { DialogEntry.select(reducedMotion) },
        )
    }
}

/** The scrim and the centred panel, with the entry motion; the dialog window around it is [PlayMenuDialog]. */
@Composable
internal fun PlayMenuDialogContent(
    onContinue: () -> Unit,
    onSelectLevel: () -> Unit,
    onDismiss: () -> Unit,
    entry: DialogEntry,
    modifier: Modifier = Modifier,
) {
    val alpha = remember { Animatable(if (entry.fadeMs == 0) 1f else 0f) }
    val scale = remember { Animatable(entry.initialScale) }
    LaunchedEffect(entry) {
        if (entry.fadeMs > 0) alpha.animateTo(1f, tween(entry.fadeMs, easing = LinearOutSlowInEasing))
    }
    LaunchedEffect(entry) {
        entry.scaleSpec?.let { scale.animateTo(1f, it) }
    }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .testTag(PlayMenuTags.SCRIM)
                .fillMaxSize()
                .graphicsLayer { this.alpha = alpha.value }
                .background(LeakOutColors.Scrim)
                .pointerInput(onDismiss) { detectTapGestures { onDismiss() } },
        )
        PlayMenuPanel(
            onContinue = onContinue,
            onSelectLevel = onSelectLevel,
            modifier = Modifier
                .padding(FloatingPanelMetrics.Gutter)
                .semantics { panelScale = scale.value }
                .graphicsLayer {
                    this.alpha = alpha.value
                    scaleX = scale.value
                    scaleY = scale.value
                },
        )
    }
}

/** The level 3 panel: title "Play", brass Continue and glass Select Level, Medium buttons. */
@Composable
private fun PlayMenuPanel(onContinue: () -> Unit, onSelectLevel: () -> Unit, modifier: Modifier = Modifier) {
    val paneName = stringResource(R.string.play_menu_title)
    FloatingPanel(
        modifier = modifier
            .testTag(PlayMenuTags.DIALOG)
            .semantics { paneTitle = paneName }
            // Taps inside the panel but off the buttons must not reach the scrim and close it.
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        Column(
            modifier = Modifier.padding(FloatingPanelMetrics.Padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FloatingPanelMetrics.ButtonSpacing),
        ) {
            Text(
                text = paneName,
                style = LeakOutTextStyles.DisplayLg,
                color = LeakOutColors.TextPrimary,
                modifier = Modifier.testTag(PlayMenuTags.TITLE),
            )
            FlowButton(
                text = stringResource(R.string.play_menu_continue),
                onClick = onContinue,
                variant = FlowButtonVariant.Primary,
                size = FlowButtonSize.Medium,
                modifier = Modifier.fillMaxWidth().testTag(PlayMenuTags.CONTINUE),
            )
            FlowButton(
                text = stringResource(R.string.play_menu_select_level),
                onClick = onSelectLevel,
                variant = FlowButtonVariant.Secondary,
                size = FlowButtonSize.Medium,
                modifier = Modifier.fillMaxWidth().testTag(PlayMenuTags.SELECT_LEVEL),
            )
        }
    }
}

@Preview(name = "Play menu, 640 × 360", widthDp = 640, heightDp = 360)
@Composable
private fun PlayMenuDialogPreview() {
    LeakOutTheme {
        // At rest: no fade or scale, so the static preview shows the final look.
        PlayMenuDialogContent(onContinue = {}, onSelectLevel = {}, onDismiss = {}, entry = DialogEntry(1f, null, 0))
    }
}
