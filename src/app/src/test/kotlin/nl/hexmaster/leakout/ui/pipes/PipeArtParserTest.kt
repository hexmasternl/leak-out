package nl.hexmaster.leakout.ui.pipes

import nl.hexmaster.leakout.core.level.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PipeArtParserTest {
    private fun parse(body: String, viewBox: String = "0 0 100 100", extraNs: String = ""): PipeArt =
        PipeArtParser.parse(
            """<svg xmlns="http://www.w3.org/2000/svg"$extraNs viewBox="$viewBox">$body</svg>""".byteInputStream(),
        )

    private val minimal = """
        <g id="under"><path d="M0 50 L100 50" fill="none" stroke="#5C7189" stroke-width="36"/></g>
        <path id="channel-main" data-from="left" data-to="right" d="M0 50 L100 50" fill="none" stroke="none"/>
        <g id="over"/>
    """

    @Test
    fun `reads layers channels and styles`() {
        val art = parse(minimal)
        assertEquals(1, art.under.size)
        assertEquals(0, art.over.size)
        val body = art.under.single() as SvgShape.PathShape
        assertEquals("M0 50 L100 50", body.pathData)
        assertNull(body.style.fill)
        assertEquals(SvgPaint.Solid(0x5C7189), body.style.stroke)
        assertEquals(36f, body.style.strokeWidth)
        assertEquals(listOf(ArtChannel("channel-main", Side.LEFT, Side.RIGHT, "M0 50 L100 50")), art.channels)
        assertNull(art.tank)
    }

    @Test
    fun `groups pass their style on to children`() {
        val art = parse(
            """
            <g id="under" stroke="#22303F" opacity="0.5">
              <g stroke-width="4" stroke-linecap="round"><rect x="1" y="2" width="3" height="4" fill="#FFF"/></g>
              <circle cx="50" cy="50" r="10" opacity="0.5"/>
            </g>
            <path id="channel-main" data-from="top" data-to="bottom" d="M50 0 L50 100"/>
            <g id="over"/>
            """,
        )
        val rect = art.under[0] as SvgShape.RectShape
        assertEquals(SvgPaint.Solid(0x22303F), rect.style.stroke)
        assertEquals(SvgPaint.Solid(0xFFFFFF), rect.style.fill)
        assertEquals(4f, rect.style.strokeWidth)
        assertEquals(SvgLineCap.ROUND, rect.style.lineCap)
        assertEquals(0.5f, rect.style.opacity)
        val circle = art.under[1] as SvgShape.CircleShape
        assertEquals(SvgPaint.Solid(0x000000), circle.style.fill) // the SVG default
        assertEquals(0.25f, circle.style.opacity)
    }

    @Test
    fun `reads gradients tank and bridge`() {
        val art = parse(
            """
            <defs>
              <linearGradient id="g" gradientUnits="userSpaceOnUse" x1="50" y1="20" x2="50" y2="80">
                <stop offset="0" stop-color="#A78BFA"/><stop offset="1" stop-color="#7C5CE6" stop-opacity="0.5"/>
              </linearGradient>
            </defs>
            <g id="under"><circle cx="50" cy="50" r="30" fill="url(#g)"/></g>
            <path id="channel-v" data-from="top" data-to="bottom" d="M50 0 L50 100"/>
            <path id="channel-h" data-from="left" data-to="right" d="M0 50 L100 50"/>
            <circle id="tank" cx="50" cy="50" r="26"/>
            <rect id="bridge" data-hides="channel-v" x="0" y="27" width="100" height="46"/>
            <g id="over"/>
            """,
        )
        val fill = (art.under.single().style.fill as SvgPaint.Gradient).gradient
        assertEquals(SvgLinearGradient("g", 50f, 20f, 50f, 80f, listOf(GradientStop(0f, 0xA78BFA), GradientStop(1f, 0x7C5CE6, 0.5f))), fill)
        assertEquals(TankCircle(50f, 50f, 26f), art.tank)
        assertEquals(listOf(Bridge("channel-v", 0f, 27f, 100f, 46f)), art.bridges)
        assertEquals(listOf("channel-v", "channel-h"), art.channels.map { it.id })
    }

    @Test
    fun `ignores editor namespaces and descriptive elements`() {
        val art = parse(
            """
            <title>H</title>
            <inkscape:grid id="grid"/>
            <g id="under" inkscape:label="under"><path d="M0 0" inkscape:connector-curvature="0"/></g>
            <path id="channel-main" data-from="left" data-to="right" d="M0 50 L100 50"/>
            <g id="over"/>
            """,
            extraNs = """ xmlns:inkscape="http://www.inkscape.org/namespaces/inkscape"""",
        )
        assertEquals(1, art.under.size)
    }

    @Test
    fun `unsupported element fails naming it`() {
        val error = assertThrows(UnsupportedSvgException::class.java) {
            parse(minimal.replace("<g id=\"over\"/>", "<g id=\"over\"><text>slow</text></g>"))
        }
        assertTrue(error.message, error.message!!.contains("<text>"))
    }

    @Test
    fun `style and transform attributes are refused`() {
        for (attribute in listOf("style=\"fill:red\"", "transform=\"translate(1 1)\"")) {
            val error = assertThrows(UnsupportedSvgException::class.java) {
                parse(minimal.replace("<g id=\"over\"/>", "<g id=\"over\"><path d=\"M0 0\" $attribute/></g>"))
            }
            assertTrue(error.message, error.message!!.contains(attribute.substringBefore('=')))
        }
    }

    @Test
    fun `missing layers and channels fail`() {
        assertThrows(UnsupportedSvgException::class.java) { parse(minimal.replace("<g id=\"over\"/>", "")) }
        assertThrows(UnsupportedSvgException::class.java) {
            parse(minimal.replace("<g id=\"under\">", "<g id=\"lower\">"))
        }
        assertThrows(UnsupportedSvgException::class.java) { parse("""<g id="under"/><g id="over"/>""") }
    }

    @Test
    fun `bad channel side fails`() {
        val error = assertThrows(UnsupportedSvgException::class.java) {
            parse(minimal.replace("data-to=\"right\"", "data-to=\"up\""))
        }
        assertTrue(error.message!!.contains("up"))
    }

    @Test
    fun `wrong view box fails`() {
        assertThrows(UnsupportedSvgException::class.java) { parse(minimal, viewBox = "0 0 24 24") }
    }

    @Test
    fun `art outside the layers fails`() {
        assertThrows(UnsupportedSvgException::class.java) {
            parse(minimal + """<rect x="0" y="0" width="10" height="10"/>""")
        }
    }

    @Test
    fun `unknown gradient and colour formats fail`() {
        assertThrows(UnsupportedSvgException::class.java) { parse(minimal.replace("#5C7189", "url(#nope)")) }
        assertThrows(UnsupportedSvgException::class.java) { parse(minimal.replace("#5C7189", "steelblue")) }
        assertEquals(0xAABBCC, PipeArtParser.parseColour("#ABC"))
    }
}
