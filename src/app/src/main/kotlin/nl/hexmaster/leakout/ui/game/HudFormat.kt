package nl.hexmaster.leakout.ui.game

import java.text.NumberFormat
import java.util.Locale

/**
 * Text for the HUD values, kept free of Android so it can be unit tested. The labels around the
 * values ("Level %1$s", "%1$s/s", "Score %1$s") come from string resources.
 */
object HudFormat {
    /** The level number shown after "Level". */
    fun level(id: Int): String = id.toString()

    /** Flow speed in cells per second, always one decimal with the locale's decimal sign. */
    fun speed(cellsPerSecond: Double, locale: Locale): String =
        String.format(locale, "%.1f", cellsPerSecond)

    /** Score with the locale's digit grouping. */
    fun score(value: Int, locale: Locale): String =
        NumberFormat.getIntegerInstance(locale).format(value)
}
