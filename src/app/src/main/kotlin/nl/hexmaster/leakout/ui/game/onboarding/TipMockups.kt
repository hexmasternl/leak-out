package nl.hexmaster.leakout.ui.game.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.core.board.Board
import nl.hexmaster.leakout.core.level.Endpoint
import nl.hexmaster.leakout.core.level.FlowSettings
import nl.hexmaster.leakout.core.level.GridSize
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.PlacementRules
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.core.level.StackSettings
import nl.hexmaster.leakout.core.level.StarBenchmarks
import nl.hexmaster.leakout.core.stack.Dealer
import nl.hexmaster.leakout.core.stack.PipeStack
import nl.hexmaster.leakout.ui.components.FlowButton
import nl.hexmaster.leakout.ui.components.FlowButtonSize
import nl.hexmaster.leakout.ui.components.FlowButtonVariant
import nl.hexmaster.leakout.ui.game.GameHud
import nl.hexmaster.leakout.ui.game.HudLayout
import nl.hexmaster.leakout.ui.game.PipeSectionGlyph
import nl.hexmaster.leakout.ui.game.PlayingField
import nl.hexmaster.leakout.ui.game.SectionDragState
import nl.hexmaster.leakout.ui.game.StackLayout
import nl.hexmaster.leakout.ui.game.StackPanel
import nl.hexmaster.leakout.ui.game.rememberPlayingFieldState
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTheme

/**
 * Static mock-ups of the tip overlay (design-system.md 5.9) for the design review on issue #36:
 * the real HUD, stack and field of a small level under the scrim, spotlight and callout. They are
 * still pictures: nothing animates and no button does anything.
 */
enum class TipMockupStep { Start, Placing, Cross }

object TipMockupTags {
    const val SCENE = "tip_mockup_scene"
    const val CALLOUT = "tip_mockup_callout"
}

private val MockLevel = Level(
    id = 4,
    name = "Mock-up",
    grid = GridSize(8, 5),
    start = Endpoint(Position(0, 2), Side.RIGHT),
    finish = Endpoint(Position(7, 1), Side.LEFT),
    blocked = listOf(Position(3, 2), Position(5, 3)),
    flow = FlowSettings(initialDelayMs = 10_000, cellsPerSecond = 0.8),
    stack = StackSettings(),
    rules = PlacementRules(),
    stars = StarBenchmarks(100, 200, 350),
)

/** Index of the `X` in [MockStackSections]; the next section is the last one, at the bottom. */
private const val MOCK_CROSS_SLOT = 2
private val MockStackSections = listOf(PipeSection.H, PipeSection.LT, PipeSection.X, PipeSection.V, PipeSection.H)

/** The cell the drag hint ends on: the empty cell next to the start's exit. */
private val MockHintCell = Position(1, 2)

