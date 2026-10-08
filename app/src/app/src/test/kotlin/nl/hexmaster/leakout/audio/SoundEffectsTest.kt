package nl.hexmaster.leakout.audio

import kotlin.random.Random
import nl.hexmaster.leakout.settings.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundEffectsTest {
    private val catalog = SoundCatalog.from(
        listOf(
            "section_placement_001.mp3",
            "section_placement_002.mp3",
            "section_placement_003.mp3",
            "section_destroy_001.mp3",
            "section_placement_failed.mp3",
            "level_complete.mp3",
        ),
    )
    private val player = RecordingSoundEffectPlayer()

    private fun effects(random: Random = Random(7)) = SoundEffects(catalog, player, random)

    @Test
    fun `plays at the default volume before any settings are read`() {
        effects().play(GameSound.LevelComplete)
        assertEquals(listOf(RecordingSoundEffectPlayer.Play("level_complete.mp3", Settings.DEFAULT_SOUND_VOLUME)), player.plays)
    }

    @Test
    fun `a single variant is always the one played`() {
        val effects = effects()
        repeat(5) { effects.play(GameSound.SectionDestroyed) }
        assertEquals(List(5) { "section_destroy_001.mp3" }, player.played)
    }

    @Test
    fun `placement variants are picked by the random source`() {
        val effects = effects(Random(42))
        repeat(60) { effects.play(GameSound.SectionPlaced) }
        val variants = catalog.variantsOf(GameSound.SectionPlaced)
        val expectedRandom = Random(42)
        assertEquals(List(60) { variants.random(expectedRandom) }, player.played)
        assertEquals(variants.toSet(), player.played.toSet())
    }

    @Test
    fun `sound off plays nothing`() {
        val effects = effects()
        effects.apply(Settings(soundEnabled = false))
        GameSound.entries.forEach(effects::play)
        assertTrue(player.plays.isEmpty())
    }

    @Test
    fun `volume 0 plays nothing`() {
        val effects = effects()
        effects.apply(Settings(soundVolume = 0f))
        effects.play(GameSound.SectionPlaced)
        assertTrue(player.plays.isEmpty())
    }

    @Test
    fun `the stored volume is passed to the player`() {
        val effects = effects()
        effects.apply(Settings(soundVolume = 0.3f))
        effects.play(GameSound.PlacementFailed)
        assertEquals(listOf(RecordingSoundEffectPlayer.Play("section_placement_failed.mp3", 0.3f)), player.plays)
    }

    @Test
    fun `a settings change applies to the next sound`() {
        val effects = effects()
        effects.play(GameSound.LevelComplete)
        effects.apply(Settings(soundEnabled = false))
        effects.play(GameSound.LevelComplete)
        effects.apply(Settings(soundEnabled = true, soundVolume = 0.5f))
        effects.play(GameSound.LevelComplete)
        assertEquals(listOf(Settings.DEFAULT_SOUND_VOLUME, 0.5f), player.plays.map { it.volume })
    }

    @Test
    fun `a sound without files is silent`() {
        effects().play(GameSound.LevelFailed)
        assertTrue(player.plays.isEmpty())
    }

    @Test
    fun `preview plays a placement variant at the given volume`() {
        val effects = effects()
        effects.apply(Settings(soundVolume = 0.8f))
        effects.preview(0.5f)
        assertEquals(1, player.plays.size)
        assertTrue(player.plays[0].file in catalog.variantsOf(GameSound.SectionPlaced))
        assertEquals(0.5f, player.plays[0].volume)
    }

    @Test
    fun `preview is silent while sound is off`() {
        val effects = effects()
        effects.apply(Settings(soundEnabled = false))
        effects.preview(0.5f)
        assertTrue(player.plays.isEmpty())
    }

    @Test
    fun `release frees the player`() {
        effects().release()
        assertTrue(player.released)
    }
}
