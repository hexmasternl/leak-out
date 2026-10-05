package nl.hexmaster.leakout.ui.components

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayCaseTest {
    @Test
    fun `turkish capitals keep the dot on the i`() {
        assertEquals("İPUCU", "ipucu".displayUppercase(Locale.forLanguageTag("tr-TR")))
    }

    @Test
    fun `english capitals are unchanged`() {
        assertEquals("LEVEL COMPLETE!", "Level Complete!".displayUppercase(Locale.US))
    }

    @Test
    fun `german sharp s becomes SS`() {
        assertEquals("FLIESSEN", "fließen".displayUppercase(Locale.GERMANY))
    }
}
