package nl.hexmaster.leakout.history.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import nl.hexmaster.leakout.history.LevelHistory
import nl.hexmaster.leakout.history.LevelHistoryRepository
import nl.hexmaster.leakout.history.LevelHistoryRules
import nl.hexmaster.leakout.history.RecordResult

/** The level history in [database]. */
class RoomLevelHistoryRepository(private val database: LeakOutDatabase) : LevelHistoryRepository {
    private val dao = database.levelHistoryDao()

    override suspend fun recordCompletion(levelId: Int, score: Int, stars: Int): RecordResult {
        // Before the transaction, so a rejected value never opens one.
        LevelHistoryRules.validate(score, stars)
        // Read and write in one transaction, so two wins recorded at once cannot both insert.
        return database.withTransaction {
            val recording = LevelHistoryRules.record(dao.get(levelId)?.toDomain(), levelId, score, stars)
            val row = recording.record?.let(LevelHistoryEntity::of)
            when (recording.result) {
                RecordResult.FirstCompletion -> dao.insert(checkNotNull(row))
                is RecordResult.NewHighscore -> dao.update(checkNotNull(row))
                RecordResult.NoImprovement -> Unit
            }
            recording.result
        }
    }

    override suspend fun get(levelId: Int): LevelHistory? = dao.get(levelId)?.toDomain()

    // Room re-queries on any change to the table; only a change to this level is passed on.
    override fun observe(levelId: Int): Flow<LevelHistory?> =
        dao.observe(levelId).map { it?.toDomain() }.distinctUntilChanged()

    override fun observeAll(): Flow<List<LevelHistory>> =
        dao.observeAll().map { rows -> rows.map(LevelHistoryEntity::toDomain) }
}
