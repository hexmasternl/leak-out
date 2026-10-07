package nl.hexmaster.leakout.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.ui.components.DialogEntry
import nl.hexmaster.leakout.ui.components.FloatingPanel
import nl.hexmaster.leakout.ui.components.FloatingPanelMetrics
import nl.hexmaster.leakout.ui.components.FlowButton
import nl.hexmaster.leakout.ui.components.FlowButtonMetrics
import nl.hexmaster.leakout.ui.components.FlowButtonVariant
import nl.hexmaster.leakout.ui.components.displayUppercase
import nl.hexmaster.leakout.ui.components.panelScale
import nl.hexmaster.leakout.ui.splash.rememberReducedMotion
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles

/**
 * The two layouts of the result dialogs (design-system.md 5.6): one centred column when it fits the
 * window height, otherwise two columns with the information left and the buttons right.
 */
enum class ResultDialogLayout {
    Regular,
    Compact,
    ;

    companion object {
        /** Regular when its measured [regularHeightPx] fits in [availableHeightPx], else Compact. */
        fun choose(availableHeightPx: Int, regularHeightPx: Int): ResultDialogLayout =
            if (regularHeightPx <= availableHeightPx) Regular else Compact
    }
}

/** Sizes of the result dialog layouts, from design-system.md 5.6; the shared panel sizes come from [FloatingPanelMetrics]. */
object ResultDialogMetrics {
    val Gutter: Dp = FloatingPanelMetrics.Gutter
    val Padding: Dp = FloatingPanelMetrics.Padding
    val RegularMaxWidth: Dp = FloatingPanelMetrics.MaxWidth
    val CompactMaxWidth: Dp = 600.dp
    val ButtonsColumnWidth: Dp = 200.dp
    val ColumnGap: Dp = 24.dp
    val ButtonSpacing: Dp = FloatingPanelMetrics.ButtonSpacing

    /** The horizontal padding of two buttons sharing one row (Restart + Top 10, Top 10 + Exit), so their labels fit. */
    val PairedButtonPadding: Dp = 8.dp

    /** Distance of the corner overlay (the score status dot) from the window's safe edges. */
    val CornerOverlayInset: Dp = 12.dp
}

/**
 * The window of a result dialog: a full-window, see-through `surface.scrim` (platform dim off, so the
 * field stays visible behind it) with the entry motion, and the panel in the [ResultDialogLayout]
 * chosen by [ResultDialogFrame]. [content] draws the panel's content for the chosen layout;
 * [measureRegular] draws the Regular content with its final values, for measuring only; it is
 * measured again when [measureKey] changes (content that arrives later, such as the achievement row).
 * [cornerOverlay] is drawn in the window's top-left corner, above the scrim and outside the panel.
 */
@Composable
internal fun ResultDialogWindow(
    onDismissRequest: () -> Unit,
    dismissOnBackPress: Boolean,
    panelModifier: Modifier,
    layoutTagPrefix: String,
    measureRegular: @Composable () -> Unit,
    measureKey: Any? = null,
    reducedMotion: Boolean = rememberReducedMotion(),
    cornerOverlay: @Composable () -> Unit = {},
    content: @Composable (ResultDialogLayout) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0f) }
        val entry = remember(reducedMotion) { DialogEntry.select(reducedMotion) }
        val alpha = remember { Animatable(0f) }
        val scale = remember { Animatable(entry.initialScale) }
        LaunchedEffect(entry) {
            alpha.animateTo(1f, tween(entry.fadeMs, easing = LinearOutSlowInEasing))
        }
        LaunchedEffect(entry) {
            entry.scaleSpec?.let { scale.animateTo(1f, it) }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { this.alpha = alpha.value }
                    .background(LeakOutColors.Scrim),
            )
            ResultDialogFrame(measureRegular = measureRegular, measureKey = measureKey) { layout ->
                ResultDialogPanel(
                    layout = layout,
                    modifier = panelModifier
                        .semantics { panelScale = scale.value }
                        .graphicsLayer {
                            this.alpha = alpha.value
                            scaleX = scale.value
                            scaleY = scale.value
                        },
                ) {
                    Box(Modifier.testTag("${layoutTagPrefix}_layout_${layout.name.lowercase()}")) { content(layout) }
                }
            }
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(ResultDialogMetrics.CornerOverlayInset),
            ) { cornerOverlay() }
        }
    }
}

/**
 * Measures [measureRegular] at the Regular width, once per window size and font scale, and gives
 * [content] the chosen layout. The panel is centred with a [ResultDialogMetrics.Gutter] to the
 * window edges.
 */
