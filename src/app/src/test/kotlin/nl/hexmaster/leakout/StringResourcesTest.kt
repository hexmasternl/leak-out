package nl.hexmaster.leakout

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Comment
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.Text

/**
 * Play Console translates `res/values/strings.xml` automatically (see docs/deployment.md#translations),
 * so the English strings are all a translator gets: these checks keep them ready for that
 * (openspec/specs/app-localization, "Strings ready for translators").
 */
class StringResourcesTest {
    /**
     * One `<string>` or `<plurals>` entry: its texts (one per plural item) and the comment just above it.
     * A `formatted="false"` entry is never passed to a formatter, so its `%` signs are plain text.
     */
    private data class Entry(
        val name: String,
        val translatable: Boolean,
        val formatted: Boolean,
        val texts: List<String>,
        val comment: String?,
    )

    private fun entries(path: String): List<Entry> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(path))
        val children = document.documentElement.childNodes
        return (0 until children.length).map { children.item(it) }.filterIsInstance<Element>()
            .filter { it.tagName == "string" || it.tagName == "plurals" }
            .map { element ->
                val texts = if (element.tagName == "plurals") {
                    val items = element.getElementsByTagName("item")
                    (0 until items.length).map { items.item(it).textContent }
                } else {
                    listOf(element.textContent)
                }
                Entry(
                    name = element.getAttribute("name"),
                    translatable = element.getAttribute("translatable") != "false",
                    formatted = element.getAttribute("formatted") != "false",
                    texts = texts,
                    comment = commentAbove(element),
                )
            }
    }

    /** The comment directly above [node], with only whitespace in between, or null. */
    private fun commentAbove(node: Node): String? {
        var sibling = node.previousSibling
        while (sibling is Text && sibling !is Comment && sibling.textContent.isBlank()) sibling = sibling.previousSibling
        return (sibling as? Comment)?.data?.trim()
    }

    private val strings = entries("src/main/res/values/strings.xml")
    private val formattedStrings = strings.filter { it.formatted }

    @Test
    fun `brand names are not translated`() {
        val notTranslated = strings.filterNot { it.translatable }.map { it.name }.toSet()
        listOf("app_name", "welcome_title", "splash_wordmark").forEach { name ->
            assertTrue("$name must be translatable=\"false\"", name in notTranslated)
        }
    }

    @Test
    fun `every translatable string with a placeholder has a translator comment`() {
        val missing = formattedStrings.filter { it.translatable && it.texts.any { text -> placeholders(text).isNotEmpty() } && it.comment == null }
        assertEquals("Strings with placeholders but no translator comment", emptyList<String>(), missing.map { it.name })
    }

    @Test
    fun `strings with more than one placeholder use positional placeholders`() {
        val wrong = formattedStrings.filter { entry ->
            entry.texts.any { text -> placeholders(text).let { found -> found.size > 1 && found.any { !it.positional } } }
        }
        assertEquals("Strings with several non-positional placeholders", emptyList<String>(), wrong.map { it.name })
    }

    @Test
    fun `play games ids are never translated`() {
        val translatable = entries("src/main/res/values/games-ids.xml").filter { it.translatable }
        assertEquals(emptyList<String>(), translatable.map { it.name })
    }

    @Test
    fun `placeholder detection`() {
        assertEquals(listOf(true, true), placeholders("%1\$d of %2\$d stars").map { it.positional })
        assertEquals(listOf(false), placeholders("Level %d").map { it.positional })
        assertEquals(listOf(true), placeholders("flows at %1\$d %% speed").map { it.positional })
        assertEquals(emptyList<Placeholder>(), placeholders("100 %% sure"))
    }

    private data class Placeholder(val positional: Boolean)

    private fun placeholders(text: String): List<Placeholder> =
        PLACEHOLDER.findAll(text).filter { it.value != "%%" }.map { Placeholder(positional = it.groupValues[1].isNotEmpty()) }.toList()

    private companion object {
        /** `%%`, or a format specifier with an optional `n$` position: `%s`, `%1$d`, `%2$.1f`. */
        val PLACEHOLDER = Regex("""%%|%(\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[a-zA-Z]""")
    }
}
