package nl.hexmaster.leakout.ui.pipes

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.ReservoirType

/**
 * Loads the pipe assets once and keeps them ready to draw: parsed [PipeArt] and the section's
 * glass overlay, plus Compose [Path]s and [Brush]es in the 0–100 view box. The tile scales the canvas, so one prepared set serves
 * every cell size.
 */
class PipeArtLibrary private constructor(
    private val open: (fileName: String) -> InputStream,
    private val caches: Caches,
    /** The reservoir type [prepared] draws the reservoir in when none is given; see [forReservoir]. */
    val reservoirType: ReservoirType,
) {
    constructor(open: (fileName: String) -> InputStream) : this(open, Caches(), ReservoirType.DEFAULT)

    private class Caches {
        val art = ConcurrentHashMap<PipeSection, PipeArt>()
        val glass = ConcurrentHashMap<PipeSection, List<SvgShape>>()
        val prepared = ConcurrentHashMap<Pair<PipeSection, ReservoirType>, PreparedPipeArt>()
    }

    /** A view of this library, sharing its caches, that draws the reservoir as [type]. */
    fun forReservoir(type: ReservoirType): PipeArtLibrary =
        if (type == reservoirType) this else PipeArtLibrary(open, caches, type)

    fun art(section: PipeSection): PipeArt =
        caches.art.getOrPut(section) { open(fileName(section)).use { PipeArtParser.parse(it) } }

    /** The shapes of the section's glass overlay, drawn above everything else. */
    fun glass(section: PipeSection): List<SvgShape> =
        caches.glass.getOrPut(section) { open(glassFileName(section)).use { PipeArtParser.parseGlass(it) } }

    /**
     * The section ready to draw. [reservoirType] only matters for the reservoir: its tank is tinted
     * with the type's accent and its badge shows the type's percentage. Every other section is the
     * same for every type.
     */
    fun prepared(section: PipeSection, reservoirType: ReservoirType = this.reservoirType): PreparedPipeArt {
        val type = if (section == PipeSection.RES) reservoirType else ReservoirType.DEFAULT
        return caches.prepared.getOrPut(section to type) {
            val base = art(section)
            if (section != PipeSection.RES) {
                PreparedPipeArt.from(base, glass(section))
            } else {
                val accent = ReservoirAccent.of(type)
                val tinted = base.copy(
                    under = base.under.tinted(accent),
                    over = base.over.tinted(accent) + ReservoirBadgeText.shapes(type.percent, accent.c300),
                )
                PreparedPipeArt.from(tinted, glass(section))
            }
        }
    }

    /** Parses and prepares every section now, so the first frame that draws them does not have to. */
    fun preloadAll() {
        PipeSection.entries.forEach { prepared(it) }
        ReservoirType.entries.forEach { prepared(PipeSection.RES, it) }
    }

    companion object {
        const val ASSET_DIR = "pipes"

        fun fileName(section: PipeSection): String = "$ASSET_DIR/${section.name.lowercase()}.svg"

        fun glassFileName(section: PipeSection): String = "$ASSET_DIR/${section.name.lowercase()}-glass.svg"

        @Volatile private var shared: PipeArtLibrary? = null

        /** One library per process, read from the app's assets. */
        fun get(context: Context): PipeArtLibrary =
            shared ?: synchronized(this) {
                shared ?: context.applicationContext.assets.let { assets ->
                    PipeArtLibrary { assets.open(it) }
                }.also { shared = it }
            }
    }
}

/** Provides the library to [PipeTile]; defaults to the app's shared library. */
val LocalPipeArtLibrary = staticCompositionLocalOf<PipeArtLibrary?> { null }

/** The reservoir type of the level being shown; reservoirs are drawn in its accent. Purple (30 %) when none is provided. */
val LocalReservoirType = staticCompositionLocalOf { ReservoirType.DEFAULT }

@Composable
fun rememberPipeArtLibrary(): PipeArtLibrary {
    val type = LocalReservoirType.current
    LocalPipeArtLibrary.current?.let { return it.forReservoir(type) }
    val context = LocalContext.current
    return remember(context, type) { PipeArtLibrary.get(context).forReservoir(type) }
}

class PreparedShape(
    val path: Path,
    val fill: Brush?,
    val fillAlpha: Float,
    val stroke: Brush?,
    val strokeAlpha: Float,
    val strokeStyle: Stroke,
)

class PreparedChannel(val channel: ArtChannel, val path: Path) {
    val length: Float = PathMeasure().apply { setPath(path, false) }.length
}

class PreparedPipeArt(
    val art: PipeArt,
    val under: List<PreparedShape>,
    val over: List<PreparedShape>,
    val glass: List<PreparedShape>,
    val channels: List<PreparedChannel>,
    val tankClip: Path?,
) {
    fun channel(id: String): PreparedChannel = channels.first { it.channel.id == id }

    companion object {
        fun from(art: PipeArt, glass: List<SvgShape> = emptyList()): PreparedPipeArt = PreparedPipeArt(
            art = art,
            under = art.under.map(::prepare),
            over = art.over.map(::prepare),
            glass = glass.map(::prepare),
            channels = art.channels.map { PreparedChannel(it, parsePath(it.pathData)) },
            tankClip = art.tank?.let { tank ->
                Path().apply { addOval(Rect(Offset(tank.cx, tank.cy), tank.r)) }
            },
        )

        private fun parsePath(pathData: String): Path = PathParser().parsePathString(pathData).toPath()

        private fun prepare(shape: SvgShape): PreparedShape {
            val path = when (shape) {
                is SvgShape.PathShape -> parsePath(shape.pathData)
                is SvgShape.RectShape -> Path().apply {
                    addRoundRect(
                        RoundRect(
                            Rect(shape.x, shape.y, shape.x + shape.width, shape.y + shape.height),
                            CornerRadius(shape.rx),
                        ),
                    )
                }
                is SvgShape.CircleShape -> Path().apply { addOval(Rect(Offset(shape.cx, shape.cy), shape.r)) }
            }
            val style = shape.style
            return PreparedShape(
                path = path,
                fill = style.fill?.let(::brush),
                fillAlpha = style.opacity * style.fillOpacity,
                stroke = style.stroke?.let(::brush),
                strokeAlpha = style.opacity * style.strokeOpacity,
                strokeStyle = Stroke(
                    width = style.strokeWidth,
                    cap = when (style.lineCap) {
                        SvgLineCap.BUTT -> StrokeCap.Butt
                        SvgLineCap.ROUND -> StrokeCap.Round
                        SvgLineCap.SQUARE -> StrokeCap.Square
                    },
                    join = when (style.lineJoin) {
                        SvgLineJoin.MITER -> StrokeJoin.Miter
                        SvgLineJoin.ROUND -> StrokeJoin.Round
                        SvgLineJoin.BEVEL -> StrokeJoin.Bevel
                    },
                ),
            )
        }

        private fun brush(paint: SvgPaint): Brush = when (paint) {
            is SvgPaint.Solid -> SolidColor(colour(paint.rgb))
            is SvgPaint.Gradient -> {
                val g = paint.gradient
                Brush.linearGradient(
                    colorStops = g.stops.map { it.offset to colour(it.rgb).copy(alpha = it.opacity) }.toTypedArray(),
                    start = Offset(g.x1, g.y1),
                    end = Offset(g.x2, g.y2),
                )
            }
        }

        private fun colour(rgb: Int) = Color(0xFF000000.toInt() or rgb)
    }
}
