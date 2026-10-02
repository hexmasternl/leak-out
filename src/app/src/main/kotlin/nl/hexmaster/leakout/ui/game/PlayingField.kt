package nl.hexmaster.leakout.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import nl.hexmaster.leakout.core.board.Board
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.engine.Award
import nl.hexmaster.leakout.core.engine.FlowState
import nl.hexmaster.leakout.core.engine.Replacement
import nl.hexmaster.leakout.core.flow.Channel
import nl.hexmaster.leakout.core.flow.ChannelUse
import nl.hexmaster.leakout.core.flow.MandatoryProgress
import nl.hexmaster.leakout.core.level.GridSize
import nl.hexmaster.leakout.core.level.MandatorySection
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.ui.pipes.ChannelFill
import nl.hexmaster.leakout.ui.pipes.LiquidScratch
import nl.hexmaster.leakout.ui.pipes.PipeArtLibrary
import nl.hexmaster.leakout.ui.pipes.drawPipe
import nl.hexmaster.leakout.ui.pipes.rememberPipeArtLibrary
import nl.hexmaster.leakout.ui.splash.rememberReducedMotion
import nl.hexmaster.leakout.ui.theme.Fredoka
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTheme

/** Semantics flag on a field cell: true while the dragged section can be dropped on it. */
val FieldCellHighlighted = SemanticsPropertyKey<Boolean>("FieldCellHighlighted")
var SemanticsPropertyReceiver.fieldCellHighlighted by FieldCellHighlighted

/**
 * Semantics on a field cell: the texts of the points popups shown on it, such as "+15", oldest
 * first. A test hook only: it is not part of the content description, so screen readers skip it.
 */
val PointsPopupText = SemanticsPropertyKey<List<String>>("PointsPopupText")
var SemanticsPropertyReceiver.pointsPopupText by PointsPopupText

/** Semantics on the field cell the pipe leaks from: the side it leaks through. A test hook only. */
val LeakAt = SemanticsPropertyKey<Side>("LeakAt")
var SemanticsPropertyReceiver.leakAt by LeakAt

/** Semantics flag on the field cell the liquid head is in. A test hook only. */
val LiquidHead = SemanticsPropertyKey<Boolean>("LiquidHead")
var SemanticsPropertyReceiver.liquidHead by LiquidHead

/** Semantics on a field cell: the channels the liquid has run through there. A test hook only. */
val FilledChannels = SemanticsPropertyKey<Set<Channel>>("FilledChannels")
var SemanticsPropertyReceiver.filledChannels by FilledChannels

/** Semantics on a field cell with a placed section: the section whose pipe art is drawn there. A test hook only. */
val FieldPipeSection = SemanticsPropertyKey<PipeSection>("FieldPipeSection")
var SemanticsPropertyReceiver.pipeSection by FieldPipeSection

/** Semantics flag on the finish cell while the level is won and its success ring is drawn. A test hook only. */
val FinishFlash = SemanticsPropertyKey<Boolean>("FinishFlash")
var SemanticsPropertyReceiver.finishFlash by FinishFlash

/**
 * Semantics on the start and finish cells: the alpha of the glow behind the valve wheel or the
 * finish ring, rounded to two decimals. A test hook only.
 */
val EndpointGlowAlpha = SemanticsPropertyKey<Float>("EndpointGlowAlpha")
var SemanticsPropertyReceiver.endpointGlow by EndpointGlowAlpha

/** Semantics on the start cell: how far the liquid has filled its stub, 0 to 1, rounded to two decimals. A test hook only. */
val StartFilled = SemanticsPropertyKey<Float>("StartFilled")
var SemanticsPropertyReceiver.startFilled by StartFilled

/** Semantics flag on the field cell of a running replacement. A test hook only. */
val ReplacingCell = SemanticsPropertyKey<Boolean>("ReplacingCell")
var SemanticsPropertyReceiver.replacingCell by ReplacingCell

/** Semantics flag on a field cell with a mandatory section, drawn on the red-tinted tile. A test hook only. */
val MandatoryTile = SemanticsPropertyKey<Boolean>("MandatoryTile")
var SemanticsPropertyReceiver.mandatoryTile by MandatoryTile

/** Semantics flag on a mandatory section the liquid missed, which pulses after the loss. A test hook only. */
val MissedMandatory = SemanticsPropertyKey<Boolean>("MissedMandatory")
var SemanticsPropertyReceiver.missedMandatory by MissedMandatory

/**
 * The screen state of the playing field: the viewport and where the field sits on the screen.
 * Created again for every attempt, so a restart goes back to the starting view.
 */
@Stable
class PlayingFieldState(private val grid: GridSize, private val start: Position) {
    var viewport: FieldViewport? by mutableStateOf(null)
        private set

    private var coordinates: LayoutCoordinates? = null

