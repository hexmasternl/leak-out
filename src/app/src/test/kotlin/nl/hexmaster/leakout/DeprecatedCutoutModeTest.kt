package nl.hexmaster.leakout

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Play Console reports `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` as a deprecated edge-to-edge API
 * (issue #89). Release code may only use it inside `fullScreenDialogCutoutMode`, behind the API 28–29
 * gate, so a new full-screen window cannot copy the deprecated mode.
 */
class DeprecatedCutoutModeTest {
    @Test
    fun `release code uses short edges only behind the API 28 to 29 gate`() {
        val sources = File("src/main/kotlin").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue("No sources found; is the working directory the app module?", sources.isNotEmpty())
        val found = sources.flatMap { file -> shortEdgesOutsideGate(file.readText()).map { "${file.name}:$it" } }
        assertEquals("Use fullScreenDialogCutoutMode instead of SHORT_EDGES", emptyList<String>(), found)
    }

    @Test
    fun `the scan finds uses outside the gate only`() {
        val source = """
            fun fullScreenDialogCutoutMode(sdkInt: Int): Int? = when {
                sdkInt >= 30 -> LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                sdkInt >= 28 -> LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                else -> null
            }

            fun Other() {
                window.attributes.layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        """.trimIndent()
        assertEquals(listOf(8), shortEdgesOutsideGate(source))
    }

    /** Line numbers that mention the short-edges mode outside the body of `fullScreenDialogCutoutMode`. */
    private fun shortEdgesOutsideGate(source: String): List<Int> {
        val found = mutableListOf<Int>()
        var inGate = false
        var opened = false
        var depth = 0
        source.lines().forEachIndexed { index, line ->
            if (!inGate && GATE.containsMatchIn(line)) {
                inGate = true
                opened = false
                depth = 0
            }
            if (!inGate && SHORT_EDGES in line) found += index + 1
            if (inGate) {
                depth += line.count { it == '{' } - line.count { it == '}' }
                if (line.contains('{')) opened = true
                if (opened && depth <= 0) inGate = false
            }
        }
        return found
    }

    private companion object {
        val GATE = Regex("""\bfun\s+fullScreenDialogCutoutMode\b""")
        const val SHORT_EDGES = "LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES"
    }
}
