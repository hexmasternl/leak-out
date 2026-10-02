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
    fun `speed is the whole number, without a unit`() {
        assertEquals("2", HudFormat.speed(0.2, english))
        assertEquals("8", HudFormat.speed(0.8, dutch))
        assertEquals("8", HudFormat.speed(0.82, english))
        assertEquals("9", HudFormat.speed(0.85, english))
        assertEquals("13", HudFormat.speed(1.25, english))
    }

    @Test
    fun `score uses the locale's digit grouping`() {
        assertEquals("0", HudFormat.score(0, english))
        assertEquals("150", HudFormat.score(150, dutch))
        assertEquals("1,250", HudFormat.score(1250, english))
        assertEquals("1.250", HudFormat.score(1250, dutch))
    }
}