    internal fun onMeasured(size: Size, density: Density) {
        val current = viewport
        if (size.width <= 0f || size.height <= 0f) return
        if (current == null || current.fieldSize != size) viewport = FieldViewport.initial(size, grid, start, density)
    }

    internal fun onPositioned(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
    }

    internal fun transform(zoom: Float, focus: Offset, pan: Offset) {
        viewport = viewport?.zoomBy(zoom, focus)?.panBy(pan)
    }

    /** The grid cell under [screenPoint] (game-screen coordinates), or null outside the visible field. */
    fun cellAt(screenPoint: Offset): Position? {
        val coords = coordinates?.takeIf { it.isAttached } ?: return null
        val local = screenPoint - coords.positionInRoot()
        return viewport?.cellAt(local)
    }
}

@Composable
fun rememberPlayingFieldState(grid: GridSize, start: Position): PlayingFieldState =
    remember(grid, start) { PlayingFieldState(grid, start) }

/**
 * The level grid: draws the visible cells of [board] and the [highlighted] cell, pans with one
 * finger and zooms with two within the viewport's limits. It never rotates. A tap (one finger that
 * stays within the touch slop) calls [onTap] with the cell under it. All input is ignored while
 * [enabled] is false, for example during a section drag or while paused.
 *
 * The [awards] are drawn as points popups on their tiles above everything else, timed by the game
 * clock [nowMs]. The clock is a lambda read only while drawing, so a clock tick redraws the canvas
 * without recomposing the grid.
 *
 * When the pipe has leaked at [leakAt] through [leakSide], liquid spurts out of that side, timed by
 * [lossAgeMs] (read only while drawing), above the pipes and below the popups. When the liquid
 * missed a mandatory section, the [missed] mandatory sections pulse in `leak.500` during that time.
 *
 * Placed sections are drawn with their pipe art and glass overlay (`drawPipe`), like the stack.
 * Mandatory sections get the same art on a `leak.tint` tile edged in `leak.500`.
 *
 * The liquid is drawn inside the pipes from [flow] (read only while drawing, so the flowing head
 * redraws the canvas every frame without recomposing the grid): full in every channel it has run
 * through and up to the head in the channel it is in. The tile under a pipe keeps its colour.
 * [head] is the tile the liquid head is in and [filled] the channels the liquid has run through,
 * for the cell semantics.
 *
 * While the level is won, [winAgeMs] gives the game time since the win (read only while drawing)
 * and the finish tile gets the success ring from [finishFlashFrame], above the pipes and below the
 * popups. It is null while not won.
 *
 * While a [replacement] runs, its old section breaks apart on the tile and then a busy ring fills
 * up until the new section lands; the section that landed last, [landing], pops in. Both are timed
 * by [nowMs].
 *
 * The start is drawn as the start valve and the finish as the checkered finish (design system
 * 6.4): a stub of the straight's pipe art under the brass wheel, or under the success ring filled
 * with a checker. The liquid comes out of the valve into its stub. The valve glow pulses until the
 * liquid flows and the finish glow until the win, timed by [nowMs]; with [reducedMotion] both are
 * steady.
 */
