package nl.hexmaster.leakout.history.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import nl.hexmaster.leakout.history.LevelHistory
import nl.hexmaster.leakout.history.RecordResult
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The level history on a real Room database: the DAO, the repository's transaction and the observers. */
@RunWith(AndroidJUnit4::class)
class LevelHistoryDatabaseTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: LeakOutDatabase
    private lateinit var repository: RoomLevelHistoryRepository

    @Before
    fun openDatabase() {
        database = Room.inMemoryDatabaseBuilder(context, LeakOutDatabase::class.java).build()
        repository = RoomLevelHistoryRepository(database)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun aLevelFinishedSeveralTimesHasOneRecord() = runBlocking {
        repository.recordCompletion(3, 240, 2)
        repository.recordCompletion(3, 380, 3)
        repository.recordCompletion(3, 200, 1)
        repository.recordCompletion(3, 380, 1)
        assertEquals(listOf(LevelHistory(3, 380, 3)), repository.observeAll().first())
    }

    @Test
    fun recordingFollowsTheKeepTheBestRule() = runBlocking {
        assertEquals(RecordResult.FirstCompletion, repository.recordCompletion(3, 240, 2))
        assertEquals(RecordResult.NoImprovement, repository.recordCompletion(3, 240, 3))
        assertEquals(RecordResult.NewHighscore(previous = 240), repository.recordCompletion(3, 380, 3))
        assertEquals(RecordResult.NoImprovement, repository.recordCompletion(3, 200, 1))
        assertEquals(LevelHistory(3, 380, 3), repository.get(3))
    }

    @Test
    fun invalidValuesAreRejectedAndChangeNothing() = runBlocking {
        repository.recordCompletion(3, 240, 2)
        for ((score, stars) in listOf(-5 to 2, 300 to -1, 300 to 4)) {
            for (level in listOf(3, 4)) {
                try {
                    repository.recordCompletion(level, score, stars)
                    fail("Expected $score points with $stars stars to be rejected")
                } catch (_: IllegalArgumentException) {
                }
            }
        }
        assertEquals(listOf(LevelHistory(3, 240, 2)), repository.observeAll().first())
    }

    @Test
    fun readOneLevel() = runBlocking {
        repository.recordCompletion(3, 240, 2)
        assertEquals(LevelHistory(3, 240, 2), repository.get(3))
        assertNull(repository.get(2))
    }

    @Test
    fun readAllLevels() = runBlocking {
        repository.recordCompletion(4, 40, 1)
        repository.recordCompletion(1, 10, 3)
        repository.recordCompletion(2, 20, 2)
        assertEquals(
            listOf(LevelHistory(1, 10, 3), LevelHistory(2, 20, 2), LevelHistory(4, 40, 1)),
            repository.observeAll().first(),
        )
    }

    @Test
    fun theDaoRefusesASecondRowForALevel() = runBlocking {
        val dao = database.levelHistoryDao()
        dao.insert(LevelHistoryEntity(3, 240, 2))
        try {
            dao.insert(LevelHistoryEntity(3, 380, 3))
            fail("Expected the primary key to refuse a second row")
        } catch (_: SQLiteConstraintException) {
        }
        assertEquals(LevelHistoryEntity(3, 240, 2), dao.get(3))
    }

    @Test
    fun anObserverSeesANewHighscore() = runBlocking {
        repository.recordCompletion(3, 240, 2)
        // Undispatched, so the observer is collecting before the update is recorded.
        val next = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(5_000) { repository.observe(3).first { it?.highscore == 380 } }
        }
        repository.recordCompletion(3, 380, 3)
        assertEquals(LevelHistory(3, 380, 3), next.await())
    }

    @Test
    fun anObserverOfAllLevelsSeesAFirstCompletion() = runBlocking {
        val next = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(5_000) { repository.observeAll().first { it.isNotEmpty() } }
        }
        repository.recordCompletion(2, 20, 2)
        assertEquals(listOf(LevelHistory(2, 20, 2)), next.await())
    }

    @Test
    fun anObserverOfOneLevelIgnoresOtherLevels() = runBlocking {
        repository.recordCompletion(3, 240, 2)
        val change = async(start = CoroutineStart.UNDISPATCHED) { withTimeoutOrNull(1_000) { repository.observe(3).drop(1).firstOrNull() } }
        repository.recordCompletion(4, 100, 1)
        assertNull(change.await())
    }

    @Test
    fun recordsSurviveReopeningTheDatabase() = runBlocking {
        val name = "level-history-reopen-test.db"
        context.deleteDatabase(name)
        try {
            LeakOutDatabase.open(context, name).let { first ->
                RoomLevelHistoryRepository(first).recordCompletion(3, 240, 2)
                first.close()
            }
            LeakOutDatabase.open(context, name).let { reopened ->
                val history = RoomLevelHistoryRepository(reopened)
                assertEquals(LevelHistory(3, 240, 2), history.get(3))
                assertNull(history.get(1))
                reopened.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
