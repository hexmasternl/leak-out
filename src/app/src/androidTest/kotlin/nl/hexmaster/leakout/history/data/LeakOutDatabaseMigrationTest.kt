package nl.hexmaster.leakout.history.data

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.hexmaster.leakout.achievements.data.RoomAchievementRepository
import nl.hexmaster.leakout.core.achievements.PlayerStats
import nl.hexmaster.leakout.history.LevelHistory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Version 1 to 2 adds the achievement tables and keeps every level history record. */
@RunWith(AndroidJUnit4::class)
class LeakOutDatabaseMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), LeakOutDatabase::class.java)

    @After
    fun deleteDatabase() {
        context.deleteDatabase(NAME)
    }

    @Test
    fun historyRowsSurviveAndTheAchievementTablesAreEmpty() {
        helper.createDatabase(NAME, 1).use { db ->
            for ((level, score, stars) in listOf(Triple(1, 240, 2), Triple(2, 380, 3), Triple(7, 90, 0))) {
                db.execSQL("INSERT INTO level_history (level_id, highscore, stars) VALUES ($level, $score, $stars)")
            }
        }

        // Validates the migrated schema against the exported version 2 schema.
        helper.runMigrationsAndValidate(NAME, 2, true, *LeakOutDatabase.MIGRATIONS).use { db ->
            for (table in listOf("achievement_state", "player_stats", "level_loss_streak")) {
                db.query("SELECT COUNT(*) FROM $table").use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals("$table rows", 0, cursor.getInt(0))
                }
            }
        }

        val database = LeakOutDatabase.open(context, NAME)
        try {
            val achievements = RoomAchievementRepository(database)
            runBlocking {
                val history = RoomLevelHistoryRepository(database).observeAll().first()
                assertEquals(listOf(LevelHistory(1, 240, 2), LevelHistory(2, 380, 3), LevelHistory(7, 90, 0)), history)
                assertEquals(PlayerStats.NONE, achievements.stats())
                assertTrue(achievements.stored().isEmpty())
                assertTrue(achievements.observeAll().first().none { it.unlocked || it.steps > 0 })
            }
        } finally {
            database.close()
        }
    }

    private companion object {
        const val NAME = "migration-test.db"
    }
}
