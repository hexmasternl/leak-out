package nl.hexmaster.leakout.audio

import android.content.res.AssetManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.VisibleForTesting

/**
 * Plays tracks from `assets/music/` through one [MediaPlayer] at a time. MP3 is stored
 * uncompressed in the APK, so each track opens from its own asset file descriptor. Use it from the
 * main thread: the player's callbacks arrive on the looper of the thread that created the player.
 */
class MediaPlayerMusicEngine(private val assets: AssetManager) : MusicEngine {
    override var listener: MusicEngine.Listener? = null

    private val main = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var track: Track? = null
    private var prepared = false
    private var wantsPlaying = false
    private var started = false
    private var volume = 1f

    override fun play(track: Track) {
        closePlayer()
        val player = MediaPlayer()
        this.player = player
        this.track = track
        wantsPlaying = true
        try {
            player.setAudioAttributes(MUSIC_ATTRIBUTES)
            assets.openFd("${BundledMusic.ASSET_DIR}/${track.file}").use { player.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            player.setOnPreparedListener { if (it === this.player) onPrepared() }
            player.setOnCompletionListener { if (it === this.player) listener?.onCompleted(track) }
            player.setOnErrorListener { mp, what, extra ->
                if (mp === this.player) fail(track, "error $what/$extra")
                true
            }
            player.prepareAsync()
        } catch (e: Exception) {
            Log.w(TAG, "Cannot open ${track.file}", e)
            // Reported after play() returns, so the listener never runs inside its own call.
            main.post { if (this.player === player) fail(track, "open failed") }
        }
    }

    private fun onPrepared() {
        prepared = true
        player?.setVolume(volume, volume)
        if (wantsPlaying) start()
    }

    private fun start() {
        val player = player ?: return
        val track = track ?: return
        player.start()
        if (!started) {
            started = true
            listener?.onStarted(track)
        }
    }

    private fun fail(track: Track, reason: String) {
        Log.w(TAG, "Skipping ${track.file}: $reason")
        closePlayer()
        listener?.onError(track)
    }

    override fun pause() {
        wantsPlaying = false
        if (prepared) player?.takeIf { it.isPlaying }?.pause()
    }

    override fun resume() {
        wantsPlaying = true
        if (prepared && player?.isPlaying == false) start()
    }

    override fun setVolume(volume: Float) {
        this.volume = volume
        if (prepared) player?.setVolume(volume, volume)
    }

    override fun release() = closePlayer()

    private fun closePlayer() {
        player?.release()
        player = null
        track = null
        prepared = false
        started = false
        wantsPlaying = false
    }

    /** The open track is prepared and playing. */
    @get:VisibleForTesting
    val isPlaying: Boolean get() = prepared && player?.isPlaying == true

    /** The position in the open track, in milliseconds. */
    @get:VisibleForTesting
    val positionMs: Int get() = if (prepared) player?.currentPosition ?: 0 else 0

    /** Jumps to [remainingMs] before the end of the open track, so a test does not wait for a whole song. */
    @VisibleForTesting
    fun seekToEnd(remainingMs: Int) {
        val player = player?.takeIf { prepared } ?: return
        player.seekTo((player.duration - remainingMs).coerceAtLeast(0))
    }

    companion object {
        private const val TAG = "MusicEngine"

        val MUSIC_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
    }
}
