package nl.hexmaster.leakout.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
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

/** Sizes of the result dialog layouts, from design-system.md 5.6. */
object ResultDialogMetrics {
    val Gutter: Dp = 16.dp
    val Padding: Dp = 24.dp
    val RegularMaxWidth: Dp = 360.dp
    val CompactMaxWidth: Dp = 600.dp
    val ButtonsColumnWidth: Dp = 200.dp
    val ColumnGap: Dp = 24.dp
    val ButtonSpacing: Dp = 12.dp
    val CornerRadius: Dp = 24.dp
    val ShadowElevation: Dp = 24.dp
}

/**
 * How a result dialog enters (design-system.md 7.2, 7.4): the scrim and panel fade in over
 * `motion.quick`; the panel also scales from [initialScale] to 1 with [scaleSpec], unless motion is
 * reduced, when there is no scale.
 */
data class ResultDialogEntry(
    val initialScale: Float,
    val scaleSpec: AnimationSpec<Float>?,
    val fadeMs: Int,
) {
    companion object {
        const val FADE_MS = 160
        const val INITIAL_SCALE = 0.92f

        /** `motion.pop`: spring, stiffness medium-low, damping 0.6. */
        val PopSpring: AnimationSpec<Float> = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)

        fun select(reducedMotion: Boolean): ResultDialogEntry = when {
            reducedMotion -> ResultDialogEntry(initialScale = 1f, scaleSpec = null, fadeMs = FADE_MS)
            else -> ResultDialogEntry(initialScale = INITIAL_SCALE, scaleSpec = PopSpring, fadeMs = FADE_MS)
        }
    }
}

/** Test-only semantics: the panel's current entry scale. */
val PanelScale = SemanticsPropertyKey<Float>("PanelScale")
var SemanticsPropertyReceiver.panelScale by PanelScale

/**
 * The window of a result dialog: a full-window, see-through `surface.scrim` (platform dim off, so the
 * field stays visible behind it) with the entry motion, and the panel in the [ResultDialogLayout]
 * chosen by [ResultDialogFrame]. [content] draws the panel's content for the chosen layout;
 * [measureRegular] draws the Regular content with its final values, for measuring only.
 */
@Composable
internal fun ResultDialogWindow(
    onDismissRequest: () -> Unit,
    dismissOnBackPress: Boolean,
    panelModifier: Modifier,
    layoutTagPrefix: String,
    measureRegular: @Composable () -> Unit,
    reducedMotion: Boolean = rememberReducedMotion(),
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
        val entry = remember(reducedMotion) { ResultDialogEntry.select(reducedMotion) }
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
            ResultDialogFrame(measureRegular = measureRegular) { layout ->
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
    content: @Composable (ResultDialogLayout) -> Unit,
) {
    val cache = remember { LayoutChoiceCache() }
    SubcomposeLayout(modifier.fillMaxSize()) { constraints ->
        val gutter = ResultDialogMetrics.Gutter.roundToPx()
        val maxWidth = (constraints.maxWidth - 2 * gutter).coerceAtLeast(0)
        val maxHeight = (constraints.maxHeight - 2 * gutter).coerceAtLeast(0)
        val key = listOf(constraints.maxWidth, constraints.maxHeight, density, fontScale)
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
    var key: List<Any>? = null
    var layout: ResultDialogLayout? = null
}

private const val MEASURE_SLOT = "measure"
private const val CONTENT_SLOT = "content"

/**
 * Level 3 floating panel (design-system.md 4.3, 5.6): `radius.lg`, `surface.base`, a 1 dp
 * `line.highlight` top edge, no outline; max 360 dp wide in Regular and 600 dp in Compact.
 */
@Composable
internal fun ResultDialogPanel(layout: ResultDialogLayout, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(ResultDialogMetrics.CornerRadius)
    val shadow = Color.Black.copy(alpha = 0.6f)
    Box(
        modifier
            .widthIn(max = if (layout == ResultDialogLayout.Regular) ResultDialogMetrics.RegularMaxWidth else ResultDialogMetrics.CompactMaxWidth)
            .shadow(ResultDialogMetrics.ShadowElevation, shape, ambientColor = shadow, spotColor = shadow)
            .background(LeakOutColors.SurfaceBase, shape)
            .drawWithContent {
                drawContent()
                val inset = ResultDialogMetrics.CornerRadius.toPx()
                val stroke = 1.dp.toPx()
                drawLine(LeakOutColors.LineHighlight, Offset(inset, stroke / 2), Offset(size.width - inset, stroke / 2), stroke)
            },
    ) { content() }
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
        Text(caption.uppercase(), style = LeakOutTextStyles.LabelMd, color = LeakOutColors.TextSecondary, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        val style = when {
            glow -> LeakOutTextStyles.NumericLg.copy(shadow = Shadow(LeakOutColors.Brass500.copy(alpha = 0.45f), Offset.Zero, 24f))
            else -> LeakOutTextStyles.NumericLg
        }
        Text(value, style = style, color = valueColor, maxLines = 1)
    }
}
