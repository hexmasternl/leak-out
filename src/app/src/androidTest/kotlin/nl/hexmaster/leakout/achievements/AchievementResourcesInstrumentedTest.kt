package nl.hexmaster.leakout.achievements

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.hexmaster.leakout.core.achievements.Achievement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The packaged resources, as the device resolves them: Play Games ids, texts and icons for the whole catalogue. */
@RunWith(AndroidJUnit4::class)
class AchievementResourcesInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    /**
     * Not yet created in the Play Console: their id is empty until the next export (task 5.3 of
     * app-game-more-achievements, which empties this), and the sync skips them.
     */
    private val awaitingExport = setOf(
        "tips_addict", "long_way_round_2", "long_way_round_3", "big_spender_2", "crossroad_master", "efficiency_master",
        "highway", "snake", "eraser", "dramatic_failure", "on_your_nerves", "star_collector_3", "star_collector_4", "snail",
    )

    @Test
    fun everyKeyHasANonEmptyPlayGamesIdAndNoTwoShareOne() {
        val ids = Achievement.entries.filter { it.key !in awaitingExport }.map { achievement ->
            // By name, as the spec states it, and through the mapping the sync uses.
            @Suppress("DiscouragedApi")
            val byName = context.resources.getIdentifier("achievement_${achievement.key}", "string", context.packageName)
            assertNotEquals("achievement_${achievement.key}", 0, byName)
            assertEquals(byName, AchievementResources.playGamesId(achievement))
            context.getString(byName).also { assertTrue(achievement.key, it.isNotBlank()) }
        }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun anAchievementAwaitingTheExportResolvesToAnEmptyId() {
        for (key in awaitingExport) {
            val achievement = Achievement.fromKey(key) ?: error("$key is not in the catalogue")
            assertEquals(key, "", context.getString(AchievementResources.playGamesId(achievement)))
        }
    }

    @Test
    fun everyAchievementHasANameADescriptionAndAnIcon() {
        for (achievement in Achievement.entries) {
            assertTrue(context.getString(AchievementResources.name(achievement)).isNotBlank())
            assertTrue(context.getString(AchievementResources.description(achievement)).isNotBlank())
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(context.resources, AchievementResources.icon(achievement), options)
            assertEquals(achievement.key, 512, options.outWidth)
        }
        val hidden = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, AchievementResources.HIDDEN_ICON, hidden)
        assertEquals(512, hidden.outWidth)
    }

    @Test
    fun theAppIdIsAPositiveNumber() {
        val appId = context.getString(nl.hexmaster.leakout.R.string.app_id)
        assertTrue(appId, appId.toLong() > 0)
    }
}
