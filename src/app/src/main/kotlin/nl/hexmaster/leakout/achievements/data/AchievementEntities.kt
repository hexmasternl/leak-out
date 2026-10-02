package nl.hexmaster.leakout.achievements.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The progress of one achievement. Stored by [key], never by the enum's position, so the catalogue
 * can be reordered; a key this version does not know is kept and ignored.
 *
 * @param steps the progress of an incremental achievement; 0 for a standard one.
 * @param unlockedAtMs when it was unlocked (epoch milliseconds), null while locked.
 */
@Entity(tableName = "achievement_state")
data class AchievementStateEntity(
    @PrimaryKey @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "steps") val steps: Int,
    @ColumnInfo(name = "unlocked_at_ms") val unlockedAtMs: Long?,
)

/** The lifetime player stats: a single row with id [SINGLE_ROW]. */
@Entity(tableName = "player_stats")
data class PlayerStatsEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Int = SINGLE_ROW,
    @ColumnInfo(name = "sections_placed") val sectionsPlaced: Int,
    @ColumnInfo(name = "losses") val losses: Int,
) {
    companion object {
        const val SINGLE_ROW = 0
    }
}

/** The losses on one level since the last win on it. A level without a streak has no row. */
@Entity(tableName = "level_loss_streak")
data class LevelLossStreakEntity(
    @PrimaryKey @ColumnInfo(name = "level_id") val levelId: Int,
    @ColumnInfo(name = "streak") val streak: Int,
)
