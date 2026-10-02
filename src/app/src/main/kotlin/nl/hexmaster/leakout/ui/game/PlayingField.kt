package nl.hexmaster.leakout.ui.game

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import androidx.compose.ui.unit.Density
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
import nl.hexmaster.leakout.core.level.GridSize
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.ui.pipes.ChannelFill
import nl.hexmaster.leakout.ui.pipes.LiquidScratch
import nl.hexmaster.leakout.ui.pipes.PipeArtLibrary
import nl.hexmaster.leakout.ui.pipes.drawPipe
import nl.hexmaster.leakout.ui.pipes.rememberPipeArtLibrary
import nl.hexmaster.leakout.ui.theme.Fredoka
import nl.hexmaster.leakout.ui.theme.LeakOutColors

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

/** Semantics flag on the field cell of a running replacement. A test hook only. */
val ReplacingCell = SemanticsPropertyKey<Boolean>("ReplacingCell")
var SemanticsPropertyReceiver.replacingCell by ReplacingCell

/**
 * The screen state of the playing field: the viewport and where the field sits on the screen.
 * Created again for every attempt, so a restart goes back to the starting view.
 */
@Stable
class PlayingFieldState(private val grid: GridSize, private val start: Position) {
    var viewport: FieldViewport? by mutableStateOf(null)
        private set

    private var coordinates: LayoutCoordinates? = null
    private var density: Density? = null

    /** Where the field's top-left corner is in the root, updated on every layout pass. */
    var originInRoot: Offset? by mutableStateOf(null)
        private set

    internal fun onMeasured(size: Size, density: Density) {
        val current = viewport
        if (size.width <= 0f || size.height <= 0f) return
        this.density = density
        if (current == null || current.fieldSize != size) viewport = FieldViewport.initial(size, grid, start, density)
    }

    internal fun onPositioned(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
        originInRoot = coordinates.positionInRoot()
    }

    /** The on-screen rect of [position] in root coordinates, at the current view; null before layout. */
    fun cellInRoot(position: Position): Rect? {
        val origin = originInRoot ?: return null
        return viewport?.cellRect(position)?.translate(origin)
    }

    /** The field's own rect in root coordinates; null before layout. */
    fun boundsInRoot(): Rect? {
        val origin = originInRoot ?: return null
        return viewport?.let { Rect(origin, it.fieldSize) }
    }

    /** The view the level starts with, at the current field size; null before layout. */
    fun initialView(): FieldViewport? {
        val current = viewport ?: return null
        val density = density ?: return null
        return FieldViewport.initial(current.fieldSize, grid, start, density)
    }

    /**
     * Moves the view to [target]: at once with [reducedMotion], otherwise over `motion.settle`
     * (240 ms, decelerating, so the pan never overshoots its limits).
     */
    suspend fun animateTo(target: FieldViewport?, reducedMotion: Boolean) {
        val from = viewport ?: return
        if (target == null || target == from) return
        if (reducedMotion) {
            viewport = target
            return
        }
        animate(0f, 1f, animationSpec = tween(FIELD_SETTLE_MS, easing = FastOutSlowInEasing)) { t, _ ->
            viewport = target.copy(
                cellPx = from.cellPx + (target.cellPx - from.cellPx) * t,
                offset = from.offset + (target.offset - from.offset) * t,
            )
        }
        viewport = target
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

private const val FIELD_SETTLE_MS = 240

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
 * [lossAgeMs] (read only while drawing), above the pipes and below the popups.
 *
 * Placed sections are drawn with their pipe art and glass overlay (`drawPipe`), like the stack.
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
    head: Position? = null,
    filled: Set<ChannelUse> = emptySet(),
    flow: () -> FlowState? = { null },
    winAgeMs: (() -> Long)? = null,
    replacement: Replacement? = null,
    landing: Landing? = null,
    onTap: (Position) -> Unit = {},
) {
    val density = LocalDensity.current
    val currentEnabled by rememberUpdatedState(enabled)
    val currentOnTap by rememberUpdatedState(onTap)
    val textMeasurer = rememberTextMeasurer()
    val pipes = rememberPipeArtLibrary()
    val scratch = remember { LiquidScratch() }
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
            for (position in cells) {
                val cell = board[position]
                val fills = if (cell is Cell.Pipe) FieldLiquid.fills(position, cell.section, used, flowing) else emptyMap()
                val rect = viewport.cellRect(position)
                if (pop != null && cell is Cell.Pipe && position == landing?.at) {
                    scale(pop, pivot = rect.center) { drawCell(cell, rect, pipes, scratch, fills) }
                } else {
                    drawCell(cell, rect, pipes, scratch, fills)
                }
            }
            replacement?.let { drawReplacement(it, viewport.cellRect(it.at), replacementFrame(it, now), pipes, scratch) }
            highlighted?.let { drawHighlight(viewport.cellRect(it)) }
            if (leakAt != null && leakSide != null) drawLeak(viewport.cellRect(leakAt), leakSide, leakFrame(lossAgeMs()))
            if (winAgeMs != null) {
                val frame = finishFlashFrame(winAgeMs())
                for (position in cells) if (board[position] is Cell.Finish) drawFinishRing(viewport.cellRect(position), frame)
            }
            drawPopups(awards, now, viewport, textMeasurer)
        }
        CellSemantics(
            board, viewport, highlighted, awards, leakAt?.let { at -> leakSide?.let { at to it } }, head, filled, winAgeMs != null,
            replacing = replacement?.at,
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
) {
    val density = LocalDensity.current
    val cellDp = with(density) { viewport.cellPx.toDp() }
    val popupTexts = awards.groupBy({ it.position }, { PointsFormat.text(it.points) })
    val filledByCell = filled.groupBy({ it.position }, { it.channel })
    for (position in viewport.visibleCells()) {
        val rect = viewport.cellRect(position)
        val description = board[position].describe()
        val isHighlighted = position == highlighted
        val texts = popupTexts[position].orEmpty()
        val leakSide = leak?.takeIf { it.first == position }?.second
        val isHead = position == head
        val channels = filledByCell[position].orEmpty().toSet()
        val flashing = won && board[position] is Cell.Finish
        val section = (board[position] as? Cell.Pipe)?.section
        val isReplacing = position == replacing
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
                },
        )
    }
}