@Composable
fun TipMockupScene(step: TipMockupStep, modifier: Modifier = Modifier) {
    val board = remember { Board.from(MockLevel) }
    val stack = remember { PipeStack(MockStackSections, Dealer.initial(MockLevel.stack)) }
    val dragState = remember { SectionDragState() }
    val fieldState = rememberPlayingFieldState(MockLevel.grid, MockLevel.start.position)
    val density = LocalDensity.current
    var scene by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var stackPanel by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var field by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(modifier.fillMaxSize().background(LeakOutColors.SurfaceDeep).testTag(TipMockupTags.SCENE).onGloballyPositioned { scene = it }) {
        Column(Modifier.fillMaxSize()) {
            GameHud(MockLevel.id, 0.8, 10_000, 10_000, MockLevel.stars, 0, onPause = {}, modifier = Modifier.fillMaxWidth(), reducedMotion = true)
            Row(Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {
                StackPanel(
                    stack = stack,
                    dragState = dragState,
                    enabled = false,
                    modifier = Modifier
                        .width(StackLayout.metrics(360.dp - HudLayout.HEIGHT, stack.size).columnWidth + 16.dp)
                        .onGloballyPositioned { stackPanel = it },
                )
                PlayingField(
                    board = board,
                    state = fieldState,
                    highlighted = if (step == TipMockupStep.Placing) MockHintCell else null,
                    enabled = false,
                    modifier = Modifier.padding(start = 12.dp).weight(1f).fillMaxHeight().onGloballyPositioned { field = it },
                )
            }
        }

        val root = scene
        val viewport = fieldState.viewport
        val fieldCoords = field
        val stackCoords = stackPanel
        if (root == null || viewport == null || fieldCoords == null || stackCoords == null) return@Box
        val fieldOrigin = root.localBoundingBoxOf(fieldCoords).topLeft
        fun cell(position: Position): Rect = viewport.cellRect(position).translate(fieldOrigin)
        val panel = root.localBoundingBoxOf(stackCoords)
        val slots = slotRects(panel, stack.size, density)

        val targets = when (step) {
            TipMockupStep.Start -> listOf(cell(MockLevel.start.position))
            TipMockupStep.Placing -> listOf(slots.last(), cell(MockHintCell))
            TipMockupStep.Cross -> listOf(slots[MOCK_CROSS_SLOT])
        }
        TipSpotlightScrim(targets)
        if (step == TipMockupStep.Placing) {
            // The reduced-motion hint: a ghost at rest on the cell and an arrow from the next slot.
            val hint = cell(MockHintCell)
            Canvas(Modifier.fillMaxSize()) { drawHintArrow(slots.last().centerRight, hint.centerLeft) }
            with(density) {
                PipeSectionGlyph(
                    stack.next,
                    Modifier
                        .graphicsLayer {
                            translationX = hint.left
                            translationY = hint.top
                            alpha = TipOverlayMetrics.GHOST_ALPHA
                        }
                        .size(hint.width.toDp(), hint.height.toDp()),
                )
            }
        }
        // The callout is placed next to the first target (the next slot on the placing step).
        MockCalloutLayout(target = targets.first(), density = density) { pointer ->
            MockCallout(step, pointer)
        }
    }
}

@Composable
private fun MockCallout(step: TipMockupStep, pointer: TipPointer?) {
    val modifier = Modifier.testTag(TipMockupTags.CALLOUT)
    when (step) {
        TipMockupStep.Start -> TourCallout(1, R.string.tip_intro_start_title, R.string.tip_intro_start_text, pointer, modifier)
        TipMockupStep.Placing -> TourCallout(4, R.string.tip_intro_place_title, R.string.tip_intro_place_text, pointer, modifier)
        TipMockupStep.Cross -> TipCallout(
            eyebrow = stringResource(R.string.tip_eyebrow),
            eyebrowColor = LeakOutColors.Brass300,
            title = stringResource(R.string.tip_cross_title),
            text = stringResource(R.string.tip_cross_text),
            pointer = pointer,
            modifier = modifier,
        ) {
            FlowButton(stringResource(R.string.tip_got_it), onClick = {}, variant = FlowButtonVariant.Primary, size = FlowButtonSize.Small)
        }
    }
}

@Composable
private fun TourCallout(step: Int, title: Int, text: Int, pointer: TipPointer?, modifier: Modifier) {
    TipCallout(
        eyebrow = stringResource(R.string.tip_step_counter, step, 5),
        eyebrowColor = LeakOutColors.Liquid300,
        title = stringResource(title),
        text = stringResource(text),
        pointer = pointer,
        modifier = modifier,
    ) {
        FlowButton(stringResource(R.string.tip_skip_tour), onClick = {}, variant = FlowButtonVariant.Tertiary)
        FlowButton(stringResource(R.string.tip_next), onClick = {}, variant = FlowButtonVariant.Primary, size = FlowButtonSize.Small)
    }
}

/** The stack slot rects inside [panel], laid out as `StackPanel` does: a centred column, the next slot last. */
private fun slotRects(panel: Rect, count: Int, density: Density): List<Rect> = with(density) {
    val padding = 8.dp.toPx()
    val metrics = StackLayout.metrics((panel.height - 2 * padding).toDp(), count)
    val preview = metrics.previewSlotSize.toPx()
    val next = metrics.nextSlotSize.toPx()
    val gap = metrics.gap.toPx()
    val total = metrics.totalHeight(count).toPx()
    var top = panel.center.y - total / 2
    List(count) { index ->
        val size = if (index == count - 1) next else preview
        Rect(Offset(panel.center.x - size / 2, top), Size(size, size)).also { top += size + gap }
    }
}

/**
 * A mock-up placement only: the card goes on the side of [target] with the most room, centred on
 * it and kept inside the screen gutter. The real placement is a pure, tested function (task 3.3).
 */
@Composable
private fun MockCalloutLayout(target: Rect, density: Density, content: @Composable (TipPointer?) -> Unit) {
    var pointer by remember { mutableStateOf<TipPointer?>(null) }
    Layout(content = { content(pointer) }, modifier = Modifier.fillMaxSize()) { measurables, constraints ->
        val placeable = measurables.single().measure(Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight))
        val gutter = with(density) { TipOverlayMetrics.ScreenGutter.toPx() }
        val gap = with(density) { (TipOverlayMetrics.CardGap + TipOverlayMetrics.SpotlightPadding).toPx() }
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val right = width - gutter - (target.right + gap)
        val left = target.left - gap - gutter
        val placeRight = right >= placeable.width || right >= left
        val x = if (placeRight) target.right + gap else target.left - gap - placeable.width
        val y = (target.center.y - placeable.height / 2).coerceIn(gutter, (height - gutter - placeable.height).coerceAtLeast(gutter))
        val newPointer = TipPointer(if (placeRight) TipPointerSide.Left else TipPointerSide.Right, target.center.y - y)
        if (pointer != newPointer) pointer = newPointer
        layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(x.toInt(), y.toInt()) }
    }
}

@Preview(name = "Tip mock-up: start step, 640 × 360", widthDp = 640, heightDp = 360)
@Composable
private fun TipMockupStartPreview() {
    LeakOutTheme { TipMockupScene(TipMockupStep.Start) }
}

@Preview(name = "Tip mock-up: placing step with hint, 640 × 360", widthDp = 640, heightDp = 360)
@Composable
private fun TipMockupPlacingPreview() {
    LeakOutTheme { TipMockupScene(TipMockupStep.Placing) }
}

@Preview(name = "Tip mock-up: cross tip on the stack, 640 × 360", widthDp = 640, heightDp = 360)
@Composable
private fun TipMockupCrossPreview() {
    LeakOutTheme { TipMockupScene(TipMockupStep.Cross) }
}
