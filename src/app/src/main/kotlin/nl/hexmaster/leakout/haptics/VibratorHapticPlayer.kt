package nl.hexmaster.leakout.haptics

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Vibrates through the device's default [Vibrator]. A device without one stays silent. The strength
 * only applies with amplitude control; without it a soft vibration is just short. A missing
 * permission is logged and ignored, so it cannot crash the game.
 */
class VibratorHapticPlayer(context: Context) : HapticPlayer {
    private val vibrator: Vibrator? = defaultVibrator(context)?.takeIf { it.hasVibrator() }

    /** Whether the device has a vibrator this player uses. */
    val hasVibrator: Boolean get() = vibrator != null

    override fun vibrate(durationMs: Long, amplitude: Int) {
        val vibrator = vibrator ?: return
        val strength = if (vibrator.hasAmplitudeControl()) amplitude.coerceIn(1, 255) else VibrationEffect.DEFAULT_AMPLITUDE
        val effect = VibrationEffect.createOneShot(durationMs, strength)
        guarded {
            vibrator.cancel()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_MEDIA))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build())
            }
        }
    }

    override fun cancel() {
        val vibrator = vibrator ?: return
        guarded { vibrator.cancel() }
    }

    private inline fun guarded(block: () -> Unit) {
        try {
            block()
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot vibrate", e)
        }
    }

    private companion object {
        const val TAG = "Haptics"

        fun defaultVibrator(context: Context): Vibrator? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
    }
}
