package nl.hexmaster.leakout.achievements.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.hexmaster.leakout.core.achievements.Achievement
import nl.hexmaster.leakout.core.achievements.Evaluation
import nl.hexmaster.leakout.core.achievements.PlayerStats
import nl.hexmaster.leakout.core.achievements.StoredAchievement
import nl.hexmaster.leakout.history.data.LeakOutDatabase
import nl.hexmaster.leakout.history.data.RoomLevelHistoryRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The achievement store on a real Room database file: what is written, kept across a reopen, and kept by a progress reset. */
@RunWith(AndroidJUnit4::class)
class AchievementDatabaseTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private var now = 1_000L
    private lateinit var database: LeakOutDatabase

    @Before
    fun open() {
        context.deleteDatabase(NAME)
        database = LeakOutDatabase.open(context, NAME)
    }

    @After
    fun close() {
        database.close()
        context.deleteDatabase(NAME)
    }

    private fun repository() = RoomAchievementRepository(database) { now }

    private val stats = PlayerStats(sectionsPlaced = 250, losses = 4, lossStreakByLevel = mapOf(3 to 2, 9 to 1))

    @Test
    fun unlocksStepsAndStatsSurviveAReopen() = runBlocking {
        repository().apply(Evaluation(listOf(Achievement.ZERO_WASTE), mapOf(Achievement.APPRENTICE to 100, Achievement.JOURNEYMAN to 250)), stats)
        database.close()
        database = LeakOutDatabase.open(context, NAME)
        now = 9_000L

        val reopened = repository()
        assertEquals(stats, reopened.stats())
        val all = reopened.observeAll().first().associateBy { it.achievement }
        assertEquals(1_000L, all.getValue(Achievement.ZERO_WASTE).unlockedAtMs)
        assertEquals(250, all.getValue(Achievement.JOURNEYMAN).steps)
        assertNull(all.getValue(Achievement.JOURNEYMAN).unlockedAtMs)
        assertEquals(StoredAchievement(100, unlocked = false), reopened.stored()[Achievement.APPRENTICE])
    }

    @Test
    fun anUnlockKeepsItsFirstTimeAndAnEndedStreakIsDeleted() = runBlocking {
        val store = repository()
        store.apply(Evaluation(listOf(Achievement.WET_FEET), emptyMap()), stats)
        now = 5_000L
        store.apply(Evaluation(emptyList(), emptyMap()), stats.copy(lossStreakByLevel = mapOf(9 to 1)))
        assertEquals(1_000L, store.observeAll().first().first { it.achievement == Achievement.WET_FEET }.unlockedAtMs)
        assertEquals(mapOf(9 to 1), store.stats().lossStreakByLevel)
    }

    @Test
    fun resettingTheHistoryKeepsTheAchievementTables() = runBlocking {
        val history = RoomLevelHistoryRepository(database)
        history.recordCompletion(1, 200, 3)
        val store = repository()
        store.apply(Evaluation(listOf(Achievement.FIRST_LEAK_FIXED), emptyMap()), stats)

        history.clearAll()

        assertEquals(emptyList<Any>(), history.observeAll().first())
        assertEquals(stats, store.stats())
        assertEquals(StoredAchievement(0, unlocked = true), store.stored()[Achievement.FIRST_LEAK_FIXED])
    }

    private companion object {
        const val NAME = "achievement-test.db"
    }
}
