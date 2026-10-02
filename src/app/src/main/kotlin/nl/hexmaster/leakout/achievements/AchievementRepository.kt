package nl.hexmaster.leakout.achievements

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import nl.hexmaster.leakout.core.achievements.Achievement
import nl.hexmaster.leakout.core.achievements.Evaluation
import nl.hexmaster.leakout.core.achievements.PlayerStats
import nl.hexmaster.leakout.core.achievements.StoredAchievement

/**
 * One achievement as the app shows it: its progress and, once unlocked, when.
 *
 * @param steps the progress of an incremental achievement, at most [Achievement.steps]; 0 for a standard one.
 * @param unlockedAtMs when it was unlocked (epoch milliseconds), null while locked.
 */
data class AchievementProgress(val achievement: Achievement, val steps: Int = 0, val unlockedAtMs: Long? = null) {
    val unlocked: Boolean get() = unlockedAtMs != null

    /** What the rules read back from the store. */
    fun toStored(): StoredAchievement = StoredAchievement(steps, unlocked)
}

/**
 * The local achievement store: the source of truth, so achievements work without Play Games. It
 * holds every achievement's progress and unlock time, and the player stats with their loss streaks.
 * A progress reset never touches it.
 */
interface AchievementRepository {
    /** Every catalogue achievement in catalogue order, now and after every change; one without a row is locked with no progress. */
    fun observeAll(): Flow<List<AchievementProgress>>

    /** The stored lifetime player stats. */
    suspend fun stats(): PlayerStats

    /** What is stored per achievement, for the rules; achievements without a row are left out. */
    suspend fun stored(): Map<Achievement, StoredAchievement>

    /**
     * Stores [evaluation] and [stats] in one transaction: the evaluation's steps, an unlock time for
     * each newly unlocked achievement, and the stats with their loss streaks. An achievement that is
     * already unlocked keeps its unlock time.
     */
    suspend fun apply(evaluation: Evaluation, stats: PlayerStats)

    /** Runs [block] in one transaction, so a read, an evaluation and its [apply] cannot interleave with another. */
    suspend fun <T> transaction(block: suspend () -> T): T
}

/** The full catalogue from the stored rows; unknown keys are ignored. */
internal fun progressOf(rows: Map<String, AchievementProgressRow>): List<AchievementProgress> =
    Achievement.entries.map { achievement ->
        rows[achievement.key]?.let { AchievementProgress(achievement, it.steps, it.unlockedAtMs) } ?: AchievementProgress(achievement)
    }

/** A stored row, independent of the storage. */
internal data class AchievementProgressRow(val steps: Int, val unlockedAtMs: Long?)

/**
 * The rows [evaluation] changes, by key: new steps for incremental achievements and an unlock time
 * of [nowMs] for newly unlocked ones. Rows that stay the same are left out.
 */
internal fun changedRows(
    current: Map<String, AchievementProgressRow>,
    evaluation: Evaluation,
    nowMs: Long,
): Map<String, AchievementProgressRow> {
    val changed = mutableMapOf<String, AchievementProgressRow>()
    for (achievement in Achievement.entries) {
        val before = current[achievement.key] ?: AchievementProgressRow(0, null)
        val steps = evaluation.steps[achievement] ?: before.steps
        val unlockedAt = before.unlockedAtMs ?: nowMs.takeIf { achievement in evaluation.newlyUnlocked }
        val after = AchievementProgressRow(steps, unlockedAt)
        if (after != before) changed[achievement.key] = after
    }
    return changed
}

/** The store in memory, for tests and previews. It writes exactly what the Room store writes. */
class InMemoryAchievementRepository(
    initialStats: PlayerStats = PlayerStats.NONE,
    initial: List<AchievementProgress> = emptyList(),
    private val clock: () -> Long = System::currentTimeMillis,
) : AchievementRepository {
    private val rows = MutableStateFlow(initial.associate { it.achievement.key to AchievementProgressRow(it.steps, it.unlockedAtMs) })
    private val playerStats = MutableStateFlow(initialStats)
    private val lock = Mutex()

    /** The stats as stored now, for tests. */
    val currentStats: PlayerStats get() = playerStats.value

    /** Every call to [apply], in order, for tests. */
    val applied = mutableListOf<Evaluation>()

    override fun observeAll(): Flow<List<AchievementProgress>> = rows.asStateFlow().map(::progressOf)

    override suspend fun stats(): PlayerStats = playerStats.value

    override suspend fun stored(): Map<Achievement, StoredAchievement> =
        progressOf(rows.value).filter { it.achievement.key in rows.value }.associate { it.achievement to it.toStored() }

    override suspend fun apply(evaluation: Evaluation, stats: PlayerStats) {
        val changed = changedRows(rows.value, evaluation, clock())
        if (changed.isNotEmpty()) rows.value = rows.value + changed
        playerStats.value = stats
        applied += evaluation
    }

    override suspend fun <T> transaction(block: suspend () -> T): T = lock.withLock { block() }
}