@Composable
fun PlayingField(
    board: Board,
    state: PlayingFieldState,
    highlighted: Position?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    awards: List<Award> = emptyList(),
    nowMs: () -> Long = { 0L },
    leakAt: Position? = null,
    leakSide: Side? = null,
    lossAgeMs: () -> Long = { 0L },
    missed: List<Position> = emptyList(),
    head: Position? = null,
    filled: Set<ChannelUse> = emptySet(),
    flow: () -> FlowState? = { null },
    winAgeMs: (() -> Long)? = null,
    replacement: Replacement? = null,
    landing: Landing? = null,
    reducedMotion: Boolean = rememberReducedMotion(),
    onTap: (Position) -> Unit = {},
) {
    val density = LocalDensity.current
    val currentEnabled by rememberUpdatedState(enabled)
    val currentOnTap by rememberUpdatedState(onTap)
    val textMeasurer = rememberTextMeasurer()
    val pipes = rememberPipeArtLibrary()
    val scratch = remember { LiquidScratch() }
    val won = winAgeMs != null
    val currentNowMs by rememberUpdatedState(nowMs)
    val currentFlow by rememberUpdatedState(flow)
    val currentWon by rememberUpdatedState(won)
    val currentReducedMotion by rememberUpdatedState(reducedMotion)
    // Rounded, so the cell semantics only recompose when the value they report changes.
    val valveGlow = remember {
        derivedStateOf { EndpointGlow.valve(currentNowMs(), currentFlow().isFlowing(), currentReducedMotion).rounded() }
    }
    val finishGlow = remember { derivedStateOf { EndpointGlow.finish(currentNowMs(), currentWon, currentReducedMotion).rounded() } }
    Box(
        modifier = modifier
            .testTag(GameScreenTags.FIELD)
            .background(LeakOutColors.SurfaceDeep, FieldShape)
            .border(1.dp, Color.White.copy(alpha = 0.16f), FieldShape)
            .clip(FieldShape)
            .onSizeChanged { state.onMeasured(it.toSize(), density) }
            .onGloballyPositioned { state.onPositioned(it) }
            .pointerInput(state) { detectPanAndZoom(state, { currentEnabled }) { point -> state.viewport?.cellAt(point)?.let(currentOnTap) } },
    ) {
        val viewport = state.viewport ?: return@Box
        Canvas(Modifier.fillMaxSize()) {
            val cells = viewport.visibleCells()
            val liquid = flow()
            val flowing = liquid as? FlowState.Flowing
            val used = liquid?.used.orEmpty()
            val now = nowMs()
            val pop = landing?.let { landingPopScale(now - it.atMs) }
            val glows = EndpointGlows(
                valve = EndpointGlow.valve(now, liquid.isFlowing(), reducedMotion),
                finish = EndpointGlow.finish(now, won, reducedMotion),
                reachPx = EndpointGeometry.GLOW_REACH_DP.dp.toPx(),
            )
            for (position in cells) {
                val cell = board[position]
                val fills = when (cell) {
                    is Cell.Pipe -> FieldLiquid.fills(position, cell.section, used, flowing)
                    is Cell.Start -> FieldLiquid.startFills(position, cell.openSide, used, flowing)
                    else -> emptyMap()
                }
                val rect = viewport.cellRect(position)
                if (pop != null && cell is Cell.Pipe && position == landing?.at) {
                    scale(pop, pivot = rect.center) { drawCell(cell, rect, pipes, scratch, fills, glows) }
                } else {
                    drawCell(cell, rect, pipes, scratch, fills, glows)
                }
            }
            replacement?.let { drawReplacement(it, viewport.cellRect(it.at), replacementFrame(it, now), pipes, scratch) }
            highlighted?.let { drawHighlight(viewport.cellRect(it)) }
            if (leakAt != null && leakSide != null) {
                val ageMs = lossAgeMs()
                val pulse = missedPulseAlpha(ageMs)
                if (pulse > 0f) for (position in missed) drawMissedPulse(viewport.cellRect(position), pulse)
                drawLeak(viewport.cellRect(leakAt), leakSide, leakFrame(ageMs))
            }
            if (winAgeMs != null) {
                val frame = finishFlashFrame(winAgeMs())
                for (position in cells) if (board[position] is Cell.Finish) drawFinishRing(viewport.cellRect(position), frame)
            }
            drawPopups(awards, now, viewport, textMeasurer)
        }
        CellSemantics(
            board, viewport, highlighted, awards, leakAt?.let { at -> leakSide?.let { at to it } }, head, filled, won,
            replacing = replacement?.at,
            missed = missed.toSet(),
            valveGlow = valveGlow,
            finishGlow = finishGlow,
            flow = { currentFlow() },
        )
    }
}

/**
 * One invisible node per visible cell, so tests and accessibility can find and read a cell. The
 * nodes carry semantics only and take no pointer input, so they never block pan or zoom.
 */
@Composable
private fun CellSemantics(
    board: Board,
    viewport: FieldViewport,
    highlighted: Position?,
    awards: List<Award>,
    leak: Pair<Position, Side>?,
    head: Position?,
    filled: Set<ChannelUse>,
    won: Boolean,
    replacing: Position?,
    missed: Set<Position>,
    valveGlow: State<Float>,
    finishGlow: State<Float>,
    flow: () -> FlowState?,
) {
    val density = LocalDensity.current
    val cellDp = with(density) { viewport.cellPx.toDp() }
    val popupTexts = awards.groupBy({ it.position }, { PointsFormat.text(it.points) })
    val filledByCell = filled.groupBy({ it.position }, { it.channel })
    for (position in viewport.visibleCells()) {
        val rect = viewport.cellRect(position)
        val description = board[position].describe(position, filled)
        val mandatory = (board[position] as? Cell.Pipe)?.mandatory == true
        val isHighlighted = position == highlighted
        val texts = popupTexts[position].orEmpty()
        val leakSide = leak?.takeIf { it.first == position }?.second
        val isHead = position == head
        val channels = filledByCell[position].orEmpty().toSet()
        val flashing = won && board[position] is Cell.Finish
        val section = (board[position] as? Cell.Pipe)?.section
        val isReplacing = position == replacing
        val glow = when (board[position]) {
            is Cell.Start -> valveGlow
            is Cell.Finish -> finishGlow
            else -> null
        }
        val startFilled = if (board[position] is Cell.Start) {
            remember(position) {
                derivedStateOf { flow().let { FieldLiquid.startProgress(position, it?.used.orEmpty(), it as? FlowState.Flowing).rounded() } }
            }
        } else {
            null
        }
        CellNode(
            position, rect, cellDp, description, isHighlighted, texts, leakSide, isHead, channels, flashing, section, isReplacing,
            glow, startFilled, mandatory, isMissed = position in missed,
        )
    }
}

