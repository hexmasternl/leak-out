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
 * @param isOpen page 1 always; a later page once the page before it is open and has earned the
 *   page unlock stars.
 */
data class LibraryPage(val number: Int, val tiles: List<LevelTile>, val earnedStars: Int, val isOpen: Boolean) {
    /** Only open pages can be shown, even when a closed page holds a finished level. */
    val isAccessible: Boolean get() = isOpen
}

/** The whole library, derived from the level count and the history; nothing is stored per page. */
data class LevelLibrary(val pages: List<LibraryPage>, val pageUnlockStars: Int) {
    val accessiblePages: List<LibraryPage> get() = pages.filter { it.isAccessible }

    fun page(number: Int): LibraryPage? = pages.getOrNull(number - 1)

    fun tile(levelId: Int): LevelTile? =
        page(LevelLibraryLayout.pageOf(levelId))?.tiles?.firstOrNull { it.levelId == levelId }

    /**
     * The page to open on: the page of the level after the highest finished level when that level
     * is playable, else the last open page; page 1 when nothing is finished. It follows the highest
     * finished level, as [NextLevelResolver] does, so a skipped level on an earlier page does not
     * pull the library back.
     */
    fun initialPage(): Int {
        val tiles = pages.flatMap { it.tiles }
        val highest = tiles.lastOrNull { it.state is TileState.Completed }?.levelId ?: return 1
        val next = tile((highest + 1).coerceAtMost(tiles.size))
        return if (next?.isPlayable == true) {
            LevelLibraryLayout.pageOf(next.levelId)
        } else {
            accessiblePages.lastOrNull()?.number ?: 1
        }
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
 * The unlock rules. Page 1 is always open; page `p` opens once page `p-1` is open and totals at
 * least `{x}` stars. On an open page a level is unlocked when it is finished, among the first
 * [LevelLibraryConfig.unlockWindow] levels of the page, or within that window after a finished
 * level. On a closed page only finished levels are playable, so the window never crosses into it.
 */
object LevelLibraryRules {
    /**
     * @param totalLevels the number of levels, ids `1..totalLevels`.
     * @param stars best stars per finished level id; a level missing from the map is unfinished.
     */
    fun compute(totalLevels: Int, stars: Map<Int, Int>, config: LevelLibraryConfig): LevelLibrary {
        val count = totalLevels.coerceAtLeast(0)
        val pageCount = LevelLibraryLayout.pageCount(count)
        val window = config.unlockWindow
        fun starsOf(id: Int): Int = (stars[id] ?: 0).coerceIn(0, LevelLibraryLayout.MAX_STARS_PER_LEVEL)
        fun levelsOf(page: Int): IntRange {
            val first = LevelLibraryLayout.firstLevelOf(page)
            return first..minOf(first + LevelLibraryLayout.LEVELS_PER_PAGE - 1, count)
        }
        val pageStars = IntArray(pageCount) { index -> levelsOf(index + 1).sumOf(::starsOf) }
        val pageOpen = BooleanArray(pageCount)
        for (index in 0 until pageCount) {
            pageOpen[index] = index == 0 || (pageOpen[index - 1] && pageStars[index - 1] >= config.pageUnlockStars)
        }

        fun isUnlocked(id: Int): Boolean {
            val page = LevelLibraryLayout.pageOf(id)
            if (!pageOpen[page - 1]) return false
            if (id < LevelLibraryLayout.firstLevelOf(page) + window) return true
            return (1..window).any { (id - it) in stars }
        }

        val pages = (1..pageCount).map { number ->
            val tiles = levelsOf(number).map { id ->
                val state = when {
                    id in stars -> TileState.Completed(starsOf(id))
                    isUnlocked(id) -> TileState.Unlocked
                    else -> TileState.Locked
                }
                LevelTile(id, state)
            }
            LibraryPage(number, tiles, pageStars[number - 1], pageOpen[number - 1])
        }
        return LevelLibrary(pages, config.pageUnlockStars)
    }
}
