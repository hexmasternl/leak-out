package nl.hexmaster.leakout

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lint's HardcodedText only looks at XML layouts, so this scans the release Kotlin sources for text
 * literals handed to Compose: they would never reach Play's translation (openspec/specs/app-localization,
 * "Translatable text"). `@Preview` functions and the debug-only level designer are skipped.
 */
class HardcodedTextTest {
    @Test
    fun `release code shows no hard-coded text`() {
        val sources = File("src/main/kotlin").walkTopDown()
            .filter { it.isFile && it.extension == "kt" && "ui${File.separator}designer" !in it.path }
            .toList()
        assertTrue("No sources found; is the working directory the app module?", sources.isNotEmpty())
        val found = sources.flatMap { file -> hardcodedText(file.readText()).map { "${file.name}:${it.first}: ${it.second}" } }
        assertEquals("Hard-coded text: use a string resource", emptyList<String>(), found)
    }

    @Test
    fun `the scan finds literals outside previews only`() {
        val source = """
            @Composable
            fun Real() {
                Text("Hello there")
                Icon(painter, contentDescription = "Close")
                Text(stringResource(R.string.ok))
                Text(text = "")
                Text("%1${'$'}d / %2${'$'}d")
            }

            @Preview(name = "Preview")
            @Composable
            private fun RealPreview() {
                Text("Only in a preview") { nested() }
            }
        """.trimIndent()
        assertEquals(listOf(3, 4), hardcodedText(source).map { it.first })
    }

    /** Line number and text of each Compose text literal with a letter in it, outside `@Preview` functions. */
    private fun hardcodedText(source: String): List<Pair<Int, String>> {
        val found = mutableListOf<Pair<Int, String>>()
        var previewPending = false
        var previewDepth = -1
        var depth = 0
        source.lines().forEachIndexed { index, line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("@Preview")) previewPending = true
            if (previewPending && FUN.containsMatchIn(trimmed)) {
                previewPending = false
                previewDepth = depth
            }
            val inPreview = previewDepth >= 0
            if (!inPreview && !trimmed.startsWith("//") && !trimmed.startsWith("*")) {
                LITERAL.findAll(line).map { it.groupValues[1] }
                    .filter { text -> LETTER.containsMatchIn(text.replace(FORMAT, "")) }
                    .forEach { found += (index + 1) to it }
            }
            depth += line.count { it == '{' } - line.count { it == '}' }
            if (inPreview && depth <= previewDepth && line.contains('}')) previewDepth = -1
        }
        return found
    }

    private companion object {
        val FUN = Regex("""\bfun\b""")

        /** The literal in `Text("…")`, `text = "…"`, `contentDescription = "…"` or `stateDescription = "…"`. */
        val LITERAL = Regex("""(?:\bText\(|\b(?:text|contentDescription|stateDescription)\s*=\s*)"([^"]*)"""")

        /** A format specifier such as `%1$d`, which is not text a player reads. */
        val FORMAT = Regex("""%(\d+\$)?[a-zA-Z]""")

        val LETTER = Regex("""\p{L}""")
    }
}