/**
 * The invisible semantics node of one cell. Its own composable, so the endpoint values ([glow] and
 * [startFilled]), which change while the liquid flows, recompose this node only.
 */
@Composable
private fun CellNode(
    position: Position,
    rect: Rect,
    cellDp: Dp,
    description: String,
    isHighlighted: Boolean,
    texts: List<String>,
    leakSide: Side?,
    isHead: Boolean,
    channels: Set<Channel>,
    flashing: Boolean,
    section: PipeSection?,
    isReplacing: Boolean,
    glow: State<Float>?,
    startFilled: State<Float>?,
    mandatory: Boolean,
    isMissed: Boolean,
) {
    val glowAlpha = glow?.value
    val filledFraction = startFilled?.value
    Box(
        Modifier
            .offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
            .size(cellDp)
            .testTag(GameScreenTags.cell(position.x, position.y))
            .semantics {
                contentDescription = description
                fieldCellHighlighted = isHighlighted
                pointsPopupText = texts
                if (leakSide != null) this.leakAt = leakSide
                liquidHead = isHead
                filledChannels = channels
                if (flashing) finishFlash = true
                if (section != null) pipeSection = section
                if (isReplacing) replacingCell = true
                if (glowAlpha != null) endpointGlow = glowAlpha
                if (filledFraction != null) this.startFilled = filledFraction
                if (mandatory) mandatoryTile = true
                if (isMissed) missedMandatory = true
            },
    )
}

private fun FlowState?.isFlowing(): Boolean = this != null && this !is FlowState.Waiting

private fun Float.rounded(): Float = (this * 100f).roundToInt() / 100f

/** The content description of a cell at [position]; a mandatory section says whether [filled] uses it. */
private fun Cell.describe(position: Position, filled: Set<ChannelUse>): String = when (this) {
    Cell.Empty -> "Empty"
    Cell.Blocked -> "Blocked"
    is Cell.Start -> "Start"
    is Cell.Finish -> "Finish"
    is Cell.Pipe -> when {
        mandatory -> {
            val used = MandatoryProgress.isUsed(MandatorySection(position, section), filled)
            "Mandatory ${section.name}, ${if (used) "used" else "not used yet"}"
        }
        liquidEntered -> "Placed ${section.name}, filled"
        else -> "Placed ${section.name}"
    }
}

/**
 * One finger pans after the touch slop, two fingers zoom around their centroid and pan with it.
 * Rotation is never read, so a twist does nothing. One finger that lifts within the touch slop is
 * a tap at its down position (field coordinates) and goes to [onTap]. A gesture stops as soon as
 * [enabled] is false, and then is no tap either.
 */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectPanAndZoom(
    state: PlayingFieldState,
    enabled: () -> Boolean,
    onTap: (Offset) -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (!enabled()) return@awaitEachGesture
        var pastSlop = false
        var multiTouch = false
        var pendingPan = Offset.Zero
        var pendingZoom = 1f
        while (true) {
            val event = awaitPointerEvent()
            if (!enabled()) break
            if (event.changes.size > 1) multiTouch = true
            if (event.changes.none { it.pressed }) {
                val up = event.changes.firstOrNull { it.id == down.id }
                if (!pastSlop && !multiTouch && up != null && !up.isConsumed) onTap(down.position)
                break
            }
            val zoom = event.calculateZoom()
            val pan = event.calculatePan()
            if (!pastSlop) {
                pendingPan += pan
                pendingZoom *= zoom
                val zoomMotion = abs(1f - pendingZoom) * (size.width + size.height) / 4f
                pastSlop = pendingPan.getDistance() > viewConfiguration.touchSlop || zoomMotion > viewConfiguration.touchSlop
            }
            if (pastSlop) {
                if (zoom != 1f || pan != Offset.Zero) state.transform(zoom, event.calculateCentroid(useCurrent = false), pan)
                event.changes.forEach { if (it.positionChanged()) it.consume() }
            }
        }
    }
}

