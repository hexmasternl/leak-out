package nl.hexmaster.leakout

import android.content.pm.ActivityInfo
import java.time.LocalDate
import nl.hexmaster.leakout.scores.online.DailyStreak
import nl.hexmaster.leakout.core.level.LevelParser
import nl.hexmaster.leakout.core.level.ParseResult
import nl.hexmaster.leakout.ui.daily.DailyPipeGame
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenOrientationTest {

    private val level = (LevelParser.parse(LEVEL_JSON) as ParseResult.Success).level

    @Test
    fun `the welcome screen follows the device`() {
        assertEquals(ScreenOrientation.FollowDevice, orientationFor(Screen.Welcome))
    }

    @Test
    fun `every other screen is landscape`() {
        val screens = listOf(
            Screen.Library,
            Screen.Settings,
            Screen.Achievements,
            Screen.Designer,
            Screen.Game(level),
            Screen.TestPlay(level),
            Screen.DailyPipe(DailyPipeGame(LocalDate.of(2026, 11, 17), level, 1L, DailyStreak(0, 0, wonToday = false))),
        )
        for (screen in screens) {
            assertEquals(screen.toString(), ScreenOrientation.Landscape, orientationFor(screen))
        }
    }

    @Test
    fun `follow device respects the rotation lock and landscape follows the sensor`() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_FULL_USER, ScreenOrientation.FollowDevice.activityInfo)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, ScreenOrientation.Landscape.activityInfo)
    }

    private companion object {
        const val LEVEL_JSON =
            """{"id":1,"name":"Orientation","grid":{"columns":6,"rows":5},"start":{"x":0,"y":0,"openSide":"RIGHT"},""" +
                """"finish":{"x":5,"y":4,"openSide":"LEFT"},"blocked":[],"flow":{"initialDelayMs":20000,"cellsPerSecond":0.3},""" +
                """"stars":{"one":80,"two":120,"three":160}}"""
    }
}
