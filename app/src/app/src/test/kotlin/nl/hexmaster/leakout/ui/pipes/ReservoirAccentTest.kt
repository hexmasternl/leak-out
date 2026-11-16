package nl.hexmaster.leakout.ui.pipes

import java.io.File
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.ReservoirType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ReservoirAccentTest {
    private val assetDir = File("src/main/assets/${PipeArtLibrary.ASSET_DIR}")
    private val library = PipeArtLibrary { name -> File(assetDir, name.removePrefix("${PipeArtLibrary.ASSET_DIR}/")).inputStream() }

    private val purple = ReservoirAccent.of(ReservoirType.PURPLE_30)

    private fun colours(shapes: List<SvgShape>): Set<Int> = shapes.flatMap { listOfNotNull(it.style.fill, it.style.stroke) }.flatMap {
        when (it) {
            is SvgPaint.Solid -> listOf(it.rgb)
            is SvgPaint.Gradient -> it.gradient.stops.map { stop -> stop.rgb }
        }
    }.toSet()

    private fun reservoir(type: ReservoirType) = library.prepared(PipeSection.RES, type).art

    @Test
    fun `purple is the violet set the asset is drawn in`() {
        assertEquals(ReservoirAccent(0xC7B5FF, 0xA78BFA, 0x7C5CE6), purple)
        assertEquals(colours(library.art(PipeSection.RES).under + library.art(PipeSection.RES).over).intersect(setOf(purple.c300, purple.c500, purple.c700)).size, 3)
    }

    @Test
    fun `the four accents differ from each other`() {
        val accents = ReservoirType.entries.map { ReservoirAccent.of(it) }
        assertEquals(4, accents.toSet().size)
        assertEquals(4, accents.map { it.c500 }.toSet().size)
    }

    @Test
    fun `each type tints the tank and the badge with its own accent`() {
        for (type in ReservoirType.entries) {
            val accent = ReservoirAccent.of(type)
            val art = reservoir(type)
            val used = colours(art.under + art.over)
            assertTrue("$type tank", accent.c500 in used && accent.c700 in used)
            assertTrue("$type badge", accent.c300 in used)
            if (type != ReservoirType.PURPLE_30) {
                assertFalse("$type keeps purple", used.any { it in setOf(purple.c300, purple.c500, purple.c700) })
            }
        }
    }

    @Test
    fun `the tint only changes colours, not the geometry or the channels`() {
        val base = library.art(PipeSection.RES)
        val red = reservoir(ReservoirType.RED_10)
        assertEquals(base.channels, red.channels)
        assertEquals(base.tank, red.tank)
        assertEquals(base.under.size, red.under.size)
    }

    @Test
    fun `the badge text has one glyph path per character`() {
        for (type in ReservoirType.entries) {
            val text = ReservoirBadgeText.shapes(type.percent, 0xC7B5FF)
            assertEquals("${type.percent}%".length, text.size)
        }
        val added = reservoir(ReservoirType.RED_10).over.size - library.art(PipeSection.RES).over.size
        assertEquals(3, added)
    }

    @Test
    fun `different percentages draw different badge text`() {
        val paths = ReservoirType.entries.map { type ->
            ReservoirBadgeText.shapes(type.percent, 0).map { (it as SvgShape.PathShape).pathData }
        }
        assertEquals(4, paths.toSet().size)
    }

    @Test
    fun `badge text stays inside the badge pill`() {
        for (type in ReservoirType.entries) {
            for (shape in ReservoirBadgeText.shapes(type.percent, 0)) {
                val xs = mutableListOf<Float>()
                var command = ' '
                var index = 0
                for (token in Regex("""[A-Z]|-?\d+(\.\d+)?""").findAll((shape as SvgShape.PathShape).pathData).map { it.value }) {
                    if (token[0].isLetter()) {
                        command = token[0]
                        index = 0
                        continue
                    }
                    val isX = command == 'H' || (command != 'V' && index % 2 == 0)
                    if (isX) xs += token.toFloat()
                    index++
                }
                assertTrue("$type x ${xs.min()}..${xs.max()}", xs.min() >= 74f && xs.max() <= 97f)
            }
        }
    }

    @Test
    fun `only the reservoir depends on the type`() {
        val library = library.forReservoir(ReservoirType.ORANGE_40)
        assertSame(this.library.prepared(PipeSection.H), library.prepared(PipeSection.H))
        assertNotEquals(this.library.prepared(PipeSection.RES), library.prepared(PipeSection.RES))
    }

    @Test
    fun `a library view draws the reservoir in its type and shares the caches`() {
        val orange = library.forReservoir(ReservoirType.ORANGE_40)
        assertEquals(ReservoirType.ORANGE_40, orange.reservoirType)
        assertSame(orange.prepared(PipeSection.RES), library.prepared(PipeSection.RES, ReservoirType.ORANGE_40))
        assertSame(library, library.forReservoir(ReservoirType.DEFAULT))
        assertSame(library.art(PipeSection.RES), orange.art(PipeSection.RES))
    }
}
