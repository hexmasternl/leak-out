package nl.hexmaster.leakout.ui.game.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.settings.TipId
import nl.hexmaster.leakout.ui.components.FlowButton
import nl.hexmaster.leakout.ui.components.FlowButtonSize
import nl.hexmaster.leakout.ui.components.FlowButtonVariant
import nl.hexmaster.leakout.ui.game.INTRO_STEPS
import nl.hexmaster.leakout.ui.game.PipeSectionGlyph
import nl.hexmaster.leakout.ui.theme.LeakOutColors

object TipOverlayTags {
    const val OVERLAY = "tip_overlay"
    const val CARD = "tip_card"
    const val NEXT = "tip_next"
    const val SKIP = "tip_skip"

    /** "Let's go" on the last tour step, "Got it" on a single tip. */
    const val DONE = "tip_done"
    const val HINT_GHOST = "tip_hint_ghost"
    const val HINT_ARROW = "tip_hint_arrow"
}

/** Semantics on the overlay: the spotlight rects in root pixels. A test hook only. */
val TipSpotlights = SemanticsPropertyKey<List<Rect>>("TipSpotlights")
var SemanticsPropertyReceiver.tipSpotlights by TipSpotlights

/** Semantics on the overlay: which tip it shows. A test hook only. */
val ShownTip = SemanticsPropertyKey<TipId>("ShownTip")
var SemanticsPropertyReceiver.shownTip by ShownTip

/** What one tip says. [step] is the intro tour step (0-based), null on a single tip. */
@Immutable
data class TipContent(val tip: TipId, val step: Int?, val title: String, val text: String, val target: String)

/** The texts of [tip] at tour [step] (design-system.md 5.9); [reservoirPercent] fills the reservoir text. */
@Composable
fun tipContent(tip: TipId, step: Int, reservoirPercent: Int): TipContent = when (tip) {
    TipId.Intro -> {
        val (title, text, target) = when (step) {
            0 -> Triple(R.string.tip_intro_start_title, R.string.tip_intro_start_text, R.string.tip_intro_start_target)
            1 -> Triple(R.string.tip_intro_finish_title, R.string.tip_intro_finish_text, R.string.tip_intro_finish_target)
            2 -> Triple(R.string.tip_intro_stack_title, R.string.tip_intro_stack_text, R.string.tip_intro_stack_target)
            3 -> Triple(R.string.tip_intro_place_title, R.string.tip_intro_place_text, R.string.tip_intro_place_target)
            else -> Triple(R.string.tip_intro_zoom_title, R.string.tip_intro_zoom_text, R.string.tip_intro_zoom_target)
        }
        TipContent(tip, step, stringResource(title), stringResource(text), stringResource(target))
    }
    TipId.Mandatory -> TipContent(tip, null, stringResource(R.string.tip_mandatory_title), stringResource(R.string.tip_mandatory_text), stringResource(R.string.tip_mandatory_target))
    TipId.Cross -> TipContent(tip, null, stringResource(R.string.tip_cross_title), stringResource(R.string.tip_cross_text), stringResource(R.string.tip_cross_target))
    TipId.Reservoir -> TipContent(
        tip, null, stringResource(R.string.tip_reservoir_title), stringResource(R.string.tip_reservoir_text, reservoirPercent),
        stringResource(R.string.tip_reservoir_target),
    )
}

/** The drag hint of the placing step: the [section] ghost travels from [from] to [to] (root pixels). */
@Immutable
data class DragHint(val section: PipeSection, val from: Rect, val to: Rect)

/**
 * The tip overlay of design-system.md 5.9 over the whole game screen: the scrim with a spotlight on
 * every rect of [spotlights] (root pixels), the callout card beside them and, on the placing step,
 * the [hint]. It takes all pointer input, so nothing under it can be dragged, pinched or panned.
 * [onNext] and [onSkip] are the tour's Next and Skip tour, [onDone] its Let's go and a single tip's
 * Got it.
 */
