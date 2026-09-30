package nl.hexmaster.leakout.ui.game

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class HudFormatTest {
    private val english = Locale.forLanguageTag("en-US")
    private val dutch = Locale.forLanguageTag("nl-NL")

    @Test
    fun `level is the level id`() {
        assertEquals("7", HudFormat.level(7))
    }

    @Test
    fun `speed has one decimal with the locale's decimal sign`() {
        assertEquals("0.8", HudFormat.speed(0.8, english))
        assertEquals("0,8", HudFormat.speed(0.8, dutch))
        assertEquals("1.0", HudFormat.speed(1.0, english))
        assertEquals("0.9", HudFormat.speed(0.94, english))
    }

    @Test
    fun `score uses the locale's digit grouping`() {
        assertEquals("0", HudFormat.score(0, english))
        assertEquals("150", HudFormat.score(150, dutch))
        assertEquals("1,250", HudFormat.score(1250, english))
        assertEquals("1.250", HudFormat.score(1250, dutch))
    }
}
