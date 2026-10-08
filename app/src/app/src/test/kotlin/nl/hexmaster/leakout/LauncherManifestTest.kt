package nl.hexmaster.leakout

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.w3c.dom.Element

/**
 * Debug builds have two launchers, the game and the Pipe Lab. Only the game's launcher carries
 * DEFAULT, which is what makes Android Studio's "default activity" start the game.
 */
class LauncherManifestTest {
    private val android = "http://schemas.android.com/apk/res/android"

    /** Activity name to the categories of its MAIN + LAUNCHER intent filter, for [path]. */
    private fun launchers(path: String): Map<String, Set<String>> {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val document = factory.newDocumentBuilder().parse(File(path))
        val activities = document.getElementsByTagName("activity")
        return (0 until activities.length).map { activities.item(it) as Element }.mapNotNull { activity ->
            val filters = activity.getElementsByTagName("intent-filter")
            (0 until filters.length).map { filters.item(it) as Element }.firstNotNullOfOrNull { filter ->
                fun values(tag: String): Set<String> {
                    val nodes = filter.getElementsByTagName(tag)
                    return (0 until nodes.length).map { (nodes.item(it) as Element).getAttributeNS(android, "name") }.toSet()
                }
                val categories = values("category")
                if ("android.intent.action.MAIN" in values("action") && "android.intent.category.LAUNCHER" in categories) {
                    activity.getAttributeNS(android, "name") to categories
                } else {
                    null
                }
            }
        }.toMap()
    }

    @Test
    fun `the game is the default launcher`() {
        val main = launchers("src/main/AndroidManifest.xml")
        assertEquals(setOf(".MainActivity"), main.keys)
        assertEquals(
            setOf("android.intent.category.LAUNCHER", "android.intent.category.DEFAULT"),
            main.getValue(".MainActivity"),
        )
    }

    @Test
    fun `the pipe lab keeps its own launcher without DEFAULT`() {
        val debug = launchers("src/debug/AndroidManifest.xml")
        assertEquals(setOf(".lab.PipeLabActivity"), debug.keys)
        assertFalse("android.intent.category.DEFAULT" in debug.getValue(".lab.PipeLabActivity"))
    }
}
