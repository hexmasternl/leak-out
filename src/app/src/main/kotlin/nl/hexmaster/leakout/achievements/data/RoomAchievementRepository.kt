package nl.hexmaster.leakout.achievements.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import nl.hexmaster.leakout.achievements.AchievementProgress
import nl.hexmaster.leakout.achievements.AchievementProgressRow
import nl.hexmaster.leakout.achievements.AchievementRepository
import nl.hexmaster.leakout.achievements.changedRows
import nl.hexmaster.leakout.achievements.progressOf
import nl.hexmaster.leakout.core.achievements.Achievement
import nl.hexmaster.leakout.core.achievements.Evaluation
import nl.hexmaster.leakout.core.achievements.PlayerStats
import nl.hexmaster.leakout.core.achievements.StoredAchievement
import nl.hexmaster.leakout.history.data.LeakOutDatabase

/** The achievement store in [database], next to the level history but never cleared with it. */
class RoomAchievementRepository(
    private val database: LeakOutDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) : AchievementRepository {
    private val dao = database.achievementDao()

    // Room re-queries on any write to the table; only real changes are passed on, so the syncer
    // does not push the same state again after every stats update.
    override fun observeAll(): Flow<List<AchievementProgress>> =
        dao.observeStates().map { rows -> progressOf(rows.toRows()) }.distinctUntilChanged()

    override suspend fun stats(): PlayerStats = database.withTransaction {
        val row = dao.stats()
        PlayerStats(
            sectionsPlaced = row?.sectionsPlaced ?: 0,
            losses = row?.losses ?: 0,
            lossStreakByLevel = dao.lossStreaks().associate { it.levelId to it.streak },
        )
    }

    override suspend fun stored(): Map<Achievement, StoredAchievement> =
        dao.states().mapNotNull { row ->
            Achievement.fromKey(row.key)?.let { it to StoredAchievement(row.steps, row.unlockedAtMs != null) }
        }.toMap()

    override suspend fun apply(evaluation: Evaluation, stats: PlayerStats) = database.withTransaction {
        val changed = changedRows(dao.states().toRows(), evaluation, clock())
        if (changed.isNotEmpty()) {
            dao.upsertStates(changed.map { (key, row) -> AchievementStateEntity(key, row.steps, row.unlockedAtMs) })
        }
        dao.upsertStats(PlayerStatsEntity(sectionsPlaced = stats.sectionsPlaced, losses = stats.losses))
        val ended = dao.lossStreaks().map { it.levelId }.filter { it !in stats.lossStreakByLevel }
        if (ended.isNotEmpty()) dao.deleteLossStreaks(ended)
        if (stats.lossStreakByLevel.isNotEmpty()) {
            dao.upsertLossStreaks(stats.lossStreakByLevel.map { (level, streak) -> LevelLossStreakEntity(level, streak) })
        }
    }

    override suspend fun <T> transaction(block: suspend () -> T): T = database.withTransaction { block() }

    private fun List<AchievementStateEntity>.toRows(): Map<String, AchievementProgressRow> =
        associate { it.key to AchievementProgressRow(it.steps, it.unlockedAtMs) }
}
