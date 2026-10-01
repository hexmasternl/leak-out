package nl.hexmaster.leakout.audio

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper

/** What the system says about the app's audio focus after it was granted. */
enum class FocusChange { Gain, Loss, TransientLoss, CanDuck }

/** Asks the system for audio focus. The seam between [BackgroundMusic] and [AudioManager]. */
interface AudioFocus {
    /** Requests focus for music; true when granted. Later changes arrive at [onChange] on the main thread. */
    fun request(onChange: (FocusChange) -> Unit): Boolean

    /** Gives the focus back. Safe to call without holding it. */
    fun abandon()
}

/** [AudioFocus] through [AudioFocusRequest] (API 26, the minimum the app supports). */
class AudioManagerAudioFocus(context: Context) : AudioFocus {
    private val audioManager = context.applicationContext.getSystemService(AudioManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private var request: AudioFocusRequest? = null

    override fun request(onChange: (FocusChange) -> Unit): Boolean {
        abandon()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(MediaPlayerMusicEngine.MUSIC_ATTRIBUTES)
            // The app ducks itself, to 20 % of the set volume, so the system reports ducking instead of doing it.
            .setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener({ change -> map(change)?.let(onChange) }, main)
            .build()
        this.request = request
        return audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    override fun abandon() {
        request?.let(audioManager::abandonAudioFocusRequest)
        request = null
    }

    private fun map(change: Int): FocusChange? = when (change) {
        AudioManager.AUDIOFOCUS_GAIN -> FocusChange.Gain
        AudioManager.AUDIOFOCUS_LOSS -> FocusChange.Loss
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> FocusChange.TransientLoss
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> FocusChange.CanDuck
        else -> null
    }
}