@Composable
internal fun ResultDialogFrame(
    measureRegular: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    measureKey: Any? = null,
    content: @Composable (ResultDialogLayout) -> Unit,
) {
    val cache = remember { LayoutChoiceCache() }
    SubcomposeLayout(modifier.fillMaxSize()) { constraints ->
        val gutter = ResultDialogMetrics.Gutter.roundToPx()
        val maxWidth = (constraints.maxWidth - 2 * gutter).coerceAtLeast(0)
        val maxHeight = (constraints.maxHeight - 2 * gutter).coerceAtLeast(0)
        val key = listOf(constraints.maxWidth, constraints.maxHeight, density, fontScale, measureKey)
        val layout = cache.layout.takeIf { cache.key == key } ?: run {
            val regularWidth = minOf(maxWidth, ResultDialogMetrics.RegularMaxWidth.roundToPx())
            val measured = subcompose(MEASURE_SLOT) {
                // Never placed and hidden from accessibility and tests: it only gives the height.
                ResultDialogPanel(ResultDialogLayout.Regular, Modifier.clearAndSetSemantics { }) { measureRegular() }
            }
                .map { it.measure(Constraints(maxWidth = regularWidth)) }
            ResultDialogLayout.choose(maxHeight, measured.maxOfOrNull { it.height } ?: 0)
                .also {
                    cache.key = key
                    cache.layout = it
                }
        }
        val placeables = subcompose(CONTENT_SLOT) { content(layout) }
            .map { it.measure(Constraints(maxWidth = maxWidth, maxHeight = maxHeight)) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEach { it.place((constraints.maxWidth - it.width) / 2, (constraints.maxHeight - it.height) / 2) }
        }
    }
}

private class LayoutChoiceCache {
    var key: List<Any?>? = null
    var layout: ResultDialogLayout? = null
}

private const val MEASURE_SLOT = "measure"
private const val CONTENT_SLOT = "content"

/** The [FloatingPanel] of a result dialog: max 360 dp wide in Regular and 600 dp in Compact. */
@Composable
internal fun ResultDialogPanel(layout: ResultDialogLayout, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    FloatingPanel(
        modifier = modifier,
        maxWidth = if (layout == ResultDialogLayout.Regular) ResultDialogMetrics.RegularMaxWidth else ResultDialogMetrics.CompactMaxWidth,
        content = content,
    )
}

/** The Regular layout: one centred column with the panel padding. */
@Composable
internal fun RegularColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(ResultDialogMetrics.Padding), horizontalAlignment = Alignment.CenterHorizontally, content = content)
}

/**
 * The Compact layout: [info] on the left (scrolls when it does not fit), a 1 dp `line.subtle`
 * divider, and [buttons] in a fixed 200 dp column on the right that always stays fully visible.
 */
@Composable
internal fun CompactColumns(
    info: @Composable ColumnScope.() -> Unit,
    buttons: @Composable ColumnScope.() -> Unit,
    infoModifier: Modifier = Modifier,
) {
    Row(
        Modifier.height(IntrinsicSize.Min).padding(ResultDialogMetrics.Padding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            infoModifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = info,
        )
        Box(
            Modifier
                .padding(horizontal = ResultDialogMetrics.ColumnGap)
                .width(1.dp)
                .fillMaxHeight()
                .background(LeakOutColors.LineSubtle),
        )
        Column(
            Modifier.width(ResultDialogMetrics.ButtonsColumnWidth),
            verticalArrangement = Arrangement.spacedBy(ResultDialogMetrics.ButtonSpacing),
            content = buttons,
        )
    }
}

/**
 * One column of a result dialog's score row (design-system.md 5.6): a `label.md` [caption] in
 * `text.secondary` above its [value] in `numeric.lg`, read as one item: [description].
 */
@Composable
internal fun ResultScoreColumn(
    caption: String,
    value: String,
    valueColor: Color,
    description: String,
    modifier: Modifier = Modifier,
    glow: Boolean = false,
) {
    Column(
        modifier.semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(caption.displayUppercase(), style = LeakOutTextStyles.LabelMd, color = LeakOutColors.TextSecondary, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        val style = when {
            glow -> LeakOutTextStyles.NumericLg.copy(shadow = Shadow(LeakOutColors.Brass500.copy(alpha = 0.45f), Offset.Zero, 24f))
            else -> LeakOutTextStyles.NumericLg
        }
        Text(value, style = style, color = valueColor, maxLines = 1)
    }
}

/**
 * The last button row of a result dialog: tertiary Top 10 and Exit side by side, equally wide, or
 * Exit alone when [onTopScores] is null (design-system.md 5.6). Sharing the row keeps the dialog's
 * height as it was without Top 10.
 */
@Composable
internal fun TopScoresAndExitRow(
    onTopScores: (() -> Unit)?,
    onExit: () -> Unit,
    exitText: String,
    enabled: Boolean,
    topScoresTag: String,
    exitTag: String,
    tag: Modifier.(String) -> Modifier,
) {
    Row(Modifier.fillMaxWidth()) {
        if (onTopScores != null) {
            FlowButton(
                text = stringResource(R.string.top10_open),
                onClick = onTopScores,
                variant = FlowButtonVariant.Tertiary,
                enabled = enabled,
                horizontalPadding = ResultDialogMetrics.PairedButtonPadding,
                modifier = Modifier.weight(1f).tag(topScoresTag),
            )
        }
        FlowButton(
            text = exitText,
            onClick = onExit,
            variant = FlowButtonVariant.Tertiary,
            enabled = enabled,
            horizontalPadding = if (onTopScores != null) ResultDialogMetrics.PairedButtonPadding else FlowButtonMetrics.HorizontalPadding,
            modifier = Modifier.weight(1f).tag(exitTag),
        )
    }
}
