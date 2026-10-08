package nl.hexmaster.leakout.audio

import kotlin.random.Random
import nl.hexmaster.leakout.audio.RecordingLoopingSoundPlayer.Call
import nl.hexmaster.leakout.settings.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** [SoundEffects.heartbeat]: the looping gap warning (spec `game-play-gap-warning`). */
class HeartbeatSoundEffectsTest {
    private val withHeartbeat = SoundCatalog.from(listOf("heartbeat.mp3", "level_complete.mp3"))
    private val oneShots = RecordingSoundEffectPlayer()
    private val loop = RecordingLoopingSoundPlayer()

    private fun effects(catalog: SoundCatalog = withHeartbeat, volume: Float = 0.8f) =
        SoundEffects(catalog, oneShots, Random(1), loop).apply { apply(Settings(soundVolume = volume)) }

    private fun assertVolume(expected: Float, actual: Float?) = assertEquals(expected, actual ?: -1f, 1e-4f)

    @Test
    fun `starts at factor times the sound volume`() {
        effects().heartbeat(0.15f)
        assertEquals(1, loop.calls.size)
        val start = loop.calls.single() as Call.Start
        assertEquals("heartbeat.mp3", start.file)
        assertVolume(0.12f, start.volume)
    }

    @Test
    fun `gets louder as the factor grows`() {
        val effects = effects()
        effects.heartbeat(0.15f)
        effects.heartbeat(0.83f)
        assertVolume(0.664f, loop.playingAt)
        assertEquals(Call.Volume(loop.playingAt ?: -1f), loop.calls.last())
    }

    @Test
    fun `volume changes below the step are not passed on`() {
        val effects = effects(volume = 1f)
        effects.heartbeat(0.5f)
        effects.heartbeat(0.505f)
        effects.heartbeat(0.509f)
        assertEquals(1, loop.calls.size)
        effects.heartbeat(0.51f)
        assertEquals(2, loop.calls.size)
        assertVolume(0.51f, loop.playingAt)
    }

    @Test
    fun `a null factor stops the loop and a later factor starts it again`() {
        val effects = effects()
        effects.heartbeat(0.5f)
        effects.heartbeat(null)
        assertNull(loop.playingAt)
        effects.heartbeat(null)
        effects.heartbeat(0.5f)
        assertEquals(listOf(Call.Start::class, Call.Stop::class, Call.Start::class), loop.calls.map { it::class })
    }

    @Test
    fun `sound off plays no heartbeat`() {
        val effects = effects()
        effects.apply(Settings(soundEnabled = false))
        effects.heartbeat(1f)
        assertTrue(loop.calls.isEmpty())
    }

    @Test
    fun `volume 0 plays no heartbeat`() {
        effects(volume = 0f).heartbeat(1f)
        assertTrue(loop.calls.isEmpty())
    }

    @Test
    fun `a settings change applies to a running loop at once`() {
        val effects = effects()
        effects.heartbeat(0.5f)
        effects.apply(Settings(soundVolume = 0.4f))
        assertVolume(0.2f, loop.playingAt)
        effects.apply(Settings(soundEnabled = false, soundVolume = 0.4f))
        assertNull(loop.playingAt)
        effects.apply(Settings(soundEnabled = true, soundVolume = 0.4f))
        assertVolume(0.2f, loop.playingAt)
    }

    @Test
    fun `a settings change while stopped starts nothing`() {
        val effects = effects()
        effects.apply(Settings(soundVolume = 0.3f))
        assertTrue(loop.calls.isEmpty())
    }

    @Test
    fun `without a heartbeat file it stays silent`() {
        effects(catalog = SoundCatalog.from(listOf("level_complete.mp3"))).heartbeat(1f)
        assertTrue(loop.calls.isEmpty())
    }

    @Test
    fun `without a looping player it stays silent and the one-shot effects still play`() {
        val effects = SoundEffects(withHeartbeat, oneShots, Random(1))
        effects.heartbeat(1f)
        effects.play(GameSound.LevelComplete)
        assertEquals(listOf("level_complete.mp3"), oneShots.played)
    }

    @Test
    fun `the heartbeat is never played as a one-shot effect`() {
        effects().play(GameSound.Heartbeat)
        assertTrue(oneShots.plays.isEmpty())
        assertTrue(loop.calls.isEmpty())
    }

    @Test
    fun `release frees the looping player`() {
        val effects = effects()
        effects.heartbeat(0.5f)
        effects.release()
        assertTrue(loop.released)
        assertTrue(oneShots.released)
    }
}
