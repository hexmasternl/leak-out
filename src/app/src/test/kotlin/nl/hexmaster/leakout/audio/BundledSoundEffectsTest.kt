package nl.hexmaster.leakout.audio

import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The app's copy of the sound effects must stay identical to the design source in the repository's `assets/sfx/`. */
class BundledSoundEffectsTest {
    private val bundledDir = File("src/main/assets/sfx")
    private val sourceDir = File("../../assets/sfx")

    private fun names(dir: File): Set<String> {
        assertTrue("missing $dir", dir.isDirectory)
        return dir.list().orEmpty().toSet()
    }

    @Test
    fun `bundled effects equal the design source`() {
        // A design source file lands in the repository with its change's proposal, before the game
        // has a sound for it (heartbeat.mp3, #80). Only the files a game sound uses must be bundled.
        val used = names(sourceDir).filter { GameSound.ofGroup(SoundCatalog.groupOf(it)) != null }.toSet()
        assertEquals(used, names(bundledDir))
        for (name in names(bundledDir)) {
            assertArrayEquals("content of $name", File(sourceDir, name).readBytes(), File(bundledDir, name).readBytes())
        }
    }

    @Test
    fun `every bundled effect belongs to a game sound`() {
        val catalog = SoundCatalog.from(names(bundledDir))
        assertEquals(names(bundledDir), GameSound.entries.flatMap(catalog::variantsOf).toSet())
    }

    @Test
    fun `three placement variants and one of every other sound`() {
        val catalog = SoundCatalog.from(names(bundledDir))
        assertEquals(
            listOf("section_placement_001.mp3", "section_placement_002.mp3", "section_placement_003.mp3"),
            catalog.variantsOf(GameSound.SectionPlaced),
        )
        assertEquals(listOf("section_destroy_001.mp3"), catalog.variantsOf(GameSound.SectionDestroyed))
        assertEquals(listOf("section_placement_failed.mp3"), catalog.variantsOf(GameSound.PlacementFailed))
        assertEquals(listOf("level_complete.mp3"), catalog.variantsOf(GameSound.LevelComplete))
        assertEquals(listOf("level_failed.mp3"), catalog.variantsOf(GameSound.LevelFailed))
        assertEquals(listOf("drop.mp3"), catalog.variantsOf(GameSound.PointsAwarded))
        assertEquals(listOf("heartbeat.mp3"), catalog.variantsOf(GameSound.Heartbeat))
    }
}
