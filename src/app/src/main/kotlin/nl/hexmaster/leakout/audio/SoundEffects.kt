package nl.hexmaster.leakout.audio

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
 * [GameSound]; [apply] keeps the settings current, so a change counts from the next sound on.
 */
class SoundEffects(
    private val catalog: SoundCatalog,
    private val player: SoundEffectPlayer,
    private val random: Random = Random.Default,
) {
    private var enabled = Settings.DEFAULT_SOUND_ENABLED
    private var volume = Settings.DEFAULT_SOUND_VOLUME

    /** Takes over the stored sound settings. */
    fun apply(settings: Settings) {
        enabled = settings.soundEnabled
        volume = Settings.clampVolume(settings.soundVolume)
    }

    fun play(sound: GameSound) = playAt(sound, volume)

    /** Plays a placement sound at [volume], so the player hears the level picked on the slider before it is stored. */
    fun preview(volume: Float) = playAt(GameSound.SectionPlaced, Settings.clampVolume(volume))

    fun release() = player.release()

    private fun playAt(sound: GameSound, volume: Float) {
        if (!enabled || volume <= 0f) return
        val variants = catalog.variantsOf(sound)
        if (variants.isEmpty()) return
        player.play(variants.random(random), volume)
    }
}
