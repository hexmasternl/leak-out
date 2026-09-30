package nl.hexmaster.leakout.ui.pipes

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.ui.theme.LeakOutColors

/** The fills a [PipeTile] shows, by channel id, for tests. */
val PipeFillsKey = SemanticsPropertyKey<Map<String, ChannelFill>>("PipeFills")
var SemanticsPropertyReceiver.pipeFills by PipeFillsKey

fun pipeTileTag(section: PipeSection) = "pipe-tile-${section.name}"

/**
 * Draws [section] from its SVG asset, scaled to the tile, with the liquid of each channel in
 * [fills] (by channel id: `channel-main`, or `channel-h` / `channel-v` for the cross). A channel
 * without a fill is dry. Layers are those of [drawPipe].
 */
@Composable
fun PipeTile(
    section: PipeSection,
    modifier: Modifier = Modifier,
    fills: Map<String, ChannelFill> = emptyMap(),
) {
    val library = rememberPipeArtLibrary()
    val art = remember(library, section) { library.prepared(section) }
    val scratch = remember { LiquidScratch() }
    require(fills.keys.all { id -> art.channels.any { it.channel.id == id } }) {
        "Unknown channel in $fills for $section; it has ${art.channels.map { it.channel.id }}"
    }
    Canvas(
        modifier
            .clipToBounds()
            .testTag(pipeTileTag(section))
            .semantics { pipeFills = fills },
    ) {
        drawPipe(art, size.toRect(), fills, scratch)
    }
}

/**
 * Draws [art] into [rect] of this canvas, scaled so the cell fills the shorter side and centred on
 * the longer one, with the liquid of each channel in [fills]. Layers, bottom to top: the asset's
 * `under` group, the liquid in channel order, the `over` group, the glass overlay. The tile and
 * the playing field both draw pipes with this, so they look the same.
 */
internal fun DrawScope.drawPipe(
    art: PreparedPipeArt,
    rect: Rect,
    fills: Map<String, ChannelFill> = emptyMap(),
    scratch: LiquidScratch,
) {
    val cell = rect.minDimension
    val scale = cell / PipeArt.VIEW_BOX_SIZE
    translate(rect.left + (rect.width - cell) / 2f, rect.top + (rect.height - cell) / 2f) {
        scale(scale, scale, pivot = Offset.Zero) {
            drawShapes(art.under)
            for (channel in art.channels) {
                val fill = fills[channel.channel.id] ?: continue
                val tank = art.art.tank
                if (tank != null && art.tankClip != null) {
                    drawReservoirLiquid(channel, tank, art.tankClip, fill, scratch)
                } else {
                    val bridges = art.art.bridges.filter { it.hides == channel.channel.id }
                    drawWithout(bridges) {
                        drawLiquid(channel, LiquidSpans.forChannel(channel.channel, fill), fill.clamped, scratch)
                    }
                }
            }
            drawShapes(art.over)
            drawShapes(art.glass)
        }
    }
}

/** Reused per canvas so drawing the liquid does not allocate a path every frame. */
internal class LiquidScratch {
    val measure = PathMeasure()
    val segment = Path()
}

private fun DrawScope.drawShapes(shapes: List<PreparedShape>) {
    for (shape in shapes) {
        shape.fill?.let { drawPath(shape.path, it, alpha = shape.fillAlpha, style = Fill) }
        shape.stroke?.let { drawPath(shape.path, it, alpha = shape.strokeAlpha, style = shape.strokeStyle) }
    }
}

private fun DrawScope.drawWithout(bridges: List<Bridge>, block: DrawScope.() -> Unit) {
    if (bridges.isEmpty()) {
        block()
        return
    }
    val bridge = bridges.first()
    clipRect(bridge.x, bridge.y, bridge.x + bridge.width, bridge.y + bridge.height, clipOp = ClipOp.Difference) {
        drawWithout(bridges.drop(1), block)
    }
}

// Liquid sizes in view box units (design system 6.3/6.5): liquid 21 wide inside a 25 bore.
private const val LIQUID_WIDTH = 21f
private const val LIQUID_CORE_WIDTH = 15f
private const val LIQUID_HIGHLIGHT_WIDTH = 5f
private const val GLOW_WIDTH = 31f
private const val HEAD_GLOW_RADIUS = 8f

/**
 * One span of liquid: the liquid gradient across the bore (edge `liquid.700`, body `liquid.500`,
 * a `liquid.300` highlight in the middle) as three strokes, so it follows straights and elbows
 * alike; plus a glow and a rounded, glowing head while the channel is filling.
 */
private fun DrawScope.drawLiquid(channel: PreparedChannel, span: PathSpan, progress: Float, scratch: LiquidScratch) {
    if (span.isEmpty) return
    val segment = scratch.segment
    segment.rewind()
    scratch.measure.setPath(channel.path, false)
    val length = scratch.measure.length
    scratch.measure.getSegment(span.start * length, span.end * length, segment, true)

    val filling = progress > 0f && progress < 1f
    if (filling) {
        drawPath(segment, LeakOutColors.LiquidGlow, alpha = 0.3f, style = Stroke(GLOW_WIDTH, cap = StrokeCap.Butt))
    }
    drawPath(segment, LeakOutColors.Liquid700, style = Stroke(LIQUID_WIDTH, cap = StrokeCap.Butt))
    drawPath(segment, LeakOutColors.Liquid500, style = Stroke(LIQUID_CORE_WIDTH, cap = StrokeCap.Butt))
    drawPath(segment, LeakOutColors.Liquid300, alpha = 0.9f, style = Stroke(LIQUID_HIGHLIGHT_WIDTH, cap = StrokeCap.Butt))

    span.head?.let { head ->
        val at = scratch.measure.getPosition(head * length)
        drawCircle(LeakOutColors.Liquid700, radius = LIQUID_WIDTH / 2f, center = at)
        drawCircle(LeakOutColors.Liquid500, radius = LIQUID_CORE_WIDTH / 2f, center = at)
        drawCircle(LeakOutColors.LiquidGlow, radius = HEAD_GLOW_RADIUS / 2f + 2f, center = at)
    }
}

private fun DrawScope.drawReservoirLiquid(
    channel: PreparedChannel,
    tank: TankCircle,
    tankClip: Path,
    fill: ChannelFill,
    scratch: LiquidScratch,
) {
    val reservoir = LiquidSpans.forReservoir(channel.channel, tank, fill)
    val p = fill.clamped
    drawLiquid(channel, reservoir.inlet, if (reservoir.inlet.head != null) p else 1f, scratch)
    if (reservoir.tankLevel > 0f) {
        val bottom = tank.cy + tank.r
        val top = bottom - reservoir.tankLevel * 2f * tank.r
        clipPath(tankClip) {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to LeakOutColors.Liquid300,
                    0.45f to LeakOutColors.Liquid500,
                    1f to LeakOutColors.Liquid700,
                    startY = top,
                    endY = bottom,
                ),
                topLeft = Offset(tank.cx - tank.r, top),
                size = Size(2f * tank.r, bottom - top),
            )
            if (reservoir.tankLevel < 1f) {
                // The rising surface.
                drawLine(
                    LeakOutColors.LiquidGlow,
                    Offset(tank.cx - tank.r, top),
                    Offset(tank.cx + tank.r, top),
                    strokeWidth = 2.5f,
                )
            }
        }
    }
    drawLiquid(channel, reservoir.outlet, if (reservoir.outlet.head != null) p else 1f, scratch)
}
