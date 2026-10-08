package nl.hexmaster.leakout.ui.pipes

import nl.hexmaster.leakout.core.level.Side

/**
 * One pipe section's artwork as read from its SVG asset (`assets/pipes/<id>.svg`, format in
 * `docs/pipe-assets.md`). Coordinates are in the asset's 0–100 view box, which is one cell.
 *
 * [under] is drawn below the liquid, [over] above it. [channels] are the centre lines the liquid
 * runs along, in drawing order. [tank] is the inside of the reservoir tank; [bridges] are areas
 * where a channel's liquid is hidden under another pipe (the cross).
 */
data class PipeArt(
    val under: List<SvgShape>,
    val over: List<SvgShape>,
    val channels: List<ArtChannel>,
    val tank: TankCircle? = null,
    val bridges: List<Bridge> = emptyList(),
) {
    fun channel(id: String): ArtChannel =
        channels.firstOrNull { it.id == id } ?: throw IllegalArgumentException("No channel '$id'")

    companion object {
        const val VIEW_BOX_SIZE = 100f
    }
}

/** A liquid channel from side [from] to side [to]; [pathData] is SVG path data written from [from]. */
data class ArtChannel(val id: String, val from: Side, val to: Side, val pathData: String) {
    val sides: Set<Side> get() = setOf(from, to)

    fun isHorizontal(): Boolean = from.dy == 0 && to.dy == 0
}

data class TankCircle(val cx: Float, val cy: Float, val r: Float)

/** A rectangle in which the liquid of channel [hides] is not drawn. */
data class Bridge(val hides: String, val x: Float, val y: Float, val width: Float, val height: Float)

sealed interface SvgShape {
    val style: SvgStyle

    data class PathShape(val pathData: String, override val style: SvgStyle) : SvgShape
    data class RectShape(
        val x: Float, val y: Float, val width: Float, val height: Float, val rx: Float,
        override val style: SvgStyle,
    ) : SvgShape
    data class CircleShape(val cx: Float, val cy: Float, val r: Float, override val style: SvgStyle) : SvgShape
}

/** The presentation attributes of a shape after inheritance from its groups, with SVG defaults. */
data class SvgStyle(
    val fill: SvgPaint? = SvgPaint.Solid(0x000000),
    val stroke: SvgPaint? = null,
    val strokeWidth: Float = 1f,
    val lineCap: SvgLineCap = SvgLineCap.BUTT,
    val lineJoin: SvgLineJoin = SvgLineJoin.MITER,
    val opacity: Float = 1f,
    val fillOpacity: Float = 1f,
    val strokeOpacity: Float = 1f,
)

enum class SvgLineCap { BUTT, ROUND, SQUARE }
enum class SvgLineJoin { MITER, ROUND, BEVEL }

sealed interface SvgPaint {
    /** A colour as `0xRRGGBB`. */
    data class Solid(val rgb: Int) : SvgPaint
    data class Gradient(val gradient: SvgLinearGradient) : SvgPaint
}

/** A linear gradient in user space (the 0–100 view box). */
data class SvgLinearGradient(
    val id: String,
    val x1: Float, val y1: Float, val x2: Float, val y2: Float,
    val stops: List<GradientStop>,
)

data class GradientStop(val offset: Float, val rgb: Int, val opacity: Float = 1f)

/** Thrown for SVG that uses anything outside the supported subset, or breaks the asset format. */
class UnsupportedSvgException(message: String) : IllegalArgumentException(message)
