package nl.hexmaster.leakout.core.library

/** What a level tile shows: locked, playable but not finished, or finished with its best stars. */
sealed interface TileState {
    data object Locked : TileState
    data object Unlocked : TileState
    data class Completed(val stars: Int) : TileState
}

data class LevelTile(val levelId: Int, val state: TileState) {
    /** Unlocked and completed levels can be started; locked ones cannot. */
    val isPlayable: Boolean get() = state != TileState.Locked
}

/**
 * One page of the library: up to [LevelLibraryLayout.LEVELS_PER_PAGE] tiles, fewer on the last page.
 *
 * @param number 1-based page number.
 * @param earnedStars stars earned over the levels of this page; unplayed levels count 0.
 */
data class LibraryPage(val number: Int, val tiles: List<LevelTile>, val earnedStars: Int) {
    /** A page can be shown only when it holds at least one playable level. */
    val isAccessible: Boolean get() = tiles.any { it.isPlayable }
}

/** The whole library, derived from the level count and the history; nothing is stored per page. */
data class LevelLibrary(val pages: List<LibraryPage>, val pageUnlockStars: Int) {
    val accessiblePages: List<LibraryPage> get() = pages.filter { it.isAccessible }

    fun page(number: Int): LibraryPage? = pages.getOrNull(number - 1)

    fun tile(levelId: Int): LevelTile? =
        page(LevelLibraryLayout.pageOf(levelId))?.tiles?.firstOrNull { it.levelId == levelId }

    /**
     * The page to open on: the one holding the lowest unlocked, unfinished level, or the last
     * accessible page when every unlocked level is finished. Page 1 when there are no levels.
     */
    fun initialPage(): Int {
        val next = pages.asSequence().flatMap { it.tiles }.firstOrNull { it.state == TileState.Unlocked }
        return next?.let { LevelLibraryLayout.pageOf(it.levelId) } ?: accessiblePages.lastOrNull()?.number ?: 1
    }
}

/** The grid is 6 columns by 4 rows: page `p` holds levels `(p-1)*24+1` to `p*24`. */
object LevelLibraryLayout {
    const val COLUMNS = 6
    const val ROWS = 4
    const val LEVELS_PER_PAGE = COLUMNS * ROWS
    const val MAX_STARS_PER_LEVEL = 3

    fun pageOf(levelId: Int): Int = (levelId - 1) / LEVELS_PER_PAGE + 1

    fun firstLevelOf(page: Int): Int = (page - 1) * LEVELS_PER_PAGE + 1

    fun pageCount(totalLevels: Int): Int = (totalLevels + LEVELS_PER_PAGE - 1) / LEVELS_PER_PAGE
}

/**
 * The unlock rules. Level 1 is always unlocked; level `n` unlocks once `n-1` is finished; the
 * first level of page `p+1` also unlocks once page `p` totals at least `{x}` stars. A finished
 * level is always unlocked.
 */
object LevelLibraryRules {
    /**
     * @param totalLevels the number of levels, ids `1..totalLevels`.
     * @param stars best stars per finished level id; a level missing from the map is unfinished.
     */
    fun compute(totalLevels: Int, stars: Map<Int, Int>, config: LevelLibraryConfig): LevelLibrary {
        val count = totalLevels.coerceAtLeast(0)
        val pageCount = LevelLibraryLayout.pageCount(count)
        fun starsOf(id: Int): Int = (stars[id] ?: 0).coerceIn(0, LevelLibraryLayout.MAX_STARS_PER_LEVEL)
        val pageStars = IntArray(pageCount) { index ->
            val first = LevelLibraryLayout.firstLevelOf(index + 1)
            (first..minOf(first + LevelLibraryLayout.LEVELS_PER_PAGE - 1, count)).sumOf(::starsOf)
        }

        fun isUnlocked(id: Int): Boolean {
            if (id == 1 || id in stars || (id - 1) in stars) return true
            val page = LevelLibraryLayout.pageOf(id)
            return page > 1 && id == LevelLibraryLayout.firstLevelOf(page) &&
                pageStars[page - 2] >= config.pageUnlockStars
        }

        val pages = (1..pageCount).map { number ->
            val first = LevelLibraryLayout.firstLevelOf(number)
            val last = minOf(first + LevelLibraryLayout.LEVELS_PER_PAGE - 1, count)
            val tiles = (first..last).map { id ->
                val state = when {
                    id in stars -> TileState.Completed(starsOf(id))
                    isUnlocked(id) -> TileState.Unlocked
                    else -> TileState.Locked
                }
                LevelTile(id, state)
            }
            LibraryPage(number, tiles, pageStars[number - 1])
        }
        return LevelLibrary(pages, config.pageUnlockStars)
    }
}
