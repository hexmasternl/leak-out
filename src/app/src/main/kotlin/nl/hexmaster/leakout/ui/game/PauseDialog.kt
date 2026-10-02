package nl.hexmaster.leakout.ui.game

import android.os.Build
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.ui.components.FlowButton
import nl.hexmaster.leakout.ui.components.FlowButtonSize
import nl.hexmaster.leakout.ui.components.FlowButtonVariant
import nl.hexmaster.leakout.ui.splash.rememberReducedMotion
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles
import nl.hexmaster.leakout.ui.theme.LeakOutTheme

object PauseTags {
    const val DIALOG = "pause_dialog"
    const val BACKDROP = "pause_backdrop"
    const val TITLE = "pause_title"
    const val RESUME = "pause_resume"
    const val RESTART = "pause_restart"
    const val DIVIDER = "pause_divider"
    const val EXIT = "pause_exit"
}

/**
 * How the pause dialog enters (design-system.md 5.6, 7.2, 7.4). The opaque backdrop fades in over
 * [backdropFadeMs] (0: at once); the panel fades in over [panelFadeMs] and scales from [initialScale]
 * to 1 with [scaleSpec] (null: no scale).
 */
data class PauseDialogEntry(
    val backdropFadeMs: Int,
    val panelFadeMs: Int,
    val initialScale: Float,
    val scaleSpec: AnimationSpec<Float>?,
) {
    companion object {
        /** `motion.quick`. */
        const val FADE_MS = 160

        fun select(reducedMotion: Boolean): PauseDialogEntry = when {
            // The game is hidden at once; the panel fades in without scaling.
            reducedMotion -> PauseDialogEntry(backdropFadeMs = 0, panelFadeMs = FADE_MS, initialScale = 1f, scaleSpec = null)
            else -> PauseDialogEntry(
                backdropFadeMs = FADE_MS,
                panelFadeMs = FADE_MS,
                initialScale = ResultDialogEntry.INITIAL_SCALE,
                scaleSpec = ResultDialogEntry.PopSpring,
            )
        }
    }
}

/** The opaque pause backdrop: a diagonal gradient from `surface.abyss` (top left) to `surface.deep` (bottom right). */
val PauseBackdropBrush: Brush = Brush.linearGradient(
    colors = listOf(LeakOutColors.SurfaceAbyss, LeakOutColors.SurfaceDeep),
    start = Offset.Zero,
    end = Offset.Infinite,
)

/**
 * Modal pause menu: Resume, Restart, a separator and Exit, on an opaque backdrop that hides the
 * frozen game entirely, so a pause cannot be used to study the board. Back works as Resume; a tap on
 * the backdrop does nothing so the player cannot close it by accident. Closing is immediate.
 */
@Composable
fun PauseDialog(onResume: () -> Unit, onRestart: () -> Unit, onExit: () -> Unit) {
    val reducedMotion = rememberReducedMotion()
    Dialog(
        onDismissRequest = onResume,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.apply {
                // The backdrop is opaque by itself; the platform dim would darken it off its tokens.
                setDimAmount(0f)
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    attributes = attributes.apply {
                        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }
            }
        }
        PauseDialogContent(onResume, onRestart, onExit, entry = remember(reducedMotion) { PauseDialogEntry.select(reducedMotion) })
    }
}

/** The backdrop and the centred panel, with the entry motion; the dialog window around it is [PauseDialog]. */
@Composable
internal fun PauseDialogContent(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    entry: PauseDialogEntry,
    modifier: Modifier = Modifier,
) {
    val backdropAlpha = remember { Animatable(if (entry.backdropFadeMs == 0) 1f else 0f) }
    val panelAlpha = remember { Animatable(if (entry.panelFadeMs == 0) 1f else 0f) }
    val scale = remember { Animatable(entry.initialScale) }
    LaunchedEffect(entry) {
        if (entry.backdropFadeMs > 0) backdropAlpha.animateTo(1f, tween(entry.backdropFadeMs, easing = LinearOutSlowInEasing))
    }
    LaunchedEffect(entry) {
        if (entry.panelFadeMs > 0) panelAlpha.animateTo(1f, tween(entry.panelFadeMs, easing = LinearOutSlowInEasing))
    }
    LaunchedEffect(entry) {
        entry.scaleSpec?.let { scale.animateTo(1f, it) }
    }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .testTag(PauseTags.BACKDROP)
                .fillMaxSize()
                .graphicsLayer { alpha = backdropAlpha.value }
                .background(PauseBackdropBrush)
                // Taps on the backdrop are swallowed: they neither close the dialog nor reach the game.
                .pointerInput(Unit) { detectTapGestures { } },
        )
        PausePanel(
            onResume = onResume,
            onRestart = onRestart,
            onExit = onExit,
            modifier = Modifier
                .padding(ResultDialogMetrics.Gutter)
                .semantics { panelScale = scale.value }
                .graphicsLayer {
                    alpha = panelAlpha.value
                    scaleX = scale.value
                    scaleY = scale.value
                },
        )
    }
}

/**
 * The level 3 panel of design-system.md 5.6: title "Paused", brass Resume, glass Restart, a
 * `line.subtle` separator and destructive Exit, Medium buttons so it fits a 360 dp high screen.
 */
@Composable
private fun PausePanel(onResume: () -> Unit, onRestart: () -> Unit, onExit: () -> Unit, modifier: Modifier = Modifier) {
    val paneName = stringResource(R.string.hud_pause_menu)
    ResultDialogPanel(
        layout = ResultDialogLayout.Regular,
        modifier = modifier
            .testTag(PauseTags.DIALOG)
            .semantics { paneTitle = paneName },
    ) {
        Column(
            modifier = Modifier.padding(ResultDialogMetrics.Padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ResultDialogMetrics.ButtonSpacing),
        ) {
            Text(
                text = stringResource(R.string.pause_title),
                style = LeakOutTextStyles.DisplayLg,
                color = LeakOutColors.TextPrimary,
                modifier = Modifier.testTag(PauseTags.TITLE),
            )
            FlowButton(
                text = stringResource(R.string.pause_resume),
                onClick = onResume,
                variant = FlowButtonVariant.Primary,
                size = FlowButtonSize.Medium,
                modifier = Modifier.fillMaxWidth().testTag(PauseTags.RESUME),
            )
            FlowButton(
                text = stringResource(R.string.pause_restart),
                onClick = onRestart,
                variant = FlowButtonVariant.Secondary,
                size = FlowButtonSize.Medium,
                modifier = Modifier.fillMaxWidth().testTag(PauseTags.RESTART),
            )
            Box(
                Modifier
                    .testTag(PauseTags.DIVIDER)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(LeakOutColors.LineSubtle),
            )
            FlowButton(
                text = stringResource(R.string.pause_exit),
                onClick = onExit,
                variant = FlowButtonVariant.Destructive,
                size = FlowButtonSize.Medium,
                modifier = Modifier.fillMaxWidth().testTag(PauseTags.EXIT),
            )
        }
    }
}

@Preview(name = "Pause dialog, 640 × 360", widthDp = 640, heightDp = 360)
@Composable
private fun PauseDialogPreview() {
    LeakOutTheme {
        // At rest: no fade or scale, so the static preview shows the final look.
        PauseDialogContent(onResume = {}, onRestart = {}, onExit = {}, entry = PauseDialogEntry(0, 0, 1f, null))
    }
}
