package nl.hexmaster.leakout.history.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import nl.hexmaster.leakout.achievements.data.AchievementDao
import nl.hexmaster.leakout.achievements.data.AchievementStateEntity
import nl.hexmaster.leakout.achievements.data.LevelLossStreakEntity
import nl.hexmaster.leakout.achievements.data.PlayerStatsEntity

/**
 * The on-device database. Every version's schema is exported to `app/schemas` and committed, and a
 * version bump needs a written migration: there is no destructive fallback, so progress is never lost.
 *
 * Version 2 adds the achievement tables, which a progress reset leaves alone. Version 3 adds the
 * tips-reset count to the player stats.
 */
@Database(
    entities = [LevelHistoryEntity::class, AchievementStateEntity::class, PlayerStatsEntity::class, LevelLossStreakEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class LeakOutDatabase : RoomDatabase() {
    abstract fun levelHistoryDao(): LevelHistoryDao

    abstract fun achievementDao(): AchievementDao

    companion object {
        const val FILE_NAME = "leak-out.db"

        /** Only creates the achievement tables, so the level history is untouched. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `achievement_state` (`key` TEXT NOT NULL, `steps` INTEGER NOT NULL, " +
                        "`unlocked_at_ms` INTEGER, PRIMARY KEY(`key`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `player_stats` (`id` INTEGER NOT NULL, `sections_placed` INTEGER NOT NULL, " +
                        "`losses` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `level_loss_streak` (`level_id` INTEGER NOT NULL, `streak` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`level_id`))",
                )
            }
        }

        /** Adds the tips-reset count to the player stats, starting at 0; every other row is kept. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `player_stats` ADD COLUMN `tips_resets` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Every migration, oldest first; tests open past versions with them. */
        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)

        @Volatile
        private var instance: LeakOutDatabase? = null

        /** The single database of the app process. */
        fun get(context: Context): LeakOutDatabase =
            instance ?: synchronized(this) {
                instance ?: open(context.applicationContext, FILE_NAME).also { instance = it }
            }

        /** Opens the database file [name]; tests use their own file to check persistence. */
        fun open(context: Context, name: String): LeakOutDatabase =
            Room.databaseBuilder(context, LeakOutDatabase::class.java, name).addMigrations(*MIGRATIONS).build()
    }
}
