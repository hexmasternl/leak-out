package nl.hexmaster.leakout.audio

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.hexmaster.leakout.settings.DataStoreSettingsRepository
import nl.hexmaster.leakout.settings.Settings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The last-started track id on a real Preferences file, next to the settings on the same instance. */
@RunWith(AndroidJUnit4::class)
class DataStoreMusicRotationStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val fileName = "music-rotation-store-test"
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
    fun aFreshFileHasNoLastStartedTrack() = runBlocking {
        assertNull(DataStoreMusicRotationStore(openStore()).lastStartedTrackId())
    }

    @Test
    fun theIdSurvivesANewStoreOnTheSameFile() = runBlocking {
        DataStoreMusicRotationStore(openStore()).setLastStartedTrackId("bg_music_0004")
        closeAll()
        assertEquals("bg_music_0004", DataStoreMusicRotationStore(openStore()).lastStartedTrackId())
    }

    @Test
    fun itSharesTheInstanceWithTheSettings() = runBlocking {
        val shared = openStore()
        val rotation = DataStoreMusicRotationStore(shared)
        val settings = DataStoreSettingsRepository(shared)

        rotation.setLastStartedTrackId("bg_music_0007")
        settings.setMusicVolume(0.25f)
        rotation.setLastStartedTrackId("bg_music_0008")

        assertEquals("bg_music_0008", rotation.lastStartedTrackId())
        assertEquals(Settings(musicVolume = 0.25f), settings.settings.first())
        assertEquals("bg_music_0008", shared.data.first()[DataStoreMusicRotationStore.Key])
    }
}
