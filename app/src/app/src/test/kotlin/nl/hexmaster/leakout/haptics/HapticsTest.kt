package nl.hexmaster.leakout.haptics

import nl.hexmaster.leakout.settings.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticsTest {
    private val player = RecordingHapticPlayer()
    private val haptics = Haptics(player)

    @Test
    fun `each effect passes its duration and amplitude`() {
        haptics.apply(Settings(vibrationEnabled = true))
        HapticEffect.entries.forEach(haptics::play)
        assertEquals(
            listOf(
                RecordingHapticPlayer.Vibration(30, 60),
                RecordingHapticPlayer.Vibration(50, 140),
                RecordingHapticPlayer.Vibration(1_000, 255),
            ),
            player.vibrations,
        )
    }

    @Test
    fun `vibration is on before the settings are applied, like the default`() {
        haptics.play(HapticEffect.SectionReplaced)
        assertEquals(1, player.vibrations.size)
    }

    @Test
    fun `nothing vibrates while vibration is off`() {
        haptics.apply(Settings(vibrationEnabled = false))
        HapticEffect.entries.forEach(haptics::play)
        assertTrue(player.vibrations.isEmpty())
    }

    @Test
    fun `turning vibration off cancels a running vibration`() {
        haptics.apply(Settings(vibrationEnabled = true))
        haptics.play(HapticEffect.LevelLost)
        haptics.apply(Settings(vibrationEnabled = false))
        assertEquals(1, player.cancels)
    }

    @Test
    fun `settings that leave vibration on do not cancel`() {
        haptics.apply(Settings(vibrationEnabled = true))
        haptics.play(HapticEffect.LevelLost)
        haptics.apply(Settings(vibrationEnabled = true, soundEnabled = false))
        assertEquals(0, player.cancels)
    }

    @Test
    fun `turning vibration back on applies to the next effect`() {
        haptics.apply(Settings(vibrationEnabled = false))
        haptics.play(HapticEffect.GapWarning)
        haptics.apply(Settings(vibrationEnabled = true))
        haptics.play(HapticEffect.GapWarning)
        assertEquals(listOf(RecordingHapticPlayer.Vibration(50, 140)), player.vibrations)
    }

    @Test
    fun `stop cancels`() {
        haptics.play(HapticEffect.LevelLost)
        haptics.stop()
        assertEquals(1, player.cancels)
    }

    @Test
    fun `without a player nothing happens`() {
        val silent = Haptics(null)
        silent.apply(Settings(vibrationEnabled = false))
        silent.play(HapticEffect.LevelLost)
        silent.stop()
    }
}
