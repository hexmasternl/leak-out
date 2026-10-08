package nl.hexmaster.leakout

import java.awt.Font
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/** openspec/specs/app-localization, "Supported languages". */
class SupportedLanguagesTest {
    private val android = "http://schemas.android.com/apk/res/android"

    @Test
    fun `locales config lists exactly the supported languages`() {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val document = factory.newDocumentBuilder().parse(File("src/main/res/xml/locales_config.xml"))
        val locales = document.getElementsByTagName("locale")
        val tags = (0 until locales.length).map { (locales.item(it) as Element).getAttributeNS(android, "name") }
        assertEquals(SupportedLanguages.TAGS, tags)
    }

    @Test
    fun `english comes first and every tag is a well-formed language`() {
        assertEquals("en", SupportedLanguages.TAGS.first())
        SupportedLanguages.TAGS.forEach { tag ->
            assertEquals(tag, Locale.forLanguageTag(tag).toLanguageTag())
        }
    }

    @Test
    fun `the manifest points at the locales config`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("android:localeConfig=\"@xml/locales_config\""))
    }

    @Test
    fun `both fonts draw every letter the supported languages need`() {
        listOf("fredoka.ttf", "wendy_one_regular.ttf").forEach { name ->
            val font = File("src/main/res/font/$name").inputStream().use { Font.createFont(Font.TRUETYPE_FONT, it) }
            val missing = SPECIAL_CHARACTERS.filterNot { font.canDisplay(it) || it in KNOWN_GAPS[name].orEmpty() }
            assertEquals("$name cannot draw", "", missing)
        }
    }

    private companion object {
        /**
         * Glyphs a font lacks that we accept: Android draws them from the system font instead. Wendy One
         * has no "Ÿ", which French only uses in a few place names.
         */
        val KNOWN_GAPS = mapOf("wendy_one_regular.ttf" to "Ÿ")

        /** Beyond A to Z: the letters of Dutch, German, French, Spanish, Italian and Portuguese in both cases, and their punctuation. */
        const val SPECIAL_CHARACTERS =
            "àáâãäçèéêëìíîïñòóôõöùúûüÿßœæ" + "ÀÁÂÃÄÇÈÉÊËÌÍÎÏÑÒÓÔÕÖÙÚÛÜŸŒÆ" + "¿¡«»"
    }
}