@Composable
fun TipOverlay(
    content: TipContent,
    spotlights: List<Rect>,
    hint: DragHint?,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onDone: () -> Unit,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    val local = spotlights.map { it.relativeTo(origin) }
    val entry = remember { Animatable(0f) }
    val scale = remember { Animatable(if (reducedMotion) 1f else 0.92f) }
    LaunchedEffect(Unit) { entry.animateTo(1f, tween(160, easing = LinearOutSlowInEasing)) }
    LaunchedEffect(Unit) { if (!reducedMotion) scale.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)) }
    // The animations below are read while drawing, never here: a recomposition every frame (120 Hz
    // on some phones) would rebuild the card and its buttons under the player's finger.
    val glow: () -> Float = if (reducedMotion) {
        { TipOverlayMetrics.RING_GLOW_REDUCED_ALPHA }
    } else {
        val pulse = rememberInfiniteTransition(label = "tipGlow")
        pulse.animateFloat(
            TipOverlayMetrics.RING_GLOW_ALPHA, TipOverlayMetrics.RING_GLOW_PULSE_ALPHA,
            infiniteRepeatable(tween(1_000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "tipGlowAlpha",
        ).let { alpha -> { alpha.value } }
    }
    // A single spotlight glides to its next target (`motion.settle`); several jump.
    val single = local.singleOrNull()
    val glide = remember { Animatable(Rect.Zero, Rect.VectorConverter) }
    LaunchedEffect(single) {
        if (single == null) return@LaunchedEffect
        if (reducedMotion || glide.value == Rect.Zero) glide.snapTo(single) else glide.animateTo(single, tween(240, easing = FastOutSlowInEasing))
    }
    val shown = { if (single != null && glide.value != Rect.Zero) listOf(glide.value) else local }
    SideEffect { TipOverlayProbe.compositions++ }

    Box(
        modifier
            .fillMaxSize()
            .testTag(TipOverlayTags.OVERLAY)
            .semantics {
                tipSpotlights = spotlights
                shownTip = content.tip
            }
            .onGloballyPositioned { origin = it.positionInRoot() }
            .graphicsLayer { alpha = entry.value }
            .pointerInput(Unit) { swallowTouchesOutsideButtons() },
    ) {
        TipSpotlightScrim(shown, Modifier.clearAndSetSemantics { }, glowAlpha = glow)
        hint?.let { DragHintLayer(it.copy(from = it.from.relativeTo(origin), to = it.to.relativeTo(origin)), reducedMotion) }
        CalloutLayer(spotlights = local, origin = { origin }, scale = { scale.value }) { pointer -> Callout(content, pointer, onNext, onSkip, onDone) }
    }
}

/**
 * Every touch on the overlay ends here, except the ones a button took. A button takes a touch by
 * consuming its down; its tap is cancelled when a later move of that finger is found consumed, and a
 * finger on a real touchscreen nearly always moves a little between down and up. So the moves and
 * the up of a finger that went down on a button are left to the button.
 */
private suspend fun PointerInputScope.swallowTouchesOutsideButtons() = awaitPointerEventScope {
    val onButtons = mutableSetOf<PointerId>()
    while (true) {
        awaitPointerEvent().changes.forEach { change ->
            if (change.changedToDownIgnoreConsumed() && change.isConsumed) onButtons += change.id
            if (change.id !in onButtons) change.consume()
            if (!change.pressed) onButtons -= change.id
        }
    }
}

/** How often [TipOverlay] has been composed. A test hook only: a shown tip must not recompose every frame. */
internal object TipOverlayProbe {
    var compositions = 0
}

@Composable
private fun Callout(content: TipContent, pointer: () -> TipPointer?, onNext: () -> Unit, onSkip: () -> Unit, onDone: () -> Unit) {
    val step = content.step
    val stepDescription = step?.let { stringResource(R.string.tip_step_description, it + 1, INTRO_STEPS) }
    val highlighted = stringResource(R.string.tip_highlighted, content.target)
    TipCallout(
        eyebrow = if (step != null) stringResource(R.string.tip_step_counter, step + 1, INTRO_STEPS) else stringResource(R.string.tip_eyebrow),
        eyebrowColor = if (step != null) LeakOutColors.Liquid300 else LeakOutColors.Brass300,
        title = content.title,
        text = content.text,
        pointer = pointer,
        eyebrowDescription = stepDescription,
        textDescription = "${content.text} $highlighted",
        modifier = Modifier
            .testTag(TipOverlayTags.CARD)
            .semantics {
                paneTitle = content.title
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        val last = step == null || step == INTRO_STEPS - 1
        if (!last) {
            // Next on top and full width so it reads as the way on; Skip tour below it.
            FlowButton(stringResource(R.string.tip_next), onClick = onNext, size = FlowButtonSize.Small, modifier = Modifier.fillMaxWidth().testTag(TipOverlayTags.NEXT))
            FlowButton(stringResource(R.string.tip_skip_tour), onClick = onSkip, variant = FlowButtonVariant.Tertiary, modifier = Modifier.fillMaxWidth().testTag(TipOverlayTags.SKIP))
        } else {
            val label = if (step == null) R.string.tip_got_it else R.string.tip_lets_go
            FlowButton(stringResource(label), onClick = onDone, size = FlowButtonSize.Small, modifier = Modifier.testTag(TipOverlayTags.DONE))
        }
    }
}

/**
 * Measures the card and places it with [CalloutPlacement] next to the union of the [spotlights],
 * clear of the display cutout and the system gesture areas of the window; [origin] is where this
 * layer sits in it. While a finger is down on the card it keeps its place, even when the spotlights
 * move (the field still panning to a target): a button that slides away under the finger loses its tap.
 */
@Composable
private fun CalloutLayer(
    spotlights: List<Rect>,
    origin: () -> Offset,
    scale: () -> Float,
    content: @Composable (pointer: () -> TipPointer?) -> Unit,
) {
    val density = LocalDensity.current
    val unsafeInsets = WindowInsets.displayCutout.union(WindowInsets.systemGestures)
    val windowSize = LocalWindowInfo.current.containerSize
    // Known only once the card is placed; read while drawing, so setting it here recomposes nothing.
    var pointer by remember { mutableStateOf<TipPointer?>(null) }
    // A release lays the card out again, so it catches up with spotlights that moved meanwhile.
    var pressed by remember { mutableStateOf(false) }
    val held = remember { arrayOfNulls<CalloutPosition>(1) }
    Layout(
        content = {
            Box(Modifier.pointerInput(Unit) { trackPress { pressed = it } }) { content { pointer } }
        },
        modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scale(); scaleY = scale() },
    ) { measurables, constraints ->
        val gutter = TipOverlayMetrics.ScreenGutter.toPx()
        val maxWidth = (constraints.maxWidth - 2 * gutter).toInt().coerceAtLeast(0)
        val placeable = measurables.single().measure(Constraints(maxWidth = maxWidth, maxHeight = constraints.maxHeight))
        val window = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        val padding = with(density) { TipOverlayMetrics.SpotlightPadding.toPx() }
        val union = spotlights.reduceOrNull { a, b -> Rect(minOf(a.left, b.left), minOf(a.top, b.top), maxOf(a.right, b.right), maxOf(a.bottom, b.bottom)) }
            ?.inflate(padding) ?: Rect(Offset(window.width / 2, window.height / 2), Size.Zero)
        // The insets are the window's; only the part that reaches into this layer counts.
        val at = origin()
        val unsafe = UnsafeEdges(
            left = (unsafeInsets.getLeft(this, layoutDirection) - at.x).coerceAtLeast(0f),
            top = (unsafeInsets.getTop(this) - at.y).coerceAtLeast(0f),
            right = (unsafeInsets.getRight(this, layoutDirection) - (windowSize.width - at.x - window.width)).coerceAtLeast(0f),
            bottom = (unsafeInsets.getBottom(this) - (windowSize.height - at.y - window.height)).coerceAtLeast(0f),
        )
        val placed = held[0].takeIf { pressed }
            ?: CalloutPlacement.place(window, union, Size(placeable.width.toFloat(), placeable.height.toFloat()), gutter, TipOverlayMetrics.CardGap.toPx(), unsafe)
        held[0] = placed
        if (pointer != placed.pointer) pointer = placed.pointer
        layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(placed.topLeft.x.toInt(), placed.topLeft.y.toInt()) }
    }
}

