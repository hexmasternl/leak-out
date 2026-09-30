package nl.hexmaster.leakout.ui.levels

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nl.hexmaster.leakout.history.LevelHistoryRepository

/** How many levels there are, ids `1..levelCount`. The bundled levels implement it. */
fun interface LevelCatalog {
    fun levelCount(): Int
}

/** Best stars per finished level id; a level missing from the map is unfinished. */
fun interface HistoryReader {
    fun stars(): Flow<Map<Int, Int>>
}

/**
 * The level history as the library reads it: the stars stored with each finished level's
 * highscore. A level finished with 0 stars is in the map, so it counts as completed and unlocks
 * the next one. Every recorded win emits again, so the library is up to date after a game.
 */
fun LevelHistoryRepository.asHistoryReader(): HistoryReader = HistoryReader {
    observeAll().map { records -> records.associate { it.levelId to it.stars } }
}
