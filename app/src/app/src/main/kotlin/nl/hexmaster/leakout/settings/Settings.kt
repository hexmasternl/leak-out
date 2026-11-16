package nl.hexmaster.leakout.settings

/**
 * The player's preferences. The defaults are what a first launch reads, so nothing has to be
 * written before the settings are used. Volumes are 0.0-1.0; repositories keep them in range with
 * [clampVolume] on write and on read.
 */
data class Settings(
    val soundEnabled: Boolean = DEFAULT_SOUND_ENABLED,
    val soundVolume: Float = DEFAULT_SOUND_VOLUME,
    val musicEnabled: Boolean = DEFAULT_MUSIC_ENABLED,
    val musicVolume: Float = DEFAULT_MUSIC_VOLUME,
    val vibrationEnabled: Boolean = DEFAULT_VIBRATION_ENABLED,
    val remindersEnabled: Boolean = DEFAULT_REMINDERS_ENABLED,
) {
    companion object {
        const val DEFAULT_SOUND_ENABLED = true
        const val DEFAULT_SOUND_VOLUME = 0.8f
        const val DEFAULT_MUSIC_ENABLED = true
        const val DEFAULT_MUSIC_VOLUME = 0.6f
        const val DEFAULT_VIBRATION_ENABLED = true
        const val DEFAULT_REMINDERS_ENABLED = true

        const val MIN_VOLUME = 0f
        const val MAX_VOLUME = 1f

        /** The defaults, as read on first launch. */
        val Default = Settings()

        /**
         * [value] clamped to [MIN_VOLUME]..[MAX_VOLUME]. NaN cannot be ordered, so it reads as
         * silent rather than leaking into a slider.
         */
        fun clampVolume(value: Float): Float = if (value.isNaN()) MIN_VOLUME else value.coerceIn(MIN_VOLUME, MAX_VOLUME)
    }
}
