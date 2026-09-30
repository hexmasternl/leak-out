package nl.hexmaster.leakout.history.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The on-device database. Every version's schema is exported to `app/schemas` and committed, and a
 * version bump needs a written migration: there is no destructive fallback, so progress is never lost.
 */
@Database(entities = [LevelHistoryEntity::class], version = 1, exportSchema = true)
abstract class LeakOutDatabase : RoomDatabase() {
    abstract fun levelHistoryDao(): LevelHistoryDao

    companion object {
        const val FILE_NAME = "leak-out.db"

        @Volatile
        private var instance: LeakOutDatabase? = null

        /** The single database of the app process. */
        fun get(context: Context): LeakOutDatabase =
            instance ?: synchronized(this) {
                instance ?: open(context.applicationContext, FILE_NAME).also { instance = it }
            }

        /** Opens the database file [name]; tests use their own file to check persistence. */
        fun open(context: Context, name: String): LeakOutDatabase =
            Room.databaseBuilder(context, LeakOutDatabase::class.java, name).build()
    }
}
