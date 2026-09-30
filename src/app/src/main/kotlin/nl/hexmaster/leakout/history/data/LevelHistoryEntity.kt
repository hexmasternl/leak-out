package nl.hexmaster.leakout.history.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import nl.hexmaster.leakout.history.LevelHistory

/**
 * One finished level. The level id is the primary key, so there is at most one row per level; it
 * has no foreign key because the levels live in JSON assets, not in the database.
 */
@Entity(tableName = "level_history")
data class LevelHistoryEntity(
    @PrimaryKey @ColumnInfo(name = "level_id") val levelId: Int,
    @ColumnInfo(name = "highscore") val highscore: Int,
    @ColumnInfo(name = "stars") val stars: Int,
) {
    fun toDomain(): LevelHistory = LevelHistory(levelId, highscore, stars)

    companion object {
        fun of(history: LevelHistory): LevelHistoryEntity =
            LevelHistoryEntity(history.levelId, history.highscore, history.stars)
    }
}