private fun DrawScope.drawCell(
    cell: Cell,
    rect: Rect,
    pipes: PipeArtLibrary,
    scratch: LiquidScratch,
    fills: Map<String, ChannelFill>,
    glows: EndpointGlows,
) {
    val inset = rect.deflate(rect.width * 0.04f)
    val corner = CornerRadius(rect.width * 0.14f)
    when (cell) {
        Cell.Empty -> drawRoundRect(LeakOutColors.SurfaceCard.copy(alpha = 0.06f), inset.topLeft, inset.size, corner)
        Cell.Blocked -> {
            drawRoundRect(LeakOutColors.Steel700, inset.topLeft, inset.size, corner)
            drawRoundRect(LeakOutColors.Steel900, inset.topLeft, inset.size, corner, style = Stroke(rect.width * 0.06f))
            val a = inset.deflate(inset.width * 0.25f)
            drawLine(LeakOutColors.Steel500, a.topLeft, a.bottomRight, rect.width * 0.06f, StrokeCap.Round)
            drawLine(LeakOutColors.Steel500, a.bottomLeft, a.topRight, rect.width * 0.06f, StrokeCap.Round)
        }
        is Cell.Start -> {
            drawRoundRect(LeakOutColors.SurfaceCard.copy(alpha = 0.06f), inset.topLeft, inset.size, corner)
            drawEndpointGlow(rect, LeakOutColors.Brass500, glows.valve, glows.reachPx)
            drawStub(rect, cell.openSide, pipes, scratch, fills)
            drawValveWheel(rect, cell.openSide)
        }
        is Cell.Finish -> {
            drawRoundRect(LeakOutColors.SurfaceCard.copy(alpha = 0.06f), inset.topLeft, inset.size, corner)
            drawEndpointGlow(rect, LeakOutColors.Success500, glows.finish, glows.reachPx)
            drawStub(rect, cell.openSide, pipes, scratch, emptyMap())
            drawCheckeredFinish(rect)
        }
        is Cell.Pipe -> {
            if (cell.mandatory) {
                drawRoundRect(LeakOutColors.LeakTint, inset.topLeft, inset.size, corner)
                val edge = 1.dp.toPx()
                val edged = inset.deflate(edge / 2f)
                drawRoundRect(LeakOutColors.Leak500, edged.topLeft, edged.size, corner, style = Stroke(edge))
            } else {
                drawRoundRect(LeakOutColors.SurfaceCard.copy(alpha = 0.06f), inset.topLeft, inset.size, corner)
            }
            // Clipped to the cell so no stroke cap or shadow reaches the neighbour; channels end on the edge.
            clipRect(rect.left, rect.top, rect.right, rect.bottom) {
                drawPipe(pipes.prepared(cell.section), rect, fills, scratch)
            }
        }
    }
}

/** The glow alphas of this frame, and how far past the wheel or ring the glow reaches, in pixels. */
private class EndpointGlows(val valve: Float, val finish: Float, val reachPx: Float)

/**
 * The stub of a start or finish: the straight's pipe art (`H` for a left or right open side, `V`
 * otherwise) clipped to the half of the cell between the centre and [openSide], with [fills].
 * Identical to a placed straight, so a section against the open side joins it like two sections.
 */
private fun DrawScope.drawStub(
    rect: Rect,
    openSide: Side,
    pipes: PipeArtLibrary,
    scratch: LiquidScratch,
    fills: Map<String, ChannelFill>,
) {
    val section = if (openSide == Side.LEFT || openSide == Side.RIGHT) PipeSection.H else PipeSection.V
    val c = rect.center
    val half = when (openSide) {
        Side.LEFT -> Rect(rect.left, rect.top, c.x, rect.bottom)
        Side.RIGHT -> Rect(c.x, rect.top, rect.right, rect.bottom)
        Side.TOP -> Rect(rect.left, rect.top, rect.right, c.y)
        Side.BOTTOM -> Rect(rect.left, c.y, rect.right, rect.bottom)
    }
    clipRect(half.left, half.top, half.right, half.bottom) { drawPipe(pipes.prepared(section), rect, fills, scratch) }
}

/** A soft circle of [color] at [alpha] behind the wheel or ring, fading out [reachPx] past it. */
private fun DrawScope.drawEndpointGlow(rect: Rect, color: Color, alpha: Float, reachPx: Float) {
    if (alpha <= 0f) return
    val inner = rect.width * EndpointGeometry.WHEEL_RADIUS
    val radius = inner + reachPx
    drawCircle(
        Brush.radialGradient(
            0f to color.copy(alpha = alpha),
            inner / radius to color.copy(alpha = alpha),
            1f to color.copy(alpha = 0f),
            center = rect.center,
            radius = radius,
        ),
        radius = radius,
        center = rect.center,
    )
}

