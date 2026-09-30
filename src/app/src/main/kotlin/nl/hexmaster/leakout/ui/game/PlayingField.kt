package nl.hexmaster.leakout.ui.game

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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
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
import nl.hexmaster.leakout.core.board.Board
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.engine.Award
import nl.hexmaster.leakout.core.flow.Channel
import nl.hexmaster.leakout.core.flow.ChannelUse
import nl.hexmaster.leakout.core.level.GridSize
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
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

/** Semantics flag on the field cell the liquid head is in. A test hook only, until the liquid is drawn. */
val LiquidHead = SemanticsPropertyKey<Boolean>("LiquidHead")
var SemanticsPropertyReceiver.liquidHead by LiquidHead

/** Semantics on a field cell: the channels the liquid has run through there. A test hook only, until the liquid is drawn. */
val FilledChannels = SemanticsPropertyKey<Set<Channel>>("FilledChannels")
var SemanticsPropertyReceiver.filledChannels by FilledChannels

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
 * finger and zooms with two within the viewport's limits. It never rotates. All input is ignored
 * while [enabled] is false, for example during a section drag or while paused.
 *
 * The [awards] are drawn as points popups on their tiles above everything else, timed by the game
 * clock [nowMs]. The clock is a lambda read only while drawing, so a clock tick redraws the canvas
 * without recomposing the grid.
 *
 * When the pipe has leaked at [leakAt] through [leakSide], liquid spurts out of that side, timed by
 * [lossAgeMs] (read only while drawing), above the pipes and below the popups.
 *
 * [head] is the tile the liquid head is in and [filled] the channels the liquid has run through.
 * They are not drawn yet; the liquid drawing will use them.
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
) {
    val density = LocalDensity.current
    val currentEnabled by rememberUpdatedState(enabled)
    val textMeasurer = rememberTextMeasurer()
    Box(
        modifier = modifier
            .testTag(GameScreenTags.FIELD)
            .background(LeakOutColors.SurfaceDeep, FieldShape)
            .border(1.dp, Color.White.copy(alpha = 0.16f), FieldShape)
            .clip(FieldShape)
            .onSizeChanged { state.onMeasured(it.toSize(), density) }
            .onGloballyPositioned { state.onPositioned(it) }
            .pointerInput(state) { detectPanAndZoom(state) { currentEnabled } },
    ) {
        val viewport = state.viewport ?: return@Box
        Canvas(Modifier.fillMaxSize()) {
            val cells = viewport.visibleCells()
            for (position in cells) drawCell(board[position], viewport.cellRect(position))
            highlighted?.let { drawHighlight(viewport.cellRect(it)) }
            if (leakAt != null && leakSide != null) drawLeak(viewport.cellRect(leakAt), leakSide, leakFrame(lossAgeMs()))
            drawPopups(awards, nowMs(), viewport, textMeasurer)
        }
        CellSemantics(board, viewport, highlighted, awards, leakAt?.let { at -> leakSide?.let { at to it } }, head, filled)
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
 * Rotation is never read, so a twist does nothing. A gesture stops as soon as [enabled] is false.
 */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectPanAndZoom(
    state: PlayingFieldState,
    enabled: () -> Boolean,
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        if (!enabled()) return@awaitEachGesture
        var pastSlop = false
        var pendingPan = Offset.Zero
        var pendingZoom = 1f
        while (true) {
            val event = awaitPointerEvent()
            if (!enabled() || event.changes.none { it.pressed }) break
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

private fun DrawScope.drawCell(cell: Cell, rect: Rect) {
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
            val fill = if (cell.liquidEntered) LeakOutColors.Liquid700.copy(alpha = 0.35f) else LeakOutColors.SurfaceCard.copy(alpha = 0.06f)
            drawRoundRect(fill, inset.topLeft, inset.size, corner)
            drawPipeSection(cell.section, rect)
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
