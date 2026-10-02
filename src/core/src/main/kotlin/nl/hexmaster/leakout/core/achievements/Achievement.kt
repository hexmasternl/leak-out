package nl.hexmaster.leakout.core.achievements

/** Where an achievement's condition comes from, and how it is grouped on the achievements screen. */
enum class AchievementGroup {
    /** From the level history alone. */
    PROGRESS,

    /** From one attempt. */
    SKILL,

    /** From the player stats that last across attempts. */
    LIFETIME,

    /** Not shown until unlocked. */
    HIDDEN,
}

/** What an incremental achievement counts. Every measure is an absolute count, never a share of the library. */
enum class Measure {
    /** Levels with a history record, any number of stars. */
    LEVELS_FINISHED,

    /** The sum of the stored stars of every level. */
    STARS,

    /** Levels stored with 3 stars. */
    THREE_STAR_LEVELS,

    /** Levels of library page [Achievement.pack] with a history record. */
    PACK_LEVELS_FINISHED,

    /** Levels of library page [Achievement.pack] stored with 3 stars. */
    PACK_THREE_STAR_LEVELS,

    /** Sections ever placed, replacements and abandoned attempts included. */
    SECTIONS_PLACED,
}

/** Standard achievements unlock at once; incremental ones count [Incremental.steps] of a [Incremental.measure]. */
sealed interface AchievementType {
    data object Standard : AchievementType

    data class Incremental(val steps: Int, val measure: Measure) : AchievementType
}

/**
 * The achievement catalogue. Play Games can neither delete a published achievement nor change its
 * steps, so entries are only ever added: never remove one or change its [key], steps, [hidden] or
 * [xp]. `AchievementCatalogueTest` pins them. Names and descriptions are string resources in the
 * app, so they can be translated; [key] is also the `achievement_<key>` resource with the Play
 * Games id.
 *
 * @param pack the library page of a `pack_<n>_*` achievement, null for every other one.
 */
enum class Achievement(
    val key: String,
    val group: AchievementGroup,
    val type: AchievementType,
    val xp: Int,
    val hidden: Boolean = false,
    val pack: Int? = null,
) {
    FIRST_LEAK_FIXED("first_leak_fixed", AchievementGroup.PROGRESS, AchievementType.Standard, xp = 10),
    LEVELS_FINISHED_1("levels_finished_1", AchievementGroup.PROGRESS, incremental(10, Measure.LEVELS_FINISHED), xp = 15),
    STAR_COLLECTOR_1("star_collector_1", AchievementGroup.PROGRESS, incremental(25, Measure.STARS), xp = 15),
    STAR_COLLECTOR_2("star_collector_2", AchievementGroup.PROGRESS, incremental(60, Measure.STARS), xp = 30),
    PERFECTIONIST_1("perfectionist_1", AchievementGroup.PROGRESS, incremental(10, Measure.THREE_STAR_LEVELS), xp = 30),
    PACK_1_COMPLETE("pack_1_complete", AchievementGroup.PROGRESS, incremental(24, Measure.PACK_LEVELS_FINISHED), xp = 30, pack = 1),
    PACK_1_PERFECT("pack_1_perfect", AchievementGroup.PROGRESS, incremental(24, Measure.PACK_THREE_STAR_LEVELS), xp = 40, pack = 1),

    ZERO_WASTE("zero_waste", AchievementGroup.SKILL, AchievementType.Standard, xp = 30),
    DOUBLE_CROSS("double_cross", AchievementGroup.SKILL, AchievementType.Standard, xp = 20),
    CROSSROADS_KING("crossroads_king", AchievementGroup.SKILL, AchievementType.Standard, xp = 40),
    AHEAD_OF_THE_FLOW("ahead_of_the_flow", AchievementGroup.SKILL, AchievementType.Standard, xp = 30),
    FULL_THROTTLE("full_throttle", AchievementGroup.SKILL, AchievementType.Standard, xp = 15),
    MEASURE_TWICE("measure_twice", AchievementGroup.SKILL, AchievementType.Standard, xp = 20),
    STEADY_HANDS("steady_hands", AchievementGroup.SKILL, AchievementType.Standard, xp = 15),
    LONG_WAY_ROUND("long_way_round", AchievementGroup.SKILL, AchievementType.Standard, xp = 30),
    BIG_SPENDER("big_spender", AchievementGroup.SKILL, AchievementType.Standard, xp = 30),

    APPRENTICE("apprentice", AchievementGroup.LIFETIME, incremental(100, Measure.SECTIONS_PLACED), xp = 10),
    JOURNEYMAN("journeyman", AchievementGroup.LIFETIME, incremental(1_000, Measure.SECTIONS_PLACED), xp = 25),
    GUILDMASTER("guildmaster", AchievementGroup.LIFETIME, incremental(5_000, Measure.SECTIONS_PLACED), xp = 40),
    NEVER_GIVE_UP("never_give_up", AchievementGroup.LIFETIME, AchievementType.Standard, xp = 20),
    PERSONAL_BEST("personal_best", AchievementGroup.LIFETIME, AchievementType.Standard, xp = 15),
    WET_FEET("wet_feet", AchievementGroup.LIFETIME, AchievementType.Standard, xp = 5),

    INSTANT_FLOOD("instant_flood", AchievementGroup.HIDDEN, AchievementType.Standard, xp = 20, hidden = true),
    WALL_TO_WALL("wall_to_wall", AchievementGroup.HIDDEN, AchievementType.Standard, xp = 30, hidden = true),
    PHOTO_FINISH("photo_finish", AchievementGroup.HIDDEN, AchievementType.Standard, xp = 30, hidden = true),
    ;

    /** The number of steps of an incremental achievement, null for a standard one. */
    val steps: Int? get() = (type as? AchievementType.Incremental)?.steps

    val isIncremental: Boolean get() = type is AchievementType.Incremental

    companion object {
        /** The most XP Play Games allows a game's achievements to add up to. */
        const val MAX_TOTAL_XP = 1_000

        /** Play Games' limits on the steps of an incremental achievement. */
        val STEPS_RANGE = 2..10_000

        val totalXp: Int get() = entries.sumOf { it.xp }

        /** The achievement stored under [key], or null for a key this version does not know. */
        fun fromKey(key: String): Achievement? = entries.firstOrNull { it.key == key }
    }
}

private fun incremental(steps: Int, measure: Measure) = AchievementType.Incremental(steps, measure)
