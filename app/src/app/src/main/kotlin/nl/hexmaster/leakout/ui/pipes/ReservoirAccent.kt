package nl.hexmaster.leakout.ui.pipes

import nl.hexmaster.leakout.core.level.ReservoirType

/**
 * The accent colours of one reservoir type as `0xRRGGBB`: the design system's `reservoir.300`
 * (highlight), `reservoir.500` (tank tint) and `reservoir.700` (tank shadow, badge fill).
 */
data class ReservoirAccent(val c300: Int, val c500: Int, val c700: Int) {
    companion object {
        /** The accent of [type]; purple is the violet set `res.svg` is drawn in. */
        fun of(type: ReservoirType): ReservoirAccent = when (type) {
            ReservoirType.RED_10 -> ReservoirAccent(0xFF7A90, 0xF0506E, 0xB83250)
            ReservoirType.BLUE_20 -> ReservoirAccent(0x7FA8FF, 0x4D7CFE, 0x2F56C9)
            ReservoirType.PURPLE_30 -> PURPLE
            ReservoirType.ORANGE_40 -> ReservoirAccent(0xFFC48A, 0xFF9F43, 0xD9771F)
        }

        private val PURPLE = ReservoirAccent(0xC7B5FF, 0xA78BFA, 0x7C5CE6)
    }
}

/** Replaces the purple reservoir colours of `res.svg` with those of [accent] in every shape. */
internal fun List<SvgShape>.tinted(accent: ReservoirAccent): List<SvgShape> {
    val purple = ReservoirAccent.of(ReservoirType.PURPLE_30)
    if (accent == purple) return this
    val map = mapOf(purple.c300 to accent.c300, purple.c500 to accent.c500, purple.c700 to accent.c700)
    fun paint(p: SvgPaint?): SvgPaint? = when (p) {
        is SvgPaint.Solid -> SvgPaint.Solid(map[p.rgb] ?: p.rgb)
        is SvgPaint.Gradient -> SvgPaint.Gradient(
            p.gradient.copy(stops = p.gradient.stops.map { it.copy(rgb = map[it.rgb] ?: it.rgb) }),
        )
        null -> null
    }
    fun style(s: SvgStyle) = s.copy(fill = paint(s.fill), stroke = paint(s.stroke))
    return map { shape ->
        when (shape) {
            is SvgShape.PathShape -> shape.copy(style = style(shape.style))
            is SvgShape.RectShape -> shape.copy(style = style(shape.style))
            is SvgShape.CircleShape -> shape.copy(style = style(shape.style))
        }
    }
}

/**
 * The percentage on a reservoir's "slow" badge as stroked vector glyphs, so it needs no text
 * layout inside the pipe drawing. Laid out in the view box, right of the hourglass, in
 * [ink] (the accent's `reservoir.300`).
 */
internal object ReservoirBadgeText {
    private const val SCALE = 0.85f
    private const val GLYPH_WIDTH = 6f * SCALE
    private const val SPACING = 2f
    private const val RIGHT = 96f
    private const val CENTRE_Y = 14f

    // Glyphs in a 6 x 10 box, y down.
    private val GLYPHS = mapOf(
        '0' to "M1 0 H5 L6 1 V9 L5 10 H1 L0 9 V1 Z",
        '1' to "M1 2.5 L3.5 0 V10",
        '2' to "M0 1.5 L1.5 0 H4.5 L6 1.5 V3.5 L0 10 H6",
        '3' to "M0.5 0 H5.5 L3 4.5 C5 4.5 6 5.5 6 7.2 C6 9 5 10 3 10 C1.5 10 0.5 9.5 0 8.5",
        '4' to "M4.5 10 V0 L0 6.5 H6",
        '%' to "M5.5 0 L0.5 10 M0.3 0.3 H2 V2.2 H0.3 Z M4 7.8 H5.7 V9.7 H4 Z",
    )

    fun shapes(percent: Int, ink: Int): List<SvgShape> {
        val text = buildString { append(percent); append('%') }
        val width = text.length * GLYPH_WIDTH + (text.length - 1) * SPACING
        val left = RIGHT - width
        val style = SvgStyle(
            fill = null,
            stroke = SvgPaint.Solid(ink),
            strokeWidth = 1.5f,
            lineCap = SvgLineCap.ROUND,
            lineJoin = SvgLineJoin.ROUND,
        )
        return text.mapIndexedNotNull { i, ch ->
            val glyph = GLYPHS[ch] ?: return@mapIndexedNotNull null
            val dx = left + i * (GLYPH_WIDTH + SPACING)
            val dy = CENTRE_Y - 5f * SCALE
            SvgShape.PathShape(transform(glyph, dx, dy), style)
        }
    }

    /** Scales and moves the path data, which only holds M L H V C Z commands with absolute coordinates. */
    internal fun transform(data: String, dx: Float, dy: Float): String {
        val tokens = data.split(' ')
        val out = StringBuilder()
        var command = ' '
        var index = 0 // position within the command's arguments
        for (token in tokens) {
            val first = token[0]
            val numberText: String
            if (first.isLetter()) {
                command = first
                index = 0
                out.append(command)
                numberText = token.substring(1)
                if (numberText.isEmpty()) continue
            } else {
                numberText = token
            }
            val value = numberText.toFloat()
            val isX = when (command) {
                'H' -> true
                'V' -> false
                else -> index % 2 == 0
            }
            val moved = value * SCALE + if (isX) dx else dy
            if (out.isNotEmpty() && !out.last().isLetter()) out.append(' ')
            out.append(moved)
            index++
        }
        return out.toString()
    }
}
