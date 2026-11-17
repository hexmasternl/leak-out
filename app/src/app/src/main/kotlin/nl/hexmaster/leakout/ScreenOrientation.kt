package nl.hexmaster.leakout

import android.content.pm.ActivityInfo

/**
 * How the activity may be oriented while a screen is shown (openspec/specs/landscape-orientation).
 * [activityInfo] is the value for `Activity.requestedOrientation`.
 */
internal enum class ScreenOrientation(val activityInfo: Int) {
    /** Portrait or landscape, as the device is held; the user's rotation lock is respected. */
    FollowDevice(ActivityInfo.SCREEN_ORIENTATION_FULL_USER),

    /** Landscape only, flipping between both landscape sides with the sensor. */
    Landscape(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE),
}

/**
 * The main menu (and the splash before it, which is shown while the screen is still the welcome
 * screen) follows the device; every other screen, a level above all, is played in landscape.
 *
 * The session-start ad is shown while the menu screen is still current, so it follows that screen's
 * orientation; the landscape lock only applies once the level replaces it.
 */
internal fun orientationFor(screen: Screen): ScreenOrientation = when (screen) {
    Screen.Welcome -> ScreenOrientation.FollowDevice
    Screen.Library,
    Screen.Settings,
    Screen.Achievements,
    Screen.Designer,
    is Screen.Game,
    is Screen.TestPlay,
    is Screen.DailyPipe,
    -> ScreenOrientation.Landscape
}
