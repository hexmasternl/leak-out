package nl.hexmaster.leakout.settings

import androidx.datastore.preferences.core.mutablePreferencesOf
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsTest {

    @Test
    fun `defaults are sound on at 0_8, music on at 0_6 and vibration on`() {
        val defaults = Settings()
        assertTrue(defaults.soundEnabled)
        assertEquals(0.8f, defaults.soundVolume, 0f)
        assertTrue(defaults.musicEnabled)
        assertEquals(0.6f, defaults.musicVolume, 0f)
        assertTrue(defaults.vibrationEnabled)
        assertTrue(defaults.remindersEnabled)
        assertEquals(defaults, Settings.Default)
    }

    @Test
    fun `the reminders setter writes only its own field`() = runTest {
        val repository = InMemorySettingsRepository()
        repository.setRemindersEnabled(false)
        assertEquals(Settings.Default.copy(remindersEnabled = false), repository.settings.first())
        assertEquals(listOf("remindersEnabled"), repository.writes)
    }

    @Test
    fun `reminders read as on when nothing is stored and off when stored off`() {
        val keys = DataStoreSettingsRepository.Keys
        assertTrue(DataStoreSettingsRepository.read(mutablePreferencesOf()).remindersEnabled)
        assertFalse(DataStoreSettingsRepository.read(mutablePreferencesOf(keys.RemindersEnabled to false)).remindersEnabled)
        assertEquals("reminders_enabled", keys.RemindersEnabled.name)
    }

    @Test
    fun `volumes are clamped to 0 to 1`() {
        val cases = mapOf(
            -0.2f to 0f, 0f to 0f, 0.35f to 0.35f, 1f to 1f, 1.5f to 1f,
            Float.NEGATIVE_INFINITY to 0f, Float.POSITIVE_INFINITY to 1f, Float.NaN to 0f,
        )
        for ((value, expected) in cases) assertEquals("clamp($value)", expected, Settings.clampVolume(value), 0f)
    }

    @Test
    fun `in memory repository starts on the defaults`() = runTest {
        assertEquals(Settings.Default, InMemorySettingsRepository().settings.first())
    }

    @Test
    fun `every setter writes its own field only`() = runTest {
        val repository = InMemorySettingsRepository()
        repository.setSoundEnabled(false)
        repository.setSoundVolume(0.3f)
        repository.setMusicEnabled(false)
        repository.setMusicVolume(0.2f)
        repository.setVibrationEnabled(false)
        assertEquals(Settings(false, 0.3f, false, 0.2f, false), repository.settings.first())
        assertEquals(
            listOf("soundEnabled", "soundVolume", "musicEnabled", "musicVolume", "vibrationEnabled"),
            repository.writes,
        )
    }

    @Test
    fun `a collector sees a change`() = runTest {
        val repository = InMemorySettingsRepository()
        val next = async(start = CoroutineStart.UNDISPATCHED) { repository.settings.first { !it.vibrationEnabled } }
        repository.setVibrationEnabled(false)
        assertFalse(next.await().vibrationEnabled)
    }

    @Test
    fun `a volume above range is stored as 1`() = runTest {
        val repository = InMemorySettingsRepository()
        repository.setSoundVolume(1.5f)
        repository.setMusicVolume(7f)
        val settings = repository.settings.first()
        assertEquals(1f, settings.soundVolume, 0f)
        assertEquals(1f, settings.musicVolume, 0f)
    }

    @Test
    fun `a volume below range is stored as 0`() = runTest {
        val repository = InMemorySettingsRepository()
        repository.setMusicVolume(-0.2f)
        repository.setSoundVolume(-3f)
        val settings = repository.settings.first()
        assertEquals(0f, settings.musicVolume, 0f)
        assertEquals(0f, settings.soundVolume, 0f)
    }

    @Test
    fun `out of range initial values are clamped`() = runTest {
        val repository = InMemorySettingsRepository(Settings(soundVolume = 2f, musicVolume = -1f))
        assertEquals(1f, repository.settings.first().soundVolume, 0f)
        assertEquals(0f, repository.settings.first().musicVolume, 0f)
    }

    @Test
    fun `empty preferences read as the defaults`() {
        assertEquals(Settings.Default, DataStoreSettingsRepository.read(mutablePreferencesOf()))
    }

    @Test
    fun `stored preferences are read per key and volumes are clamped`() {
        val keys = DataStoreSettingsRepository.Keys
        val preferences = mutablePreferencesOf(
            keys.SoundEnabled to false,
            keys.SoundVolume to 1.7f,
            keys.MusicEnabled to false,
            keys.MusicVolume to -0.4f,
            keys.VibrationEnabled to false,
        )
        assertEquals(Settings(false, 1f, false, 0f, false), DataStoreSettingsRepository.read(preferences))
    }

    @Test
    fun `the preference key names are the file format`() {
        val keys = DataStoreSettingsRepository.Keys
        assertEquals(
            listOf("sound_enabled", "sound_volume", "music_enabled", "music_volume", "vibration_enabled"),
            listOf(keys.SoundEnabled, keys.SoundVolume, keys.MusicEnabled, keys.MusicVolume, keys.VibrationEnabled).map { it.name },
        )
        assertEquals("settings", SETTINGS_DATASTORE_NAME)
    }
}
