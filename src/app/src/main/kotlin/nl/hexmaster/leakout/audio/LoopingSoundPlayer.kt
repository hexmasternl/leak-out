package nl.hexmaster.leakout.audio

import android.content.res.AssetManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.annotation.VisibleForTesting

/**
 * Loops one effect file from `assets/sfx/` with a volume that changes while it plays: the seam
 * between [SoundEffects.heartbeat] and `MediaPlayer`. `SoundPool` cannot do this for the heartbeat,
 * which decodes to more than its per-sample limit.
 */
interface LoopingSoundPlayer {
    /** Starts looping [file] (a bare name from the catalogue) from its beginning at [volume], 0.0-1.0. */
    fun start(file: String, volume: Float)

    fun setVolume(volume: Float)

    /** Stops the loop; the next [start] plays from the beginning again. */
    fun stop()

    /** Frees the player. Safe to call more than once. */
    fun release()
}

/** Plays nothing and remembers every call, for tests and previews, like [RecordingSoundEffectPlayer]. */
class RecordingLoopingSoundPlayer : LoopingSoundPlayer {
    sealed interface Call {
        data class Start(val file: String, val volume: Float) : Call
        data class Volume(val volume: Float) : Call
        data object Stop : Call
    }

    val calls = mutableListOf<Call>()

    /** The volume the loop plays at now, or null while it is stopped. */
    var playingAt: Float? = null
        private set

    var released = false
        private set

    override fun start(file: String, volume: Float) {
        calls += Call.Start(file, volume)
        playingAt = volume
    }

    override fun setVolume(volume: Float) {
        calls += Call.Volume(volume)
        if (playingAt != null) playingAt = volume
    }

    override fun stop() {
        calls += Call.Stop
        playingAt = null
    }

    override fun release() {
        released = true
        playingAt = null
    }
}

/**
 * Loops a file through one [MediaPlayer], prepared on the first [start] and kept prepared while
 * stopped (paused at the beginning), so a warning that comes back starts without a new prepare. It
 * asks for no audio focus: like every effect, the loop never pauses or ducks the music. Use it from
 * the main thread.
 */
class MediaPlayerLoopingSoundPlayer(private val assets: AssetManager) : LoopingSoundPlayer {
    private var player: MediaPlayer? = null
    private var file: String? = null
    private var prepared = false
    private var wantsPlaying = false
    private var volume = 1f
    private var released = false

    override fun start(file: String, volume: Float) {
        if (released) return
        this.volume = volume
        wantsPlaying = true
        if (file != this.file) open(file)
        val player = player?.takeIf { prepared } ?: return
        player.setVolume(volume, volume)
        player.seekTo(0)
        player.start()
    }

    private fun open(file: String) {
        closePlayer()
        val player = MediaPlayer()
        this.player = player
        this.file = file
        try {
            player.setAudioAttributes(EFFECT_ATTRIBUTES)
            assets.openFd("${SoundCatalog.ASSET_DIR}/$file").use { player.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            player.isLooping = true
            player.setOnPreparedListener {
                if (it !== this.player) return@setOnPreparedListener
                prepared = true
                it.setVolume(volume, volume)
                if (wantsPlaying) it.start()
            }
            player.setOnErrorListener { mp, what, extra ->
                if (mp === this.player) {
                    Log.w(TAG, "Stopping $file: error $what/$extra")
                    closePlayer()
                }
                true
            }
            player.prepareAsync()
        } catch (e: Exception) {
            Log.w(TAG, "Cannot open $file", e)
            closePlayer()
        }
    }

    override fun setVolume(volume: Float) {
        this.volume = volume
        if (prepared) player?.setVolume(volume, volume)
    }

    override fun stop() {
        wantsPlaying = false
        val player = player?.takeIf { prepared } ?: return
        if (player.isPlaying) player.pause()
        player.seekTo(0)
    }

    override fun release() {
        released = true
        closePlayer()
    }

    private fun closePlayer() {
        player?.release()
        player = null
        file = null
        prepared = false
        wantsPlaying = false
    }

    /** The loop is prepared and playing. */
    @get:VisibleForTesting
    val isPlaying: Boolean get() = prepared && player?.isPlaying == true

    /** The position in the file, in milliseconds. */
    @get:VisibleForTesting
    val positionMs: Int get() = if (prepared) player?.currentPosition ?: 0 else 0

    /** The length of the file, in milliseconds, once prepared. */
    @get:VisibleForTesting
    val durationMs: Int get() = if (prepared) player?.duration ?: 0 else 0

    /** Jumps to [remainingMs] before the end, so a test can see the loop wrap without waiting for it. */
    @VisibleForTesting
    fun seekToEnd(remainingMs: Int) {
        val player = player?.takeIf { prepared } ?: return
        player.seekTo((player.duration - remainingMs).coerceAtLeast(0))
    }

    private companion object {
        const val TAG = "LoopingSound"

        val EFFECT_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }
}
