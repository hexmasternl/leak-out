package nl.hexmaster.leakout.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundCatalogTest {
    @Test
    fun `a three digit suffix is the variant number`() {
        assertEquals("section_placement", SoundCatalog.groupOf("section_placement_001.mp3"))
        assertEquals("section_destroy", SoundCatalog.groupOf("section_destroy_042.mp3"))
    }

    @Test
    fun `a word suffix is part of the group`() {
        assertEquals("section_placement_failed", SoundCatalog.groupOf("section_placement_failed.mp3"))
        assertEquals("level_complete", SoundCatalog.groupOf("level_complete.mp3"))
    }

    @Test
    fun `other digit counts are not a variant number`() {
        assertEquals("level_complete_01", SoundCatalog.groupOf("level_complete_01.mp3"))
        assertEquals("level_complete_0001", SoundCatalog.groupOf("level_complete_0001.mp3"))
    }

    @Test
    fun `the failed sound is not a placement variant`() {
        val catalog = SoundCatalog.from(listOf("section_placement_failed.mp3", "section_placement_001.mp3"))
        assertEquals(listOf("section_placement_001.mp3"), catalog.variantsOf(GameSound.SectionPlaced))
        assertEquals(listOf("section_placement_failed.mp3"), catalog.variantsOf(GameSound.PlacementFailed))
    }

    @Test
    fun `the drop is the points awarded sound and can gain numbered variants`() {
        val catalog = SoundCatalog.from(listOf("drop_002.mp3", "drop.mp3", "drop_001.mp3"))
        assertEquals(listOf("drop.mp3", "drop_001.mp3", "drop_002.mp3"), catalog.variantsOf(GameSound.PointsAwarded))
    }

    @Test
    fun `variants are in name order`() {
        val catalog = SoundCatalog.from(listOf("section_placement_003.mp3", "section_placement_001.mp3", "section_placement_002.mp3"))
        assertEquals(
            listOf("section_placement_001.mp3", "section_placement_002.mp3", "section_placement_003.mp3"),
            catalog.variantsOf(GameSound.SectionPlaced),
        )
    }

    @Test
    fun `unknown groups and other file types are ignored`() {
        val catalog = SoundCatalog.from(listOf("whoosh_001.mp3", "level_failed.wav", "README.md", "level_failed.mp3"))
        assertEquals(listOf("level_failed.mp3"), catalog.files)
    }

    @Test
    fun `a sound without files has no variants`() {
        val catalog = SoundCatalog.from(listOf("level_complete.mp3"))
        assertTrue(catalog.variantsOf(GameSound.LevelFailed).isEmpty())
    }
}