/** Tells [onPressed] whether any finger is down on this element. It only looks; it consumes nothing. */
private suspend fun PointerInputScope.trackPress(onPressed: (Boolean) -> Unit) = awaitPointerEventScope {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        onPressed(event.changes.any { it.pressed })
    }
}

/**
 * The placing step's hint: a 50 % ghost of the next section, the size of the target cell, looping
 * from the next slot to the hint cell over `motion.pulse`; with [reducedMotion] a static arrow and
 * the ghost at rest on the cell.
 */
@Composable
private fun DragHintLayer(hint: DragHint, reducedMotion: Boolean) {
    val density = LocalDensity.current
    val travel: Float
    val ghostAlpha: Float
    if (reducedMotion) {
        travel = 1f
        ghostAlpha = TipOverlayMetrics.GHOST_ALPHA
        Canvas(Modifier.fillMaxSize().testTag(TipOverlayTags.HINT_ARROW)) { drawHintArrow(hint.from.center, hint.to.center) }
    } else {
        val loop = rememberInfiniteTransition(label = "dragHint")
        travel = loop.animateFloat(
            0f, 1f,
            infiniteRepeatable(
                keyframes {
                    durationMillis = 2_000
                    0f at 160 using FastOutSlowInEasing
                    1f at 1_360
                    1f at 2_000
                },
            ),
            label = "dragHintTravel",
        ).value
        ghostAlpha = loop.animateFloat(
            0f, 0f,
            infiniteRepeatable(
                keyframes {
                    durationMillis = 2_000
                    0f at 0
                    TipOverlayMetrics.GHOST_ALPHA at 160 using LinearEasing
                    TipOverlayMetrics.GHOST_ALPHA at 1_760
                    0f at 1_920
                },
            ),
            label = "dragHintAlpha",
        ).value
    }
    val size = hint.to.size
    val center = lerp(hint.from.center, hint.to.center, travel)
    with(density) {
        Box(
            Modifier
                .testTag(TipOverlayTags.HINT_GHOST)
                .graphicsLayer {
                    translationX = center.x - size.width / 2
                    translationY = center.y - size.height / 2
                    alpha = ghostAlpha
                }
                .size(size.width.toDp(), size.height.toDp()),
        ) {
            if (travel >= 1f) Canvas(Modifier.fillMaxSize()) { drawValidTarget() }
            PipeSectionGlyph(hint.section, Modifier.fillMaxSize())
        }
    }
}

/** The "valid target" look of design-system.md 6.2 on the hint cell. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawValidTarget() {
    val radius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx())
    drawRoundRect(LeakOutColors.Liquid500.copy(alpha = 0.14f), cornerRadius = radius)
    drawRoundRect(Color.White.copy(alpha = 0.16f), cornerRadius = radius, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
}
