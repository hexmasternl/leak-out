package nl.hexmaster.leakout.core.library

/**
 * Tuning for the level library.
 *
 * @param pageUnlockStars `{x}`: the stars a page must total before the next page opens. Between 0
 *   and the 72 stars a full page can hold.
 * @param unlockWindow how many levels after a finished level can be started, and how many levels
 *   at the start of an open page are playable without finishing anything. 1 is strictly sequential.
 */
data class LevelLibraryConfig(
    val pageUnlockStars: Int = DEFAULT_PAGE_UNLOCK_STARS,
    val unlockWindow: Int = DEFAULT_UNLOCK_WINDOW,
) {
    init {
        require(pageUnlockStars in 0..MAX_PAGE_STARS) {
            "pageUnlockStars must be in 0..$MAX_PAGE_STARS, was $pageUnlockStars"
        }
        require(unlockWindow >= 1) { "unlockWindow must be at least 1, was $unlockWindow" }
    }

    companion object {
        /** 50 of the 72 stars a full page can hold. The one place to change `{x}`. */
        const val DEFAULT_PAGE_UNLOCK_STARS = 50

        /** Lets the player skip at most two levels in a row. */
        const val DEFAULT_UNLOCK_WINDOW = 3

        const val MAX_PAGE_STARS = LevelLibraryLayout.LEVELS_PER_PAGE * LevelLibraryLayout.MAX_STARS_PER_LEVEL

        val Default = LevelLibraryConfig()
    }
}
