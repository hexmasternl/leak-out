package nl.hexmaster.leakout.haptics

/**
 * Vibrates nothing and remembers every [vibrate], for tests and previews, like
 * `RecordingSoundEffectPlayer`. [vibrations] lists them in order; [cancels] counts the cancels.
 */
class RecordingHapticPlayer : HapticPlayer {
    data class Vibration(val durationMs: Long, val amplitude: Int)

    val vibrations = mutableListOf<Vibration>()

    var cancels = 0
        private set

    override fun vibrate(durationMs: Long, amplitude: Int) {
        vibrations += Vibration(durationMs, amplitude)
    }

    override fun cancel() {
        cancels++
    }
}
