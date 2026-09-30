package nl.hexmaster.leakout.core.library

/**
 * Tuning for the level library.
 *
 * @param pageUnlockStars `{x}`: the stars a page must total before the first level of the next
 *   page unlocks. Its final value is not decided yet; change [DEFAULT_PAGE_UNLOCK_STARS], nothing else.
 */
data class LevelLibraryConfig(val pageUnlockStars: Int = DEFAULT_PAGE_UNLOCK_STARS) {
    init {
        require(pageUnlockStars >= 0) { "pageUnlockStars must not be negative, was $pageUnlockStars" }
    }

    companion object {
        /** Provisional: half of the 72 stars a full page can hold. The one place to change `{x}`. */
        const val DEFAULT_PAGE_UNLOCK_STARS = 36

        val Default = LevelLibraryConfig()
    }
}
