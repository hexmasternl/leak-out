package nl.hexmaster.leakout.achievements.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Plain row access to the achievement tables. The rules live in core; the transaction in [RoomAchievementRepository]. */
@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievement_state")
    fun observeStates(): Flow<List<AchievementStateEntity>>

    @Query("SELECT * FROM achievement_state")
    suspend fun states(): List<AchievementStateEntity>

    @Upsert
    suspend fun upsertStates(states: List<AchievementStateEntity>)

    @Query("SELECT * FROM player_stats WHERE id = ${PlayerStatsEntity.SINGLE_ROW}")
    suspend fun stats(): PlayerStatsEntity?

    @Upsert
    suspend fun upsertStats(stats: PlayerStatsEntity)

    @Query("SELECT * FROM level_loss_streak")
    suspend fun lossStreaks(): List<LevelLossStreakEntity>

    @Upsert
    suspend fun upsertLossStreaks(streaks: List<LevelLossStreakEntity>)

    @Query("DELETE FROM level_loss_streak WHERE level_id IN (:levelIds)")
    suspend fun deleteLossStreaks(levelIds: List<Int>)
}
