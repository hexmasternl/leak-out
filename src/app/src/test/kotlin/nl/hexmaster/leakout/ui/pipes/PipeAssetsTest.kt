package nl.hexmaster.leakout.ui.pipes

import java.io.File
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks the real SVG assets in `src/main/assets/pipes` against the asset format. */
class PipeAssetsTest {
    private val assetDir = File("src/main/assets/${PipeArtLibrary.ASSET_DIR}")
    private val library = PipeArtLibrary { name -> File(assetDir, name.removePrefix("${PipeArtLibrary.ASSET_DIR}/")).inputStream() }

    private fun file(section: PipeSection) = File(assetDir, "${section.name.lowercase()}.svg")

    @Test
    fun `every section has an asset that loads`() {
        for (section in PipeSection.entries) {
            assertTrue("Missing ${file(section)}", file(section).isFile)
            val art = library.art(section)
            assertTrue("$section has no art under the liquid", art.under.isNotEmpty())
            assertTrue("$section has no art over the liquid", art.over.isNotEmpty())
        }
    }

    @Test
    fun `every asset uses the one-cell view box`() {
        for (section in PipeSection.entries) {
            assertTrue(section.name, file(section).readText().contains("viewBox=\"0 0 100 100\""))
        }
    }

    @Test
    fun `channels match the sections`() {
        val expected = mapOf(
            PipeSection.H to listOf(Side.LEFT to Side.RIGHT),
            PipeSection.RES to listOf(Side.LEFT to Side.RIGHT),
            PipeSection.V to listOf(Side.TOP to Side.BOTTOM),
            PipeSection.LT to listOf(Side.LEFT to Side.TOP),
            PipeSection.LB to listOf(Side.LEFT to Side.BOTTOM),
            PipeSection.RT to listOf(Side.RIGHT to Side.TOP),
            PipeSection.RB to listOf(Side.RIGHT to Side.BOTTOM),
            PipeSection.X to listOf(Side.TOP to Side.BOTTOM, Side.LEFT to Side.RIGHT),
        )
        for (section in PipeSection.entries) {
            val channels = library.art(section).channels
            assertEquals(section.name, expected.getValue(section), channels.map { it.from to it.to })
        }
        assertEquals(listOf("channel-v", "channel-h"), library.art(PipeSection.X).channels.map { it.id })
    }

    @Test
    fun `channel paths start and end in the middle of their sides`() {
        for (section in PipeSection.entries) {
            for (channel in library.art(section).channels) {
                val (start, end) = endPoints(channel.pathData)
                assertEquals("${section.name} ${channel.id} start", edgeMiddle(channel.from), start)
                assertEquals("${section.name} ${channel.id} end", edgeMiddle(channel.to), end)
            }
        }
    }

    @Test
    fun `only the reservoir has a tank and only the cross has a bridge`() {
        for (section in PipeSection.entries) {
            val art = library.art(section)
            if (section == PipeSection.RES) {
                val tank = checkNotNull(art.tank) { "The reservoir has no tank" }
                assertEquals(50f, tank.cx)
                assertEquals(50f, tank.cy)
            } else {
                assertNull(section.name, art.tank)
            }
            if (section == PipeSection.X) {
                assertEquals(listOf("channel-v"), art.bridges.map { it.hides })
            } else {
                assertTrue(section.name, art.bridges.isEmpty())
            }
        }
    }

    @Test
    fun `every colour is a design system token`() {
        val tokens = setOf(
            0xC9D6E2, 0x8FA3B8, 0x5C7189, 0x3A4B60, 0x22303F, // steel.100 – steel.900
            0x0C1A2A, // surface.deep (the cross's bridge gap)
            0xC7B5FF, 0xA78BFA, 0x7C5CE6, // reservoir.300 – reservoir.700
            0xFFFFFF, // line.highlight
        )
        for (section in PipeSection.entries) {
            val art = library.art(section)
            val colours = (art.under + art.over).flatMap { listOfNotNull(it.style.fill, it.style.stroke) }.flatMap {
                when (it) {
                    is SvgPaint.Solid -> listOf(it.rgb)
                    is SvgPaint.Gradient -> it.gradient.stops.map { stop -> stop.rgb }
                }
            }
            for (colour in colours) {
                assertTrue("${section.name} uses #%06X, not a design token".format(colour), colour in tokens)
            }
        }
    }

    @Test
    fun `every open side has a flange`() {
        // Flanges are the steel.300 shapes in the over group; each one sits on the edge of its side.
        for (section in PipeSection.entries) {
            val art = library.art(section)
            val flanges = art.over.filterIsInstance<SvgShape.PathShape>()
                .filter { it.style.fill == SvgPaint.Solid(0x8FA3B8) }
            val sides = flanges.map { sideOf(it.pathData) }.toSet()
            assertEquals(section.name, art.channels.flatMap { it.sides }.toSet(), sides)
        }
    }

    private fun numbers(pathData: String): List<Float> =
        Regex("-?\\d+(\\.\\d+)?").findAll(pathData).map { it.value.toFloat() }.toList()

    /** Start is the first move-to; the channel paths end with an absolute `L` or `A` end point. */
    private fun endPoints(pathData: String): Pair<Pair<Float, Float>, Pair<Float, Float>> {
        assertTrue("Channel path must start with M: $pathData", pathData.trimStart().startsWith("M"))
        val n = numbers(pathData)
        return (n[0] to n[1]) to (n[n.size - 2] to n[n.size - 1])
    }

    private fun edgeMiddle(side: Side): Pair<Float, Float> = when (side) {
        Side.LEFT -> 0f to 50f
        Side.RIGHT -> 100f to 50f
        Side.TOP -> 50f to 0f
        Side.BOTTOM -> 50f to 100f
    }

    /** The side a flange sits on, from the corners of its outline (`M`, `L`, `H`, `V`, `Z` only). */
    private fun sideOf(pathData: String): Side {
        val points = mutableListOf<Pair<Float, Float>>()
        var x = 0f
        var y = 0f
        for (match in Regex("([MLHVZ])([^MLHVZ]*)").findAll(pathData)) {
            val args = numbers(match.groupValues[2])
            when (match.groupValues[1]) {
                "M", "L" -> { x = args[0]; y = args[1] }
                "H" -> x = args[0]
                "V" -> y = args[0]
                else -> continue
            }
            points += x to y
        }
        return when {
            points.all { it.first <= 10f } -> Side.LEFT
            points.all { it.first >= 90f } -> Side.RIGHT
            points.all { it.second <= 10f } -> Side.TOP
            points.all { it.second >= 90f } -> Side.BOTTOM
            else -> throw AssertionError("Flange not on an edge: $pathData")
        }
    }
}
