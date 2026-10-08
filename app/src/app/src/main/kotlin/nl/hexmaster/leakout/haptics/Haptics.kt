package nl.hexmaster.leakout.haptics

import nl.hexmaster.leakout.settings.Settings

/** A vibration the game asks for, with its length and its strength on Android's 1-255 scale. */
enum class HapticEffect(val durationMs: Long, val amplitude: Int) {
    /** A placed section destroyed by a replacement: short and soft. */
    SectionReplaced(30, 60),

    /** The score bar of the gap warning switching to "Warning": brief, medium strength. */
    GapWarning(50, 140),

    /** The attempt lost: one second at full strength. */
    LevelLost(1_000, 255),
}

/** Vibrates the device. The seam between [Haptics] and the system vibrator. */
interface HapticPlayer {
    /** Vibrates once for [durationMs] at [amplitude], 1-255, replacing a running vibration. */
    fun vibrate(durationMs: Long, amplitude: Int)

    /** Stops a running vibration. Safe to call when nothing vibrates. */
    fun cancel()
}

/**
 * Decides whether an effect vibrates: nothing while vibration is off or without a [player], and
 * otherwise the effect's duration and strength. [apply] keeps the setting current, and turning
 * vibration off stops a running vibration at once.
 */
class Haptics(private val player: HapticPlayer?) {
    private var enabled = Settings.DEFAULT_VIBRATION_ENABLED

    /** Takes over the stored vibration setting. */
    fun apply(settings: Settings) {
        val wasEnabled = enabled
        enabled = settings.vibrationEnabled
        if (wasEnabled && !enabled) player?.cancel()
    }

    fun play(effect: HapticEffect) {
        if (!enabled) return
        player?.vibrate(effect.durationMs, effect.amplitude)
    }

    /** Stops a running vibration, such as when the game leaves the screen or the foreground. */
    fun stop() {
        player?.cancel()
    }
}
