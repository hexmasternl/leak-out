package nl.hexmaster.leakout.history.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Plain row access. The keep-the-best rule lives in [RoomLevelHistoryRepository], not here. */
@Dao
interface LevelHistoryDao {
    @Query("SELECT * FROM level_history WHERE level_id = :levelId")
    suspend fun get(levelId: Int): LevelHistoryEntity?

    @Query("SELECT * FROM level_history WHERE level_id = :levelId")
    fun observe(levelId: Int): Flow<LevelHistoryEntity?>

    @Query("SELECT * FROM level_history ORDER BY level_id")
    fun observeAll(): Flow<List<LevelHistoryEntity>>

    /** Fails on an existing level id: a second row for a level is a bug, not something to replace. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: LevelHistoryEntity)

    @Update
    suspend fun update(entity: LevelHistoryEntity)
}