/** The start valve's brass wheel with its spokes, hub and highlight, and the chevron towards [openSide]. */
private fun DrawScope.drawValveWheel(rect: Rect, openSide: Side) {
    val c = rect.center
    val cell = rect.width
    fun at(p: CellPoint) = Offset(c.x + p.x * cell, c.y + p.y * cell)
    val g = EndpointGeometry
    val wheel = cell * g.WHEEL_RADIUS
    drawCircle(Brush.radialGradient(listOf(LeakOutColors.Brass300, LeakOutColors.Brass500), center = c, radius = wheel), wheel, c)
    drawCircle(LeakOutColors.Brass700, wheel - cell * g.RIM_WIDTH / 2f, c, style = Stroke(cell * g.RIM_WIDTH))
    val reach = cell * g.SPOKE_REACH
    drawLine(LeakOutColors.Brass900, Offset(c.x, c.y - reach), Offset(c.x, c.y + reach), cell * g.SPOKE_WIDTH, StrokeCap.Round)
    drawLine(LeakOutColors.Brass900, Offset(c.x - reach, c.y), Offset(c.x + reach, c.y), cell * g.SPOKE_WIDTH, StrokeCap.Round)
    drawCircle(LeakOutColors.Brass700, cell * g.HUB_RADIUS, c)
    drawCircle(LeakOutColors.Brass900, cell * (g.HUB_RADIUS - g.HUB_EDGE / 2f), c, style = Stroke(cell * g.HUB_EDGE))
    drawCircle(LeakOutColors.Brass100.copy(alpha = g.HIGHLIGHT_ALPHA), cell * g.HIGHLIGHT_RADIUS, at(g.HIGHLIGHT_CENTER))
    val chevron = g.chevron(openSide)
    val path = Path().apply {
        moveTo(at(chevron.legA).x, at(chevron.legA).y)
        lineTo(at(chevron.tip).x, at(chevron.tip).y)
        lineTo(at(chevron.legB).x, at(chevron.legB).y)
    }
    drawPath(path, LeakOutColors.Brass300, style = Stroke(cell * g.CHEVRON_WIDTH, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** The finish: the ring's inner disc filled with the light and dark checker, under the success ring. */
private fun DrawScope.drawCheckeredFinish(rect: Rect) {
    val c = rect.center
    val cell = rect.width
    val g = EndpointGeometry
    val inner = cell * g.RING_INNER_RADIUS
    val disc = Path().apply { addOval(Rect(c, inner)) }
    clipPath(disc) {
        for (square in g.checker) {
            drawRect(
                if (square.light) LeakOutColors.TextPrimary else LeakOutColors.Steel900,
                topLeft = Offset(c.x + square.left * cell, c.y + square.top * cell),
                size = Size(square.size * cell, square.size * cell),
            )
        }
    }
    drawCircle(LeakOutColors.Success500, cell * g.RING_RADIUS, c, style = Stroke(cell * g.RING_WIDTH))
}

private fun DrawScope.drawHighlight(rect: Rect) {
    val inset = rect.deflate(rect.width * 0.04f)
    val corner = CornerRadius(rect.width * 0.14f)
    drawRoundRect(LeakOutColors.Brass500.copy(alpha = 0.22f), inset.topLeft, inset.size, corner)
    drawRoundRect(LeakOutColors.Brass300, inset.topLeft, inset.size, corner, style = Stroke(3.dp.toPx()))
}

/**
 * A fan of droplets out of the middle of [side] of the tile [rect], in the liquid colour: a short
 * jet on the side and three rows of drops that spread wider the further they fly. Drawn in tile
 * coordinates, so it follows pan and zoom; drops past the field edge are clipped by the field.
 */
private fun DrawScope.drawLeak(rect: Rect, side: Side, frame: LeakFrame) {
    val cell = rect.width
    val out = Offset(side.dx.toFloat(), side.dy.toFloat())
    val across = Offset(-out.y, out.x)
    val origin = rect.center + out * (cell / 2f)
    val reach = frame.length * cell
    drawLine(
        LeakOutColors.Liquid500.copy(alpha = frame.alpha),
        origin - out * (cell * 0.08f),
        origin + out * (reach * 0.35f),
        cell * 0.12f,
        StrokeCap.Round,
    )
    for (row in 1..LEAK_ROWS) {
        val along = row.toFloat() / LEAK_ROWS
        val center = origin + out * (reach * along)
        val radius = cell * (0.075f - 0.015f * row)
        for (i in -1..1) {
            val drop = center + across * (i * frame.spread * cell * 0.5f * along)
            drawCircle(LeakOutColors.Liquid300.copy(alpha = frame.alpha), radius, drop)
            drawCircle(LeakOutColors.LiquidGlow.copy(alpha = frame.alpha), radius * 0.4f, drop - Offset(radius * 0.3f, radius * 0.3f))
        }
    }
}

private const val LEAK_ROWS = 3

/** The pulse on a missed mandatory section: a `leak.500` fill and a 3 dp edge over the tile, at [alpha]. */
private fun DrawScope.drawMissedPulse(rect: Rect, alpha: Float) {
    val inset = rect.deflate(rect.width * 0.04f)
    val corner = CornerRadius(rect.width * 0.14f)
    val edge = 3.dp.toPx()
    drawRoundRect(LeakOutColors.Leak500.copy(alpha = 0.35f * alpha), inset.topLeft, inset.size, corner)
    val edged = inset.deflate(edge / 2f)
    drawRoundRect(LeakOutColors.Leak500.copy(alpha = alpha), edged.topLeft, edged.size, corner, style = Stroke(edge))
}

/**
 * The replacement on the tile [rect]. While breaking, the old section shakes and cracks along its
 * quarters, then the quarters fly apart, turn, fall and fade, over the neighbours if need be. Once
 * it is gone, the tile is tinted and a ring fills clockwise from the top up to the landing.
 */
private fun DrawScope.drawReplacement(
    replacement: Replacement,
    rect: Rect,
    frame: ReplacementFrame,
    pipes: PipeArtLibrary,
    scratch: LiquidScratch,
) {
    val cell = rect.width
    val breakProgress = frame.breakProgress
    if (breakProgress == null) {
        drawBusyRing(rect, frame.progress)
        return
    }
    val art = pipes.prepared(replacement.removed)
    val scatter = frame.scatter
    if (scatter == 0f) {
        val shake = sin(breakProgress * SHAKE_WAVES) * cell * 0.025f * frame.crack
        translate(left = shake) {
            clipRect(rect.left, rect.top, rect.right, rect.bottom) { drawPipe(art, rect, emptyMap(), scratch) }
            drawCracks(rect, frame.crack)
        }
        return
    }
    val half = cell / 2f
    for (fragment in fragmentsFor(replacement.at)) {
        val piece = Rect(Offset(rect.left + fragment.column * half, rect.top + fragment.row * half), Size(half, half))
        val dx = fragment.dx * cell * scatter
        val dy = (fragment.dy * scatter + FRAGMENT_FALL * scatter * scatter) * cell
        withTransform({
            translate(dx, dy)
            rotate(fragment.spinDegrees * scatter, piece.center)
            clipRect(piece.left, piece.top, piece.right, piece.bottom)
        }) {
            withAlpha(1f - scatter, piece) { drawPipe(art, rect, emptyMap(), scratch) }
        }
    }
}

/** Cracks from the tile centre towards the middle of each side, along the seams the quarters break on, [crack] of the way. */
private fun DrawScope.drawCracks(rect: Rect, crack: Float) {
    if (crack <= 0f) return
    val cell = rect.width
    val c = rect.center
    for (side in Side.entries) {
        val out = Offset(side.dx.toFloat(), side.dy.toFloat())
        val across = Offset(-out.y, out.x)
        val kink = c + out * (cell * 0.22f * crack) + across * (cell * 0.05f)
        val end = c + out * (cell * 0.5f * crack)
        drawLine(LeakOutColors.InkDark, c, kink, cell * 0.035f, StrokeCap.Round)
        drawLine(LeakOutColors.InkDark, kink, end, cell * 0.03f, StrokeCap.Round)
    }
}

/** The tile tint and the ring that fills to [progress] while the new section is on its way. */
private fun DrawScope.drawBusyRing(rect: Rect, progress: Float) {
    val cell = rect.width
    val inset = rect.deflate(cell * 0.04f)
    drawRoundRect(LeakOutColors.Brass500.copy(alpha = 0.12f), inset.topLeft, inset.size, CornerRadius(cell * 0.14f))
    val radius = cell * 0.28f
    val stroke = cell * 0.07f
    drawCircle(LeakOutColors.Steel700.copy(alpha = 0.7f), radius, rect.center, style = Stroke(stroke))
    drawArc(
        LeakOutColors.Brass300,
        startAngle = -90f,
        sweepAngle = 360f * progress,
        useCenter = false,
        topLeft = rect.center - Offset(radius, radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(stroke, cap = StrokeCap.Round),
    )
}

/** Draws [block] into a layer over [bounds] at [alpha], so overlapping strokes fade as one. */
private inline fun DrawScope.withAlpha(alpha: Float, bounds: Rect, block: DrawScope.() -> Unit) {
    val canvas = drawContext.canvas
    canvas.saveLayer(bounds, Paint().apply { this.alpha = alpha.coerceIn(0f, 1f) })
    block()
    canvas.restore()
}

private const val SHAKE_WAVES = 40f
private const val FRAGMENT_FALL = 0.3f

/**
 * The success ring on the finish tile [rect]: a soft green fill and a solid green outline, both at
 * the frame's alpha, scaled around the tile centre. In tile coordinates, so it follows pan and zoom.
 */
private fun DrawScope.drawFinishRing(rect: Rect, frame: FinishFlashFrame) {
    val cell = rect.width
    scale(frame.ringScale, pivot = rect.center) {
        val inset = rect.deflate(cell * 0.06f)
        val corner = CornerRadius(cell * 0.16f)
        drawRoundRect(LeakOutColors.Success500.copy(alpha = 0.25f * frame.ringAlpha), inset.topLeft, inset.size, corner)
        drawRoundRect(
            LeakOutColors.Success500.copy(alpha = frame.ringAlpha),
            inset.topLeft,
            inset.size,
            corner,
            style = Stroke(cell * 0.08f),
        )
    }
}

/**
 * "+N" for every popup whose tile touches the field, as [PopupStyle] describes: Fredoka bold at a
 * third of a tile, a liquid-glow halo and stroke under an opaque deep fill, centred on the tile and
 * lifted by its rise. Off-field popups are skipped but keep their timing, since the age comes from
 * [nowMs].
 */
private fun DrawScope.drawPopups(popups: List<Award>, nowMs: Long, viewport: FieldViewport, textMeasurer: TextMeasurer) {
    if (popups.isEmpty()) return
    val field = Rect(Offset.Zero, size)
    val style = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = (viewport.cellPx * PopupStyle.TEXT_FRACTION).toSp())
    val edge = Stroke(PopupStyle.strokeWidth.toPx(), join = StrokeJoin.Round)
    val glowRadius = PopupStyle.glowRadius.toPx()
    for (popup in popups) {
        val frame = popupFrame(popup.ageAt(nowMs)) ?: continue
        val rect = viewport.cellRect(popup.position)
        if (!rect.overlaps(field)) continue
        val layout = textMeasurer.measure(PointsFormat.text(popup.points), style)
        val center = rect.center - Offset(0f, frame.rise * viewport.cellPx)
        val topLeft = center - Offset(layout.size.width / 2f, layout.size.height / 2f)
        scale(frame.scale, pivot = center) {
            drawText(layout, PopupStyle.glow, topLeft, alpha = frame.alpha, shadow = Shadow(PopupStyle.glowColor(frame.alpha), Offset.Zero, glowRadius))
            drawText(layout, PopupStyle.stroke, topLeft, alpha = frame.alpha, drawStyle = edge)
            drawText(layout, PopupStyle.fill, topLeft, alpha = frame.alpha)
        }
    }
}

private val FieldShape = RoundedCornerShape(24.dp)

/**
 * A start or finish opening to [side] with a placed straight against that side, as in the
 * design-system gallery ("Start + straight", "Straight + checkered finish").
 */
@Composable
private fun EndpointPairPreview(endpoint: Cell, side: Side) {
    val horizontal = side == Side.LEFT || side == Side.RIGHT
    val grid = if (horizontal) GridSize(2, 1) else GridSize(1, 2)
    val at = Position(if (side == Side.LEFT) 1 else 0, if (side == Side.TOP) 1 else 0)
    val pipe = Position(at.x + side.dx, at.y + side.dy)
    val board = Board(grid, mapOf(at to endpoint, pipe to Cell.Pipe(if (horizontal) PipeSection.H else PipeSection.V)))
    val cell = FieldLayout.MIN_CELL
    PlayingField(
        board = board,
        state = rememberPlayingFieldState(grid, at),
        highlighted = null,
        enabled = false,
        modifier = Modifier.size(cell * grid.columns, cell * grid.rows),
        reducedMotion = true,
    )
}

/** The start valve or the checkered finish ([start]) for every open side, each next to a placed straight. */
@Composable
internal fun EndpointPreviewStates(start: Boolean, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (side in Side.entries) EndpointPairPreview(if (start) Cell.Start(side) else Cell.Finish(side), side)
    }
}

@Preview(name = "Start valve, all open sides", widthDp = 280, heightDp = 120, backgroundColor = 0xFF0C1A2A, showBackground = true)
@Composable
private fun StartValvePreview() {
    LeakOutTheme { EndpointPreviewStates(start = true) }
}

@Preview(name = "Checkered finish, all open sides", widthDp = 280, heightDp = 120, backgroundColor = 0xFF0C1A2A, showBackground = true)
@Composable
private fun CheckeredFinishPreview() {
    LeakOutTheme { EndpointPreviewStates(start = false) }
}

/**
 * Popups as in the design-system gallery: "+10" over a filled straight and "+50" over an empty
 * tile, halfway through the hold, at the [cell] size. Without [popups] the same tiles are drawn bare.
 */
@Composable
internal fun PopupPreviewStates(cell: Dp, modifier: Modifier = Modifier, popups: Boolean = true) {
    val grid = GridSize(2, 1)
    val straight = Position(0, 0)
    val board = Board(grid, mapOf(straight to Cell.Pipe(PipeSection.H)))
    val used = setOf(ChannelUse(straight, Channel.SINGLE))
    PlayingField(
        board = board,
        state = rememberPlayingFieldState(grid, straight),
        highlighted = null,
        enabled = false,
        modifier = modifier.size(cell * grid.columns, cell * grid.rows),
        awards = if (popups) listOf(Award(straight, 10, atMs = 0L), Award(Position(1, 0), 50, atMs = 0L)) else emptyList(),
        nowMs = { PopupTiming.FADE_IN_MS + PopupTiming.HOLD_MS / 2 },
        filled = used,
        flow = { FlowState.Ended(used) },
        reducedMotion = true,
    )
}

@Preview(name = "Points popup at 44 dp and 120 dp", widthDp = 360, heightDp = 140, backgroundColor = 0xFF0C1A2A, showBackground = true)
@Composable
private fun PointsPopupPreview() {
    LeakOutTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PopupPreviewStates(FieldLayout.MIN_CELL)
            PopupPreviewStates(120.dp)
        }
    }
}