private fun Cell.describe(): String = when (this) {
    Cell.Empty -> "Empty"
    Cell.Blocked -> "Blocked"
    is Cell.Start -> "Start"
    is Cell.Finish -> "Finish"
    is Cell.Pipe -> if (liquidEntered) "Placed ${section.name}, filled" else "Placed ${section.name}"
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
        is Cell.Start -> drawEndpoint(inset, cell.openSide, LeakOutColors.Brass500)
        is Cell.Finish -> drawEndpoint(inset, cell.openSide, LeakOutColors.SuccessGreen)
        is Cell.Pipe -> {
            drawRoundRect(LeakOutColors.SurfaceCard.copy(alpha = 0.06f), inset.topLeft, inset.size, corner)
            // Clipped to the cell so no stroke cap or shadow reaches the neighbour; channels end on the edge.
            clipRect(rect.left, rect.top, rect.right, rect.bottom) {
                drawPipe(pipes.prepared(cell.section), rect, fills, scratch)
            }
        }
    }
}

/** A start or finish: a coloured disc with a stub out of its open side. */
private fun DrawScope.drawEndpoint(rect: Rect, openSide: Side, color: Color) {
    drawRoundRect(color.copy(alpha = 0.18f), rect.topLeft, rect.size, CornerRadius(rect.width * 0.14f))
    val center = rect.center
    val edge = when (openSide) {
        Side.LEFT -> Offset(rect.left, center.y)
        Side.RIGHT -> Offset(rect.right, center.y)
        Side.TOP -> Offset(center.x, rect.top)
        Side.BOTTOM -> Offset(center.x, rect.bottom)
    }
    drawLine(LeakOutColors.Steel300, center, edge, rect.width * 0.22f, StrokeCap.Butt)
    drawCircle(color, radius = rect.width * 0.26f, center = center)
    drawCircle(LeakOutColors.Steel900, radius = rect.width * 0.10f, center = center)
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
        drawRoundRect(LeakOutColors.SuccessGreen.copy(alpha = 0.25f * frame.ringAlpha), inset.topLeft, inset.size, corner)
        drawRoundRect(
            LeakOutColors.SuccessGreen.copy(alpha = frame.ringAlpha),
            inset.topLeft,
            inset.size,
            corner,
            style = Stroke(cell * 0.08f),
        )
    }
}

/**
 * "+N" for every popup whose tile touches the field: Fredoka bold at 40 % of a tile, an ink outline
 * under a white fill, centred on the tile and lifted by its rise. Off-field popups are skipped but
 * keep their timing, since the age comes from [nowMs].
 */
private fun DrawScope.drawPopups(popups: List<Award>, nowMs: Long, viewport: FieldViewport, textMeasurer: TextMeasurer) {
    if (popups.isEmpty()) return
    val field = Rect(Offset.Zero, size)
    val style = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = (viewport.cellPx * POPUP_TEXT_FRACTION).toSp())
    val outline = Stroke(POPUP_OUTLINE.toPx(), join = StrokeJoin.Round)
    for (popup in popups) {
        val frame = popupFrame(popup.ageAt(nowMs)) ?: continue
        val rect = viewport.cellRect(popup.position)
        if (!rect.overlaps(field)) continue
        val layout = textMeasurer.measure(PointsFormat.text(popup.points), style)
        val center = rect.center - Offset(0f, frame.rise * viewport.cellPx)
        val topLeft = center - Offset(layout.size.width / 2f, layout.size.height / 2f)
        scale(frame.scale, pivot = center) {
            drawText(layout, LeakOutColors.InkDark, topLeft, alpha = frame.alpha, drawStyle = outline)
            drawText(layout, Color.White, topLeft, alpha = frame.alpha)
        }
    }
}

private const val POPUP_TEXT_FRACTION = 0.4f
private val POPUP_OUTLINE = 3.dp

private val FieldShape = RoundedCornerShape(24.dp)
