package nl.hexmaster.leakout.ui.game

import java.text.NumberFormat
import java.util.Locale

/**
 * Text for the HUD values, kept free of Android so it can be unit tested. The labels around the
 * values ("Level %1$s", "Flow speed %1$s", "Score %1$s") come from string resources.
 */
object HudFormat {
    /** The level number shown after "Level". */
    fun level(id: Int): String = id.toString()

    /** Flow speed as the whole number of [HudModel.speedNumber], in the locale's digits. */
    fun speed(cellsPerSecond: Double, locale: Locale): String =
        NumberFormat.getIntegerInstance(locale).format(HudModel.speedNumber(cellsPerSecond))

    /** Score with the locale's digit grouping. */
    fun score(value: Int, locale: Locale): String =
        NumberFormat.getIntegerInstance(locale).format(value)
}
