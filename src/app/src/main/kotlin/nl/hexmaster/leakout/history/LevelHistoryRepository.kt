package nl.hexmaster.leakout.history

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** The player's finished levels. Only a win is ever recorded; a leak never reaches it. */
interface LevelHistoryRepository {
    /**
     * Records a win on [levelId] with [score] and [stars] by the [LevelHistoryRules], atomically.
     *
     * @throws IllegalArgumentException for a negative score or stars outside 0-3; nothing changes.
     */
    suspend fun recordCompletion(levelId: Int, score: Int, stars: Int): RecordResult

    /** The record of [levelId], null when that level has not been finished. */
    suspend fun get(levelId: Int): LevelHistory?

    /** The record of [levelId] now and after every change to it. */
    fun observe(levelId: Int): Flow<LevelHistory?>

    /** Every record, ordered by level id, now and after every change. */
    fun observeAll(): Flow<List<LevelHistory>>
}

/** Whether [levelId] has been finished: it has a record. */
suspend fun LevelHistoryRepository.isFinished(levelId: Int): Boolean = get(levelId) != null

/**
 * History kept in memory, for tests and previews. It applies the same [LevelHistoryRules] as the
 * database, so a test cannot pass on it with a result the real history would not give.
 */
class InMemoryLevelHistoryRepository(initial: List<LevelHistory> = emptyList()) : LevelHistoryRepository {
    private val records = MutableStateFlow(initial.associateBy { it.levelId })

    /** Every call to [recordCompletion] that was accepted, in order. */
    val recorded = mutableListOf<LevelHistory>()

    override suspend fun recordCompletion(levelId: Int, score: Int, stars: Int): RecordResult {
        var result: RecordResult = RecordResult.NoImprovement
        records.update { current ->
            val recording = LevelHistoryRules.record(current[levelId], levelId, score, stars)
            result = recording.result
            recording.record?.let { current + (levelId to it) } ?: current
        }
        recorded += LevelHistory(levelId, score, stars)
        return result
    }

    override suspend fun get(levelId: Int): LevelHistory? = records.value[levelId]

    override fun observe(levelId: Int): Flow<LevelHistory?> = records.asStateFlow().map { it[levelId] }

    override fun observeAll(): Flow<List<LevelHistory>> =
        records.asStateFlow().map { all -> all.values.sortedBy { it.levelId } }
}
