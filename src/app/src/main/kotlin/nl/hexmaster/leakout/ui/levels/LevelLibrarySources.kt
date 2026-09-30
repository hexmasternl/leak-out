package nl.hexmaster.leakout.ui.levels

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** How many levels there are, ids `1..levelCount`. The bundled levels implement it. */
fun interface LevelCatalog {
    fun levelCount(): Int
}

/** Best stars per finished level id; a level missing from the map is unfinished. */
fun interface HistoryReader {
    fun stars(): Flow<Map<Int, Int>>
}

/**
 * History kept in memory, keeping the best stars per level. Stands in for the persistent
 * `game-level-history` repository until that exists; it starts empty, so only level 1 is open.
 */
class InMemoryHistory(initial: Map<Int, Int> = emptyMap()) : HistoryReader {
    private val state = MutableStateFlow(initial)

    override fun stars(): Flow<Map<Int, Int>> = state.asStateFlow()

    /** Records a win on [levelId] with [stars]; a lower result than the stored one is ignored. */
    fun record(levelId: Int, stars: Int) {
        state.update { it + (levelId to maxOf(stars, it[levelId] ?: 0)) }
    }
}
