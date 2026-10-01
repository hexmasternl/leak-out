package nl.hexmaster.leakout.audio

import android.content.res.AssetManager
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Plays the effects in `assets/sfx/` through one [SoundPool]. Every file is loaded once, up front
 * and asynchronously; a play before its file has loaded is skipped. Four streams let quick
 * placements overlap without cutting each other off.
 */
class SoundPoolSoundEffectPlayer(assets: AssetManager, files: List<String>) : SoundEffectPlayer {
    private val pool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val loaded: MutableSet<Int> = ConcurrentHashMap.newKeySet()
    private val soundIds: Map<String, Int>
    private var released = false

    init {
        pool.setOnLoadCompleteListener { _, soundId, status -> if (status == 0) loaded += soundId }
        // MP3 is stored uncompressed in the APK, so each asset has its own file descriptor.
        soundIds = files.mapNotNull { file ->
            runCatching { assets.openFd("${SoundCatalog.ASSET_DIR}/$file").use { pool.load(it, 1) } }
                .onFailure { Log.w(TAG, "Cannot load $file", it) }
                .getOrNull()
                ?.let { file to it }
        }.toMap()
    }

    /** Whether [file] has finished loading and can play. */
    fun isLoaded(file: String): Boolean = soundIds[file]?.let { it in loaded } == true

    override fun play(file: String, volume: Float) {
        if (released) return
        val soundId = soundIds[file] ?: return
        if (soundId !in loaded) {
            Log.d(TAG, "Skipped $file: not loaded yet")
            return
        }
        pool.play(soundId, volume, volume, 1, 0, 1f)
    }

    override fun release() {
        if (released) return
        released = true
        pool.release()
    }

    private companion object {
        const val TAG = "SoundEffects"
        const val MAX_STREAMS = 4
    }
}
