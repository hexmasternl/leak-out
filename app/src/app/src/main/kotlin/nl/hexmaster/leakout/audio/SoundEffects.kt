package nl.hexmaster.leakout.audio

import kotlin.math.abs
import kotlin.random.Random
import nl.hexmaster.leakout.settings.Settings

/** Plays one effect file from `assets/sfx/`. The seam between [SoundEffects] and `SoundPool`. */
interface SoundEffectPlayer {
    /** Plays [file] (a bare name from the catalogue) once at [volume], 0.0-1.0. */
    fun play(file: String, volume: Float)

    /** Frees the loaded sounds. Safe to call more than once. */
    fun release()
}

/**
 * Decides whether and which effect plays: nothing while sound is off, at volume 0 or when a sound
 * has no files, and otherwise a random variant at the stored volume. The game only names the
 * [GameSound]; [apply] keeps the settings current, so a change counts from the next sound on. The
 * looping heartbeat goes through [loopPlayer] with [heartbeat]; without one it stays silent.
 */
class SoundEffects(
    private val catalog: SoundCatalog,
    private val player: SoundEffectPlayer,
    private val random: Random = Random.Default,
    private val loopPlayer: LoopingSoundPlayer? = null,
) {
    private var enabled = Settings.DEFAULT_SOUND_ENABLED
    private var volume = Settings.DEFAULT_SOUND_VOLUME

    /** The heartbeat's factor as last asked for, and the volume the loop plays at; null while stopped. */
    private var heartbeatFactor: Float? = null
    private var heartbeatVolume: Float? = null

    /** Takes over the stored sound settings. A running heartbeat follows them at once. */
    fun apply(settings: Settings) {
        enabled = settings.soundEnabled
        volume = Settings.clampVolume(settings.soundVolume)
        updateHeartbeat(force = true)
    }

    /** Plays a one-shot [sound]; a looping sound is only played through [heartbeat]. */
    fun play(sound: GameSound) = playAt(sound, volume)

    /** Plays a placement sound at [volume], so the player hears the level picked on the slider before it is stored. */
    fun preview(volume: Float) = playAt(GameSound.SectionPlaced, Settings.clampVolume(volume))

    /**
     * Loops the heartbeat at [factor] times the sound volume, or stops it for a null [factor]. It
     * stays silent while sound is off, at volume 0 and without a heartbeat file. A volume change
     * smaller than [VOLUME_STEP] is not passed on, so a factor that moves every frame does not
     * reach the player every frame.
     */
    fun heartbeat(factor: Float?) {
        heartbeatFactor = factor
        updateHeartbeat(force = false)
    }

    fun release() {
        player.release()
        loopPlayer?.release()
        heartbeatVolume = null
    }

    private fun updateHeartbeat(force: Boolean) {
        val loop = loopPlayer ?: return
        val factor = heartbeatFactor
        val file = catalog.variantsOf(GameSound.Heartbeat).firstOrNull()
        val target = if (enabled && file != null && factor != null) (factor.coerceIn(0f, 1f) * volume) else 0f
        val playing = heartbeatVolume
        when {
            target <= 0f || file == null -> if (playing != null) {
                loop.stop()
                heartbeatVolume = null
            }
            playing == null -> {
                loop.start(file, target)
                heartbeatVolume = target
            }
            (force && target != playing) || abs(target - playing) >= VOLUME_STEP - FLOAT_SLACK -> {
                loop.setVolume(target)
                heartbeatVolume = target
            }
        }
    }

    private fun playAt(sound: GameSound, volume: Float) {
        if (!enabled || volume <= 0f || sound.looping) return
        val variants = catalog.variantsOf(sound)
        if (variants.isEmpty()) return
        player.play(variants.random(random), volume)
    }

    companion object {
        /** The smallest heartbeat volume change passed on to the player. */
        const val VOLUME_STEP = 0.01f

        // 0.51f - 0.5f is just under 0.01f in floating point; a whole step must still count.
        private const val FLOAT_SLACK = 1e-6f
    }
}
