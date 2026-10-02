package nl.hexmaster.leakout.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.hexmaster.leakout.history.LevelHistory
import nl.hexmaster.leakout.history.data.LeakOutDatabase
import nl.hexmaster.leakout.history.data.RoomLevelHistoryRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The seen tips on a real Preferences file, next to a real level history: they never touch each other. */
@RunWith(AndroidJUnit4::class)
class SeenTipsStorageTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val fileName = "seen-tips-storage-test"
    private val scopes = mutableListOf<CoroutineScope>()
    private lateinit var database: LeakOutDatabase
    private lateinit var history: RoomLevelHistoryRepository

    @Before
    fun setUp() {
        context.preferencesDataStoreFile(fileName).delete()
        database = Room.inMemoryDatabaseBuilder(context, LeakOutDatabase::class.java).build()
        history = RoomLevelHistoryRepository(database)
    }

    @After
    fun tearDown() = runBlocking {
        closeAll()
        database.close()
        context.preferencesDataStoreFile(fileName).delete()
        Unit
    }

    private fun openStore(): DataStore<Preferences> {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob()).also { scopes += it }
        return PreferenceDataStoreFactory.create(scope = scope, produceFile = { context.preferencesDataStoreFile(fileName) })
    }

    private suspend fun closeAll() {
        scopes.forEach { it.coroutineContext[Job]?.cancelAndJoin() }
        scopes.clear()
    }

    @Test
    fun aSeenTipSurvivesARestart() = runBlocking {
        DataStoreSettingsRepository(openStore()).markTipSeen(TipId.Cross)
        closeAll()
        assertEquals(setOf(TipId.Cross), DataStoreSettingsRepository(openStore()).seenTips.first())
    }

    @Test
    fun clearingTheHistoryLeavesTheSeenTipsAlone() = runBlocking {
        val settings = DataStoreSettingsRepository(openStore())
        settings.markTipSeen(TipId.Intro)
        history.recordCompletion(2, 120, 2)
        history.clearAll()
        assertEquals(setOf(TipId.Intro), settings.seenTips.first())
    }

    @Test
    fun resettingTipsLeavesTheSettingsAndTheHistoryAlone() = runBlocking {
        val settings = DataStoreSettingsRepository(openStore())
        settings.setVibrationEnabled(false)
        settings.markTipSeen(TipId.Intro)
        settings.markTipSeen(TipId.Cross)
        history.recordCompletion(2, 120, 2)
        settings.resetTips()
        assertEquals(emptySet<TipId>(), settings.seenTips.first())
        assertEquals(false, settings.settings.first().vibrationEnabled)
        assertEquals(listOf(LevelHistory(2, 120, 2)), history.observeAll().first())
    }
}
