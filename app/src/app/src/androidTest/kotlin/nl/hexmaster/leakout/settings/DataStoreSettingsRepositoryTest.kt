package nl.hexmaster.leakout.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The settings on a real Preferences file: defaults, persistence across instances and clamping. */
@RunWith(AndroidJUnit4::class)
class DataStoreSettingsRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val fileName = "settings-repository-test"
    private val scopes = mutableListOf<CoroutineScope>()

    @Before
    fun deleteFile() {
        context.preferencesDataStoreFile(fileName).delete()
    }

    @After
    fun closeAndDelete() = runBlocking {
        closeAll()
        context.preferencesDataStoreFile(fileName).delete()
        Unit
    }

    /** A fresh DataStore on the test file. Only one may be open per file, so [closeAll] the previous one first. */
    private fun openStore(): DataStore<Preferences> {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob()).also { scopes += it }
        return PreferenceDataStoreFactory.create(scope = scope, produceFile = { context.preferencesDataStoreFile(fileName) })
    }

    private suspend fun closeAll() {
        scopes.forEach { it.coroutineContext[Job]?.cancelAndJoin() }
        scopes.clear()
    }

    @Test
    fun anEmptyFileReadsAsTheDefaults() = runBlocking {
        assertEquals(Settings.Default, DataStoreSettingsRepository(openStore()).settings.first())
    }

    @Test
    fun valuesSurviveANewRepositoryOnTheSameFile() = runBlocking {
        DataStoreSettingsRepository(openStore()).apply {
            setSoundEnabled(false)
            setSoundVolume(0.25f)
            setMusicEnabled(false)
            setMusicVolume(0.4f)
            setVibrationEnabled(false)
        }
        closeAll()

        val reopened = DataStoreSettingsRepository(openStore()).settings.first()
        assertEquals(Settings(false, 0.25f, false, 0.4f, false), reopened)
    }

    @Test
    fun outOfRangeVolumesAreClampedOnWrite() = runBlocking {
        val repository = DataStoreSettingsRepository(openStore())
        repository.setSoundVolume(1.5f)
        repository.setMusicVolume(-0.2f)
        val settings = repository.settings.first()
        assertEquals(1f, settings.soundVolume, 0f)
        assertEquals(0f, settings.musicVolume, 0f)
    }

    @Test
    fun outOfRangeStoredVolumesAreClampedOnRead() = runBlocking {
        // Written around the repository, as a corrupt or hand-edited file would hold them.
        val store = openStore()
        store.edit {
            it[DataStoreSettingsRepository.Keys.SoundVolume] = 3.2f
            it[DataStoreSettingsRepository.Keys.MusicVolume] = -1f
        }
        val settings = DataStoreSettingsRepository(store).settings.first()
        assertEquals(1f, settings.soundVolume, 0f)
        assertEquals(0f, settings.musicVolume, 0f)
    }

    @Test
    fun aCollectorSeesAChange() = runBlocking {
        val repository = DataStoreSettingsRepository(openStore())
        val next = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(5_000) { repository.settings.first { !it.vibrationEnabled } }
        }
        repository.setVibrationEnabled(false)
        assertFalse(next.await().vibrationEnabled)
    }

    @Test
    fun otherKeysInTheSharedFileAreKeptAndIgnored() = runBlocking {
        val store = openStore()
        val otherKey = stringPreferencesKey("music_last_started_track")
        store.edit { it[otherKey] = "track-1" }
        val repository = DataStoreSettingsRepository(store)
        repository.setVibrationEnabled(false)
        assertEquals("track-1", store.data.first()[otherKey])
        assertEquals(Settings(vibrationEnabled = false), repository.settings.first())
    }

    @Test
    fun skipExitConfirmationLeavesTheSettingsAloneAndSurvivesReopening() = runBlocking {
        DataStoreSettingsRepository(openStore()).apply {
            setSoundEnabled(false)
            setSkipExitConfirmation(true)
        }
        closeAll()

        val reopened = DataStoreSettingsRepository(openStore())
        assertEquals(Settings(soundEnabled = false), reopened.settings.first())
        assertEquals(true, reopened.skipExitConfirmation.first())
    }
}
