package nl.hexmaster.leakout.achievements

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.core.achievements.Achievement

/**
 * The Android resources of one achievement.
 *
 * @param name the name, exactly as in the Play Console.
 * @param description the description, exactly as in the Play Console.
 * @param icon the colour icon (512 × 512, `drawable-nodpi`).
 * @param playGamesId the Play Games id from `games-ids.xml`.
 */
data class AchievementRes(
    @StringRes val name: Int,
    @StringRes val description: Int,
    @DrawableRes val icon: Int,
    @StringRes val playGamesId: Int,
)

/**
 * Maps the core catalogue to the app's resources. An exhaustive `when` rather than a lookup by
 * name, so a new catalogue entry without its strings, icon or id does not compile.
 */
object AchievementResources {
    /** The icon shown for a hidden achievement while it is locked. */
    @DrawableRes
    val HIDDEN_ICON: Int = R.drawable.achievement_hidden

    @StringRes
    fun name(achievement: Achievement): Int = of(achievement).name

    @StringRes
    fun description(achievement: Achievement): Int = of(achievement).description

    @DrawableRes
    fun icon(achievement: Achievement): Int = of(achievement).icon

    @StringRes
    fun playGamesId(achievement: Achievement): Int = of(achievement).playGamesId

    fun of(achievement: Achievement): AchievementRes = when (achievement) {
        Achievement.FIRST_LEAK_FIXED -> AchievementRes(
            R.string.achievement_name_first_leak_fixed,
            R.string.achievement_desc_first_leak_fixed,
            R.drawable.achievement_first_leak_fixed,
            R.string.achievement_first_leak_fixed,
        )
        Achievement.LEVELS_FINISHED_1 -> AchievementRes(
            R.string.achievement_name_levels_finished_1,
            R.string.achievement_desc_levels_finished_1,
            R.drawable.achievement_levels_finished_1,
            R.string.achievement_levels_finished_1,
        )
        Achievement.STAR_COLLECTOR_1 -> AchievementRes(
            R.string.achievement_name_star_collector_1,
            R.string.achievement_desc_star_collector_1,
            R.drawable.achievement_star_collector_1,
            R.string.achievement_star_collector_1,
        )
        Achievement.STAR_COLLECTOR_2 -> AchievementRes(
            R.string.achievement_name_star_collector_2,
            R.string.achievement_desc_star_collector_2,
            R.drawable.achievement_star_collector_2,
            R.string.achievement_star_collector_2,
        )
        Achievement.PERFECTIONIST_1 -> AchievementRes(
            R.string.achievement_name_perfectionist_1,
            R.string.achievement_desc_perfectionist_1,
            R.drawable.achievement_perfectionist_1,
            R.string.achievement_perfectionist_1,
        )
        Achievement.PACK_1_COMPLETE -> AchievementRes(
            R.string.achievement_name_pack_1_complete,
            R.string.achievement_desc_pack_1_complete,
            R.drawable.achievement_pack_1_complete,
            R.string.achievement_pack_1_complete,
        )
        Achievement.PACK_1_PERFECT -> AchievementRes(
            R.string.achievement_name_pack_1_perfect,
            R.string.achievement_desc_pack_1_perfect,
            R.drawable.achievement_pack_1_perfect,
            R.string.achievement_pack_1_perfect,
        )
        Achievement.ZERO_WASTE -> AchievementRes(
            R.string.achievement_name_zero_waste,
            R.string.achievement_desc_zero_waste,
            R.drawable.achievement_zero_waste,
            R.string.achievement_zero_waste,
        )
        Achievement.DOUBLE_CROSS -> AchievementRes(
            R.string.achievement_name_double_cross,
            R.string.achievement_desc_double_cross,
            R.drawable.achievement_double_cross,
            R.string.achievement_double_cross,
        )
        Achievement.CROSSROADS_KING -> AchievementRes(
            R.string.achievement_name_crossroads_king,
            R.string.achievement_desc_crossroads_king,
            R.drawable.achievement_crossroads_king,
            R.string.achievement_crossroads_king,
        )
        Achievement.AHEAD_OF_THE_FLOW -> AchievementRes(
            R.string.achievement_name_ahead_of_the_flow,
            R.string.achievement_desc_ahead_of_the_flow,
            R.drawable.achievement_ahead_of_the_flow,
            R.string.achievement_ahead_of_the_flow,
        )
        Achievement.FULL_THROTTLE -> AchievementRes(
            R.string.achievement_name_full_throttle,
            R.string.achievement_desc_full_throttle,
            R.drawable.achievement_full_throttle,
            R.string.achievement_full_throttle,
        )
        Achievement.MEASURE_TWICE -> AchievementRes(
            R.string.achievement_name_measure_twice,
            R.string.achievement_desc_measure_twice,
            R.drawable.achievement_measure_twice,
            R.string.achievement_measure_twice,
        )
        Achievement.STEADY_HANDS -> AchievementRes(
            R.string.achievement_name_steady_hands,
            R.string.achievement_desc_steady_hands,
            R.drawable.achievement_steady_hands,
            R.string.achievement_steady_hands,
        )
        Achievement.LONG_WAY_ROUND -> AchievementRes(
            R.string.achievement_name_long_way_round,
            R.string.achievement_desc_long_way_round,
            R.drawable.achievement_long_way_round,
            R.string.achievement_long_way_round,
        )
        Achievement.BIG_SPENDER -> AchievementRes(
            R.string.achievement_name_big_spender,
            R.string.achievement_desc_big_spender,
            R.drawable.achievement_big_spender,
            R.string.achievement_big_spender,
        )
        Achievement.APPRENTICE -> AchievementRes(
            R.string.achievement_name_apprentice,
            R.string.achievement_desc_apprentice,
            R.drawable.achievement_apprentice,
            R.string.achievement_apprentice,
        )
        Achievement.JOURNEYMAN -> AchievementRes(
            R.string.achievement_name_journeyman,
            R.string.achievement_desc_journeyman,
            R.drawable.achievement_journeyman,
            R.string.achievement_journeyman,
        )
        Achievement.GUILDMASTER -> AchievementRes(
            R.string.achievement_name_guildmaster,
            R.string.achievement_desc_guildmaster,
            R.drawable.achievement_guildmaster,
            R.string.achievement_guildmaster,
        )
        Achievement.NEVER_GIVE_UP -> AchievementRes(
            R.string.achievement_name_never_give_up,
            R.string.achievement_desc_never_give_up,
            R.drawable.achievement_never_give_up,
            R.string.achievement_never_give_up,
        )
        Achievement.PERSONAL_BEST -> AchievementRes(
            R.string.achievement_name_personal_best,
            R.string.achievement_desc_personal_best,
            R.drawable.achievement_personal_best,
            R.string.achievement_personal_best,
        )
        Achievement.WET_FEET -> AchievementRes(
            R.string.achievement_name_wet_feet,
            R.string.achievement_desc_wet_feet,
            R.drawable.achievement_wet_feet,
            R.string.achievement_wet_feet,
        )
        Achievement.INSTANT_FLOOD -> AchievementRes(
            R.string.achievement_name_instant_flood,
            R.string.achievement_desc_instant_flood,
            R.drawable.achievement_instant_flood,
            R.string.achievement_instant_flood,
        )
        Achievement.WALL_TO_WALL -> AchievementRes(
            R.string.achievement_name_wall_to_wall,
            R.string.achievement_desc_wall_to_wall,
            R.drawable.achievement_wall_to_wall,
            R.string.achievement_wall_to_wall,
        )
        Achievement.PHOTO_FINISH -> AchievementRes(
            R.string.achievement_name_photo_finish,
            R.string.achievement_desc_photo_finish,
            R.drawable.achievement_photo_finish,
            R.string.achievement_photo_finish,
        )
        Achievement.TIPS_ADDICT -> AchievementRes(
            R.string.achievement_name_tips_addict,
            R.string.achievement_desc_tips_addict,
            R.drawable.achievement_tips_addict,
            R.string.achievement_tips_addict,
        )
        Achievement.LONG_WAY_ROUND_2 -> AchievementRes(
            R.string.achievement_name_long_way_round_2,
            R.string.achievement_desc_long_way_round_2,
            R.drawable.achievement_long_way_round_2,
            R.string.achievement_long_way_round_2,
        )
        Achievement.LONG_WAY_ROUND_3 -> AchievementRes(
            R.string.achievement_name_long_way_round_3,
            R.string.achievement_desc_long_way_round_3,
            R.drawable.achievement_long_way_round_3,
            R.string.achievement_long_way_round_3,
        )
        Achievement.BIG_SPENDER_2 -> AchievementRes(
            R.string.achievement_name_big_spender_2,
            R.string.achievement_desc_big_spender_2,
            R.drawable.achievement_big_spender_2,
            R.string.achievement_big_spender_2,
        )
        Achievement.CROSSROAD_MASTER -> AchievementRes(
            R.string.achievement_name_crossroad_master,
            R.string.achievement_desc_crossroad_master,
            R.drawable.achievement_crossroad_master,
            R.string.achievement_crossroad_master,
        )
        Achievement.EFFICIENCY_MASTER -> AchievementRes(
            R.string.achievement_name_efficiency_master,
            R.string.achievement_desc_efficiency_master,
            R.drawable.achievement_efficiency_master,
            R.string.achievement_efficiency_master,
        )
        Achievement.HIGHWAY -> AchievementRes(
            R.string.achievement_name_highway,
            R.string.achievement_desc_highway,
            R.drawable.achievement_highway,
            R.string.achievement_highway,
        )
        Achievement.SNAKE -> AchievementRes(
            R.string.achievement_name_snake,
            R.string.achievement_desc_snake,
            R.drawable.achievement_snake,
            R.string.achievement_snake,
        )
        Achievement.ERASER -> AchievementRes(
            R.string.achievement_name_eraser,
            R.string.achievement_desc_eraser,
            R.drawable.achievement_eraser,
            R.string.achievement_eraser,
        )
        Achievement.DRAMATIC_FAILURE -> AchievementRes(
            R.string.achievement_name_dramatic_failure,
            R.string.achievement_desc_dramatic_failure,
            R.drawable.achievement_dramatic_failure,
            R.string.achievement_dramatic_failure,
        )
        Achievement.ON_YOUR_NERVES -> AchievementRes(
            R.string.achievement_name_on_your_nerves,
            R.string.achievement_desc_on_your_nerves,
            R.drawable.achievement_on_your_nerves,
            R.string.achievement_on_your_nerves,
        )
        Achievement.STAR_COLLECTOR_3 -> AchievementRes(
            R.string.achievement_name_star_collector_3,
            R.string.achievement_desc_star_collector_3,
            R.drawable.achievement_star_collector_3,
            R.string.achievement_star_collector_3,
        )
        Achievement.STAR_COLLECTOR_4 -> AchievementRes(
            R.string.achievement_name_star_collector_4,
            R.string.achievement_desc_star_collector_4,
            R.drawable.achievement_star_collector_4,
            R.string.achievement_star_collector_4,
        )
        Achievement.SNAIL -> AchievementRes(
            R.string.achievement_name_snail,
            R.string.achievement_desc_snail,
            R.drawable.achievement_snail,
            R.string.achievement_snail,
        )
    }
}
