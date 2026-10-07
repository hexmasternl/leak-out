package nl.hexmaster.leakout.scores.online

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The queue on a real Preferences file: the highest score per level, kept across a restart (spec
 * `online-score-submission`: keeps only the highest score per level; survives restarts).
 */
@RunWith(AndroidJUnit4::class)
class DataStorePendingScoreQueueTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val fileName = "score-queue-test"
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

    private suspend fun closeAll() {
        scopes.forEach { it.coroutineContext[kotlinx.coroutines.Job]?.cancelAndJoin() }
        scopes.clear()
    }

    private fun open(): DataStorePendingScoreQueue {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob()).also { scopes += it }
        return DataStorePendingScoreQueue(PreferenceDataStoreFactory.create(scope = scope) { context.preferencesDataStoreFile(fileName) })
    }

    @Test
    fun aScoreForANewLevelIsAdded() = runBlocking {
        val queue = open()
        queue.offer(3, 90)
        assertEquals(mapOf(3 to 90), queue.snapshot())
    }

    @Test
    fun aHigherScoreReplacesAndALowerOneIsIgnored() = runBlocking {
        val queue = open()
        queue.offer(3, 90)
        queue.offer(3, 120)
        queue.offer(3, 100)
        queue.offer(3, 120)
        assertEquals(mapOf(3 to 120), queue.snapshot())
    }

    @Test
    fun levelsAreKeptApartAndZeroIsAScore() = runBlocking {
        val queue = open()
        queue.offer(3, 120)
        queue.offer(5, 80)
        queue.offer(2, 0)
        assertEquals(mapOf(2 to 0, 3 to 120, 5 to 80), queue.snapshot())
    }

    @Test
    fun removeTakesOutOnlyTheSentScore() = runBlocking {
        val queue = open()
        queue.offer(3, 90)
        queue.offer(5, 80)
        queue.remove(5, 80)
        // 90 was sent while 120 was queued: 120 stays.
        queue.offer(3, 120)
        queue.remove(3, 90)
        assertEquals(mapOf(3 to 120), queue.snapshot())
    }

    @Test
    fun theQueueSurvivesARestart() = runBlocking {
        open().offer(3, 120)
        closeAll()
        assertEquals(mapOf(3 to 120), open().snapshot())
    }
}
