package nl.hexmaster.leakout.ui.game

import org.junit.Assert.assertEquals
import org.junit.Test

class AchievementRowTextTest {
    private val more = { n: Int -> "+$n more" }

    @Test
    fun `one name is shown on its own`() {
        assertEquals("Zero Waste", AchievementRowText.names(listOf("Zero Waste"), more))
    }

    @Test
    fun `two names are joined with a middle dot`() {
        assertEquals("Zero Waste · Measure Twice", AchievementRowText.names(listOf("Zero Waste", "Measure Twice"), more))
    }

    @Test
    fun `three names show two and one more`() {
        assertEquals("A · B +1 more", AchievementRowText.names(listOf("A", "B", "C"), more))
    }

    @Test
    fun `four names show two and two more`() {
        val names = listOf("Zero Waste", "Measure Twice", "Steady Hands", "Personal Best")
        assertEquals("Zero Waste · Measure Twice +2 more", AchievementRowText.names(names, more))
    }

    @Test
    fun `no names is empty and never asks for more`() {
        assertEquals("", AchievementRowText.names(emptyList()) { error("no more text without names") })
    }

    @Test
    fun `a screen reader hears every name, not only the shown ones`() {
        val names = listOf("Zero Waste", "Measure Twice", "Steady Hands", "Personal Best")
        assertEquals("Zero Waste, Measure Twice, Steady Hands, Personal Best", AchievementRowText.spokenNames(names))
    }
}
