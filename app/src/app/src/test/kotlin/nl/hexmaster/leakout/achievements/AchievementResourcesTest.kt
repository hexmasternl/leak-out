package nl.hexmaster.leakout.achievements

import java.io.File
import javax.imageio.ImageIO
import javax.xml.parsers.DocumentBuilderFactory
import nl.hexmaster.leakout.core.achievements.Achievement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * Every catalogue entry has its name, description, icon and Play Games id, and the English texts
 * match the Play Console entries exactly (they are shown side by side once signed in).
 */
class AchievementResourcesTest {
    /** The Play Console texts, by key. A new achievement adds its row here together with the console entry. */
    private val console = mapOf(
        "first_leak_fixed" to ("First Leak Fixed" to "Finish level 1."),
        "levels_finished_1" to ("Getting the Hang of It" to "Finish 10 levels."),
        "star_collector_1" to ("Star Collector I" to "Collect 25 stars."),
        "star_collector_2" to ("Star Collector II" to "Collect 60 stars."),
        "perfectionist_1" to ("Perfectionist I" to "Get 3 stars on 10 levels."),
        "pack_1_complete" to ("Pack 1 Complete" to "Finish every level on page 1."),
        "pack_1_perfect" to ("Pack 1 Perfect" to "Get 3 stars on every level on page 1."),
        "zero_waste" to ("Zero Waste" to "Win a level with no unused sections."),
        "double_cross" to ("Double Cross" to "Send the liquid through both channels of a cross."),
        "crossroads_king" to ("Crossroads King" to "Send the liquid through both channels of 3 crosses in one attempt."),
        "ahead_of_the_flow" to ("Ahead of the Flow" to "Complete the route before the liquid starts flowing."),
        "full_throttle" to ("Full Throttle" to "Win a level with fast forward on."),
        "measure_twice" to ("Measure Twice" to "Win a level without replacing a section."),
        "steady_hands" to ("Steady Hands" to "Win a level without pausing or leaving the game."),
        "long_way_round" to ("The Long Way Round" to "Win with the liquid flowing through 30 or more sections."),
        "big_spender" to ("Big Spender" to "Win a level with a score of 500 or more."),
        "apprentice" to ("Apprentice" to "Place 100 sections."),
        "journeyman" to ("Journeyman" to "Place 1,000 sections."),
        "guildmaster" to ("Guildmaster" to "Place 5,000 sections."),
        "never_give_up" to ("Never Give Up" to "Win a level after losing it 3 times in a row."),
        "personal_best" to ("Personal Best" to "Beat your own highscore on a level."),
        "wet_feet" to ("Wet Feet" to "Lose a level for the first time."),
        "instant_flood" to ("Instant Flood" to "Lose before the liquid reaches any section you placed."),
        "wall_to_wall" to ("Wall to Wall" to "Win with every free cell filled."),
        "photo_finish" to ("Photo Finish" to "Complete the route on the very tile the liquid was about to enter."),
        "tips_addict" to ("Tips Addict" to "Reset the tips on the settings screen."),
        "long_way_round_2" to ("The Long Way Round II" to "Win with the liquid flowing through 50 or more sections."),
        "long_way_round_3" to ("The Long Way Round III" to "Win with the liquid flowing through 100 or more sections."),
        "big_spender_2" to ("Big Spender II" to "Win a level with a score of 1,000 or more."),
        "crossroad_master" to ("Crossroad Master" to "Win with the liquid running through 3 crosses in a row."),
        "efficiency_master" to ("Efficiency Master" to "Win with no more sections on the board than the shortest route needs."),
        "highway" to ("Highway" to "Win with the liquid running straight through 7 straight sections in a row."),
        "snake" to ("Snake" to "Win with the liquid running through 12 corners in a row."),
        "eraser" to ("Eraser" to "Win a level after replacing 20 or more sections."),
        "dramatic_failure" to ("Dramatic Failure" to "Lose a level after the liquid flowed through more than 50 sections."),
        "on_your_nerves" to ("On Your Nerves" to "Tap a tile 5 times while its section is being replaced."),
        "star_collector_3" to ("Star Collector III" to "Collect 100 stars."),
        "star_collector_4" to ("Star Collector IV" to "Collect 250 stars."),
        "snail" to ("Snail" to "Win with the liquid flowing through 3 or more reservoirs."),
    )

    /** The `<string>` resources of [path], by name. */
    private fun strings(path: String): Map<String, String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(path))
        val nodes = document.getElementsByTagName("string")
        return (0 until nodes.length).map { nodes.item(it) as Element }.associate { it.getAttribute("name") to it.textContent }
    }

    private val strings = strings("src/main/res/values/strings.xml")
    private val gamesIds = strings("src/main/res/values/games-ids.xml")

    @Test
    fun `the console texts cover exactly the catalogue`() {
        assertEquals(Achievement.entries.map { it.key }.toSet(), console.keys)
    }

    @Test
    fun `every achievement has its name and description, as in the Play Console`() {
        for (achievement in Achievement.entries) {
            val (name, description) = console.getValue(achievement.key)
            assertEquals(achievement.key, name, strings["achievement_name_${achievement.key}"])
            assertEquals(achievement.key, description, strings["achievement_desc_${achievement.key}"])
        }
    }

    /**
     * Achievements in the catalogue but not yet created in the Play Console: their id is empty until
     * the next export, and the sync skips them. Empty while every achievement has been exported.
     */
    private val awaitingExport = emptySet<String>()

    @Test
    fun `every achievement has a Play Games id and no two share one`() {
        val published = Achievement.entries.filter { it.key !in awaitingExport }
        val ids = published.map { gamesIds["achievement_${it.key}"] }
        assertTrue(ids.toString(), ids.all { !it.isNullOrBlank() })
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `an achievement awaiting the export has an empty id, never a made-up one`() {
        for (key in awaitingExport) {
            assertTrue("$key is not in the catalogue", Achievement.fromKey(key) != null)
            assertEquals(key, "", gamesIds["achievement_$key"])
        }
    }

    @Test
    fun `the shipped games-ids has the real app id, so Play Games is on`() {
        assertEquals("201820010117", gamesIds["app_id"])
        assertEquals("nl.hexmaster.leakout", gamesIds["package_name"])
    }

    @Test
    fun `every achievement and the hidden placeholder have a 512 pixel icon`() {
        for (name in Achievement.entries.map { "achievement_${it.key}" } + "achievement_hidden") {
            val file = File("src/main/res/drawable-nodpi/$name.png")
            assertTrue("$file exists", file.isFile)
            val image = ImageIO.read(file)
            assertEquals(name, 512, image.width)
            assertEquals(name, 512, image.height)
        }
    }

    @Test
    fun `the mapping gives every achievement its own resources`() {
        val all = Achievement.entries.map(AchievementResources::of)
        for (part in listOf<(AchievementRes) -> Int>({ it.name }, { it.description }, { it.icon }, { it.playGamesId })) {
            val ids = all.map(part)
            assertTrue(ids.all { it != 0 })
            assertEquals(ids.size, ids.toSet().size)
        }
        assertTrue(AchievementResources.HIDDEN_ICON !in all.map { it.icon })
        for (achievement in Achievement.entries) {
            assertEquals(AchievementResources.of(achievement).name, AchievementResources.name(achievement))
            assertEquals(AchievementResources.of(achievement).description, AchievementResources.description(achievement))
            assertEquals(AchievementResources.of(achievement).icon, AchievementResources.icon(achievement))
            assertEquals(AchievementResources.of(achievement).playGamesId, AchievementResources.playGamesId(achievement))
        }
    }
}
